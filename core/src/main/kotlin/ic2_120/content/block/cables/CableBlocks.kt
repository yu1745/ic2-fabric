package ic2_120.content.block.cables

import ic2_120.content.item.energy.ITiered
import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.registry.annotation.ModBlock
import ic2_120.content.recipes.crafting.DamageToolShapelessRecipeDatagen
import ic2_120.content.block.energy.EnergyNetworkManager
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.item.Item
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.recipe.Ingredient
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.state.property.Properties
import net.minecraft.fluid.Fluids
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import net.minecraft.world.WorldAccess
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.network.PacketByteBuf
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import java.util.function.Consumer
import ic2_120.Ic2_120
import ic2_120.content.item.*
import ic2_120.content.screen.LimiterCableScreenHandler
import ic2_120.content.recipes.ModTags
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.id
import ic2_120.registry.recipeId
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import ic2_120.registry.annotation.RecipeProvider

/**
 * 锡质导线。低压传输，32 EU/t，损耗 0.2 EU/格。
 */
@ModBlock(name = "tin_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class TinCableBlock(settings: AbstractBlock.Settings = defaultSettings()) : BaseCableBlock(settings), ITiered {

    override val tier: Int = 1
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 200L

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = TinCableBlock::class.id(),
                result = TinCableBlock::class.item(),
                resultCount = 3,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(Cutter::class.instance()),
                    Ingredient.fromTag(ModTags.Compat.Items.PLATES_TIN)
                )
            )
        }
    }
}

/**
 * 铜质导线。中低压传输，128 EU/t，损耗 0.2 EU/格。
 */
@ModBlock(name = "copper_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class CopperCableBlock(settings: AbstractBlock.Settings = defaultSettings()) : BaseCableBlock(settings), ITiered {

    override val tier: Int = 2
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 200L

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<net.minecraft.data.server.recipe.RecipeJsonProvider>) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = CopperCableBlock::class.id(),
                result = CopperCableBlock::class.item(),
                resultCount = 2,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(Cutter::class.instance()),
                    Ingredient.fromTag(ModTags.Compat.Items.PLATES_COPPER)
                )
            )
        }
    }
}

/**
 * 金质导线。中压传输，512 EU/t，损耗 0.4 EU/格。比正常导线细。
 */
@ModBlock(name = "gold_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class GoldCableBlock(settings: AbstractBlock.Settings = defaultSettings()) : BaseCableBlock(settings), ITiered {

    override val tier: Int = 3
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 400L

    override fun getCableMin(): Double = 6.5 / 16.0
    override fun getCableMax(): Double = 9.5 / 16.0

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<net.minecraft.data.server.recipe.RecipeJsonProvider>) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = GoldCableBlock::class.id(),
                result = GoldCableBlock::class.item(),
                resultCount = 4,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(Cutter::class.instance()),
                    Ingredient.fromTag(ModTags.Compat.Items.PLATES_GOLD)
                )
            )
        }
    }
}

/**
 * 高压导线（铁质）。2048 EU/t，损耗 0.8 EU/格。碰撞箱较默认导线更粗。
 */
@ModBlock(name = "iron_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class IronCableBlock(settings: AbstractBlock.Settings = defaultSettings()) : BaseCableBlock(settings), ITiered {

    override val tier: Int = 4
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 800L

    override fun getCableMin(): Double = 5.0 / 16.0
    override fun getCableMax(): Double = 11.0 / 16.0

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<net.minecraft.data.server.recipe.RecipeJsonProvider>) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = IronCableBlock::class.id(),
                result = IronCableBlock::class.item(),
                resultCount = 2,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(Cutter::class.instance()),
                    Ingredient.fromTag(ModTags.Compat.Items.PLATES_IRON)
                )
            )
        }
    }
}

/**
 * 玻璃纤维导线。超高压传输，8192 EU/t，损耗仅 0.025 EU/格。最高绝缘，绝不漏电。
 */
