package ic2_120.content.block.cables

import ic2_120.compat.isClient
import ic2_120.compat.world

import ic2_120.content.block.cables.CableBlockEntity
import ic2_120.content.block.energy.EnergyNetworkManager
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SimpleWaterloggedBlock
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import team.reborn.energy.api.EnergyStorage

/**
 * 导线基类。六方向连接状态 + 能量传输。
 * - 导线与任意 [BaseCableBlock] 导线可相互连接。
 * - 导线与实现了 Energy API（[EnergyStorage.SIDED]）的方块可连接。
 * - 每根导线拥有 [CableBlockEntity]，在 tick 中完成能量推送。
 *
 * 子类通过覆写 [getTransferRate] 和 [getEnergyLoss] 设定导线参数，
 * 也可通过 [canConnectToBlock] 扩展或限制可连接的方块类型。
 */
abstract class BaseCableBlock(settings: BlockBehaviour.Properties = defaultSettings()) :
    BaseEntityBlock(settings),
    SimpleWaterloggedBlock {

    init {
        registerDefaultState(stateDefinition.any())
            .setValue(BlockStateProperties.WATERLOGGED, false)
            .setValue(NORTH, false)
            .setValue(SOUTH, false)
            .setValue(EAST, false)
            .setValue(WEST, false)
            .setValue(UP, false)
            .setValue(DOWN, false)
    }

    // ── BlockState 属性 ─────────────────────────────────────────

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(BlockStateProperties.WATERLOGGED)
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN)
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState {
        val world = ctx.level
        val pos = ctx.pos
        val fluidState = world.getFluidState(pos)
        return defaultBlockState()
            .setValue(BlockStateProperties.WATERLOGGED, fluidState.getType() == Fluids.WATER)
            .setValue(NORTH, canConnect(world, pos, Direction.NORTH))
            .setValue(SOUTH, canConnect(world, pos, Direction.SOUTH))
            .setValue(EAST, canConnect(world, pos, Direction.EAST))
            .setValue(WEST, canConnect(world, pos, Direction.WEST))
            .setValue(UP, canConnect(world, pos, Direction.UP))
            .setValue(DOWN, canConnect(world, pos, Direction.DOWN))
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun updateShape(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        world: LevelAccessor,
        pos: BlockPos,
        neighborPos: BlockPos
    ): BlockState {
        // 与原版含水方块一致：含水时调度水 tick，不在这里反推 WATERLOGGED。
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world))
        }
        val property = propertyFor(direction)
        val previousConnected = state.getValue(property)
        val connected = canConnect(world, pos, direction)
        if (connected != previousConnected && world is Level && !world.isClient) {
            EnergyNetworkManager.invalidateConnectionCachesAt(world, pos)
        }
        return state.setValue(property, connected)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getFluidState(state: BlockState): FluidState =
        if (state.getValue(BlockStateProperties.WATERLOGGED)) Fluids.WATER.getSource(false) else Fluids.EMPTY.defaultFluidState()

    // 流动水通过 LiquidBlockContainer.tryFillWithFluid 含水；需显式实现（与栅栏等原版方块一致）。
    override fun canFillWithFluid(world: BlockGetter, pos: BlockPos, state: BlockState, fluid: Fluid): Boolean =
        !state.getValue(BlockStateProperties.WATERLOGGED) && fluid == Fluids.WATER

    override fun tryFillWithFluid(
        world: LevelAccessor,
        pos: BlockPos,
        state: BlockState,
        fluidState: FluidState
    ): Boolean {
        if (!canFillWithFluid(world, pos, state, fluidState.getType())) return false
        if (!state.getValue(BlockStateProperties.WATERLOGGED)) {
            world.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, true), Block.UPDATE_ALL)
            world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world))
        }
        return true
    }

    override fun tryDrainFluid(world: LevelAccessor, pos: BlockPos, state: BlockState): ItemStack =
        ItemStack.EMPTY

    protected fun canConnect(world: LevelAccessor, pos: BlockPos, direction: Direction): Boolean {
        val neighborPos = pos.offset(direction)
        val neighborBlock = world.getBlockState(neighborPos).block
        if (neighborBlock is BaseCableBlock) return true
        if (world is Level) {
            val storage = EnergyStorage.SIDED.find(world, neighborPos, direction.opposite)
            if (storage != null) return true
        }
        return canConnectToBlock(neighborBlock)
    }

    /**
     * 是否可与该类型的相邻方块连接（非导线、非 Energy API 方块时使用）。
     * 默认不与其它方块连接；子类可覆盖以支持更多类型。
     */
    protected open fun canConnectToBlock(neighbor: Block): Boolean = false

    // ── 生命周期 ────────────────────────────────────────────────

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onRemove(state: BlockState, world: Level, pos: BlockPos, newState: BlockState, moved: Boolean) {
        if (!state.`is`(newState.block)) {
            EnergyNetworkManager.invalidateAt(world, pos)
        }
        super.onRemove(state, world, pos, newState, moved)
    }

    // ── BlockEntity / Ticker ────────────────────────────────────

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        CableBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        // Energy networks are ticked once per world by Ic2_120.  Keeping a
        // ticker here makes every cable pay a BE-tick cost even though the
        // network itself already deduplicates work by game time.
        null

    // ── 导线能量参数（子类覆写） ────────────────────────────────

    /** 每 tick 最大传输量（EU/t */
    abstract fun getTransferRate(): Long

    /** 每格导线的能量损耗（milliEU）。电网总损耗 = sum(各导线损耗) / 1000 EU。 */
    abstract fun getEnergyLoss(): Long

    /** 导线是否绝缘。用 [IInsulatedCable] 接口判断。 */
    open fun isInsulated(): Boolean = this is IInsulatedCable

    /**
     * 绝缘等级（0–5）。0 表示未绝缘；2–5 表示可安全承受的电网输出等级上限。
     * 当电网 outputLevel > insulationLevel 时会漏电。
     */
    open val insulationLevel: Int get() = if (this is IInsulatedCable) this.insulationLevel else 0

    // ── 碰撞/轮廓形状 ──────────────────────────────────────────

    private val shapeCache: Map<BlockState, VoxelShape> by lazy { buildShapeCache() }

    private fun buildShapeCache(): Map<BlockState, VoxelShape> {
        val min = getCableMin()
        val max = getCableMax()
        val center = Shapes.box(min, min, min, max, max, max)
        val north = Shapes.box(min, min, 0.0, max, max, min)
        val south = Shapes.box(min, min, max, max, max, 1.0)
        val west = Shapes.box(0.0, min, min, min, max, max)
        val east = Shapes.box(max, min, min, 1.0, max, max)
        val down = Shapes.box(min, 0.0, min, max, min, max)
        val up = Shapes.box(min, max, min, max, 1.0, max)
        return stateDefinition.states.associateWith { state ->
            var shape = center
            if (state.getValue(NORTH)) shape = Shapes.or(shape, north)
            if (state.getValue(SOUTH)) shape = Shapes.or(shape, south)
            if (state.getValue(WEST)) shape = Shapes.or(shape, west)
            if (state.getValue(EAST)) shape = Shapes.or(shape, east)
            if (state.getValue(DOWN)) shape = Shapes.or(shape, down)
            if (state.getValue(UP)) shape = Shapes.or(shape, up)
            shape
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getCollisionShape(
        state: BlockState,
        world: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape = shapeCache[state]!!

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getShape(
        state: BlockState,
        world: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape = shapeCache[state]!!

    protected open fun getCableMin(): Double = DEFAULT_CABLE_MIN
    protected open fun getCableMax(): Double = DEFAULT_CABLE_MAX

    companion object {
        val NORTH: BooleanProperty = BlockStateProperties.NORTH
        val SOUTH: BooleanProperty = BlockStateProperties.SOUTH
        val EAST: BooleanProperty = BlockStateProperties.EAST
        val WEST: BooleanProperty = BlockStateProperties.WEST
        val UP: BooleanProperty = BlockStateProperties.UP
        val DOWN: BooleanProperty = BlockStateProperties.DOWN

        @JvmStatic
        protected val DEFAULT_CABLE_MIN: Double = 6.0 / 16.0
        @JvmStatic
        protected val DEFAULT_CABLE_MAX: Double = 10.0 / 16.0

        @JvmStatic
        protected fun defaultSettings(): BlockBehaviour.Properties =
            BlockBehaviour.Properties.ofFullCopy(Blocks.REDSTONE_WIRE)
                .strength(0.5f)
                .noCollission()

        fun propertyFor(direction: Direction): BooleanProperty = when (direction) {
            Direction.NORTH -> NORTH
            Direction.SOUTH -> SOUTH
            Direction.EAST -> EAST
            Direction.WEST -> WEST
            Direction.UP -> UP
            Direction.DOWN -> DOWN
        }

//        /** 传输速率（EU/t）到能量等级（1–5）的映射：32→1, 128→2, 512→3, 2048→4, 8192→5。 */
//        @JvmStatic
//        fun transferRateToTier(transferRate: Long): Int = when {
//            transferRate >= 8192 -> 5
//            transferRate >= 2048 -> 4
//            transferRate >= 512 -> 3
//            transferRate >= 128 -> 2
//            else -> 1
//        }
    }
}
