package ic2_120.content.block

import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

/**
 * 方块实体领地保护诊断接口。
 *
 * 实现此接口的 BlockEntity 可向 Jade HUD 报告当前被领地保护（FTB Chunks）阻止的目标位置，
 * 帮助玩家理解"为什么机器不工作"。
 *
 * 仅在服务端 Jade 数据收集阶段调用；实现应保持轻量（不做 BFS/遍历大范围）。
 * 返回空列表表示无阻断。
 */
interface IClaimSensitive {
    fun claimBlockedTargets(world: World, pos: BlockPos, state: BlockState): List<BlockPos>
}
