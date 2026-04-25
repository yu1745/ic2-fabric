package ic2_120.content.block.cables

import com.mojang.serialization.MapCodec
import ic2_120.content.block.cables.CableBlockEntity
import ic2_120.content.block.energy.EnergyNetworkManager
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.Blocks
import net.minecraft.block.Waterloggable
import net.minecraft.block.ShapeContext
import net.minecraft.item.ItemStack
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.fluid.Fluid
import net.minecraft.fluid.FluidState
import net.minecraft.fluid.Fluids
import net.minecraft.item.ItemPlacementContext
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.state.property.Properties
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.World
import net.minecraft.world.WorldAccess
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
abstract class BaseCableBlock(settings: AbstractBlock.Settings = defaultSettings()) :
    BlockWithEntity(settings),
    Waterloggable {

    override fun getCodec(): MapCodec<out BlockWithEntity> = CABLE_CODEC

    // ── BlockState 属性 ─────────────────────────────────────────

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        builder.add(Properties.WATERLOGGED)
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState {
        val world = ctx.world
        val pos = ctx.blockPos
        val fluidState = world.getFluidState(pos)
        return defaultState
            .with(Properties.WATERLOGGED, fluidState.fluid == Fluids.WATER)
            .with(NORTH, canConnect(world, pos, Direction.NORTH))
            .with(SOUTH, canConnect(world, pos, Direction.SOUTH))
            .with(EAST, canConnect(world, pos, Direction.EAST))
            .with(WEST, canConnect(world, pos, Direction.WEST))
            .with(UP, canConnect(world, pos, Direction.UP))
            .with(DOWN, canConnect(world, pos, Direction.DOWN))
    }

    override fun getStateForNeighborUpdate(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        world: WorldAccess,
        pos: BlockPos,
        neighborPos: BlockPos
    ): BlockState {
        // 与原版含水方块一致：含水时调度水 tick，不在这里反推 WATERLOGGED。
        if (state.get(Properties.WATERLOGGED)) {
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world))
        }
        val property = propertyFor(direction)
        val previousConnected = state.get(property)
        val connected = canConnect(world, pos, direction)
        if (connected != previousConnected && world is World && !world.isClient) {
            EnergyNetworkManager.invalidateConnectionCachesAt(world, pos)
        }
        return state.with(property, connected)
    }

    override fun getFluidState(state: BlockState): FluidState =
        if (state.get(Properties.WATERLOGGED)) Fluids.WATER.getStill(false) else Fluids.EMPTY.getDefaultState()

    // 流动水通过 FluidFillable.tryFillWithFluid 含水；需显式实现（与栅栏等原版方块一致）。
    override fun canFillWithFluid(player: net.minecraft.entity.player.PlayerEntity?, world: BlockView, pos: BlockPos, state: BlockState, fluid: Fluid): Boolean =
        !state.get(Properties.WATERLOGGED) && fluid == Fluids.WATER

    override fun tryFillWithFluid(
        world: WorldAccess,
        pos: BlockPos,
        state: BlockState,
        fluidState: FluidState
    ): Boolean {
        if (!canFillWithFluid(null, world, pos, state, fluidState.fluid)) return false
        if (!state.get(Properties.WATERLOGGED)) {
            world.setBlockState(pos, state.with(Properties.WATERLOGGED, true), Block.NOTIFY_ALL)
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world))
        }
        return true
    }

    override fun tryDrainFluid(player: net.minecraft.entity.player.PlayerEntity?, world: WorldAccess, pos: BlockPos, state: BlockState): ItemStack =
        ItemStack.EMPTY

    private fun canConnect(world: WorldAccess, pos: BlockPos, direction: Direction): Boolean {
        val neighborPos = pos.offset(direction)
        val neighborBlock = world.getBlockState(neighborPos).block
        if (neighborBlock is BaseCableBlock) return true
        if (world is World) {
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
    override fun onStateReplaced(state: BlockState, world: World, pos: BlockPos, newState: BlockState, moved: Boolean) {
        if (!state.isOf(newState.block)) {
            EnergyNetworkManager.invalidateAt(world, pos)
        }
        super.onStateReplaced(state, world, pos, newState, moved)
    }

    // ── BlockEntity / Ticker ────────────────────────────────────

    override fun getRenderType(state: BlockState): BlockRenderType = BlockRenderType.MODEL

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        CableBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, CableBlockEntity.TYPE) { w, p, s, be -> be.tick(w, p, s) }

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
        val center = VoxelShapes.cuboid(min, min, min, max, max, max)
        val north = VoxelShapes.cuboid(min, min, 0.0, max, max, min)
        val south = VoxelShapes.cuboid(min, min, max, max, max, 1.0)
        val west = VoxelShapes.cuboid(0.0, min, min, min, max, max)
        val east = VoxelShapes.cuboid(max, min, min, 1.0, max, max)
        val down = VoxelShapes.cuboid(min, 0.0, min, max, min, max)
        val up = VoxelShapes.cuboid(min, max, min, max, 1.0, max)
        return stateManager.states.associateWith { state ->
            var shape = center
            if (state.get(NORTH)) shape = VoxelShapes.union(shape, north)
            if (state.get(SOUTH)) shape = VoxelShapes.union(shape, south)
            if (state.get(WEST)) shape = VoxelShapes.union(shape, west)
            if (state.get(EAST)) shape = VoxelShapes.union(shape, east)
            if (state.get(DOWN)) shape = VoxelShapes.union(shape, down)
            if (state.get(UP)) shape = VoxelShapes.union(shape, up)
            shape
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getCollisionShape(
        state: BlockState,
        world: BlockView,
        pos: BlockPos,
        context: ShapeContext
    ): VoxelShape = shapeCache[state]!!

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getOutlineShape(
        state: BlockState,
        world: BlockView,
        pos: BlockPos,
        context: ShapeContext
    ): VoxelShape = shapeCache[state]!!

    protected open fun getCableMin(): Double = DEFAULT_CABLE_MIN
    protected open fun getCableMax(): Double = DEFAULT_CABLE_MAX

    companion object {
        val CABLE_CODEC: MapCodec<BaseCableBlock> = Block.createCodec { error("BaseCableBlock cannot be deserialized from JSON") }

        val NORTH: BooleanProperty = Properties.NORTH
        val SOUTH: BooleanProperty = Properties.SOUTH
        val EAST: BooleanProperty = Properties.EAST
        val WEST: BooleanProperty = Properties.WEST
        val UP: BooleanProperty = Properties.UP
        val DOWN: BooleanProperty = Properties.DOWN

        @JvmStatic
        protected val DEFAULT_CABLE_MIN: Double = 6.0 / 16.0
        @JvmStatic
        protected val DEFAULT_CABLE_MAX: Double = 10.0 / 16.0

        @JvmStatic
        protected fun defaultSettings(): AbstractBlock.Settings =
            AbstractBlock.Settings.copy(Blocks.REDSTONE_WIRE)
                .strength(0.5f)
                .noCollision()

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
