package ic2_120.content.block.transmission

import ic2_120.content.item.CarbonPlate
import ic2_120.content.item.IronPlate
import ic2_120.content.item.SteelPlate
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120.registry.recipeId
import ic2_120.registry.type
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
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.item.ItemPlacementContext
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.registry.tag.ItemTags
import net.minecraft.state.StateManager
import net.minecraft.state.property.EnumProperty
import net.minecraft.state.property.Properties
import net.minecraft.util.StringIdentifiable
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.WorldAccess
import net.minecraft.world.WorldView
import java.util.function.Consumer
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem

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

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onStateReplaced(
        state: BlockState,
        world: net.minecraft.world.World,
        pos: BlockPos,
        newState: BlockState,
        moved: Boolean
    ) {
        if (!state.isOf(newState.block)) {
            KineticNetworkManager.invalidateAt(world, pos)
        }
        super.onStateReplaced(state, world, pos, newState, moved)
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
        if (world is net.minecraft.world.World && !world.isClient) {
            KineticNetworkManager.invalidateConnectionCachesAt(world, pos)
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos)
    }

    override fun <T : BlockEntity> getTicker(
        world: net.minecraft.world.World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, TransmissionBlockEntity.TYPE) { w, _, _, be ->
            (be as TransmissionBlockEntity).tick(w)
        }
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
class WoodTransmissionShaftBlock : TransmissionShaftBlock(ShaftMaterial.WOOD) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, WoodTransmissionShaftBlock::class.instance(), 16)
                .pattern("xxx")
                .pattern("   ")
                .pattern("xxx")
                .input('x', ItemTags.PLANKS)
                .criterion(hasItem(net.minecraft.item.Items.OAK_PLANKS), conditionsFromItem(net.minecraft.item.Items.OAK_PLANKS))
                .offerTo(exporter, WoodTransmissionShaftBlock::class.recipeId("from_planks"))
        }
    }
}

@ModBlock(name = "iron_transmission_shaft", registerItem = true, tab = CreativeTab.IC2_MACHINES)
class IronTransmissionShaftBlock : TransmissionShaftBlock(ShaftMaterial.IRON) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, IronTransmissionShaftBlock::class.instance(), 16)
                .pattern("xxx")
                .pattern("   ")
                .pattern("xxx")
                .input('x', IronPlate::class.instance())
                .criterion(hasItem(IronPlate::class.instance()), conditionsFromItem(IronPlate::class.instance()))
                .offerTo(exporter, IronTransmissionShaftBlock::class.recipeId("from_plates"))
        }
    }
}

@ModBlock(name = "steel_transmission_shaft", registerItem = true, tab = CreativeTab.IC2_MACHINES)
class SteelTransmissionShaftBlock : TransmissionShaftBlock(ShaftMaterial.STEEL) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, SteelTransmissionShaftBlock::class.instance(), 16)
                .pattern("xxx")
                .pattern("   ")
                .pattern("xxx")
                .input('x', SteelPlate::class.instance())
                .criterion(hasItem(SteelPlate::class.instance()), conditionsFromItem(SteelPlate::class.instance()))
                .offerTo(exporter, SteelTransmissionShaftBlock::class.recipeId("from_plates"))
        }
    }
}

@ModBlock(name = "carbon_transmission_shaft", registerItem = true, tab = CreativeTab.IC2_MACHINES)
class CarbonTransmissionShaftBlock : TransmissionShaftBlock(ShaftMaterial.CARBON) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, CarbonTransmissionShaftBlock::class.instance(), 16)
                .pattern("xxx")
                .pattern("   ")
                .pattern("xxx")
                .input('x', CarbonPlate::class.instance())
                .criterion(hasItem(CarbonPlate::class.instance()), conditionsFromItem(CarbonPlate::class.instance()))
                .offerTo(exporter, CarbonTransmissionShaftBlock::class.recipeId("from_plates"))
        }
    }
}