@ModBlock(
    name = "glass_fibre_cable",
    registerItem = true,
    tab = CreativeTab.IC2_MATERIALS,
    group = "cables",
    renderLayer = "cutout_mipped"
)
class GlassFibreCableBlock(
    settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.GLASS).strength(0.5f).noCollision()
) : BaseCableBlock(settings), IInsulatedCable, ITiered {

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<net.minecraft.data.server.recipe.RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, GlassFibreCableBlock::class.item(), 6)
                .pattern("GGG")
                .pattern("ESE")
                .pattern("GGG")
                .input('G', Items.GLASS)
                .input('E', EnergiumDust::class.instance())
                .input('S', SilverDust::class.instance())
                .criterion(
                    hasItem(EnergiumDust::class.instance()),
                    conditionsFromItem(EnergiumDust::class.instance())
                )
                .offerTo(exporter, GlassFibreCableBlock::class.id())
        }
    }

    override val tier: Int = 5
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 25L
    override val insulationLevel: Int = 5
}

// ── 绝缘导线（损耗与裸线相同，传输率与对应裸线相同，仅防止触电） ────────────────────────

/**
 * 绝缘铜质导线。128 EU/t，损耗 0.2 EU/格。1 倍绝缘（≤128）。
 */
@ModBlock(name = "insulated_copper_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class InsulatedCopperCableBlock(settings: AbstractBlock.Settings = defaultSettings()) : BaseCableBlock(settings),
    IInsulatedCable, ITiered {

    override val tier: Int = 2
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 200L
    override val insulationLevel: Int = 2

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<net.minecraft.data.server.recipe.RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, InsulatedCopperCableBlock::class.item(), 1)
                .input(CopperCableBlock::class.item())
                .input(RubberItem::class.instance())
                .criterion(hasItem(CopperCableBlock::class.item()), conditionsFromItem(CopperCableBlock::class.item()))
                .offerTo(exporter, InsulatedCopperCableBlock::class.id())
            // 剪刀剥离绝缘层
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = InsulatedCopperCableBlock::class.recipeId("strip"),
                result = CopperCableBlock::class.item(),
                resultCount = 1,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(Cutter::class.instance()),
                    Ingredient.ofItems(InsulatedCopperCableBlock::class.item())
                )
            )
        }
    }
}

/**
 * 绝缘锡质导线。32 EU/t，损耗 0.2 EU/格。1 倍绝缘（≤128）。
 */
@ModBlock(name = "insulated_tin_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class InsulatedTinCableBlock(settings: AbstractBlock.Settings = defaultSettings()) : BaseCableBlock(settings),
    IInsulatedCable, ITiered {

    override val tier: Int = 1
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 200L
    override val insulationLevel: Int = 2

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<net.minecraft.data.server.recipe.RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, InsulatedTinCableBlock::class.item(), 1)
                .input(TinCableBlock::class.item())
                .input(RubberItem::class.instance())
                .criterion(hasItem(TinCableBlock::class.item()), conditionsFromItem(TinCableBlock::class.item()))
                .offerTo(exporter, InsulatedTinCableBlock::class.id())
            // 剪刀剥离绝缘层
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = InsulatedTinCableBlock::class.recipeId("strip"),
                result = TinCableBlock::class.item(),
                resultCount = 1,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(Cutter::class.instance()),
                    Ingredient.ofItems(InsulatedTinCableBlock::class.item())
                )
            )
        }
    }
}

/**
 * 绝缘金质导线。512 EU/t，损耗 0.4 EU/格。1 倍绝缘（≤128）。
 */
@ModBlock(name = "insulated_gold_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class InsulatedGoldCableBlock(settings: AbstractBlock.Settings = defaultSettings()) : BaseCableBlock(settings),
    IInsulatedCable, ITiered {

    override val tier: Int = 3
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 400L
    override val insulationLevel: Int = 2

    override fun getCableMin(): Double = 6.5 / 16.0
    override fun getCableMax(): Double = 9.5 / 16.0

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<net.minecraft.data.server.recipe.RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, InsulatedGoldCableBlock::class.item(), 1)
                .input(GoldCableBlock::class.item())
                .input(RubberItem::class.instance())
                .criterion(hasItem(GoldCableBlock::class.item()), conditionsFromItem(GoldCableBlock::class.item()))
                .offerTo(exporter, InsulatedGoldCableBlock::class.id())
            // 剪刀剥离绝缘层
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = InsulatedGoldCableBlock::class.recipeId("strip"),
                result = GoldCableBlock::class.item(),
                resultCount = 1,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(Cutter::class.instance()),
                    Ingredient.ofItems(InsulatedGoldCableBlock::class.item())
                )
            )
        }
    }
}

