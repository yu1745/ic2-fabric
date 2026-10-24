package ic2_120.content.block.nuclear

import ic2_120.compat.world

import ic2_120.content.block.MachineBlock
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.LevelAccessor

/**
 * 反应堆透明外壳基类：压力容器与三种接口共用（铅框 + 强化玻璃，堆芯从外面直接可见）。
 *
 * - 非不透明方块，不遮挡相邻面，不投射环境光遮蔽；
 * - 与其他外壳方块相邻的面不渲染，整圈外壳看起来是一体的透明壳而不是一格格玻璃块；
 * - 六向连接属性（相邻是外壳或反应堆核心即为连接）：方块模型只在"两侧面都未连接"的棱上画铅框，
 *   整个 5×5×3 外壳只剩外轮廓一圈框（multipart，见 tools/model3d/reactor_shell.py）。
 */
abstract class ReactorShellBlock(settings: BlockBehaviour.Properties = shellSettings()) : MachineBlock(settings) {

    companion object {
        /** 六向连接属性：该方向相邻的是外壳或反应堆核心 */
        val CONNECT: Map<Direction, BooleanProperty> = mapOf(
            Direction.NORTH to BlockStateProperties.NORTH, Direction.SOUTH to BlockStateProperties.SOUTH,
            Direction.EAST to BlockStateProperties.EAST, Direction.WEST to BlockStateProperties.WEST,
            Direction.UP to BlockStateProperties.UP, Direction.DOWN to BlockStateProperties.DOWN,
        )

        fun shellSettings(): BlockBehaviour.Properties =
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)
                .sound(SoundType.GLASS).noOcclusion()
                .isSuffocating { _, _, _ -> false }.isViewBlocking { _, _, _ -> false }
    }

    init {
        registerDefaultState(CONNECT.values.fold(defaultBlockState()) { st, prop -> st.setValue(prop, false) })
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        CONNECT.values.forEach { builder.add(it) }
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState? {
        var state = super.getStateForPlacement(ctx) ?: return null
        for ((dir, prop) in CONNECT) {
            state = state.setValue(prop, connects(ctx.level.getBlockState(ctx.clickedPos.relative(dir))))
        }
        return state
    }

    @Deprecated("Deprecated in Java")
    override fun updateShape(
        state: BlockState, direction: Direction, neighborState: BlockState,
        world: LevelAccessor, pos: BlockPos, neighborPos: BlockPos,
    ): BlockState = super.updateShape(state, direction, neighborState, world, pos, neighborPos)
        .let { if (it.block === this) it.setValue(CONNECT.getValue(direction), connects(neighborState)) else it }

    private fun connects(neighbor: BlockState): Boolean =
        neighbor.block is ReactorShellBlock || neighbor.block is NuclearReactorBlock

    @Deprecated("Deprecated in Java")
    override fun skipRendering(state: BlockState, stateFrom: BlockState, direction: Direction): Boolean =
        stateFrom.block is ReactorShellBlock || super.skipRendering(state, stateFrom, direction)

    @Deprecated("Deprecated in Java")
    override fun getShadeBrightness(state: BlockState, world: BlockGetter, pos: BlockPos): Float = 1.0f

    override fun propagatesSkylightDown(state: BlockState, world: BlockGetter, pos: BlockPos): Boolean = true
}
