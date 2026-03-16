package ic2_120.content.block.pipes

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.Blocks
import net.minecraft.block.ShapeContext
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.item.ItemPlacementContext
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.state.property.Properties
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.World
import net.minecraft.world.WorldAccess
import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.type

enum class PipeSize(val baseBucketsPerSecond: Double, val radius: Double) {
    TINY(0.4, 2.0 / 16.0),
    SMALL(0.8, 3.0 / 16.0),
    MEDIUM(2.4, 4.0 / 16.0),
    LARGE(4.8, 5.0 / 16.0)
}

enum class PipeMaterial(val multiplier: Double) {
    BRONZE(1.0),
    CARBON(2.0)
}

abstract class BasePipeBlock(
    val size: PipeSize,
    val material: PipeMaterial,
    settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(2.0f, 3.0f)
) : BlockWithEntity(settings) {
    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState {
        val world = ctx.world
        val pos = ctx.blockPos
        return defaultState
            .with(NORTH, canConnect(world, pos, Direction.NORTH, null))
            .with(SOUTH, canConnect(world, pos, Direction.SOUTH, null))
            .with(EAST, canConnect(world, pos, Direction.EAST, null))
            .with(WEST, canConnect(world, pos, Direction.WEST, null))
            .with(UP, canConnect(world, pos, Direction.UP, null))
            .with(DOWN, canConnect(world, pos, Direction.DOWN, null))
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
        val be = world.getBlockEntity(pos) as? PipeBlockEntity
        val property = propertyFor(direction)
        val connected = canConnect(world, pos, direction, be)
        val next = state.with(property, connected)
        if (world is World && !world.isClient && next != state) {
            PipeNetworkManager.invalidateConnectionCachesAt(world, pos)
        }
        return next
    }

    internal fun recomputeState(world: World, pos: BlockPos, state: BlockState, be: PipeBlockEntity): BlockState {
        var next = state
        for (dir in Direction.entries) {
            next = next.with(propertyFor(dir), canConnect(world, pos, dir, be))
        }
        return next
    }

    protected open fun canConnect(world: WorldAccess, pos: BlockPos, direction: Direction, be: PipeBlockEntity?): Boolean {
        if (be?.isDisabled(direction) == true) return false
        val neighborPos = pos.offset(direction)
        val neighborState = world.getBlockState(neighborPos)
        val neighbor = neighborState.block
        if (neighbor is BasePipeBlock) {
            val neighborBe = world.getBlockEntity(neighborPos) as? PipeBlockEntity
            return neighborBe?.isDisabled(direction.opposite) != true
        }
        if (world is World) {
            val storage = FluidStorage.SIDED.find(world, neighborPos, direction.opposite)
            if (storage != null) return true
        }
        return false
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onStateReplaced(state: BlockState, world: World, pos: BlockPos, newState: BlockState, moved: Boolean) {
        if (!state.isOf(newState.block)) {
            PipeNetworkManager.invalidateAt(world, pos)
        }
        super.onStateReplaced(state, world, pos, newState, moved)
    }

    override fun getRenderType(state: BlockState): BlockRenderType = BlockRenderType.MODEL

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = PipeBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(world: World, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, PipeBlockEntity.TYPE) { w, p, s, be -> be.tick(w, p, s) }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getCollisionShape(state: BlockState, world: BlockView, pos: BlockPos, context: ShapeContext): VoxelShape = pipeShape(state)

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getOutlineShape(state: BlockState, world: BlockView, pos: BlockPos, context: ShapeContext): VoxelShape = pipeShape(state)

    protected open fun pipeShape(state: BlockState): VoxelShape {
        val r = size.radius
        val min = 0.5 - r
        val max = 0.5 + r
        var shape = VoxelShapes.cuboid(min, min, min, max, max, max)
        if (state.get(NORTH)) shape = VoxelShapes.union(shape, VoxelShapes.cuboid(min, min, 0.0, max, max, min))
        if (state.get(SOUTH)) shape = VoxelShapes.union(shape, VoxelShapes.cuboid(min, min, max, max, max, 1.0))
        if (state.get(WEST)) shape = VoxelShapes.union(shape, VoxelShapes.cuboid(0.0, min, min, min, max, max))
        if (state.get(EAST)) shape = VoxelShapes.union(shape, VoxelShapes.cuboid(max, min, min, 1.0, max, max))
        if (state.get(DOWN)) shape = VoxelShapes.union(shape, VoxelShapes.cuboid(min, 0.0, min, max, min, max))
        if (state.get(UP)) shape = VoxelShapes.union(shape, VoxelShapes.cuboid(min, max, min, max, 1.0, max))
        return shape
    }

    companion object {
        val NORTH: BooleanProperty = Properties.NORTH
        val SOUTH: BooleanProperty = Properties.SOUTH
        val EAST: BooleanProperty = Properties.EAST
        val WEST: BooleanProperty = Properties.WEST
        val UP: BooleanProperty = Properties.UP
        val DOWN: BooleanProperty = Properties.DOWN

        fun propertyFor(direction: Direction): BooleanProperty = when (direction) {
            Direction.NORTH -> NORTH
            Direction.SOUTH -> SOUTH
            Direction.EAST -> EAST
            Direction.WEST -> WEST
            Direction.UP -> UP
            Direction.DOWN -> DOWN
        }
    }
}

abstract class PumpAttachmentBlock(material: PipeMaterial) : BasePipeBlock(PipeSize.TINY, material) {
    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(Properties.FACING)
    }

    init {
        defaultState = stateManager.defaultState
            .with(NORTH, false)
            .with(SOUTH, false)
            .with(EAST, false)
            .with(WEST, false)
            .with(UP, false)
            .with(DOWN, false)
            .with(Properties.FACING, Direction.NORTH)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState {
        val world = ctx.world
        val pos = ctx.blockPos
        val facing = ctx.side.opposite
        val back = facing.opposite
        val be = world.getBlockEntity(pos) as? PipeBlockEntity
        return defaultState
            .with(Properties.FACING, facing)
            .with(propertyFor(back), canConnect(world, pos, back, be))
    }

    override fun canConnect(world: WorldAccess, pos: BlockPos, direction: Direction, be: PipeBlockEntity?): Boolean {
        if (be?.isDisabled(direction) == true) return false
        val state = world.getBlockState(pos)
        val facing = if (state.contains(Properties.FACING)) state.get(Properties.FACING) else Direction.NORTH
        val neighborPos = pos.offset(direction)
        val neighborState = world.getBlockState(neighborPos)
        val neighbor = neighborState.block

        // 前方（贴住目标机器的一面）只允许连接非管道且拥有流体能力的方块。
        if (direction == facing) {
            if (neighbor is BasePipeBlock) return false
            if (world is World) {
                return FluidStorage.SIDED.find(world, neighborPos, direction.opposite) != null
            }
            return false
        }

        // 后方（背面）只连接管道，不连接机器，避免歧义。
        if (direction == facing.opposite) {
            if (neighbor !is BasePipeBlock) return false
            val neighborBe = world.getBlockEntity(neighborPos) as? PipeBlockEntity
            return neighborBe?.isDisabled(direction.opposite) != true
        }
        return false
    }

    override fun pipeShape(state: BlockState): VoxelShape {
        val facing = state.get(Properties.FACING)
        val back = facing.opposite
        val min = 6.0 / 16.0
        val max = 10.0 / 16.0
        var shape = VoxelShapes.cuboid(min, min, min, max, max, max)

        if (state.get(propertyFor(back))) {
            shape = when (back) {
                Direction.NORTH -> VoxelShapes.union(shape, VoxelShapes.cuboid(min, min, 0.0, max, max, min))
                Direction.SOUTH -> VoxelShapes.union(shape, VoxelShapes.cuboid(min, min, max, max, max, 1.0))
                Direction.WEST -> VoxelShapes.union(shape, VoxelShapes.cuboid(0.0, min, min, min, max, max))
                Direction.EAST -> VoxelShapes.union(shape, VoxelShapes.cuboid(max, min, min, 1.0, max, max))
                Direction.DOWN -> VoxelShapes.union(shape, VoxelShapes.cuboid(min, 0.0, min, max, min, max))
                Direction.UP -> VoxelShapes.union(shape, VoxelShapes.cuboid(min, max, min, max, 1.0, max))
            }
        }

        val plate = when (facing) {
            Direction.NORTH -> VoxelShapes.cuboid(2.0 / 16.0, 2.0 / 16.0, 0.0, 14.0 / 16.0, 14.0 / 16.0, 2.0 / 16.0)
            Direction.SOUTH -> VoxelShapes.cuboid(2.0 / 16.0, 2.0 / 16.0, 14.0 / 16.0, 14.0 / 16.0, 14.0 / 16.0, 1.0)
            Direction.WEST -> VoxelShapes.cuboid(0.0, 2.0 / 16.0, 2.0 / 16.0, 2.0 / 16.0, 14.0 / 16.0, 14.0 / 16.0)
            Direction.EAST -> VoxelShapes.cuboid(14.0 / 16.0, 2.0 / 16.0, 2.0 / 16.0, 1.0, 14.0 / 16.0, 14.0 / 16.0)
            Direction.DOWN -> VoxelShapes.cuboid(2.0 / 16.0, 0.0, 2.0 / 16.0, 14.0 / 16.0, 2.0 / 16.0, 14.0 / 16.0)
            Direction.UP -> VoxelShapes.cuboid(2.0 / 16.0, 14.0 / 16.0, 2.0 / 16.0, 14.0 / 16.0, 1.0, 14.0 / 16.0)
        }
        return VoxelShapes.union(shape, plate)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: net.minecraft.entity.player.PlayerEntity,
        hand: Hand,
        hit: BlockHitResult
    ): ActionResult {
        if (!world.isClient) {
            val be = world.getBlockEntity(pos) as? net.minecraft.screen.NamedScreenHandlerFactory
            if (be != null) {
                player.openHandledScreen(be)
                return ActionResult.SUCCESS
            }
        }
        return ActionResult.SUCCESS
    }
}

@ModBlock(name = "bronze_pipe_tiny", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "pipe")
class BronzePipeTinyBlock : BasePipeBlock(PipeSize.TINY, PipeMaterial.BRONZE)

@ModBlock(name = "bronze_pipe_small", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "pipe")
class BronzePipeSmallBlock : BasePipeBlock(PipeSize.SMALL, PipeMaterial.BRONZE)

@ModBlock(name = "bronze_pipe_medium", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "pipe")
class BronzePipeMediumBlock : BasePipeBlock(PipeSize.MEDIUM, PipeMaterial.BRONZE)

@ModBlock(name = "bronze_pipe_large", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "pipe")
class BronzePipeLargeBlock : BasePipeBlock(PipeSize.LARGE, PipeMaterial.BRONZE)

@ModBlock(name = "carbon_pipe_tiny", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "pipe")
class CarbonPipeTinyBlock : BasePipeBlock(PipeSize.TINY, PipeMaterial.CARBON)

@ModBlock(name = "carbon_pipe_small", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "pipe")
class CarbonPipeSmallBlock : BasePipeBlock(PipeSize.SMALL, PipeMaterial.CARBON)

@ModBlock(name = "carbon_pipe_medium", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "pipe")
class CarbonPipeMediumBlock : BasePipeBlock(PipeSize.MEDIUM, PipeMaterial.CARBON)

@ModBlock(name = "carbon_pipe_large", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "pipe")
class CarbonPipeLargeBlock : BasePipeBlock(PipeSize.LARGE, PipeMaterial.CARBON)

@ModBlock(name = "bronze_pump_attachment", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "pipe")
class BronzePumpAttachmentBlock : PumpAttachmentBlock(PipeMaterial.BRONZE)

@ModBlock(name = "carbon_pump_attachment", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "pipe")
class CarbonPumpAttachmentBlock : PumpAttachmentBlock(PipeMaterial.CARBON)