/**
 * 2x绝缘金质导线。512 EU/t，损耗 0.4 EU/格。2 倍绝缘（≤512）。
 */
@ModBlock(name = "double_insulated_gold_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class DoubleInsulatedGoldCableBlock(settings: AbstractBlock.Settings = defaultSettings()) : BaseCableBlock(settings),
    IInsulatedCable, ITiered {

    override val tier: Int = 3
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 400L
    override val insulationLevel: Int = 3

    override fun getCableMin(): Double = 6.5 / 16.0
    override fun getCableMax(): Double = 9.5 / 16.0

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<net.minecraft.data.server.recipe.RecipeJsonProvider>) {
            // 直接合成：金导线 + 2 橡胶
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, DoubleInsulatedGoldCableBlock::class.item(), 1)
                .input(GoldCableBlock::class.item())
                .input(RubberItem::class.instance())
                .input(RubberItem::class.instance())
                .criterion(hasItem(GoldCableBlock::class.item()), conditionsFromItem(GoldCableBlock::class.item()))
                .offerTo(exporter, DoubleInsulatedGoldCableBlock::class.id())
            // 递进合成：1x 绝缘金导线 + 1 橡胶
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, DoubleInsulatedGoldCableBlock::class.item(), 1)
                .input(InsulatedGoldCableBlock::class.item())
                .input(RubberItem::class.instance())
                .criterion(
                    hasItem(InsulatedGoldCableBlock::class.item()),
                    conditionsFromItem(InsulatedGoldCableBlock::class.item())
                )
                .offerTo(exporter, DoubleInsulatedGoldCableBlock::class.recipeId("from_insulated"))
            // 剪刀剥离绝缘层：2x → 1x
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = DoubleInsulatedGoldCableBlock::class.recipeId("strip"),
                result = InsulatedGoldCableBlock::class.item(),
                resultCount = 1,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(Cutter::class.instance()),
                    Ingredient.ofItems(DoubleInsulatedGoldCableBlock::class.item())
                )
            )
        }
    }
}

/**
 * 绝缘高压导线。2048 EU/t，损耗 0.8 EU/格。1 倍绝缘（≤128）。
 */
@ModBlock(name = "insulated_iron_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class InsulatedIronCableBlock(settings: AbstractBlock.Settings = defaultSettings()) : BaseCableBlock(settings),
    IInsulatedCable, ITiered {

    override val tier: Int = 4
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 800L
    override val insulationLevel: Int = 2

    override fun getCableMin(): Double = 5.0 / 16.0
    override fun getCableMax(): Double = 11.0 / 16.0

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<net.minecraft.data.server.recipe.RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, InsulatedIronCableBlock::class.item(), 1)
                .input(IronCableBlock::class.item())
                .input(RubberItem::class.instance())
                .criterion(hasItem(IronCableBlock::class.item()), conditionsFromItem(IronCableBlock::class.item()))
                .offerTo(exporter, InsulatedIronCableBlock::class.id())
            // 剪刀剥离绝缘层
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = InsulatedIronCableBlock::class.recipeId("strip"),
                result = IronCableBlock::class.item(),
                resultCount = 1,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(Cutter::class.instance()),
                    Ingredient.ofItems(InsulatedIronCableBlock::class.item())
                )
            )
        }
    }
}

/**
 * 2x绝缘高压导线。2048 EU/t，损耗 0.8 EU/格。2 倍绝缘（≤512）。
 */
