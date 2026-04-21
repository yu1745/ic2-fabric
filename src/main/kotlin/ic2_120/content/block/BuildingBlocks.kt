package ic2_120.content.block

import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.content.item.IronPlate
import ic2_120.content.item.Alloy
import ic2_120.content.item.Resin
import ic2_120.content.item.RubberItem
import ic2_120.content.item.Treetap
import ic2_120.content.recipes.crafting.ConsumeTreetapShapedRecipeDatagen
import ic2_120.registry.annotation.ModBlock
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockSetType
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.DoorBlock
import net.minecraft.block.PillarBlock
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Items
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.random.Random
import net.minecraft.registry.tag.ItemTags
import net.minecraft.world.World
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.recipe.book.RecipeCategory
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
    renderLayer = "cutout_mipped",
    generateBlockLootTable = false
)
class ReinforcedGlassBlock : Block(AbstractBlock.Settings.copy(Blocks.GLASS).strength(10.0f, 1200.0f).nonOpaque().dropsNothing()){
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val glass = Items.GLASS
            val alloy = Alloy::class.instance()
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ReinforcedGlassBlock::class.item(), 7)
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
    settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.IRON_DOOR).strength(25.0f, 1200.0f),
) : DoorBlock(settings, BlockSetType.IRON) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val stone = ReinforcedStoneBlock::class.item()
            val ironDoor = Items.IRON_DOOR
            ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, ReinforcedDoorBlock::class.item(), 1)
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

/**
 * 建筑泡沫：喷在 [IronScaffoldBlock] 上会得到 [ReinforcedFoamBlock]（见喷枪逻辑）。
 * 手持沙子右键或约 1 个 MC 日（24000 tick）后固化为浅灰建筑泡沫墙 [LightGrayWallBlock]。
 */
@ModBlock(name = "foam", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "building")
class FoamBlock : Block(
    AbstractBlock.Settings.copy(Blocks.WHITE_WOOL).strength(0.5f).ticksRandomly()
) {

    init {
        defaultState = defaultState.with(CURING_SCHEDULED, false)
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(CURING_SCHEDULED)
    }

    override fun onBlockAdded(state: BlockState, world: World, pos: BlockPos, oldState: BlockState, notify: Boolean) {
        super.onBlockAdded(state, world, pos, oldState, notify)
        if (world.isClient) return
        if (state.get(CURING_SCHEDULED)) return
        val sw = world as? ServerWorld ?: return
        sw.scheduleBlockTick(pos, this, MC_FULL_DAY_TICKS)
        sw.setBlockState(pos, state.with(CURING_SCHEDULED, true), Block.NOTIFY_LISTENERS)
    }

    override fun randomTick(state: BlockState, world: ServerWorld, pos: BlockPos, random: Random) {
        if (state.get(CURING_SCHEDULED)) return
        world.scheduleBlockTick(pos, this, MC_FULL_DAY_TICKS)
        world.setBlockState(pos, state.with(CURING_SCHEDULED, true), Block.NOTIFY_LISTENERS)
    }

    override fun scheduledTick(state: BlockState, world: ServerWorld, pos: BlockPos, random: Random) {
        if (world.getBlockState(pos).block !is FoamBlock) return
        val wall = LightGrayWallBlock::class.instance().defaultState
        world.setBlockState(pos, wall, Block.NOTIFY_ALL)
    }

    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult
    ): ActionResult {
        if (world.isClient) return ActionResult.SUCCESS
        val stack = player.getStackInHand(hand)
        if (!stack.isOf(Items.SAND)) return ActionResult.PASS
        val wall = LightGrayWallBlock::class.instance().defaultState
        world.setBlockState(pos, wall, Block.NOTIFY_ALL)
        world.playSound(null, pos, SoundEvents.BLOCK_SAND_PLACE, SoundCategory.BLOCKS, 0.85f, 0.9f)
        world.syncWorldEvent(2001, pos, Block.getRawIdFromState(state))
        if (!player.abilities.creativeMode) stack.decrement(1)
        return ActionResult.CONSUME
    }

    companion object {
        val CURING_SCHEDULED: BooleanProperty = BooleanProperty.of("curing_scheduled")
        private const val MC_FULL_DAY_TICKS = 24000
    }
}

/**
 * 强化建筑泡沫：由喷枪喷涂覆盖 [IronScaffoldBlock] 得到。
 * 沙子右键或约 1 MC 日后变为 [ReinforcedStoneBlock]（防爆石）。
 */
