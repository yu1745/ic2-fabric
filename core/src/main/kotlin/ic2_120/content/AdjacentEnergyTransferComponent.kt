package ic2_120.content

import ic2_120.content.block.IGenerator
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.cables.CableBlockEntity
import ic2_120.content.block.machines.TransformerBlockEntity
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
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

    fun tick(): Long {
        val world = owner.world ?: return 0L
        if (world.isClient) return 0L

        val selfMachine = owner as? ITieredMachine ?: return 0L
        var total = 0L

        for (side in Direction.values()) {
            val selfStorage = energy.getSideStorage(side)
            val neighborPos = owner.pos.offset(side)
            val neighborBe = world.getBlockEntity(neighborPos)
            val neighborStorage = EnergyStorage.SIDED.find(world, neighborPos, side.opposite) ?: continue

            if (neighborBe is CableBlockEntity) continue

            total += if (neighborBe is ITieredMachine) {
                transferWithIc2Rules(world, selfMachine, selfStorage, neighborBe, neighborStorage, side)
            } else {
                transferWithExternalRules(selfStorage, neighborStorage)
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
        if (!consumerStorage.supportsInsertion() || !providerStorage.supportsExtraction()) return 0L

        val providerSide = consumerSide.opposite
        val providerVoltage = providerMachine.effectiveVoltageTierForSide(providerSide)
        val consumerVoltage = consumerMachine.effectiveVoltageTierForSide(consumerSide)
        if (wouldOvervoltage(providerVoltage, consumerVoltage, consumerMachine)) {
            explodeConsumer(world, providerVoltage)
            return 0L
        }

        return move(providerStorage, consumerStorage)
    }

    private fun transferWithExternalRules(
        selfStorage: EnergyStorage,
        neighborStorage: EnergyStorage
    ): Long {
        return when {
            selfStorage.supportsInsertion() && neighborStorage.supportsExtraction() ->
                move(neighborStorage, selfStorage)
            selfStorage.supportsExtraction() && neighborStorage.supportsInsertion() ->
                move(selfStorage, neighborStorage)
            else -> 0L
        }
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

    private fun move(provider: EnergyStorage, consumer: EnergyStorage): Long {
        val receivable = simulateInsertion(consumer, Long.MAX_VALUE)
        if (receivable <= 0L) return 0L

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

        if (owner is Inventory) (owner as Inventory).clear()
        world.breakBlock(pos, false)

        if (world is ServerWorld) {
            val x = pos.x + 0.5
            val y = pos.y + 0.5
            val z = pos.z + 0.5
            world.spawnParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 12, 0.2, 0.2, 0.2, 0.02)
            world.spawnParticles(ParticleTypes.FLAME, x, y, z, 6, 0.15, 0.15, 0.15, 0.01)
        }

        val power = explosionPowerForOutputLevel(providerVoltage)
        world.createExplosion(null, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, power, false, World.ExplosionSourceType.BLOCK)
    }

    private fun explosionPowerForOutputLevel(level: Int): Float {
        if (level <= 0) return 0.25f
        return (2f * 2.0.pow(level - 4)).toFloat()
    }
}