@ModBlock(name = "bevel_gear", registerItem = true, tab = CreativeTab.IC2_MACHINES)
class BevelGearBlock(
    val gearThickness: Float = DEFAULT_GEAR_THICKNESS
) : BaseTransmissionBlock() {
    init {
        defaultState = stateManager.defaultState.with(PLANE, BevelPlane.XZ)
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        builder.add(PLANE)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? {
        val axes = connectedAxes(ctx.world, ctx.blockPos)
        val plane = selectPlane(axes, defaultPlaneForSide(ctx.side.axis), ctx.side.axis)
        return defaultState.with(PLANE, plane)
    }

    override fun canPlaceAt(state: BlockState, world: WorldView, pos: BlockPos): Boolean = true

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getStateForNeighborUpdate(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        world: WorldAccess,
        pos: BlockPos,
        neighborPos: BlockPos
    ): BlockState {
        val axes = connectedAxes(world, pos)
        val plane = selectPlane(axes, state.get(PLANE), direction.axis)
        if (world is net.minecraft.world.World && !world.isClient) {
            KineticNetworkManager.invalidateConnectionCachesAt(world, pos)
        }
        return state.with(PLANE, plane)
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

    private fun connectedAxes(world: BlockView, pos: BlockPos): Set<Direction.Axis> {
        val axes = mutableSetOf<Direction.Axis>()
        for (direction in Direction.entries) {
            val neighborState = world.getBlockState(pos.offset(direction))
            if (neighborState.block is TransmissionShaftBlock && neighborState.get(Properties.AXIS) == direction.axis) {
                axes.add(direction.axis)
                continue
            }
            if (isKineticMachinePortFacing(world, pos.offset(direction), direction)) {
                axes.add(direction.axis)
            }
        }
        return axes
    }

    /**
     * 判定相邻方块是否为可与伞齿轮耦合的动能机器端口。
     *
     * @param direction 从伞齿轮指向相邻方块的方向。
     */
    private fun isKineticMachinePortFacing(world: BlockView, machinePos: BlockPos, directionFromGear: Direction): Boolean {
        val be = world.getBlockEntity(machinePos) as? IKineticMachinePort ?: return false
        val sideFromMachine = directionFromGear.opposite
        return be.canOutputKuTo(sideFromMachine) || be.canInputKuFrom(sideFromMachine)
    }

    private fun defaultPlaneForSide(axis: Direction.Axis): BevelPlane? = when (axis) {
        Direction.Axis.X -> BevelPlane.XZ
        Direction.Axis.Y -> BevelPlane.XY
        Direction.Axis.Z -> BevelPlane.XZ
    }

    private fun selectPlane(axes: Set<Direction.Axis>, fallback: BevelPlane?, preferredAxis: Direction.Axis? = null): BevelPlane {
        if (axes.size >= 2) {
            // 两轴时直接确定平面。
            BevelPlane.fromAxes(axes)?.let { return it }

            // 三轴时优先使用“最近变化的轴”，避免保持旧平面导致连通错误。
            if (axes.size >= 3 && preferredAxis != null) {
                val preferredPlane = when (preferredAxis) {
                    Direction.Axis.X -> if (fallback == BevelPlane.XY) BevelPlane.XY else BevelPlane.XZ
                    Direction.Axis.Y -> if (fallback == BevelPlane.XY) BevelPlane.XY else BevelPlane.YZ
                    Direction.Axis.Z -> if (fallback == BevelPlane.XZ) BevelPlane.XZ else BevelPlane.YZ
                }
                if (preferredPlane.includes(preferredAxis)) {
                    return preferredPlane
                }
            }

            // 回退：优先保留旧平面（若仍可用）。
            if (fallback != null && axes.containsAll(setOf(fallback.axes().first, fallback.axes().second))) {
                return fallback
            }

            val hasX = axes.contains(Direction.Axis.X)
            val hasY = axes.contains(Direction.Axis.Y)
            val hasZ = axes.contains(Direction.Axis.Z)
            return when {
                hasX && hasY -> BevelPlane.XY
                hasX && hasZ -> BevelPlane.XZ
                hasY && hasZ -> BevelPlane.YZ
                else -> fallback ?: BevelPlane.XZ
            }
        }
        if (axes.size == 1) {
            val axis = axes.first()
            // 单轴情况下也自动纠正：始终选一个包含该轴的平面，避免朝向与连接轴冲突。
            val secondAxis = when {
                preferredAxis != null && preferredAxis != axis -> preferredAxis
                fallback != null && fallback.includes(axis) -> fallback.axes().let { if (it.first == axis) it.second else it.first }
                axis == Direction.Axis.X -> Direction.Axis.Z
                else -> Direction.Axis.X
            }
            return BevelPlane.fromAxes(setOf(axis, secondAxis)) ?: (fallback ?: BevelPlane.XZ)
        }
        return fallback ?: BevelPlane.XZ
    }

    companion object {
        val PLANE: EnumProperty<BevelPlane> = EnumProperty.of("plane", BevelPlane::class.java)
        // 轴向齿厚/齿轮高度。渲染端以其中线作为分度圆所在面。
        const val DEFAULT_GEAR_THICKNESS = 0.13f

        private const val HALF = 1.0 / 6.0
        private val CORE = VoxelShapes.cuboid(0.25, 0.25, 0.25, 0.75, 0.75, 0.75)
        private val BAR_X = VoxelShapes.cuboid(0.0, 0.5 - HALF, 0.5 - HALF, 1.0, 0.5 + HALF, 0.5 + HALF)
        private val BAR_Y = VoxelShapes.cuboid(0.5 - HALF, 0.0, 0.5 - HALF, 0.5 + HALF, 1.0, 0.5 + HALF)
        private val BAR_Z = VoxelShapes.cuboid(0.5 - HALF, 0.5 - HALF, 0.0, 0.5 + HALF, 0.5 + HALF, 1.0)

        private val SHAPE_XY = VoxelShapes.union(CORE, BAR_X, BAR_Y)
        private val SHAPE_XZ = VoxelShapes.union(CORE, BAR_X, BAR_Z)
        private val SHAPE_YZ = VoxelShapes.union(CORE, BAR_Y, BAR_Z)

        private fun shapeForPlane(plane: BevelPlane): VoxelShape = when (plane) {
            BevelPlane.XY -> SHAPE_XY
            BevelPlane.XZ -> SHAPE_XZ
            BevelPlane.YZ -> SHAPE_YZ
        }

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, BevelGearBlock::class.instance(), 1)
                .pattern("s s")
                .pattern("s s")
                .pattern("sss")
                .input('s', SteelPlate::class.instance())
                .criterion(hasItem(SteelPlate::class.instance()), conditionsFromItem(SteelPlate::class.instance()))
                .offerTo(exporter, BevelGearBlock::class.recipeId("from_plates"))
        }
    }
}
