package ic2_120.content.block.pipes

import net.minecraft.world.level.block.entity.BlockEntityType

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.compat.isClient
import ic2_120.compat.world

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SimpleWaterloggedBlock
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.client.item.TooltipContext
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.network.chat.Component
import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.registry.annotation.ModBlock
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.resources.ResourceLocation
import java.util.function.Consumer
import ic2_120.Ic2_120
import ic2_120.content.item.*
import ic2_120.content.recipes.ModTags
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.id
import ic2_120.registry.annotation.RecipeProvider

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
    settings: BlockBehaviour.Properties = BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0f, 3.0f)
) : BaseEntityBlock(settings), SimpleWaterloggedBlock {
    open val flowRateBucketsPerSecond: Double
        get() = size.baseBucketsPerSecond * material.multiplier

    init {
        setDefaultState(stateManager.defaultBlockState
            .setValue(BlockStateProperties.WATERLOGGED, false)
            .setValue(NORTH, false)
            .setValue(SOUTH, false)
            .setValue(EAST, false)
            .setValue(WEST, false)
            .setValue(UP, false)
            .setValue(DOWN, false)
            .setValue(TRANSPARENT, false)
        )
        buildShapeCache()
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(BlockStateProperties.WATERLOGGED)
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN)
        builder.add(TRANSPARENT)
    }

    override fun getRenderShape(state: BlockState): RenderShape =
        if (state.getValue(TRANSPARENT)) RenderShape.ENTITYBLOCK_ANIMATED else RenderShape.MODEL

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState {
        val world = ctx.world
        val pos = ctx.blockPos
        val fluidState = world.getFluidState(pos)
        return defaultState
            .setValue(BlockStateProperties.WATERLOGGED, fluidState.fluid == Fluids.WATER)
            .setValue(NORTH, canConnect(world, pos, Direction.NORTH, null))
            .setValue(SOUTH, canConnect(world, pos, Direction.SOUTH, null))
            .setValue(EAST, canConnect(world, pos, Direction.EAST, null))
            .setValue(WEST, canConnect(world, pos, Direction.WEST, null))
            .setValue(UP, canConnect(world, pos, Direction.UP, null))
            .setValue(DOWN, canConnect(world, pos, Direction.DOWN, null))
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getStateForNeighborUpdate(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        world: LevelAccessor,
        pos: BlockPos,
        neighborPos: BlockPos
    ): BlockState {
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world))
        }
        val be = world.getBlockEntity(pos) as? PipeBlockEntity
        val property = propertyFor(direction)
        val connected = canConnect(world, pos, direction, be)
        val next = state.setValue(property, connected)
        if (world is Level && !world.isClient && next != state) {
            PipeNetworkManager.invalidateConnectionCachesAt(world, pos)
        }
        return next
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getFluidState(state: BlockState): FluidState =
        if (state.getValue(BlockStateProperties.WATERLOGGED)) Fluids.WATER.getStill(false) else Fluids.EMPTY.defaultBlockState

    override fun canFillWithFluid(world: BlockGetter, pos: BlockPos, state: BlockState, fluid: Fluid): Boolean =
        !state.getValue(BlockStateProperties.WATERLOGGED) && fluid == Fluids.WATER

    override fun tryFillWithFluid(
        world: LevelAccessor,
        pos: BlockPos,
        state: BlockState,
        fluidState: FluidState
    ): Boolean {
        if (!canFillWithFluid(world, pos, state, fluidState.fluid)) return false
        if (!state.getValue(BlockStateProperties.WATERLOGGED)) {
            world.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, true), Block.NOTIFY_ALL)
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world))
        }
        return true
    }

    override fun tryDrainFluid(world: LevelAccessor, pos: BlockPos, state: BlockState): ItemStack = ItemStack.EMPTY

    internal fun recomputeState(world: Level, pos: BlockPos, state: BlockState, be: PipeBlockEntity): BlockState {
        var next = state
        for (dir in Direction.entries) {
            next = next.setValue(propertyFor(dir), canConnect(world, pos, dir, be))
        }
        return next
    }

    protected open fun canConnect(world: LevelAccessor, pos: BlockPos, direction: Direction, be: PipeBlockEntity?): Boolean {
        if (be?.isDisabled(direction) == true) return false
        val neighborPos = pos.offset(direction)
        val neighborState = world.getBlockState(neighborPos)
        val neighbor = neighborState.block
        if (neighbor is BasePipeBlock) {
            val neighborBe = world.getBlockEntity(neighborPos) as? PipeBlockEntity
            return neighborBe?.isDisabled(direction.opposite) != true
        }
        if (world is Level) {
            val storage = FluidStorage.SIDED.find(world, neighborPos, direction.opposite)
            if (storage != null) return true
        }
        return false
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onStateReplaced(state: BlockState, world: Level, pos: BlockPos, newState: BlockState, moved: Boolean) {
        if (!state.`is`(newState.block)) {
            PipeNetworkManager.invalidateAt(world, pos)
        }
        super.onStateReplaced(state, world, pos, newState, moved)
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = PipeBlockEntity(pos, state)

    /**
     * 添加物品提示信息（显示流量）
     */
    @Environment(EnvType.CLIENT)
    override fun appendTooltip(
        stack: ItemStack,
        world: BlockGetter?,
        tooltip: MutableList<Component>,
        context: TooltipContext
    ) {
        super.appendTooltip(stack, context, tooltip, type)
        val flowPerSec = flowRateBucketsPerSecond * 1000 // 转换为 mB/s
        val bucketsPerSec = flowRateBucketsPerSecond
        tooltip.add(Component.translatable(
            "tooltip.ic2_120.pipe.flow_rate",
            "%.1f".format(flowPerSec),
            "%.1f".format(bucketsPerSec)
        ))
    }

    override fun <T : BlockEntity> getTicker(world: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? =
        if (world.isClient) null
        else BlockEntityType.validateTicker(world, type, PipeBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    /** Pre-computed shape cache for all block states. Uses IdentityHashMap since BlockState is a flyweight. */
    protected var shapeCache: Map<BlockState, VoxelShape> = emptyMap()

    /** Call after setDefaultState to populate the shape cache. */
    protected fun buildShapeCache() {
        val map = java.util.IdentityHashMap<BlockState, VoxelShape>()
        for (state in stateManager.states) map[state] = pipeShape(state)
        shapeCache = map
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getCollisionShape(state: BlockState, world: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape = shapeCache[state] ?: pipeShape(state)

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getOutlineShape(state: BlockState, world: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape = shapeCache[state] ?: pipeShape(state)

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getCullingShape(state: BlockState, world: BlockGetter, pos: BlockPos): VoxelShape =
        Shapes.empty()

    protected open fun pipeShape(state: BlockState): VoxelShape {
        val r = size.radius
        val min = 0.5 - r
        val max = 0.5 + r
        var shape = Shapes.box(min, min, min, max, max, max)
        if (state.getValue(NORTH)) shape = Shapes.union(shape, Shapes.box(min, min, 0.0, max, max, min))
        if (state.getValue(SOUTH)) shape = Shapes.union(shape, Shapes.box(min, min, max, max, max, 1.0))
        if (state.getValue(WEST)) shape = Shapes.union(shape, Shapes.box(0.0, min, min, min, max, max))
        if (state.getValue(EAST)) shape = Shapes.union(shape, Shapes.box(max, min, min, 1.0, max, max))
        if (state.getValue(DOWN)) shape = Shapes.union(shape, Shapes.box(min, 0.0, min, max, min, max))
        if (state.getValue(UP)) shape = Shapes.union(shape, Shapes.box(min, max, min, max, 1.0, max))
        return shape
    }

    companion object {
        val NORTH: BooleanProperty = BlockStateProperties.NORTH
        val SOUTH: BooleanProperty = BlockStateProperties.SOUTH
        val EAST: BooleanProperty = BlockStateProperties.EAST
        val WEST: BooleanProperty = BlockStateProperties.WEST
        val UP: BooleanProperty = BlockStateProperties.UP
        val DOWN: BooleanProperty = BlockStateProperties.DOWN
        val TRANSPARENT: BooleanProperty = BooleanProperty.create("transparent")

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
    override val flowRateBucketsPerSecond: Double
        get() = when (material) {
            PipeMaterial.BRONZE -> 2.4
            PipeMaterial.CARBON -> 4.8
        }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(BlockStateProperties.FACING)
    }

    init {
        defaultState = stateManager.defaultBlockState
            .setValue(BlockStateProperties.WATERLOGGED, false)
            .setValue(NORTH, false)
            .setValue(SOUTH, false)
            .setValue(EAST, false)
            .setValue(WEST, false)
            .setValue(UP, false)
            .setValue(DOWN, false)
            .setValue(TRANSPARENT, false)
            .setValue(BlockStateProperties.FACING, Direction.NORTH)
        // Rebuild cache with our overridden pipeShape() and the expanded state space (includes FACING)
        buildShapeCache()
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState {
        val world = ctx.world
        val pos = ctx.blockPos
        val fluidState = world.getFluidState(pos)
        val facing = ctx.side.opposite
        val back = facing.opposite
        val be = world.getBlockEntity(pos) as? PipeBlockEntity
        return defaultState
            .setValue(BlockStateProperties.WATERLOGGED, fluidState.fluid == Fluids.WATER)
            .setValue(BlockStateProperties.FACING, facing)
            .setValue(propertyFor(back), canConnect(world, pos, back, be, facing))
            .setValue(propertyFor(facing), canConnect(world, pos, facing, be, facing))
            .setValue(NORTH, canConnect(world, pos, Direction.NORTH, be, facing))
            .setValue(SOUTH, canConnect(world, pos, Direction.SOUTH, be, facing))
            .setValue(EAST, canConnect(world, pos, Direction.EAST, be, facing))
            .setValue(WEST, canConnect(world, pos, Direction.WEST, be, facing))
            .setValue(UP, canConnect(world, pos, Direction.UP, be, facing))
            .setValue(DOWN, canConnect(world, pos, Direction.DOWN, be, facing))
    }

    override fun canConnect(world: LevelAccessor, pos: BlockPos, direction: Direction, be: PipeBlockEntity?): Boolean =
        canConnect(world, pos, direction, be, null)

    private fun canConnect(world: LevelAccessor, pos: BlockPos, direction: Direction, be: PipeBlockEntity?, overrideFacing: Direction?): Boolean {
        if (be?.isDisabled(direction) == true) return false
        val state = world.getBlockState(pos)
        val facing = overrideFacing ?: (if (state.contains(BlockStateProperties.FACING)) state.getValue(BlockStateProperties.FACING) else Direction.NORTH)
        val neighborPos = pos.offset(direction)
        val neighborState = world.getBlockState(neighborPos)
        val neighbor = neighborState.block

        // 前方（贴住目标机器的一面）只允许连接非管道且拥有流体能力的方块。
        if (direction == facing) {
            if (neighbor is BasePipeBlock) return false
            if (world is Level) {
                return FluidStorage.SIDED.find(world, neighborPos, direction.opposite) != null
            }
            return false
        }

        // 后方和四面（除正面外的所有面）可以连接管道，也可以直接连接有流体能力的方块。
        if (neighbor is BasePipeBlock) {
            val neighborBe = world.getBlockEntity(neighborPos) as? PipeBlockEntity
            return neighborBe?.isDisabled(direction.opposite) != true
        }
        if (world is Level) {
            return FluidStorage.SIDED.find(world, neighborPos, direction.opposite) != null
        }
        return false
    }

    override fun pipeShape(state: BlockState): VoxelShape {
        val facing = state.getValue(BlockStateProperties.FACING)
        val back = facing.opposite
        val min = 6.0 / 16.0
        val max = 10.0 / 16.0
        var shape = Shapes.box(min, min, min, max, max, max)

        for (dir in Direction.entries) {
            if (dir == facing) continue
            if (state.get(propertyFor(dir))) {
                shape = when (dir) {
                    Direction.NORTH -> Shapes.union(shape, Shapes.box(min, min, 0.0, max, max, min))
                    Direction.SOUTH -> Shapes.union(shape, Shapes.box(min, min, max, max, max, 1.0))
                    Direction.WEST -> Shapes.union(shape, Shapes.box(0.0, min, min, min, max, max))
                    Direction.EAST -> Shapes.union(shape, Shapes.box(max, min, min, 1.0, max, max))
                    Direction.DOWN -> Shapes.union(shape, Shapes.box(min, 0.0, min, max, min, max))
                    Direction.UP -> Shapes.union(shape, Shapes.box(min, max, min, max, 1.0, max))
                }
            }
        }

        val plate = when (facing) {
            Direction.NORTH -> Shapes.box(2.0 / 16.0, 2.0 / 16.0, 0.0, 14.0 / 16.0, 14.0 / 16.0, 2.0 / 16.0)
            Direction.SOUTH -> Shapes.box(2.0 / 16.0, 2.0 / 16.0, 14.0 / 16.0, 14.0 / 16.0, 14.0 / 16.0, 1.0)
            Direction.WEST -> Shapes.box(0.0, 2.0 / 16.0, 2.0 / 16.0, 2.0 / 16.0, 14.0 / 16.0, 14.0 / 16.0)
            Direction.EAST -> Shapes.box(14.0 / 16.0, 2.0 / 16.0, 2.0 / 16.0, 1.0, 14.0 / 16.0, 14.0 / 16.0)
            Direction.DOWN -> Shapes.box(2.0 / 16.0, 0.0, 2.0 / 16.0, 14.0 / 16.0, 2.0 / 16.0, 14.0 / 16.0)
            Direction.UP -> Shapes.box(2.0 / 16.0, 14.0 / 16.0, 2.0 / 16.0, 14.0 / 16.0, 1.0, 14.0 / 16.0)
        }
        return Shapes.union(shape, plate)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (!world.isClient) {
            val be = world.getBlockEntity(pos) as? net.minecraft.world.MenuProvider
            if (be != null) {
                player.openMenu(be)
                return InteractionResult.SUCCESS
            }
        }
        return InteractionResult.SUCCESS
    }
}

@ModBlock(name = "bronze_pipe_tiny", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "pipe")
class BronzePipeTinyBlock : BasePipeBlock(PipeSize.TINY, PipeMaterial.BRONZE) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BronzePipeTinyBlock::class.item(), 6)
                .pattern("XXX").pattern("   ").pattern("XXX")
                .define('X', BronzeCasing::class.instance())
                .unlockedBy(has(BronzeCasing::class.instance()), has(BronzeCasing::class.instance()))
                .save(exporter, BronzePipeTinyBlock::class.id())
        }
    }
}

@ModBlock(name = "bronze_pipe_small", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "pipe")
class BronzePipeSmallBlock : BasePipeBlock(PipeSize.SMALL, PipeMaterial.BRONZE) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BronzePipeSmallBlock::class.item(), 3)
                .pattern("X X").pattern("X X").pattern("X X")
                .define('X', BronzeCasing::class.instance())
                .unlockedBy(has(BronzeCasing::class.instance()), has(BronzeCasing::class.instance()))
                .save(exporter, BronzePipeSmallBlock::class.id())
        }
    }
}

