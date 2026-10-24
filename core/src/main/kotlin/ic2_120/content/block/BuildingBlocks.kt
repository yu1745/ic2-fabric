package ic2_120.content.block

import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.content.item.IronPlate
import ic2_120.content.item.Alloy
import ic2_120.content.item.Resin
import ic2_120.content.item.RubberItem
import ic2_120.content.item.Treetap
import ic2_120.content.item.HazmatHelmet
import ic2_120.content.recipes.ModTags
import ic2_120.content.recipes.crafting.ConsumeTreetapShapedRecipeDatagen
import ic2_120.registry.annotation.ModBlock
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.properties.BlockSetType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.level.block.RotatedPillarBlock
import net.minecraft.world.level.block.SimpleWaterloggedBlock
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.core.Direction
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.GameRules
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.minecraft.tags.ItemTags
import net.minecraft.world.level.Level
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.data.recipes.RecipeCategory
import java.util.function.Consumer
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.annotation.RecipeProvider

// ========== 建筑：防爆玻璃、泡沫、墙、垫、管道、TNT ==========

@ModBlock(
    name = "reinforced_glass",
    registerItem = true,
    tab = CreativeTab.IC2_MATERIALS,
    group = "building",
    renderLayer = "cutout",
    generateBlockLootTable = false
)
class ReinforcedGlassBlock : Block(BlockBehaviour.Settings.copy(Blocks.GLASS).strength(5.0f, 1200.0f).nonOpaque().dropsNothing()){
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val glass = Items.GLASS
            val alloy = Alloy::class.instance()
            ShapedRecipeBuilder.create(RecipeCategory.MISC, ReinforcedGlassBlock::class.item(), 7)
                .pattern("XGX")
                .pattern("XXX")
                .pattern("XGX")
                .input('X', glass)
                .input('G', alloy)
                .criterion(hasItem(glass), conditionsFromItem(glass))
                .criterion(hasItem(alloy), conditionsFromItem(alloy))
                .offerTo(exporter, ReinforcedGlassBlock::class.id())
        }
    }
}

/** 防爆门：与铁门相同需红石开关，爆炸抗性同防爆石/玻璃。 */
@ModBlock(
    name = "reinforced_door",
    registerItem = true,
    tab = CreativeTab.IC2_MATERIALS,
    group = "building",
    renderLayer = "cutout",
)
class ReinforcedDoorBlock(
    settings: BlockBehaviour.Settings = BlockBehaviour.Settings.copy(Blocks.IRON_DOOR).strength(25.0f, 1200.0f),
) : DoorBlock(settings, BlockSetType.IRON) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val stone = ReinforcedStoneBlock::class.item()
            val ironDoor = Items.IRON_DOOR
            ShapedRecipeBuilder.create(RecipeCategory.REDSTONE, ReinforcedDoorBlock::class.item(), 1)
                .pattern("SSS")
                .pattern("SDS")
                .pattern("SSS")
                .input('S', stone)
                .input('D', ironDoor)
                .criterion(hasItem(stone), conditionsFromItem(stone))
                .criterion(hasItem(ironDoor), conditionsFromItem(ironDoor))
                .offerTo(exporter, ReinforcedDoorBlock::class.id())
        }
    }
}

