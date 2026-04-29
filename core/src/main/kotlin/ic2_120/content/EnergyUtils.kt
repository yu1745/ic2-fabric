package ic2_120.content

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import team.reborn.energy.api.EnergyStorage

/**
 * 从相邻方块的 [EnergyStorage] 直接取电并注入 [receiver]。
 * 不经过导线，也不进入电网，仅与六面相邻且暴露 Energy API 的方块交换能量。
 *
 * @param excludeDirections 不向这些方向上的相邻方块取电（例如储电盒正面为输出面时排除正面）。
 */
fun pullEnergyFromNeighbors(
    world: World,
    pos: BlockPos,
    receiver: TickLimitedSidedEnergyContainer,
    excludeDirections: Set<Direction> = emptySet()
): Long {
    // 获取 null 方向的存储（通用存储）
    val receiverStorage = receiver.getSideStorage(null)

    if (!receiverStorage.supportsInsertion()) return 0L
    var totalInserted = 0L
    for (direction in Direction.values()) {
        if (direction in excludeDirections) continue
        val neighborPos = pos.offset(direction)
        val source = EnergyStorage.SIDED.find(world, neighborPos, direction.opposite)
            ?: EnergyStorage.SIDED.find(world, neighborPos, null)
            ?: continue
        if (!source.supportsExtraction()) continue

        // 使用 receiver 当前 tick 的输入上限
        val maxPull = receiver.getMaxInsert(null)
        val receivable = simulateInsertion(receiverStorage, maxPull)
        if (receivable <= 0L) continue

        Transaction.openOuter().use { tx ->
            val extracted = source.extract(receivable, tx)
            if (extracted > 0L) {
                val inserted = receiverStorage.insert(extracted, tx)
                if (inserted == extracted) {
                    tx.commit()
                    totalInserted += inserted
                }
            }
        }
    }
    return totalInserted
}

fun simulateInsertion(receiver: EnergyStorage, maxAmount: Long): Long {
    var accepted = 0L
    Transaction.openOuter().use { tx ->
        accepted = receiver.insert(maxAmount, tx)
    }
    return accepted
}
