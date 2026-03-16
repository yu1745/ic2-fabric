package ic2_120.content.block.transmission

import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.type
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.Blocks
import net.minecraft.block.ShapeContext
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.state.StateManager
import net.minecraft.state.property.EnumProperty
import net.minecraft.state.property.IntProperty
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.StringIdentifiable
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.WorldAccess
import net.minecraft.world.WorldView
import net.minecraft.world.World

enum class ShaftMaterial(
    val texturePath: String
) {
    WOOD("textures/item/rotor/wood_rotor_model.png"),
    IRON("textures/item/rotor/iron_rotor_model.png"),
    STEEL("textures/item/rotor/steel_rotor_model.png"),
    CARBON("textures/item/rotor/carbon_rotor_model.png")
}

enum class BevelPlane(private val key: String) : StringIdentifiable {
    XY("xy"),
    XZ("xz"),
    YZ("yz");

    override fun asString(): String = key

    fun includes(axis: Direction.Axis): Boolean = when (this) {
        XY -> axis == Direction.Axis.X || axis == Direction.Axis.Y
        XZ -> axis == Direction.Axis.X || axis == Direction.Axis.Z
        YZ -> axis == Direction.Axis.Y || axis == Direction.Axis.Z
    }

    fun axes(): Pair<Direction.Axis, Direction.Axis> = when (this) {
        XY -> Direction.Axis.X to Direction.Axis.Y
        XZ -> Direction.Axis.X to Direction.Axis.Z
        YZ -> Direction.Axis.Y to Direction.Axis.Z
    }

    companion object {
        fun fromAxes(axes: Set<Direction.Axis>): BevelPlane? {
            if (axes.contains(Direction.Axis.X) && axes.contains(Direction.Axis.Y)) return XY
            if (axes.contains(Direction.Axis.X) && axes.contains(Direction.Axis.Z)) return XZ
            if (axes.contains(Direction.Axis.Y) && axes.contains(Direction.Axis.Z)) return YZ
            return null
        }
    }
}

abstract class BaseTransmissionBlock(
    settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(2.0f, 3.0f)
) : BlockWithEntity(settings) {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        TransmissionBlockEntity(pos, state)

    override fun getRenderType(state: BlockState): BlockRenderType = BlockRenderType.INVISIBLE
}