@ModBlock(name = "reinforced_foam", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "building")
class ReinforcedFoamBlock : Block(
    AbstractBlock.Settings.copy(Blocks.WHITE_WOOL).strength(1.2f).ticksRandomly()
) {

    init {
        defaultState = defaultState.with(CURING_SCHEDULED, false)
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(CURING_SCHEDULED)
    }

    override fun onBlockAdded(state: BlockState, world: World, pos: BlockPos, oldState: BlockState, notify: Boolean) {
        super.onBlockAdded(state, world, pos, oldState, notify)
        if (world.isClient) return
        if (state.get(CURING_SCHEDULED)) return
        val sw = world as? ServerWorld ?: return
        sw.scheduleBlockTick(pos, this, MC_FULL_DAY_TICKS)
        sw.setBlockState(pos, state.with(CURING_SCHEDULED, true), Block.NOTIFY_LISTENERS)
    }

    override fun randomTick(state: BlockState, world: ServerWorld, pos: BlockPos, random: Random) {
        if (state.get(CURING_SCHEDULED)) return
        world.scheduleBlockTick(pos, this, MC_FULL_DAY_TICKS)
        world.setBlockState(pos, state.with(CURING_SCHEDULED, true), Block.NOTIFY_LISTENERS)
    }

    override fun scheduledTick(state: BlockState, world: ServerWorld, pos: BlockPos, random: Random) {
        if (world.getBlockState(pos).block !is ReinforcedFoamBlock) return
        val stone = ReinforcedStoneBlock::class.instance().defaultState
        world.setBlockState(pos, stone, Block.NOTIFY_ALL)
    }

    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult
    ): ActionResult {
        if (world.isClient) return ActionResult.SUCCESS
        val stack = player.getStackInHand(hand)
        if (!stack.isOf(Items.SAND)) return ActionResult.PASS
        val stone = ReinforcedStoneBlock::class.instance().defaultState
        world.setBlockState(pos, stone, Block.NOTIFY_ALL)
        world.playSound(null, pos, SoundEvents.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 0.85f, 0.85f)
        world.syncWorldEvent(2001, pos, Block.getRawIdFromState(state))
        if (!player.abilities.creativeMode) stack.decrement(1)
        return ActionResult.CONSUME
    }

    companion object {
        val CURING_SCHEDULED: BooleanProperty = BooleanProperty.of("curing_scheduled")
        private const val MC_FULL_DAY_TICKS = 24000
    }
}

@ModBlock(name = "resin_sheet", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "building")
class ResinSheetBlock : Block(
    AbstractBlock.Settings.copy(Blocks.WHITE_CARPET)
        .strength(0.5f)
        .velocityMultiplier(0.4f)
        .jumpVelocityMultiplier(0.0f)
) {
    override fun onLandedUpon(world: World, state: net.minecraft.block.BlockState, pos: BlockPos, entity: Entity, fallDistance: Float) {
        entity.handleFallDamage(fallDistance, 0.2f, world.damageSources.fall())
    }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val resin = Resin::class.instance()
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ResinSheetBlock::class.item(), 3)
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
    AbstractBlock.Settings.copy(Blocks.WHITE_CARPET)
        .strength(0.5f)
        .velocityMultiplier(0.4f)
        .jumpVelocityMultiplier(0.0f)
) {
    override fun onLandedUpon(world: World, state: net.minecraft.block.BlockState, pos: BlockPos, entity: Entity, fallDistance: Float) {
        entity.handleFallDamage(fallDistance, 0.2f, world.damageSources.fall())
    }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val rubber = RubberItem::class.instance()
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, RubberSheetBlock::class.item(), 3)
                .pattern("xxx")
                .pattern("xxx")
                .input('x', rubber)
                .criterion(hasItem(rubber), conditionsFromItem(rubber))
                .offerTo(exporter, RubberSheetBlock::class.id())
        }
    }
}

@ModBlock(name = "wool_sheet", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "building")
class WoolSheetBlock : Block(
    AbstractBlock.Settings.copy(Blocks.WHITE_CARPET)
        .strength(0.5f)
        .velocityMultiplier(0.4f)
        .jumpVelocityMultiplier(0.0f)
) {
    override fun onLandedUpon(world: World, state: net.minecraft.block.BlockState, pos: BlockPos, entity: Entity, fallDistance: Float) {
        entity.handleFallDamage(fallDistance, 0.2f, world.damageSources.fall())
    }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, WoolSheetBlock::class.item(), 3)
                .pattern("xxx")
                .pattern("xxx")
                .input('x', ItemTags.WOOL)
                .criterion(hasItem(Items.WHITE_WOOL), conditionsFromItem(Items.WHITE_WOOL))
                .offerTo(exporter, WoolSheetBlock::class.id())
        }
    }
}

@ModBlock(name = "mining_pipe", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "building")
class MiningPipeBlock : PillarBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(3.0f)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
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
class ItntBlock : Block(AbstractBlock.Settings.copy(Blocks.TNT).strength(0.0f))

// ========== 建筑泡沫墙（16 色） ==========

