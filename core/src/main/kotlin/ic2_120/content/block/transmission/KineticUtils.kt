package ic2_120.content.block.transmission

import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

/**
 * 从相邻方块的 [IKineticMachinePort] 直接取动能并注入 [receiver]。
 * 不经过传动轴/伞齿轮，也不进入动能网络，仅与六面相邻的动能端口交换 KU。
 *
 * @param excludeDirections 不向这些方向上的相邻方块取能。
 */
fun pullKuFromNeighbors(
    world: World,
    pos: BlockPos,
    receiver: IKineticMachinePort,
    excludeDirections: Set<Direction> = emptySet()
): Int {
    var totalInserted = 0
    for (direction in Direction.entries) {
        if (direction in excludeDirections) continue
        val sideFromReceiver = direction
        if (!receiver.canInputKuFrom(sideFromReceiver)) continue

        val neighborPos = pos.offset(direction)
        val source = world.getBlockEntity(neighborPos) as? IKineticMachinePort ?: continue

        val sideFromSource = direction.opposite
        if (!source.canOutputKuTo(sideFromSource)) continue

        val maxInsert = receiver.getMaxInsertableKu(sideFromReceiver)
        if (maxInsert <= 0) continue

        val maxExtract = source.getMaxExtractableKu(sideFromSource)
        val transferAmount = minOf(maxInsert, maxExtract)
        if (transferAmount <= 0) continue

        // 先模拟提取
        val simulatedExtract = source.extractKu(sideFromSource, transferAmount, true)
        if (simulatedExtract <= 0) continue

        // 先模拟插入
        val simulatedInsert = receiver.insertKu(sideFromReceiver, simulatedExtract, true)
        if (simulatedInsert <= 0) continue

        // 模拟都通过，执行真实提取
        val extracted = source.extractKu(sideFromSource, simulatedInsert, false)
        if (extracted <= 0) continue

        // 执行真实插入
        val inserted = receiver.insertKu(sideFromReceiver, extracted, false)
        totalInserted += inserted
    }
    return totalInserted
}
