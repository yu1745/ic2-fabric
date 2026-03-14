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
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.World
import net.minecraft.world.WorldAccess
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock

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

    private fun canConnect(world: WorldAccess, pos: BlockPos, direction: Direction, be: PipeBlockEntity?): Boolean {
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

    private fun pipeShape(state: BlockState): VoxelShape {
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