/** 建筑泡沫公共行为：可进入、不可生成生物、进入后窒息，并按光照进行随机刻固化。 */
abstract class Ic2FoamBlock(
    settings: BlockBehaviour.Settings,
    private val hardenTime: Int,
) : Block(settings) {

    /**
     * 泡沫必须允许实体进入，但原版 isInsideWall 依赖碰撞形状，不能直接复用它。
     * 因此仅在实体的头部位于当前泡沫方块内时，按原版频率造成 inWall 伤害。
     */
    override fun onEntityCollision(state: BlockState, world: Level, pos: BlockPos, entity: Entity) {
        val hasHazmatHelmet = entity is LivingEntity &&
            entity.getEquippedStack(EquipmentSlot.HEAD).item is HazmatHelmet
        if (!world.isClient && entity is LivingEntity && !hasHazmatHelmet &&
            BlockPos.ofFloored(entity.eyePos) == pos && entity.age % 20 == 0) {
            entity.damage(world.damageSources.inWall(), 1.0f)
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun randomTick(state: BlockState, world: ServerLevel, pos: BlockPos, random: RandomSource) {
        var light = world.getLightLevel(pos)
        if (!state.isOpaque && state.getOpacity(world, pos) == 0) {
            for (direction in Direction.values()) {
                light = maxOf(light, world.getLightLevel(pos.offset(direction)))
            }
        }

        val averageSeconds = hardenTime * (16 - light).coerceAtLeast(1)
        val randomTickSpeed = world.gameRules.getInt(GameRules.RANDOM_TICK_SPEED).coerceAtLeast(1)
        val chance = (4096.0f / (averageSeconds * 20.0f * randomTickSpeed)).coerceAtMost(1.0f)
        if (random.nextFloat() < chance) {
            world.setBlockState(pos, hardenedState(), Block.NOTIFY_ALL)
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getDroppedStacks(state: BlockState, builder: LootParams.Builder): MutableList<ItemStack> =
        foamDrops()

    protected abstract fun hardenedState(): BlockState

    protected abstract fun foamDrops(): MutableList<ItemStack>
}

/**
 * 建筑泡沫：木脚手架喷涂后得到；手持沙子右键可立即固化为浅灰建筑泡沫墙。
 */
@ModBlock(name = "foam", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "building", renderLayer = "translucent")
class FoamBlock(
    settings: BlockBehaviour.Settings = BlockBehaviour.Settings.copy(Blocks.WHITE_WOOL)
        .strength(0.01f, 10.0f)
        .noCollision()
        .nonOpaque()
        .suffocates { _, _, _ -> true }
        .blockVision { _, _, _ -> true }
        .allowsSpawning { _, _, _, _ -> false }
        .ticksRandomly()
) : Ic2FoamBlock(settings, 300) {

    override fun hardenedState(): BlockState = LightGrayWallBlock::class.instance().defaultState

    override fun foamDrops(): MutableList<ItemStack> = mutableListOf()

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onUse(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (world.isClient) return InteractionResult.SUCCESS
        val stack = player.getStackInHand(hand)
        if (!stack.isOf(Items.SAND)) return InteractionResult.PASS
        val wall = LightGrayWallBlock::class.instance().defaultState
        world.setBlockState(pos, wall, Block.NOTIFY_ALL)
        world.playSound(null, pos, SoundEvents.BLOCK_SAND_PLACE, SoundSource.BLOCKS, 0.85f, 0.9f)
        world.syncWorldEvent(2001, pos, Block.getRawIdFromState(state))
        if (!player.abilities.creativeMode) stack.decrement(1)
        return InteractionResult.CONSUME
    }

}

/**
 * 强化建筑泡沫：由喷枪喷涂覆盖 [IronScaffoldBlock] 得到。
 * 沙子右键或随机刻固化后变为 [ReinforcedStoneBlock]（防爆石）。
 */
@ModBlock(name = "reinforced_foam", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "building", renderLayer = "translucent")
class ReinforcedFoamBlock(
    settings: BlockBehaviour.Settings = BlockBehaviour.Settings.copy(Blocks.WHITE_WOOL)
        .strength(0.01f, 10.0f)
        .noCollision()
        .nonOpaque()
        .suffocates { _, _, _ -> true }
        .blockVision { _, _, _ -> true }
        .allowsSpawning { _, _, _, _ -> false }
        .ticksRandomly()
) : Ic2FoamBlock(settings, 600) {

    override fun hardenedState(): BlockState = ReinforcedStoneBlock::class.instance().defaultState

    override fun foamDrops(): MutableList<ItemStack> =
        mutableListOf(ItemStack(IronScaffoldBlock::class.item()))

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onUse(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (world.isClient) return InteractionResult.SUCCESS
        val stack = player.getStackInHand(hand)
        if (!stack.isOf(Items.SAND)) return InteractionResult.PASS
        val stone = ReinforcedStoneBlock::class.instance().defaultState
        world.setBlockState(pos, stone, Block.NOTIFY_ALL)
        world.playSound(null, pos, SoundEvents.BLOCK_STONE_PLACE, SoundSource.BLOCKS, 0.85f, 0.85f)
        world.syncWorldEvent(2001, pos, Block.getRawIdFromState(state))
        if (!player.abilities.creativeMode) stack.decrement(1)
        return InteractionResult.CONSUME
    }

}

@ModBlock(name = "resin_sheet", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "building")
class ResinSheetBlock : Block(
    BlockBehaviour.Settings.copy(Blocks.WHITE_CARPET)
        .strength(0.5f)
        .velocityMultiplier(0.4f)
        .jumpVelocityMultiplier(0.0f)
) {
    override fun onLandedUpon(world: Level, state: net.minecraft.world.level.block.state.BlockState, pos: BlockPos, entity: Entity, fallDistance: Float) {
        entity.handleFallDamage(fallDistance, 0.2f, world.damageSources.fall())
    }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val resin = Resin::class.instance()
            ShapedRecipeBuilder.create(RecipeCategory.MISC, ResinSheetBlock::class.item(), 3)
                .pattern("xxx")
                .pattern("xxx")
                .input('x', resin)
                .criterion(hasItem(resin), conditionsFromItem(resin))
                .offerTo(exporter, ResinSheetBlock::class.id())
        }
    }
}

@ModBlock(name = "rubber_sheet", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "building")
class RubberSheetBlock : Block(
    BlockBehaviour.Settings.copy(Blocks.WHITE_CARPET)
        .strength(0.5f)
        .velocityMultiplier(0.4f)
        .jumpVelocityMultiplier(0.0f)
) {
    override fun onLandedUpon(world: Level, state: net.minecraft.world.level.block.state.BlockState, pos: BlockPos, entity: Entity, fallDistance: Float) {
        entity.handleFallDamage(fallDistance, 0.2f, world.damageSources.fall())
    }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val rubber = RubberItem::class.instance()
            ShapedRecipeBuilder.create(RecipeCategory.MISC, RubberSheetBlock::class.item(), 3)
                .pattern("xxx")
                .pattern("xxx")
                .input('x', Ingredient.fromTag(ModTags.Compat.Items.RUBBER))
                .criterion(hasItem(rubber), conditionsFromItem(rubber))
                .offerTo(exporter, RubberSheetBlock::class.id())
        }
    }
}

