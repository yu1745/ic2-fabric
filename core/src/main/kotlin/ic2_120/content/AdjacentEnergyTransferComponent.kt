package ic2_120.content

import ic2_120.content.block.IGenerator
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.cables.CableBlockEntity
import ic2_120.content.block.machines.TransformerBlockEntity
import ic2_120.integration.ftbchunks.ClaimProtection
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.inventory.Inventory
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import team.reborn.energy.api.EnergyStorage
import kotlin.math.pow

/**
 * Unified adjacent energy transfer for machine-to-machine contact.
 *
 * IC2 machines use pull-only semantics with overvoltage checks. Non-IC2 Energy API
 * neighbors are treated as compatibility endpoints and are limited by sided storage
 * capabilities without IC2 overvoltage semantics.
 */
class AdjacentEnergyTransferComponent(
    private val owner: BlockEntity,
    private val energy: TickLimitedSidedEnergyContainer
) {
    companion object {
        private const val NEIGHBOR_VERIFY_INTERVAL = 20
    }

    private val verifyPhase: Int = kotlin.random.Random.nextInt(NEIGHBOR_VERIFY_INTERVAL)

    private data class NeighborStorageCache(
        var state: BlockState? = null,
        var blockEntity: BlockEntity? = null,
        var storage: EnergyStorage? = null
    )

    private val neighborStorageCache = arrayOfNulls<NeighborStorageCache>(Direction.entries.size)

    fun tick(): Long {
        val world = owner.world ?: return 0L
        if (world.isClient) return 0L
        // 停机门控：机器拒绝外部供能时（如红石停机）不再从邻居拉电，避免停工期间空耗电（#30）。
        if (!energy.canAcceptExternalEnergy()) return 0L

        val selfMachine = owner as? ITieredMachine ?: return 0L
        var total = 0L
        val nowTick = world.time
        val dueVerify = Math.floorMod(nowTick + verifyPhase, NEIGHBOR_VERIFY_INTERVAL.toLong()) == 0L

        for (side in Direction.entries) {
            val neighborPos = owner.pos.offset(side)
            val cache = neighborStorageCache[side.ordinal] ?: NeighborStorageCache().also {
                neighborStorageCache[side.ordinal] = it
            }

            val cacheValid = cache.state != null && !dueVerify &&
                (cache.blockEntity == null || !cache.blockEntity!!.isRemoved)
            val neighborBe = if (cacheValid) {
                cache.blockEntity
            } else {
                val neighborState = world.getBlockState(neighborPos)
                val be = world.getBlockEntity(neighborPos)
                cache.state = neighborState
                cache.blockEntity = be
                cache.storage = EnergyStorage.SIDED.find(world, neighborPos, side.opposite)
                be
            }
            val neighborStorage = cache.storage ?: continue

            if (neighborBe is CableBlockEntity) continue
            // Most faces have no energy endpoint. Resolve our sided wrapper only
            // after the cached neighbor lookup succeeds.
            val selfStorage = energy.getSideStorage(side)

            total += if (neighborBe is ITieredMachine) {
                transferWithIc2Rules(world, selfMachine, selfStorage, neighborBe, neighborStorage, side)
            } else {
                transferWithExternalRules(selfStorage, neighborStorage, side)
            }
        }

        return total
    }

    private fun transferWithIc2Rules(
        world: World,
        consumerMachine: ITieredMachine,
        consumerStorage: EnergyStorage,
        providerMachine: ITieredMachine,
        providerStorage: EnergyStorage,
        consumerSide: Direction
    ): Long {
        if (!providerStorage.supportsExtraction()) return 0L

        // The insertion probe is both the sided-capability check and the exact
        // per-tick budget. Calling supportsInsertion() first would run the same
        // TickLimitedSidedEnergyContainer budget path twice on every IC2 face.
        val receivable = minOf(
            energy.getMaxInsert(consumerSide),
            (energy.capacity - energy.amount).coerceAtLeast(0L)
        )
        if (receivable <= 0L) return 0L

        val providerSide = consumerSide.opposite
        val providerVoltage = providerMachine.effectiveVoltageTierForSide(providerSide)
        val consumerVoltage = consumerMachine.effectiveVoltageTierForSide(consumerSide)
        if (wouldOvervoltage(providerVoltage, consumerVoltage, consumerMachine)) {
            explodeConsumer(world, providerVoltage)
            return 0L
        }

        return move(providerStorage, consumerStorage, receivable)
    }

    private fun transferWithExternalRules(
        selfStorage: EnergyStorage,
        neighborStorage: EnergyStorage,
        selfSide: Direction
    ): Long {
        // The self endpoint is always our TickLimited container, so its exact
        // budget and capacity are available without a dry-run transaction or a
        // duplicated supportsInsertion/getMaxInsert call.
        val selfReceivable = minOf(
            energy.getMaxInsert(selfSide),
            (energy.capacity - energy.amount).coerceAtLeast(0L)
        )
        if (selfReceivable > 0L && neighborStorage.supportsExtraction()) {
            return move(neighborStorage, selfStorage, selfReceivable)
        }

        val selfAvailable = minOf(energy.getMaxExtract(selfSide), energy.amount)
        if (selfAvailable > 0L) {
            val neighborReceivable = simulateInsertion(neighborStorage, selfAvailable)
            if (neighborReceivable > 0L) {
                return move(selfStorage, neighborStorage, neighborReceivable)
            }
        }
        return 0L
    }

    private fun wouldOvervoltage(
        providerVoltage: Int,
        consumerVoltage: Int,
        consumer: ITieredMachine
    ): Boolean {
        if (!ic2_120.config.Ic2Config.current.general.enableOvervoltageExplosion) return false
        if (consumer is IGenerator) return false
        if (consumer is TransformerBlockEntity) return false
        return providerVoltage > consumerVoltage
    }

    private fun move(provider: EnergyStorage, consumer: EnergyStorage, receivable: Long): Long {
        var moved = 0L
        Transaction.openOuter().use { tx ->
            val extracted = provider.extract(receivable, tx)
            if (extracted <= 0L) return@use

            val inserted = consumer.insert(extracted, tx)
            if (inserted == extracted) {
                moved = inserted
                tx.commit()
            }
        }
        return moved
    }

    private fun simulateInsertion(storage: EnergyStorage, maxAmount: Long): Long {
        var accepted = 0L
        Transaction.openOuter().use { tx ->
            accepted = storage.insert(maxAmount, tx)
        }
        return accepted
    }

    private fun explodeConsumer(world: World, providerVoltage: Int) {
        if (world.isClient) return
        val pos = owner.pos
        val consumer = owner as? ITieredMachine ?: return
        if (consumer is IGenerator) return
        if (consumer is TransformerBlockEntity) return

        val power = explosionPowerForOutputLevel(providerVoltage)
        if (ClaimProtection.isProtected(world, pos, null as java.util.UUID?, ClaimProtection.EDIT_BLOCK) ||
            !ClaimProtection.explosionCubeAllowed(world, pos, power, null)) return
        if (owner is Inventory) (owner as Inventory).clear()
        world.breakBlock(pos, false)

        if (world is ServerWorld) {
            val x = pos.x + 0.5
            val y = pos.y + 0.5
            val z = pos.z + 0.5
            world.spawnParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 12, 0.2, 0.2, 0.2, 0.02)
            world.spawnParticles(ParticleTypes.FLAME, x, y, z, 6, 0.15, 0.15, 0.15, 0.01)
        }

        world.createExplosion(null, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, power, false, World.ExplosionSourceType.BLOCK)
    }

    private fun explosionPowerForOutputLevel(level: Int): Float {
        if (level <= 0) return 0.25f
        return (2f * 2.0.pow(level - 4)).toFloat()
    }
}
