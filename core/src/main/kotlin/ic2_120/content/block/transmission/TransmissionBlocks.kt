package ic2_120.content.block.transmission

import ic2_120.compat.validateTicker

import net.minecraft.data.recipes.RecipeProvider.getHasName

import net.minecraft.world.level.block.entity.BlockEntityType

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.compat.isClient
import ic2_120.compat.world

import ic2_120.content.item.CarbonPlate
import ic2_120.content.item.IronPlate
import ic2_120.content.item.SteelPlate
import ic2_120.content.recipes.ModTags
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120.registry.recipeId
import ic2_120.registry.type
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.level.block.SimpleWaterloggedBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.tags.ItemTags
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.util.StringRepresentable
import com.mojang.serialization.MapCodec
import net.minecraft.world.entity.player.Player
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.LevelReader
import java.util.function.Consumer

enum class ShaftMaterial(
    val texturePath: String
) {
    WOOD("textures/item/rotor/wood_rotor_model.png"),
    IRON("textures/item/rotor/iron_rotor_model.png"),
    STEEL("textures/item/rotor/steel_rotor_model.png"),
    CARBON("textures/item/rotor/carbon_rotor_model.png")
}

enum class BevelPlane(private val key: String) : StringRepresentable {
    XY("xy"),
    XZ("xz"),
    YZ("yz");

    override val serializedName: String get() = key

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
    settings: BlockBehaviour.Properties = BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0f, 3.0f)
) : BaseEntityBlock(settings), SimpleWaterloggedBlock {

    init {
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.WATERLOGGED, false))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(BlockStateProperties.WATERLOGGED)
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState {
        val fluidState = ctx.level.getFluidState(ctx.clickedPos)
        return defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, fluidState.getType() == Fluids.WATER)
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
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.tickDelay(world))
        }
        if (world is net.minecraft.world.level.Level && !world.isClient) {
            KineticNetworkManager.invalidateConnectionCachesAt(world, pos)
        }
        return state
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getFluidState(state: BlockState): FluidState =
        if (state.getValue(BlockStateProperties.WATERLOGGED)) Fluids.WATER.getSource(false) else Fluids.EMPTY.defaultFluidState()

    override fun canPlaceLiquid(player: Player?, world: BlockGetter, pos: BlockPos, state: BlockState, fluid: Fluid): Boolean =
        !state.getValue(BlockStateProperties.WATERLOGGED) && fluid == Fluids.WATER

    override fun placeLiquid(
        world: LevelAccessor,
        pos: BlockPos,
        state: BlockState,
        fluidState: FluidState
    ): Boolean {
        if (!canPlaceLiquid(null, world, pos, state, fluidState.getType())) return false
        if (!state.getValue(BlockStateProperties.WATERLOGGED)) {
            world.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, true), Block.UPDATE_ALL)
            world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.tickDelay(world))
        }
        return true
    }

    override fun pickupBlock(player: Player?, world: LevelAccessor, pos: BlockPos, state: BlockState): ItemStack =
        ItemStack.EMPTY

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        TransmissionBlockEntity(pos, state)

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.INVISIBLE

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onRemove(
        state: BlockState,
        world: net.minecraft.world.level.Level,
        pos: BlockPos,
        newState: BlockState,
        moved: Boolean
    ) {
        if (!state.`is`(newState.block)) {
            KineticNetworkManager.invalidateAt(world, pos)
        }
        super.onRemove(state, world, pos, newState, moved)
    }

    override fun <T : BlockEntity> getTicker(
        world: net.minecraft.world.level.Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else validateTicker(type, TransmissionBlockEntity.TYPE) { w, _, _, be ->
            be.tick(w)
        }
}