@ModBlock(name = "double_insulated_iron_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class DoubleInsulatedIronCableBlock(settings: AbstractBlock.Settings = defaultSettings()) : BaseCableBlock(settings),
    IInsulatedCable, ITiered {

    override val tier: Int = 4
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 800L
    override val insulationLevel: Int = 3

    override fun getCableMin(): Double = 5.0 / 16.0
    override fun getCableMax(): Double = 11.0 / 16.0

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<net.minecraft.data.server.recipe.RecipeJsonProvider>) {
            // 直接合成：高压导线 + 2 橡胶
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, DoubleInsulatedIronCableBlock::class.item(), 1)
                .input(IronCableBlock::class.item())
                .input(RubberItem::class.instance())
                .input(RubberItem::class.instance())
                .criterion(hasItem(IronCableBlock::class.item()), conditionsFromItem(IronCableBlock::class.item()))
                .offerTo(exporter, DoubleInsulatedIronCableBlock::class.id())
            // 递进合成：1x 绝缘高压导线 + 1 橡胶
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, DoubleInsulatedIronCableBlock::class.item(), 1)
                .input(InsulatedIronCableBlock::class.item())
                .input(RubberItem::class.instance())
                .criterion(
                    hasItem(InsulatedIronCableBlock::class.item()),
                    conditionsFromItem(InsulatedIronCableBlock::class.item())
                )
                .offerTo(exporter, DoubleInsulatedIronCableBlock::class.recipeId("from_insulated"))
            // 剪刀剥离绝缘层：2x → 1x
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = DoubleInsulatedIronCableBlock::class.recipeId("strip"),
                result = InsulatedIronCableBlock::class.item(),
                resultCount = 1,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(Cutter::class.instance()),
                    Ingredient.ofItems(DoubleInsulatedIronCableBlock::class.item())
                )
            )
        }
    }
}

/**
 * 3x绝缘高压导线。2048 EU/t，损耗 0.8 EU/格。3 倍绝缘（≤2048）。
 */
@ModBlock(name = "triple_insulated_iron_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class TripleInsulatedIronCableBlock(settings: AbstractBlock.Settings = defaultSettings()) : BaseCableBlock(settings),
    IInsulatedCable, ITiered {

    override val tier: Int = 4
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 800L
    override val insulationLevel: Int = 4

    override fun getCableMin(): Double = 5.0 / 16.0
    override fun getCableMax(): Double = 11.0 / 16.0

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<net.minecraft.data.server.recipe.RecipeJsonProvider>) {
            // 直接合成：高压导线 + 3 橡胶
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, TripleInsulatedIronCableBlock::class.item(), 1)
                .input(IronCableBlock::class.item())
                .input(RubberItem::class.instance())
                .input(RubberItem::class.instance())
                .input(RubberItem::class.instance())
                .criterion(hasItem(IronCableBlock::class.item()), conditionsFromItem(IronCableBlock::class.item()))
                .offerTo(exporter, TripleInsulatedIronCableBlock::class.id())
            // 递进合成：2x 绝缘高压导线 + 1 橡胶
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, TripleInsulatedIronCableBlock::class.item(), 1)
                .input(DoubleInsulatedIronCableBlock::class.item())
                .input(RubberItem::class.instance())
                .criterion(
                    hasItem(DoubleInsulatedIronCableBlock::class.item()),
                    conditionsFromItem(DoubleInsulatedIronCableBlock::class.item())
                )
                .offerTo(exporter, TripleInsulatedIronCableBlock::class.recipeId("from_double_insulated"))
            // 剪刀剥离绝缘层：3x → 2x
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = TripleInsulatedIronCableBlock::class.recipeId("strip"),
                result = DoubleInsulatedIronCableBlock::class.item(),
                resultCount = 1,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(Cutter::class.instance()),
                    Ingredient.ofItems(TripleInsulatedIronCableBlock::class.item())
                )
            )
        }
    }
}

/**
 * EU分流导线。受红石控制：高电平时断开所有连接，低电平时正常连接。
 * Tier 5（8192 EU/t），损耗 0.5 EU/格。
 */
