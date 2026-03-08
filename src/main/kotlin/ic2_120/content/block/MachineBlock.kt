package ic2_120.content.block

import net.minecraft.block.AbstractBlock
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemPlacementContext
import net.minecraft.state.StateManager
import net.minecraft.state.property.Properties
import net.minecraft.util.ItemScatterer
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

/**
 * 简单的机器方块基类。
 * 可被子类扩展以支持 BlockEntity。
 * 放置时根据玩家朝向设置 HORIZONTAL_FACING（玩家面向的方向的相反方向为方块正面）。
 *
 * 所有机器都有能量等级，用于决定能接受什么等级的电池供电。
 *
 * 默认硬度为铁方块硬度（5.0f, 6.0f），子类可通过传入自定义 settings 覆盖。
 */
abstract class MachineBlock(settings: AbstractBlock.Settings = defaultMachineSettings()) : BlockWithEntity(settings), ITieredMachine {

    companion object {
        /** 机器方块的默认设置：铁方块硬度（5.0f, 6.0f） */
        fun defaultMachineSettings(): AbstractBlock.Settings =
            AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)
    }

    /**
     * 机器的能量等级（1-4）
     *
     * 子类必须覆写此属性。
     * 决定了：
     * - 机器可以接受什么等级的电池供电
     * - 机器可以给什么等级的设备充电
     */
    abstract override val tier: Int

    init {
        defaultState = stateManager.defaultState.with(Properties.HORIZONTAL_FACING, Direction.NORTH)
    }

    override fun appendProperties(builder: StateManager.Builder<net.minecraft.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(Properties.HORIZONTAL_FACING)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? =
        defaultState.with(Properties.HORIZONTAL_FACING, ctx.horizontalPlayerFacing.opposite)

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? = null

    /**
     * 方块被破坏时，将 BlockEntity 内 Inventory 的物品散落掉落，而非凭空消失。
     * 仅当方块被替换（如挖掉）且未被活塞推动时生效。
     */
    override fun onStateReplaced(state: BlockState, world: World, pos: BlockPos, newState: BlockState, moved: Boolean) {
        if (!world.isClient && !state.isOf(newState.block) && !moved) {
            val be = world.getBlockEntity(pos)
            if (be is Inventory) {
                ItemScatterer.spawn(world, pos, be)
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved)
    }

    /**
     * BlockWithEntity 默认可能不会使用 JSON 模型渲染，显式指定为 MODEL
     * 以确保像电炉这类机器方块按 blockstate/model 显示材质。
     */
    override fun getRenderType(state: BlockState): BlockRenderType = BlockRenderType.MODEL
}