abstract class TransmissionShaftBlock(
    val material: ShaftMaterial
) : BaseTransmissionBlock() {
    init {
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(BlockStateProperties.AXIS)
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState {
        val fluidState = ctx.level.getFluidState(ctx.clickedPos)
        val axis = preferredNeighborAxis(ctx.level, ctx.clickedPos, ctx.clickedFace.axis) ?: ctx.clickedFace.axis
        return defaultBlockState()
            .setValue(BlockStateProperties.WATERLOGGED, fluidState.getType() == Fluids.WATER)
            .setValue(BlockStateProperties.AXIS, axis)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getShape(
        state: BlockState,
        world: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape = shapeForAxis(state.getValue(BlockStateProperties.AXIS))

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getCollisionShape(
        state: BlockState,
        world: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape = shapeForAxis(state.getValue(BlockStateProperties.AXIS))

    private fun preferredNeighborAxis(world: LevelAccessor, pos: BlockPos, preferred: Direction.Axis): Direction.Axis? {
        val preferredNeighbor = Direction.entries.firstOrNull { direction ->
            direction.axis == preferred && isShaftNeighborWithAxis(world, pos.relative(direction), preferred)
        }
        if (preferredNeighbor != null) return preferred

        for (direction in Direction.entries) {
            val axis = direction.axis
            if (isShaftNeighborWithAxis(world, pos.relative(direction), axis)) {
                return axis
            }
        }
        return null
    }

    private fun isShaftNeighborWithAxis(world: LevelAccessor, neighborPos: BlockPos, axis: Direction.Axis): Boolean {
        val neighborState = world.getBlockState(neighborPos)
        val neighborBlock = neighborState.block
        return neighborBlock is TransmissionShaftBlock && neighborState.getValue(BlockStateProperties.AXIS) == axis
    }

    companion object {
        private const val HALF = 1.0 / 6.0
        private val SHAPE_X = Shapes.box(0.0, 0.5 - HALF, 0.5 - HALF, 1.0, 0.5 + HALF, 0.5 + HALF)
        private val SHAPE_Y = Shapes.box(0.5 - HALF, 0.0, 0.5 - HALF, 0.5 + HALF, 1.0, 0.5 + HALF)
        private val SHAPE_Z = Shapes.box(0.5 - HALF, 0.5 - HALF, 0.0, 0.5 + HALF, 0.5 + HALF, 1.0)

        fun shapeForAxis(axis: Direction.Axis): VoxelShape = when (axis) {
            Direction.Axis.X -> SHAPE_X
            Direction.Axis.Y -> SHAPE_Y
            Direction.Axis.Z -> SHAPE_Z
        }
    }
}

@ModBlock(name = "wood_transmission_shaft", registerItem = true, tab = CreativeTab.IC2_MACHINES)
class WoodTransmissionShaftBlock : TransmissionShaftBlock(ShaftMaterial.WOOD) {
    override fun codec(): MapCodec<out BaseEntityBlock> = BlockBehaviour.simpleCodec { WoodTransmissionShaftBlock() }
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, WoodTransmissionShaftBlock::class.instance(), 16)
                .pattern("xxx")
                .pattern("   ")
                .pattern("xxx")
                .define('x', ItemTags.PLANKS)
                .unlockedBy(getHasName(net.minecraft.world.item.Items.OAK_PLANKS), has(net.minecraft.world.item.Items.OAK_PLANKS))
                .save(exporter, WoodTransmissionShaftBlock::class.recipeId("from_planks"))
        }
    }
}

@ModBlock(name = "iron_transmission_shaft", registerItem = true, tab = CreativeTab.IC2_MACHINES)
class IronTransmissionShaftBlock : TransmissionShaftBlock(ShaftMaterial.IRON) {
    override fun codec(): MapCodec<out BaseEntityBlock> = BlockBehaviour.simpleCodec { IronTransmissionShaftBlock() }
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, IronTransmissionShaftBlock::class.instance(), 16)
                .pattern("xxx")
                .pattern("   ")
                .pattern("xxx")
                .define('x', Ingredient.of(ModTags.Compat.Items.PLATES_IRON))
                .unlockedBy(getHasName(IronPlate::class.instance()), has(IronPlate::class.instance()))
                .save(exporter, IronTransmissionShaftBlock::class.recipeId("from_plates"))
        }
    }
}

@ModBlock(name = "steel_transmission_shaft", registerItem = true, tab = CreativeTab.IC2_MACHINES)
class SteelTransmissionShaftBlock : TransmissionShaftBlock(ShaftMaterial.STEEL) {
    override fun codec(): MapCodec<out BaseEntityBlock> = BlockBehaviour.simpleCodec { SteelTransmissionShaftBlock() }
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SteelTransmissionShaftBlock::class.instance(), 16)
                .pattern("xxx")
                .pattern("   ")
                .pattern("xxx")
                .define('x', Ingredient.of(ModTags.Compat.Items.PLATES_STEEL))
                .unlockedBy(getHasName(SteelPlate::class.instance()), has(SteelPlate::class.instance()))
                .save(exporter, SteelTransmissionShaftBlock::class.recipeId("from_plates"))
        }
    }
}

@ModBlock(name = "carbon_transmission_shaft", registerItem = true, tab = CreativeTab.IC2_MACHINES)
class CarbonTransmissionShaftBlock : TransmissionShaftBlock(ShaftMaterial.CARBON) {
    override fun codec(): MapCodec<out BaseEntityBlock> = BlockBehaviour.simpleCodec { CarbonTransmissionShaftBlock() }
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CarbonTransmissionShaftBlock::class.instance(), 16)
                .pattern("xxx")
                .pattern("   ")
                .pattern("xxx")
                .define('x', CarbonPlate::class.instance())
                .unlockedBy(getHasName(CarbonPlate::class.instance()), has(CarbonPlate::class.instance()))
                .save(exporter, CarbonTransmissionShaftBlock::class.recipeId("from_plates"))
        }
    }
}

