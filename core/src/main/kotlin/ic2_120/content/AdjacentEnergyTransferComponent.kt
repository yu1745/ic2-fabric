package ic2_120.content

import ic2_120.content.block.IGenerator
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.cables.CableBlockEntity
import ic2_120.content.block.machines.TransformerBlockEntity
import ic2_120.integration.ftbchunks.ClaimProtection
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.BlockState
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
        /**
         * 邻居缓存强制重查间隔（tick）。
         *
         * Fabric yarn 的 BlockEntity 没有 neighborUpdate 回调，无法在方块替换时
         * 精确失效缓存。用固定间隔全方向重查作为兜底：
         * - 有 BE 的邻居被替换（含区块卸载/重载）→ 旧 BE isRemoved() 实时兜底，无需等间隔；
         * - 无 BE 的邻居被替换（如石头→电池盒）→ 最多延迟一个间隔感知（默认 20 tick）。
         */
        private const val NEIGHBOR_VERIFY_INTERVAL = 20
    }

    /**
     * 每台组件构造时随机生成的重查相位（0..INTERVAL-1）。
     *
     * 强制重查按 `floorMod(worldTime + phase, INTERVAL) == 0` 触发，各机器相位不同，
     * 把全服务器的 20 tick 重查分散到不同 tick——避免所有机器在同一 tick 同时
     * 重查（否则会形成周期性的 CPU 采样尖峰）。
     */
    private val verifyPhase: Int = kotlin.random.Random.nextInt(NEIGHBOR_VERIFY_INTERVAL)

    /**
     * Energy API capability lookup is relatively expensive and the adjacent block
     * usually does not change between ticks. Keep the lookup result tied to the
     * current neighbor BlockEntity; a replaced neighbor naturally invalidates it.
     */
    private data class NeighborStorageCache(
        var state: BlockState? = null,
        var blockEntity: BlockEntity? = null,
        var storage: EnergyStorage? = null
    )

    private val neighborStorageCache = arrayOfNulls<NeighborStorageCache>(Direction.values().size)

    fun tick(): Long {
        val world = owner.world ?: return 0L
        if (world.isClient) return 0L

        val selfMachine = owner as? ITieredMachine ?: return 0L
        var total = 0L
        val nowTick = world.time
        // 强制重查相位判定与方向无关，提出循环外（每 tick 每机器 6 方向各算一次 floorMod 是纯浪费）
        val dueVerify = Math.floorMod(nowTick + verifyPhase, NEIGHBOR_VERIFY_INTERVAL.toLong()) == 0L

        for (side in Direction.values()) {
            val selfStorage = energy.getSideStorage(side)
            val neighborPos = owner.pos.offset(side)
            val cache = neighborStorageCache[side.ordinal] ?: NeighborStorageCache().also {
                neighborStorageCache[side.ordinal] = it
            }

            // 缓存有效判定：
            //  1. 已初始化（state != null）；
            //  2. 未到强制重查相位 tick（防“无 BE 邻居被替换”漏检，相位随机分散）；
            //  3. 非空 BE 缓存未 removed（isRemoved 实时兜底，捕捉方块替换/区块重载的
            //     BE 实例替换——被移除的旧实例标记为 removed，无需等重查间隔）。
            val cacheValid = cache.state != null && !dueVerify &&
                (cache.blockEntity == null || !cache.blockEntity!!.isRemoved)

            val neighborBe: BlockEntity? = if (cacheValid) {
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
        // provider 无能量时直接短路，避免每次探测都开事务（每机器每 tick 6 方向）
        if (provider.amount <= 0L) return 0L

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
