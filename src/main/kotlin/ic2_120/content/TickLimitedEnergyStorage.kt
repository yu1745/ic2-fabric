package ic2_120.content

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import team.reborn.energy.api.EnergyStorage
import team.reborn.energy.api.base.SimpleEnergyStorage

/**
 * 从相邻方块的 [EnergyStorage] 直接取电并注入 [receiver]。
 * 不经过导线，也不进入电网，仅与六面相邻且暴露 Energy API 的方块交换能量。
 */
fun pullEnergyFromNeighbors(
    world: World,
    pos: BlockPos,
    receiver: EnergyStorage,
    maxPullPerNeighbor: Long = 32L
): Long {
    if (!receiver.supportsInsertion()) return 0L
    var totalInserted = 0L
    for (direction in Direction.values()) {
        val neighborPos = pos.offset(direction)
        val source = EnergyStorage.SIDED.find(world, neighborPos, direction.opposite)
            ?: EnergyStorage.SIDED.find(world, neighborPos, null)
            ?: continue
        if (!source.supportsExtraction()) continue

        val receivable = simulateInsertion(receiver, maxPullPerNeighbor)
        if (receivable <= 0L) continue

        Transaction.openOuter().use { tx ->
            val extracted = source.extract(receivable, tx)
            if (extracted > 0L) {
                val inserted = receiver.insert(extracted, tx)
                if (inserted == extracted) {
                    tx.commit()
                    totalInserted += inserted
                }
            }
        }
    }
    return totalInserted
}

private fun simulateInsertion(receiver: EnergyStorage, maxAmount: Long): Long {
    var accepted = 0L
    Transaction.openOuter().use { tx ->
        accepted = receiver.insert(maxAmount, tx)
    }
    return accepted
}

/**
 * 将 maxInsert 语义从“单次调用上限”提升为“每 tick 总输入上限”的可复用基类。
 *
 * - 真实存储仍由 [SimpleEnergyStorage] 处理；
 * - 通过 [currentTickProvider] 识别 tick 边界并累计本 tick 已输入量；
 * - 使用额外快照参与事务，确保模拟插入/回滚不会污染本 tick 计数。
 */
open class TickLimitedEnergyStorage(
    capacity: Long,
    private val maxInsertPerTick: Long,
    maxExtract: Long,
    private val currentTickProvider: () -> Long? = { null }
) : SimpleEnergyStorage(capacity, maxInsertPerTick, maxExtract) {

    private var insertTrackedTick: Long = Long.MIN_VALUE
    private var insertedThisTick: Long = 0L

    private val tickBudgetSnapshots = object : SnapshotParticipant<Long>() {
        override fun createSnapshot(): Long = insertedThisTick
        override fun readSnapshot(snapshot: Long) {
            insertedThisTick = snapshot
        }
    }

    override fun insert(maxAmount: Long, transaction: TransactionContext): Long {
        val currentTick = currentTickProvider()
        if (currentTick == null) {
            // 无可用世界时间时（例如客户端仅用于 GUI 同步），退化为单次调用限流。
            return super.insert(minOf(maxAmount, maxInsertPerTick), transaction)
        }
        if (insertTrackedTick != currentTick) {
            insertTrackedTick = currentTick
            insertedThisTick = 0L
        }
        val remainingThisTick = (maxInsertPerTick - insertedThisTick).coerceAtLeast(0L)
        if (remainingThisTick <= 0L) return 0L

        val inserted = super.insert(minOf(maxAmount, remainingThisTick), transaction)
        if (inserted > 0L) {
            tickBudgetSnapshots.updateSnapshots(transaction)
            insertedThisTick += inserted
        }
        return inserted
    }
}