@ModBlock(name = "bronze_pipe_medium", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "pipe")
class BronzePipeMediumBlock : BasePipeBlock(PipeSize.MEDIUM, PipeMaterial.BRONZE) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BronzePipeMediumBlock::class.item(), 2)
                .pattern("XXX").pattern("   ").pattern("XXX")
                .define('X', Ingredient.of(ModTags.Compat.Items.PLATES_BRONZE))
                .unlockedBy(has(BronzePlate::class.instance()), has(BronzePlate::class.instance()))
                .save(exporter, BronzePipeMediumBlock::class.id())
        }
    }
}

@ModBlock(name = "bronze_pipe_large", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "pipe")
class BronzePipeLargeBlock : BasePipeBlock(PipeSize.LARGE, PipeMaterial.BRONZE) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BronzePipeLargeBlock::class.item(), 1)
                .pattern("X X").pattern("X X").pattern("X X")
                .define('X', Ingredient.of(ModTags.Compat.Items.PLATES_BRONZE))
                .unlockedBy(has(BronzePlate::class.instance()), has(BronzePlate::class.instance()))
                .save(exporter, BronzePipeLargeBlock::class.id())
        }
    }
}

@ModBlock(name = "carbon_pipe_tiny", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "pipe")
class CarbonPipeTinyBlock : BasePipeBlock(PipeSize.TINY, PipeMaterial.CARBON) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CarbonPipeTinyBlock::class.item(), 6)
                .pattern("XXX").pattern("   ").pattern("XXX")
                .define('X', CarbonFibre::class.instance())
                .unlockedBy(has(CarbonFibre::class.instance()), has(CarbonFibre::class.instance()))
                .save(exporter, CarbonPipeTinyBlock::class.id())
        }
    }
}