@ModBlock(name = "white_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class WhiteWallBlock : Block(AbstractBlock.Settings.copy(Blocks.WHITE_CONCRETE).strength(2.0f))

@ModBlock(name = "orange_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class OrangeWallBlock : Block(AbstractBlock.Settings.copy(Blocks.ORANGE_CONCRETE).strength(2.0f))

@ModBlock(name = "magenta_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class MagentaWallBlock : Block(AbstractBlock.Settings.copy(Blocks.MAGENTA_CONCRETE).strength(2.0f))

@ModBlock(name = "light_blue_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class LightBlueWallBlock : Block(AbstractBlock.Settings.copy(Blocks.LIGHT_BLUE_CONCRETE).strength(2.0f))

@ModBlock(name = "yellow_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class YellowWallBlock : Block(AbstractBlock.Settings.copy(Blocks.YELLOW_CONCRETE).strength(2.0f))

@ModBlock(name = "lime_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class LimeWallBlock : Block(AbstractBlock.Settings.copy(Blocks.LIME_CONCRETE).strength(2.0f))

@ModBlock(name = "pink_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class PinkWallBlock : Block(AbstractBlock.Settings.copy(Blocks.PINK_CONCRETE).strength(2.0f))

@ModBlock(name = "gray_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class GrayWallBlock : Block(AbstractBlock.Settings.copy(Blocks.GRAY_CONCRETE).strength(2.0f))

@ModBlock(name = "light_gray_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class LightGrayWallBlock : Block(AbstractBlock.Settings.copy(Blocks.LIGHT_GRAY_CONCRETE).strength(2.0f))

@ModBlock(name = "cyan_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class CyanWallBlock : Block(AbstractBlock.Settings.copy(Blocks.CYAN_CONCRETE).strength(2.0f))

@ModBlock(name = "purple_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class PurpleWallBlock : Block(AbstractBlock.Settings.copy(Blocks.PURPLE_CONCRETE).strength(2.0f))

@ModBlock(name = "blue_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class BlueWallBlock : Block(AbstractBlock.Settings.copy(Blocks.BLUE_CONCRETE).strength(2.0f))

@ModBlock(name = "brown_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class BrownWallBlock : Block(AbstractBlock.Settings.copy(Blocks.BROWN_CONCRETE).strength(2.0f))

@ModBlock(name = "green_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class GreenWallBlock : Block(AbstractBlock.Settings.copy(Blocks.GREEN_CONCRETE).strength(2.0f))

@ModBlock(name = "red_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class RedWallBlock : Block(AbstractBlock.Settings.copy(Blocks.RED_CONCRETE).strength(2.0f))

@ModBlock(name = "black_wall", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "wall")
class BlackWallBlock : Block(AbstractBlock.Settings.copy(Blocks.BLACK_CONCRETE).strength(2.0f))

// ========== 脚手架 ==========

@ModBlock(
    name = "wooden_scaffold",
    registerItem = true,
    tab = CreativeTab.IC2_MATERIALS,
    group = "scaffold",
    renderLayer = "cutout"
)
class WoodenScaffoldBlock(
    settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).strength(1.0f).nonOpaque()
) : PillarBlock(settings) {
    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult
    ): ActionResult {
        if (world.isClient) return ActionResult.SUCCESS
        val stack = player.getStackInHand(hand)
        if (!stack.isOf(Items.STICK) || stack.count <= 2) return ActionResult.PASS
        world.setBlockState(pos, ReinforcedWoodenScaffoldBlock::class.instance().defaultState.with(AXIS, state.get(AXIS)), Block.NOTIFY_ALL)
        world.playSound(null, pos, SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.BLOCKS, 1.0f, 1.0f)
        if (!player.abilities.creativeMode) stack.decrement(2)
        return ActionResult.CONSUME
    }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, WoodenScaffoldBlock::class.item(), 4)
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
    settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).strength(2.0f).nonOpaque()
) : PillarBlock(settings)

@ModBlock(
    name = "iron_scaffold",
    registerItem = true,
    tab = CreativeTab.IC2_MATERIALS,
    group = "scaffold",
    renderLayer = "cutout"
)
class IronScaffoldBlock(
    settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(3.0f).nonOpaque()
) : PillarBlock(settings) {
    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult
    ): ActionResult {
        if (world.isClient) return ActionResult.SUCCESS
        val stack = player.getStackInHand(hand)
        if (!stack.isOf(IronFenceBlock::class.item())) return ActionResult.PASS
        world.setBlockState(pos, ReinforcedIronScaffoldBlock::class.instance().defaultState.with(AXIS, state.get(AXIS)), Block.NOTIFY_ALL)
        world.playSound(null, pos, SoundEvents.BLOCK_METAL_PLACE, SoundCategory.BLOCKS, 1.0f, 1.0f)
        if (!player.abilities.creativeMode) stack.decrement(1)
        return ActionResult.CONSUME
    }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val ironPlate = IronPlate::class.instance()
            val ironFence = IronFenceBlock::class.item()
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, IronScaffoldBlock::class.item(), 16)
                .pattern("PPP")
                .pattern("FFF")
                .pattern("PPP")
                .input('P', ironPlate)
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
    settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f).nonOpaque()
) : PillarBlock(settings)