@ModBlock(name = "wool_sheet", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "building")
class WoolSheetBlock : Block(
    BlockBehaviour.Settings.copy(Blocks.WHITE_CARPET)
        .strength(0.5f)
        .velocityMultiplier(0.4f)
        .jumpVelocityMultiplier(0.0f)
) {
    override fun onLandedUpon(world: Level, state: net.minecraft.world.level.block.state.BlockState, pos: BlockPos, entity: Entity, fallDistance: Float) {
        entity.handleFallDamage(fallDistance, 0.2f, world.damageSources.fall())
    }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            ShapedRecipeBuilder.create(RecipeCategory.MISC, WoolSheetBlock::class.item(), 3)
                .pattern("xxx")
                .pattern("xxx")
                .input('x', ItemTags.WOOL)
                .criterion(hasItem(Items.WHITE_WOOL), conditionsFromItem(Items.WHITE_WOOL))
                .offerTo(exporter, WoolSheetBlock::class.id())
        }
    }
}

@ModBlock(name = "mining_pipe", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "building")
class MiningPipeBlock(settings: BlockBehaviour.Settings = BlockBehaviour.Settings.copy(Blocks.IRON_BLOCK).strength(3.0f)) :
    Block(settings), SimpleWaterloggedBlock {

    init {
        defaultState = defaultState
            .with(NORTH, false).with(SOUTH, false)
            .with(EAST, false).with(WEST, false)
            .with(UP, false).with(DOWN, false)
            .with(WATERLOGGED, false)
    }

    override fun appendProperties(builder: StateDefinition.Builder<Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN, WATERLOGGED)
    }

    override fun getPlacementState(ctx: BlockPlaceContext): BlockState {
        val world = ctx.world
        val pos = ctx.blockPos
        val fluidState = world.getFluidState(pos)
        return defaultState
            .with(NORTH, canConnect(world, pos, Direction.NORTH))
            .with(SOUTH, canConnect(world, pos, Direction.SOUTH))
            .with(EAST, canConnect(world, pos, Direction.EAST))
            .with(WEST, canConnect(world, pos, Direction.WEST))
            .with(UP, canConnect(world, pos, Direction.UP))
            .with(DOWN, canConnect(world, pos, Direction.DOWN))
            .with(WATERLOGGED, fluidState.fluid == Fluids.WATER)
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
        // 与原版含水方块一致：含水时调度水 tick，不在这里反推 WATERLOGGED。
        if (state.get(WATERLOGGED)) {
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world))
        }
        return state.with(propertyFor(direction), canConnect(world, pos, direction))
    }

    // ── 含水（SimpleWaterloggedBlock）：让流水「填入」而非破坏管道 ──
    // 否则管道的细管碰撞箱不 blocksMovement()，会被流水当掉落物冲走（同红石/火把）。

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getFluidState(state: BlockState): FluidState =
        if (state.get(WATERLOGGED)) Fluids.WATER.getStill(false) else Fluids.EMPTY.getDefaultState()

    override fun canFillWithFluid(world: BlockGetter, pos: BlockPos, state: BlockState, fluid: Fluid): Boolean =
        !state.get(WATERLOGGED) && fluid == Fluids.WATER

    override fun tryFillWithFluid(
        world: LevelAccessor,
        pos: BlockPos,
        state: BlockState,
        fluidState: FluidState
    ): Boolean {
        if (!canFillWithFluid(world, pos, state, fluidState.fluid)) return false
        if (!state.get(WATERLOGGED)) {
            world.setBlockState(pos, state.with(WATERLOGGED, true), Block.NOTIFY_ALL)
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world))
        }
        return true
    }

    override fun tryDrainFluid(world: LevelAccessor, pos: BlockPos, state: BlockState): ItemStack = ItemStack.EMPTY

    private fun canConnect(world: LevelAccessor, pos: BlockPos, direction: Direction): Boolean {
        val neighborPos = pos.offset(direction)
        val neighborBlock = world.getBlockState(neighborPos).block
        return neighborBlock is MiningPipeBlock || neighborBlock is BaseMinerBlock
    }

    // ── Shapes ──

    private val shapeCache: Map<BlockState, VoxelShape> by lazy { buildShapeCache() }

    private fun buildShapeCache(): Map<BlockState, VoxelShape> {
        val min = 6.0 / 16.0
        val max = 10.0 / 16.0
        val center = Shapes.cuboid(min, min, min, max, max, max)
        val north = Shapes.cuboid(min, min, 0.0, max, max, min)
        val south = Shapes.cuboid(min, min, max, max, max, 1.0)
        val west = Shapes.cuboid(0.0, min, min, min, max, max)
        val east = Shapes.cuboid(max, min, min, 1.0, max, max)
        val down = Shapes.cuboid(min, 0.0, min, max, min, max)
        val up = Shapes.cuboid(min, max, min, max, 1.0, max)
        return stateManager.states.associateWith { state ->
            var shape = center
            if (state.get(NORTH)) shape = Shapes.union(shape, north)
            if (state.get(SOUTH)) shape = Shapes.union(shape, south)
            if (state.get(WEST)) shape = Shapes.union(shape, west)
            if (state.get(EAST)) shape = Shapes.union(shape, east)
            if (state.get(DOWN)) shape = Shapes.union(shape, down)
            if (state.get(UP)) shape = Shapes.union(shape, up)
            shape
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getCollisionShape(state: BlockState, world: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape =
        shapeCache[state]!!

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getOutlineShape(state: BlockState, world: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape =
        shapeCache[state]!!

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getCullingShape(state: BlockState, world: BlockGetter, pos: BlockPos): VoxelShape =
        Shapes.empty()

    companion object {
        val NORTH: BooleanProperty = BlockStateProperties.NORTH
        val SOUTH: BooleanProperty = BlockStateProperties.SOUTH
        val EAST: BooleanProperty = BlockStateProperties.EAST
        val WEST: BooleanProperty = BlockStateProperties.WEST
        val UP: BooleanProperty = BlockStateProperties.UP
        val DOWN: BooleanProperty = BlockStateProperties.DOWN
        val WATERLOGGED: BooleanProperty = BlockStateProperties.WATERLOGGED

        fun propertyFor(direction: Direction): BooleanProperty = when (direction) {
            Direction.NORTH -> NORTH
            Direction.SOUTH -> SOUTH
            Direction.EAST -> EAST
            Direction.WEST -> WEST
            Direction.UP -> UP
            Direction.DOWN -> DOWN
        }

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val ironPlate = IronPlate::class.instance()
            val treetap = Treetap::class.instance()
            ConsumeTreetapShapedRecipeDatagen.offer(
                exporter = exporter,
                recipeId = MiningPipeBlock::class.id(),
                result = MiningPipeBlock::class.item(),
                pattern = listOf("x x", "x x", "xyx"),
                keys = mapOf('x' to ironPlate, 'y' to treetap),
                count = 16
            )
        }
    }
}

// @ModBlock(name = "itnt", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "building")
//todo 暂时不注册
class ItntBlock : Block(BlockBehaviour.Settings.copy(Blocks.TNT).strength(0.0f))

// ========== 建筑泡沫墙（16 色） ==========

@ModBlock(name = "white_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class WhiteWallBlock : Block(BlockBehaviour.Settings.copy(Blocks.WHITE_CONCRETE).strength(3.0f, 30.0f))

@ModBlock(name = "orange_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class OrangeWallBlock : Block(BlockBehaviour.Settings.copy(Blocks.ORANGE_CONCRETE).strength(3.0f, 30.0f))

@ModBlock(name = "magenta_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class MagentaWallBlock : Block(BlockBehaviour.Settings.copy(Blocks.MAGENTA_CONCRETE).strength(3.0f, 30.0f))

@ModBlock(name = "light_blue_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class LightBlueWallBlock : Block(BlockBehaviour.Settings.copy(Blocks.LIGHT_BLUE_CONCRETE).strength(3.0f, 30.0f))

@ModBlock(name = "yellow_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class YellowWallBlock : Block(BlockBehaviour.Settings.copy(Blocks.YELLOW_CONCRETE).strength(3.0f, 30.0f))

@ModBlock(name = "lime_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class LimeWallBlock : Block(BlockBehaviour.Settings.copy(Blocks.LIME_CONCRETE).strength(3.0f, 30.0f))

@ModBlock(name = "pink_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class PinkWallBlock : Block(BlockBehaviour.Settings.copy(Blocks.PINK_CONCRETE).strength(3.0f, 30.0f))

@ModBlock(name = "gray_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class GrayWallBlock : Block(BlockBehaviour.Settings.copy(Blocks.GRAY_CONCRETE).strength(3.0f, 30.0f))

@ModBlock(name = "light_gray_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class LightGrayWallBlock : Block(BlockBehaviour.Settings.copy(Blocks.LIGHT_GRAY_CONCRETE).strength(3.0f, 30.0f))

@ModBlock(name = "cyan_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class CyanWallBlock : Block(BlockBehaviour.Settings.copy(Blocks.CYAN_CONCRETE).strength(3.0f, 30.0f))

@ModBlock(name = "purple_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class PurpleWallBlock : Block(BlockBehaviour.Settings.copy(Blocks.PURPLE_CONCRETE).strength(3.0f, 30.0f))

@ModBlock(name = "blue_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class BlueWallBlock : Block(BlockBehaviour.Settings.copy(Blocks.BLUE_CONCRETE).strength(3.0f, 30.0f))

@ModBlock(name = "brown_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class BrownWallBlock : Block(BlockBehaviour.Settings.copy(Blocks.BROWN_CONCRETE).strength(3.0f, 30.0f))

@ModBlock(name = "green_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class GreenWallBlock : Block(BlockBehaviour.Settings.copy(Blocks.GREEN_CONCRETE).strength(3.0f, 30.0f))

@ModBlock(name = "red_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class RedWallBlock : Block(BlockBehaviour.Settings.copy(Blocks.RED_CONCRETE).strength(3.0f, 30.0f))

@ModBlock(name = "black_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class BlackWallBlock : Block(BlockBehaviour.Settings.copy(Blocks.BLACK_CONCRETE).strength(3.0f, 30.0f))

// ========== 脚手架 ==========

/**
 * IC2 脚手架公共行为：原版脚手架式碰撞/攀爬、有限支撑传播、失稳掉落与强化交互。
 * 四种注册方块仍保持独立 ID，以兼容当前资源和配方。
 */
abstract class Ic2ScaffoldBlock(
    settings: BlockBehaviour.Settings,
    private val supportStrength: Int,
) : RotatedPillarBlock(settings) {

    /** IC2 脚手架始终竖直放置，不跟随点击面或玩家朝向旋转。 */
    override fun getPlacementState(ctx: BlockPlaceContext): BlockState =
        defaultState.with(AXIS, Direction.Axis.Y)

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getOutlineShape(state: BlockState, world: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape =
        if (context.isHolding(state.block.asItem())) {
            Shapes.fullCube()
        } else {
            if (isBottom(state, world, pos)) BOTTOM_OUTLINE_SHAPE else NORMAL_OUTLINE_SHAPE
        }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getRaycastShape(state: BlockState, world: BlockGetter, pos: BlockPos): VoxelShape =
        Shapes.fullCube()

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getCollisionShape(state: BlockState, world: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        if (context.isAbove(Shapes.fullCube(), pos, true) && !context.isDescending) {
            return NORMAL_OUTLINE_SHAPE
        }
        return if (isBottom(state, world, pos) && context.isAbove(OUTLINE_SHAPE, pos, true)) {
            COLLISION_SHAPE
        } else {
            Shapes.empty()
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBlockAdded(state: BlockState, world: Level, pos: BlockPos, oldState: BlockState, notify: Boolean) {
        super.onBlockAdded(state, world, pos, oldState, notify)
        if (!world.isClient) world.scheduleBlockTick(pos, this, 1)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getStateForNeighborUpdate(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        world: LevelAccessor,
        pos: BlockPos,
        neighborPos: BlockPos,
    ): BlockState {
        if (!world.isClient) world.scheduleBlockTick(pos, this, 1)
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun neighborUpdate(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        sourceBlock: Block,
        sourcePos: BlockPos,
        notify: Boolean,
    ) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify)
        if (!world.isClient) world.scheduleBlockTick(pos, this, 1)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun scheduledTick(state: BlockState, world: ServerLevel, pos: BlockPos, random: RandomSource) {
        checkSupport(world, pos)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun randomTick(state: BlockState, world: ServerLevel, pos: BlockPos, random: RandomSource) {
        if (random.nextInt(8) == 0) checkSupport(world, pos)
    }

    override fun canPlaceAt(state: BlockState, world: net.minecraft.world.level.LevelReader, pos: BlockPos): Boolean =
        hasSupportForPlacement(world, pos)

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onUse(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult,
    ): InteractionResult {
        if (world.isClient) return InteractionResult.SUCCESS
        if (player.isSneaking || !isVerticalPillar(world, pos)) return InteractionResult.PASS

        val stack = player.getStackInHand(hand)
        val target: BlockState
        val required: Int
        val sound: net.minecraft.sounds.SoundEvent
        when (this) {
            is WoodenScaffoldBlock -> {
                target = ReinforcedWoodenScaffoldBlock::class.instance().defaultState.with(AXIS, state.get(AXIS))
                required = 2
                if (!stack.isOf(Items.STICK) || stack.count < required) return InteractionResult.PASS
                sound = SoundEvents.BLOCK_WOOD_PLACE
            }
            is IronScaffoldBlock -> {
                target = ReinforcedIronScaffoldBlock::class.instance().defaultState.with(AXIS, state.get(AXIS))
                required = 1
                if (!stack.isOf(IronFenceBlock::class.item())) return InteractionResult.PASS
                sound = SoundEvents.BLOCK_METAL_PLACE
            }
            else -> return InteractionResult.PASS
        }

        world.setBlockState(pos, target, Block.NOTIFY_ALL)
        world.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0f, 1.0f)
        if (!player.abilities.creativeMode) stack.decrement(required)
        return InteractionResult.CONSUME
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getDroppedStacks(state: BlockState, builder: LootParams.Builder): MutableList<ItemStack> =
        when (this) {
            is ReinforcedWoodenScaffoldBlock -> mutableListOf(
                ItemStack(WoodenScaffoldBlock::class.item()),
                ItemStack(Items.STICK, 2),
            )
            is ReinforcedIronScaffoldBlock -> mutableListOf(
                ItemStack(IronScaffoldBlock::class.item()),
                ItemStack(IronFenceBlock::class.item()),
            )
            else -> mutableListOf(ItemStack(asItem()))
        }

    private fun isBottom(state: BlockState, world: BlockGetter, pos: BlockPos): Boolean =
        !isScaffold(world.getBlockState(pos.down()).block)

    private fun hasSupportForPlacement(world: BlockGetter, pos: BlockPos): Boolean {
        val below = pos.down()
        if (isSolidFoundation(world, below)) return true

        if (isScaffold(world.getBlockState(below).block)) {
            val component = connectedComponent(world, below)
            if ((calculateSupport(world, component)[below] ?: -1) >= 0) return true
        }

        return listOf(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)
            .map { pos.offset(it) }
            .any { neighbor ->
                isScaffold(world.getBlockState(neighbor).block) &&
                    calculateSupport(world, connectedComponent(world, neighbor))[neighbor]!! >= 0
            }
    }

    private fun isVerticalPillar(world: Level, pos: BlockPos): Boolean {
        var cursor = pos
        while (isScaffold(world.getBlockState(cursor).block)) cursor = cursor.down()
        return world.getBlockState(cursor).isSideSolidFullSquare(world, cursor, Direction.UP)
    }

    private fun checkSupport(world: ServerLevel, start: BlockPos) {
        if (!isScaffold(world.getBlockState(start).block)) return
        val component = connectedComponent(world, start)
        val support = calculateSupport(world, component)
        for (pos in component) {
            if ((support[pos] ?: -1) >= 0) continue
            val state = world.getBlockState(pos)
            if (!isScaffold(state.block)) continue
            Block.dropStacks(state, world, pos)
            world.setBlockState(pos, Blocks.AIR.defaultState, Block.NOTIFY_ALL)
        }
    }

    /**
     * 返回当前位置还能横向延伸的脚手架格数，供 Jade 与实际失稳判定共享。
     * 竖直搭建不消耗该余量；负数表示当前连通结构没有有效支撑。
     */
    fun remainingHorizontalSupport(world: BlockGetter, pos: BlockPos): Int {
        if (!isScaffold(world.getBlockState(pos).block)) return -1
        val component = connectedComponent(world, pos)
        return calculateSupport(world, component)[pos] ?: -1
    }

    private fun connectedComponent(world: BlockGetter, start: BlockPos): Set<BlockPos> {
        if (!isScaffold(world.getBlockState(start).block)) return emptySet()
        val result = linkedSetOf<BlockPos>()
        val queue = java.util.ArrayDeque<BlockPos>()
        result.add(start)
        queue.add(start)
        while (queue.isNotEmpty()) {
            val pos = queue.removeFirst()
            for (direction in Direction.values()) {
                val next = pos.offset(direction)
                if (!result.contains(next) && isScaffold(world.getBlockState(next).block)) {
                    result.add(next)
                    queue.add(next)
                }
            }
        }
        return result
    }

    private fun calculateSupport(world: BlockGetter, component: Set<BlockPos>): Map<BlockPos, Int> {
        val support = component.associateWith { -1 }.toMutableMap()
        var changed: Boolean
        do {
            changed = false
            for (pos in component) {
                val block = world.getBlockState(pos).block as? Ic2ScaffoldBlock ?: continue
                var best = support[pos] ?: -1
                val below = pos.down()
                if (isSolidFoundation(world, below)) {
                    best = maxOf(best, block.supportStrength)
                }

                // 竖直传播不消耗距离，横向传播每格递减。支撑余量由接地底座的材质决定：
                // 仅加固底座即可让上方/侧向整段按加固材质的上限延伸（对齐原版 IC2
                // “强化铁质脚手架可延伸 12 格”的语义），中继方块不再按自身材质钳制。
                if (isScaffold(world.getBlockState(below).block)) {
                    best = maxOf(best, support[below] ?: -1)
                }
                for (direction in listOf(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)) {
                    val neighborStrength = support[pos.offset(direction)] ?: -1
                    best = maxOf(best, neighborStrength - 1)
                }
                if (best > (support[pos] ?: -1)) {
                    support[pos] = best
                    changed = true
                }
            }
        } while (changed)
        return support
    }

    /** 脚手架只能传递已有支撑，不能因为自身顶面形状而被当成新的落地支点。 */
    private fun isSolidFoundation(world: BlockGetter, pos: BlockPos): Boolean {
        val state = world.getBlockState(pos)
        return !isScaffold(state.block) && state.isSideSolidFullSquare(world, pos, Direction.UP)
    }

    private fun isScaffold(block: Block): Boolean = block is Ic2ScaffoldBlock

    companion object {
        private val COLLISION_SHAPE: VoxelShape = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 2.0, 16.0)
        private val OUTLINE_SHAPE: VoxelShape = Shapes.fullCube().offset(0.0, -1.0, 0.0)
        private val NORMAL_OUTLINE_SHAPE: VoxelShape
        private val BOTTOM_OUTLINE_SHAPE: VoxelShape

        init {
            val top = Block.createCuboidShape(0.0, 14.0, 0.0, 16.0, 16.0, 16.0)
            val cornerA = Block.createCuboidShape(0.0, 0.0, 0.0, 2.0, 16.0, 2.0)
            val cornerB = Block.createCuboidShape(14.0, 0.0, 0.0, 16.0, 16.0, 2.0)
            val cornerC = Block.createCuboidShape(0.0, 0.0, 14.0, 2.0, 16.0, 16.0)
            val cornerD = Block.createCuboidShape(14.0, 0.0, 14.0, 16.0, 16.0, 16.0)
            NORMAL_OUTLINE_SHAPE = Shapes.union(top, cornerA, cornerB, cornerC, cornerD)
            val edgeA = Block.createCuboidShape(0.0, 0.0, 0.0, 2.0, 2.0, 16.0)
            val edgeB = Block.createCuboidShape(14.0, 0.0, 0.0, 16.0, 2.0, 16.0)
            val edgeC = Block.createCuboidShape(0.0, 0.0, 14.0, 16.0, 2.0, 16.0)
            val edgeD = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 2.0, 2.0)
            BOTTOM_OUTLINE_SHAPE = Shapes.union(COLLISION_SHAPE, NORMAL_OUTLINE_SHAPE, edgeA, edgeB, edgeC, edgeD)
        }
    }
}

@ModBlock(
    name = "wooden_scaffold",
    registerItem = true,
    tab = CreativeTab.IC2_MATERIALS,
    group = "scaffold",
    renderLayer = "cutout"
)
class WoodenScaffoldBlock(
    settings: BlockBehaviour.Settings = BlockBehaviour.Settings.copy(Blocks.OAK_PLANKS).strength(1.0f).nonOpaque()
) : Ic2ScaffoldBlock(settings, 2) {

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            ShapedRecipeBuilder.create(RecipeCategory.MISC, WoodenScaffoldBlock::class.item(), 4)
                .pattern("PPP")
                .pattern(" S ")
                .pattern("S S")
                .input('P', ItemTags.PLANKS)
                .input('S', Items.STICK)
                .criterion(hasItem(Items.OAK_PLANKS), conditionsFromItem(Items.OAK_PLANKS))
                .offerTo(exporter, WoodenScaffoldBlock::class.id())
        }
    }
}

@ModBlock(
    name = "reinforced_wooden_scaffold",
    registerItem = true,
    tab = CreativeTab.IC2_MATERIALS,
    group = "scaffold",
    renderLayer = "cutout"
)
class ReinforcedWoodenScaffoldBlock(
    settings: BlockBehaviour.Settings = BlockBehaviour.Settings.copy(Blocks.OAK_PLANKS).strength(2.0f).nonOpaque()
) : Ic2ScaffoldBlock(settings, 5)

@ModBlock(
    name = "iron_scaffold",
    registerItem = true,
    tab = CreativeTab.IC2_MATERIALS,
    group = "scaffold",
    renderLayer = "cutout"
)
class IronScaffoldBlock(
    settings: BlockBehaviour.Settings = BlockBehaviour.Settings.copy(Blocks.IRON_BLOCK).strength(3.0f).nonOpaque()
) : Ic2ScaffoldBlock(settings, 5) {

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            val ironPlate = IronPlate::class.instance()
            val ironFence = IronFenceBlock::class.item()
            ShapedRecipeBuilder.create(RecipeCategory.MISC, IronScaffoldBlock::class.item(), 16)
                .pattern("PPP")
                .pattern("FFF")
                .pattern("PPP")
                .input('P', Ingredient.fromTag(ModTags.Compat.Items.PLATES_IRON))
                .input('F', ironFence)
                .criterion(hasItem(ironPlate), conditionsFromItem(ironPlate))
                .criterion(hasItem(ironFence), conditionsFromItem(ironFence))
                .offerTo(exporter, IronScaffoldBlock::class.id())
        }
    }
}

@ModBlock(
    name = "reinforced_iron_scaffold",
    registerItem = true,
    tab = CreativeTab.IC2_MATERIALS,
    group = "scaffold",
    renderLayer = "cutout"
)
class ReinforcedIronScaffoldBlock(
    settings: BlockBehaviour.Settings = BlockBehaviour.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f).nonOpaque()
) : Ic2ScaffoldBlock(settings, 12)