@ModBlock(name = "carbon_pipe_small", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "pipe")
class CarbonPipeSmallBlock : BasePipeBlock(PipeSize.SMALL, PipeMaterial.CARBON) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CarbonPipeSmallBlock::class.item(), 3)
                .pattern("X X").pattern("X X").pattern("X X")
                .define('X', CarbonFibre::class.instance())
                .unlockedBy(has(CarbonFibre::class.instance()), has(CarbonFibre::class.instance()))
                .save(exporter, CarbonPipeSmallBlock::class.id())
        }
    }
}

@ModBlock(name = "carbon_pipe_medium", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "pipe")
class CarbonPipeMediumBlock : BasePipeBlock(PipeSize.MEDIUM, PipeMaterial.CARBON) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CarbonPipeMediumBlock::class.item(), 2)
                .pattern("XXX").pattern("   ").pattern("XXX")
                .define('X', CarbonMesh::class.instance())
                .unlockedBy(has(CarbonMesh::class.instance()), has(CarbonMesh::class.instance()))
                .save(exporter, CarbonPipeMediumBlock::class.id())
        }
    }
}

@ModBlock(name = "carbon_pipe_large", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "pipe")
class CarbonPipeLargeBlock : BasePipeBlock(PipeSize.LARGE, PipeMaterial.CARBON) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CarbonPipeLargeBlock::class.item(), 1)
                .pattern("X X").pattern("X X").pattern("X X")
                .define('X', CarbonMesh::class.instance())
                .unlockedBy(has(CarbonMesh::class.instance()), has(CarbonMesh::class.instance()))
                .save(exporter, CarbonPipeLargeBlock::class.id())
        }
    }
}