@ModBlock(name = "bevel_gear", registerItem = true, tab = CreativeTab.IC2_MACHINES)
class BevelGearBlock(
    val gearThickness: Float = DEFAULT_GEAR_THICKNESS
) : BaseTransmissionBlock() {
    override fun codec(): MapCodec<out BaseEntityBlock> = BlockBehaviour.simpleCodec { BevelGearBlock() }
    init {
        registerDefaultState(defaultBlockState().setValue(PLANE, BevelPlane.XZ))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(PLANE)
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState {
        val fluidState = ctx.level.getFluidState(ctx.clickedPos)
        val axes = connectedAxes(ctx.level, ctx.clickedPos)
        val plane = selectPlane(axes, defaultPlaneForSide(ctx.clickedFace.axis), ctx.clickedFace.axis)
        return defaultBlockState()
            .setValue(BlockStateProperties.WATERLOGGED, fluidState.getType() == Fluids.WATER)
            .setValue(PLANE, plane)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun canSurvive(state: BlockState, world: LevelReader, pos: BlockPos): Boolean = true

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun updateShape(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        world: LevelAccessor,
        pos: BlockPos,
        neighborPos: BlockPos
    ): BlockState {
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.tickDelay(world))
        }
        val axes = connectedAxes(world, pos)
        val plane = selectPlane(axes, state.getValue(PLANE), direction.axis)
        if (world is net.minecraft.world.level.Level && !world.isClient) {
            KineticNetworkManager.invalidateConnectionCachesAt(world, pos)
        }
        return state.setValue(PLANE, plane)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getShape(
        state: BlockState,
        world: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape = shapeForPlane(state.getValue(PLANE))

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getCollisionShape(
        state: BlockState,
        world: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape = shapeForPlane(state.getValue(PLANE))

    private fun connectedAxes(world: BlockGetter, pos: BlockPos): Set<Direction.Axis> {
        val axes = mutableSetOf<Direction.Axis>()
        for (direction in Direction.entries) {
            val neighborState = world.getBlockState(pos.relative(direction))
            if (neighborState.block is TransmissionShaftBlock && neighborState.getValue(BlockStateProperties.AXIS) == direction.axis) {
                axes.add(direction.axis)
                continue
            }
            if (isKineticMachinePortFacing(world, pos.relative(direction), direction)) {
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
    private fun isKineticMachinePortFacing(world: BlockGetter, machinePos: BlockPos, directionFromGear: Direction): Boolean {
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
        val PLANE: EnumProperty<BevelPlane> = EnumProperty.create("plane", BevelPlane::class.java)
        // 轴向齿厚/齿轮高度。渲染端以其中线作为分度圆所在面。
        const val DEFAULT_GEAR_THICKNESS = 0.13f

        private const val HALF = 1.0 / 6.0
        private val CORE = Shapes.box(0.25, 0.25, 0.25, 0.75, 0.75, 0.75)
        private val BAR_X = Shapes.box(0.0, 0.5 - HALF, 0.5 - HALF, 1.0, 0.5 + HALF, 0.5 + HALF)
        private val BAR_Y = Shapes.box(0.5 - HALF, 0.0, 0.5 - HALF, 0.5 + HALF, 1.0, 0.5 + HALF)
        private val BAR_Z = Shapes.box(0.5 - HALF, 0.5 - HALF, 0.0, 0.5 + HALF, 0.5 + HALF, 1.0)

        private val SHAPE_XY = Shapes.or(CORE, BAR_X, BAR_Y)
        private val SHAPE_XZ = Shapes.or(CORE, BAR_X, BAR_Z)
        private val SHAPE_YZ = Shapes.or(CORE, BAR_Y, BAR_Z)

        private fun shapeForPlane(plane: BevelPlane): VoxelShape = when (plane) {
            BevelPlane.XY -> SHAPE_XY
            BevelPlane.XZ -> SHAPE_XZ
            BevelPlane.YZ -> SHAPE_YZ
        }

        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BevelGearBlock::class.instance(), 1)
                .pattern("s s")
                .pattern("s s")
                .pattern("sss")
                .define('s', Ingredient.of(ModTags.Compat.Items.PLATES_IRON))
                .unlockedBy(getHasName(IronPlate::class.instance()), has(IronPlate::class.instance()))
                .save(exporter, BevelGearBlock::class.recipeId("from_plates"))
        }
    }
}