@ModBlock(name = "splitter_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class SplitterCableBlock(settings: AbstractBlock.Settings = defaultSettings()) : BaseCableBlock(settings), ITiered {

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, SplitterCableBlock::class.item(), 1)
                .pattern("RCR")
                .pattern("LRL")
                .pattern("RCR")
                .input('R', Items.REDSTONE)
                .input('C', Circuit::class.instance())
                .input('L', Items.LEVER)
                .criterion(hasItem(Circuit::class.instance()), conditionsFromItem(Circuit::class.instance()))
                .offerTo(exporter)
        }
    }

    override val tier: Int = 5
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 500L

    override fun getCableMin(): Double = 4.0 / 16.0
    override fun getCableMax(): Double = 12.0 / 16.0

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState {
        val powered = ctx.world.isReceivingRedstonePower(ctx.blockPos)
        val superState = super.getPlacementState(ctx)
        return if (powered) {
            var state = superState.with(ACTIVE, true)
            for (dir in Direction.values()) {
                state = state.with(propertyFor(dir), false)
            }
            state
        } else {
            superState.with(ACTIVE, false)
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun neighborUpdate(
        state: BlockState, world: World, pos: BlockPos,
        block: Block, sourcePos: BlockPos, notify: Boolean
    ) {
        if (world.isClient) {
            super.neighborUpdate(state, world, pos, block, sourcePos, notify)
            return
        }
        val powered = world.isReceivingRedstonePower(pos)
        val currentlyActive = state.get(ACTIVE)
        if (powered != currentlyActive) {
            var newState = state.with(ACTIVE, powered)
            if (powered) {
                for (dir in Direction.values()) {
                    newState = newState.with(propertyFor(dir), false)
                }
            } else {
                for (dir in Direction.values()) {
                    newState = newState.with(propertyFor(dir), canConnect(world, pos, dir))
                }
            }
            world.setBlockState(pos, newState)
            EnergyNetworkManager.invalidateAt(world, pos)
        }
        super.neighborUpdate(state, world, pos, block, sourcePos, notify)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getStateForNeighborUpdate(
        state: BlockState, direction: Direction, neighborState: BlockState,
        world: WorldAccess, pos: BlockPos, neighborPos: BlockPos
    ): BlockState {
        if (state.get(ACTIVE)) {
            if (state.get(Properties.WATERLOGGED)) {
                world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world))
            }
            return state.with(propertyFor(direction), false)
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos)
    }
}

/**
 * EU限流导线。右键打开 GUI 设置限流值，限制经过该导线的最大传输速率。
 * Tier 5（8192 EU/t），损耗 0.5 EU/格。
 */
@ModBlock(name = "limiter_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class LimiterCableBlock(settings: AbstractBlock.Settings = defaultSettings()) : BaseCableBlock(settings), ITiered {

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, LimiterCableBlock::class.item(), 1)
                .pattern(" C ")
                .pattern("RGR")
                .pattern(" C ")
                .input('C', Circuit::class.instance())
                .input('R', Items.REDSTONE)
                .input('G', GoldCableBlock::class.item())
                .criterion(hasItem(GoldCableBlock::class.item()), conditionsFromItem(GoldCableBlock::class.item()))
                .offerTo(exporter)
        }
    }

    override val tier: Int = 5
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 500L

    override fun getCableMin(): Double = 4.0 / 16.0
    override fun getCableMax(): Double = 12.0 / 16.0

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onUse(
        state: BlockState, world: World, pos: BlockPos,
        player: PlayerEntity, hand: Hand, hit: BlockHitResult
    ): ActionResult {
        if (world.isClient) return ActionResult.SUCCESS
        val be = world.getBlockEntity(pos) as? CableBlockEntity ?: return ActionResult.PASS
        player.openHandledScreen(object : ExtendedScreenHandlerFactory {
            override fun getDisplayName(): Text = Text.translatable("block.ic2_120.limiter_cable")
            override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
                buf.writeBlockPos(pos)
                buf.writeLong(be.configuredLimit)
            }
            override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity): ScreenHandler {
                val handler = LimiterCableScreenHandler(syncId, playerInventory, pos)
                handler.limit = be.configuredLimit
                return handler
            }
        })
        return ActionResult.SUCCESS
    }
}
