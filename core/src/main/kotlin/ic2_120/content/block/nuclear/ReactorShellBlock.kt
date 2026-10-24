package ic2_120.content.block.nuclear

import ic2_120.content.block.MachineBlock
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.item.ItemPlacementContext
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.state.property.Properties
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.BlockView
import net.minecraft.world.WorldAccess

/**
 * 反应堆透明外壳基类：压力容器与三种接口共用（铅框 + 强化玻璃，堆芯从外面直接可见）。
 *
 * - 非不透明方块，不遮挡相邻面，不投射环境光遮蔽；
 * - 与其他外壳方块相邻的面不渲染，整圈外壳看起来是一体的透明壳而不是一格格玻璃块；
 * - 六向连接属性（相邻是外壳或反应堆核心即为连接）：方块模型只在"两侧面都未连接"的棱上画铅框，
 *   整个 5×5×3 外壳只剩外轮廓一圈框（multipart，见 tools/model3d/reactor_shell.py）。
 */
abstract class ReactorShellBlock(settings: AbstractBlock.Settings = shellSettings()) : MachineBlock(settings) {

    companion object {
        /** 六向连接属性：该方向相邻的是外壳或反应堆核心 */
        val CONNECT: Map<Direction, BooleanProperty> = mapOf(
            Direction.NORTH to Properties.NORTH, Direction.SOUTH to Properties.SOUTH,
            Direction.EAST to Properties.EAST, Direction.WEST to Properties.WEST,
            Direction.UP to Properties.UP, Direction.DOWN to Properties.DOWN,
        )

        fun shellSettings(): AbstractBlock.Settings =
            AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)
                .sounds(BlockSoundGroup.GLASS).nonOpaque()
                .solidBlock { _, _, _ -> false }.suffocates { _, _, _ -> false }.blockVision { _, _, _ -> false }
    }

    init {
        defaultState = CONNECT.values.fold(defaultState) { st, prop -> st.with(prop, false) }
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        super.appendProperties(builder)
        CONNECT.values.forEach { builder.add(it) }
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? {
        var state = super.getPlacementState(ctx) ?: return null
        for ((dir, prop) in CONNECT) {
            state = state.with(prop, connects(ctx.world.getBlockState(ctx.blockPos.offset(dir))))
        }
        return state
    }

    @Deprecated("Deprecated in Java")
    override fun getStateForNeighborUpdate(
        state: BlockState, direction: Direction, neighborState: BlockState,
        world: WorldAccess, pos: BlockPos, neighborPos: BlockPos,
    ): BlockState = super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos)
        .let { if (it.block === this) it.with(CONNECT.getValue(direction), connects(neighborState)) else it }

    private fun connects(neighbor: BlockState): Boolean =
        neighbor.block is ReactorShellBlock || neighbor.block is NuclearReactorBlock

    @Deprecated("Deprecated in Java")
    override fun isSideInvisible(state: BlockState, stateFrom: BlockState, direction: Direction): Boolean =
        stateFrom.block is ReactorShellBlock || super.isSideInvisible(state, stateFrom, direction)

    @Deprecated("Deprecated in Java")
    override fun getAmbientOcclusionLightLevel(state: BlockState, world: BlockView, pos: BlockPos): Float = 1.0f

    override fun isTransparent(state: BlockState, world: BlockView, pos: BlockPos): Boolean = true
}