abstract class TransmissionShaftBlock(
    val material: ShaftMaterial
) : BaseTransmissionBlock() {
    init {
        defaultState = stateManager.defaultState.with(Properties.AXIS, Direction.Axis.Y)
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        builder.add(Properties.AXIS)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState {
        val axis = preferredNeighborAxis(ctx.world, ctx.blockPos, ctx.side.axis) ?: ctx.side.axis
        return defaultState.with(Properties.AXIS, axis)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getOutlineShape(
        state: BlockState,
        world: BlockView,
        pos: BlockPos,
        context: ShapeContext
    ): VoxelShape = shapeForAxis(state.get(Properties.AXIS))

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getCollisionShape(
        state: BlockState,
        world: BlockView,
        pos: BlockPos,
        context: ShapeContext
    ): VoxelShape = shapeForAxis(state.get(Properties.AXIS))

    private fun preferredNeighborAxis(world: WorldAccess, pos: BlockPos, preferred: Direction.Axis): Direction.Axis? {
        val preferredNeighbor = Direction.entries.firstOrNull { direction ->
            direction.axis == preferred && isShaftNeighborWithAxis(world, pos.offset(direction), preferred)
        }
        if (preferredNeighbor != null) return preferred

        for (direction in Direction.entries) {
            val axis = direction.axis
            if (isShaftNeighborWithAxis(world, pos.offset(direction), axis)) {
                return axis
            }
        }
        return null
    }

    private fun isShaftNeighborWithAxis(world: WorldAccess, neighborPos: BlockPos, axis: Direction.Axis): Boolean {
        val neighborState = world.getBlockState(neighborPos)
        val neighborBlock = neighborState.block
        return neighborBlock is TransmissionShaftBlock && neighborState.get(Properties.AXIS) == axis
    }

    companion object {
        private const val HALF = 1.0 / 6.0
        private val SHAPE_X = VoxelShapes.cuboid(0.0, 0.5 - HALF, 0.5 - HALF, 1.0, 0.5 + HALF, 0.5 + HALF)
        private val SHAPE_Y = VoxelShapes.cuboid(0.5 - HALF, 0.0, 0.5 - HALF, 0.5 + HALF, 1.0, 0.5 + HALF)
        private val SHAPE_Z = VoxelShapes.cuboid(0.5 - HALF, 0.5 - HALF, 0.0, 0.5 + HALF, 0.5 + HALF, 1.0)

        fun shapeForAxis(axis: Direction.Axis): VoxelShape = when (axis) {
            Direction.Axis.X -> SHAPE_X
            Direction.Axis.Y -> SHAPE_Y
            Direction.Axis.Z -> SHAPE_Z
        }
    }
}

@ModBlock(name = "wood_transmission_shaft", registerItem = true, tab = CreativeTab.IC2_MACHINES)
class WoodTransmissionShaftBlock : TransmissionShaftBlock(ShaftMaterial.WOOD)

@ModBlock(name = "iron_transmission_shaft", registerItem = true, tab = CreativeTab.IC2_MACHINES)
class IronTransmissionShaftBlock : TransmissionShaftBlock(ShaftMaterial.IRON)

@ModBlock(name = "steel_transmission_shaft", registerItem = true, tab = CreativeTab.IC2_MACHINES)
class SteelTransmissionShaftBlock : TransmissionShaftBlock(ShaftMaterial.STEEL)

@ModBlock(name = "carbon_transmission_shaft", registerItem = true, tab = CreativeTab.IC2_MACHINES)
class CarbonTransmissionShaftBlock : TransmissionShaftBlock(ShaftMaterial.CARBON)

@ModBlock(name = "bevel_gear", registerItem = true, tab = CreativeTab.IC2_MACHINES)
class BevelGearBlock : BaseTransmissionBlock() {
    init {
        defaultState = stateManager.defaultState
            .with(PLANE, BevelPlane.XZ)
            .with(DISTANCE_STEP, DEFAULT_DISTANCE_STEP)
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        builder.add(PLANE, DISTANCE_STEP)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? {
        val axes = connectedAxes(ctx.world, ctx.blockPos)
        val plane = BevelPlane.fromAxes(axes)
            ?: defaultPlaneForSide(ctx.side.axis)
        return if (plane == null) null else defaultState
            .with(PLANE, plane)
            .with(DISTANCE_STEP, DEFAULT_DISTANCE_STEP)
    }

    override fun canPlaceAt(state: BlockState, world: WorldView, pos: BlockPos): Boolean {
        return connectedAxes(world, pos).size >= 2
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getStateForNeighborUpdate(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        world: WorldAccess,
        pos: BlockPos,
        neighborPos: BlockPos
    ): BlockState {
        return if (canPlaceAt(state, world, pos)) state else Blocks.AIR.defaultState
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getOutlineShape(
        state: BlockState,
        world: BlockView,
        pos: BlockPos,
        context: ShapeContext
    ): VoxelShape = shapeForPlane(state.get(PLANE))

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getCollisionShape(
        state: BlockState,
        world: BlockView,
        pos: BlockPos,
        context: ShapeContext
    ): VoxelShape = shapeForPlane(state.get(PLANE))

    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult
    ): ActionResult {
        if (world.isClient) return ActionResult.SUCCESS
        val nextStep = if (state.get(DISTANCE_STEP) >= MAX_DISTANCE_STEP) 0 else state.get(DISTANCE_STEP) + 1
        val newState = state.with(DISTANCE_STEP, nextStep)
        world.setBlockState(pos, newState, Block.NOTIFY_ALL)

        val value = distanceFromStep(nextStep)
        player.sendMessage(Text.literal("伞齿轮距离: ${"%.3f".format(value)}"), false)
        return ActionResult.SUCCESS
    }

    private fun connectedAxes(world: BlockView, pos: BlockPos): Set<Direction.Axis> {
        val axes = mutableSetOf<Direction.Axis>()
        for (direction in Direction.entries) {
            val neighborState = world.getBlockState(pos.offset(direction))
            if (neighborState.block is TransmissionShaftBlock && neighborState.get(Properties.AXIS) == direction.axis) {
                axes.add(direction.axis)
            }
        }
        return axes
    }

    private fun defaultPlaneForSide(axis: Direction.Axis): BevelPlane? = when (axis) {
        Direction.Axis.X -> BevelPlane.XZ
        Direction.Axis.Y -> BevelPlane.XY
        Direction.Axis.Z -> BevelPlane.XZ
    }

    companion object {
        val PLANE: EnumProperty<BevelPlane> = EnumProperty.of("plane", BevelPlane::class.java)
        val DISTANCE_STEP: IntProperty = IntProperty.of("distance_step", 0, MAX_DISTANCE_STEP)

        const val MIN_DISTANCE = 0.12f
        const val DISTANCE_STEP_SIZE = 0.02f
        const val MAX_DISTANCE_STEP = 16
        const val DEFAULT_DISTANCE_STEP = 4

        private const val HALF = 1.0 / 6.0
        private val CORE = VoxelShapes.cuboid(0.25, 0.25, 0.25, 0.75, 0.75, 0.75)
        private val BAR_X = VoxelShapes.cuboid(0.0, 0.5 - HALF, 0.5 - HALF, 1.0, 0.5 + HALF, 0.5 + HALF)
        private val BAR_Y = VoxelShapes.cuboid(0.5 - HALF, 0.0, 0.5 - HALF, 0.5 + HALF, 1.0, 0.5 + HALF)
        private val BAR_Z = VoxelShapes.cuboid(0.5 - HALF, 0.5 - HALF, 0.0, 0.5 + HALF, 0.5 + HALF, 1.0)

        private fun shapeForPlane(plane: BevelPlane): VoxelShape = when (plane) {
            BevelPlane.XY -> VoxelShapes.union(CORE, BAR_X, BAR_Y)
            BevelPlane.XZ -> VoxelShapes.union(CORE, BAR_X, BAR_Z)
            BevelPlane.YZ -> VoxelShapes.union(CORE, BAR_Y, BAR_Z)
        }

        fun distanceFromStep(step: Int): Float = MIN_DISTANCE + (step.coerceIn(0, MAX_DISTANCE_STEP) * DISTANCE_STEP_SIZE)
    }
}