@ModBlock(name = "bronze_pump_attachment", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "pipe")
class BronzePumpAttachmentBlock : PumpAttachmentBlock(PipeMaterial.BRONZE) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BronzePumpAttachmentBlock::class.item(), 1)
                .pattern(" P ").pattern(" T ").pattern(" P ")
                .define('P', Ingredient.of(ModTags.Compat.Items.PLATES_BRONZE))
                .define('T', BronzePipeTinyBlock::class.item())
                .unlockedBy(has(BronzePipeTinyBlock::class.item()), has(BronzePipeTinyBlock::class.item()))
                .save(exporter, BronzePumpAttachmentBlock::class.id())
        }
    }
}

@ModBlock(name = "carbon_pump_attachment", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "pipe")
class CarbonPumpAttachmentBlock : PumpAttachmentBlock(PipeMaterial.CARBON) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CarbonPumpAttachmentBlock::class.item(), 1)
                .pattern(" P ").pattern(" T ").pattern(" P ")
                .define('P', CarbonPlate::class.instance())
                .define('T', CarbonPipeTinyBlock::class.item())
                .unlockedBy(has(CarbonPipeTinyBlock::class.item()), has(CarbonPipeTinyBlock::class.item()))
                .save(exporter, CarbonPumpAttachmentBlock::class.id())
        }
    }
}
