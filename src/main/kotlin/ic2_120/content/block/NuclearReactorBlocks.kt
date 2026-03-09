package ic2_120.content.block

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.item.ItemPlacementContext
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.state.property.Properties

/**
 * 核反应仓。暂时仅作为合成材料，无逻辑。
 */
@ModBlock(name = "reactor_chamber", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "reactor")
class ReactorChamberBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)) : Block(settings)

/**
 * 核反应堆。暂时仅作为合成材料，无逻辑。
 * 支持 facing 与 active 状态以正确显示模型。
 */
@ModBlock(name = "nuclear_reactor", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "reactor")
class NuclearReactorBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)) : Block(settings) {

    init {
        defaultState = stateManager.defaultState
            .with(Properties.HORIZONTAL_FACING, net.minecraft.util.math.Direction.NORTH)
            .with(ACTIVE, false)
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(Properties.HORIZONTAL_FACING, ACTIVE)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? =
        defaultState.with(Properties.HORIZONTAL_FACING, ctx.horizontalPlayerFacing.opposite)

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")
    }
}
