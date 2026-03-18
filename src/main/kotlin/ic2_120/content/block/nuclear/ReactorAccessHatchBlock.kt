package ic2_120.content.block.nuclear

import ic2_120.content.block.MachineBlock
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import net.minecraft.block.AbstractBlock
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

/**
 * 反应堆访问接口。与核反应堆或反应仓相邻时，右键可打开反应堆 UI。
 */
@ModBlock(name = "reactor_access_hatch", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "reactor")
class ReactorAccessHatchBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)) : MachineBlock(settings) {

    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult
    ): ActionResult {
        if (world.isClient) return ActionResult.SUCCESS
        // 查找反应堆，仅当访问接口在其 5×5×5 结构内部时才打开 UI（不根据距离，根据结构归属）
        for (dx in -2..2) {
            for (dy in -2..2) {
                for (dz in -2..2) {
                    val reactorPos = pos.add(dx, dy, dz)
                    val be = world.getBlockEntity(reactorPos)
                    if (be is NuclearReactorBlockEntity && be.isPositionInStructure(pos)) {
                        player.openHandledScreen(be)
                        return ActionResult.SUCCESS
                    }
                }
            }
        }
        return ActionResult.PASS
    }
}
