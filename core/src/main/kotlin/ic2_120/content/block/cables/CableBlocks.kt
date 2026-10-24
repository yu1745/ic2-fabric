package ic2_120.content.block.cables

import net.minecraft.network.FriendlyByteBuf

import io.netty.buffer.Unpooled

import net.minecraft.data.recipes.RecipeProvider.has
import net.minecraft.data.recipes.RecipeProvider.getHasName
import com.mojang.serialization.MapCodec
import net.minecraft.world.level.block.BaseEntityBlock

import ic2_120.compat.isClient
import ic2_120.compat.world

import ic2_120.content.item.energy.ITiered
import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.registry.annotation.ModBlock
import ic2_120.content.recipes.crafting.DamageToolShapelessRecipeDatagen
import ic2_120.content.block.energy.EnergyNetworkManager
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.data.recipes.ShapelessRecipeBuilder
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.material.Fluids
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.server.level.ServerPlayer
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import java.util.function.Consumer
import ic2_120.Ic2_120
import ic2_120.content.item.*
import ic2_120.content.screen.LimiterCableScreenHandler
import ic2_120.content.screen.SplitterCableScreenHandler
import ic2_120.content.recipes.ModTags
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.id
import ic2_120.registry.recipeId
import ic2_120.registry.annotation.RecipeProvider

/**
 * 锡质导线。低压传输，32 EU/t，损耗 0.2 EU/格。
 */
@ModBlock(name = "tin_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class TinCableBlock(settings: BlockBehaviour.Properties = defaultSettings()) : BaseCableBlock(settings), ITiered {
    override fun codec(): MapCodec<out BaseEntityBlock> = BlockBehaviour.simpleCodec { TinCableBlock() }

    override val tier: Int = 1
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 200L

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = TinCableBlock::class.id(),
                result = TinCableBlock::class.item(),
                resultCount = 3,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(Cutter::class.instance()),
                    Ingredient.of(ModTags.Compat.Items.PLATES_TIN)
                )
            )
        }
    }
}

/**
 * 铜质导线。中低压传输，128 EU/t，损耗 0.2 EU/格。
 */
@ModBlock(name = "copper_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class CopperCableBlock(settings: BlockBehaviour.Properties = defaultSettings()) : BaseCableBlock(settings), ITiered {
    override fun codec(): MapCodec<out BaseEntityBlock> = BlockBehaviour.simpleCodec { CopperCableBlock() }

    override val tier: Int = 2
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 200L

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = CopperCableBlock::class.id(),
                result = CopperCableBlock::class.item(),
                resultCount = 2,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(Cutter::class.instance()),
                    Ingredient.of(ModTags.Compat.Items.PLATES_COPPER)
                )
            )
        }
    }
}

/**
 * 金质导线。中压传输，512 EU/t，损耗 0.4 EU/格。比正常导线细。
 */
@ModBlock(name = "gold_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class GoldCableBlock(settings: BlockBehaviour.Properties = defaultSettings()) : BaseCableBlock(settings), ITiered {
    override fun codec(): MapCodec<out BaseEntityBlock> = BlockBehaviour.simpleCodec { GoldCableBlock() }

    override val tier: Int = 3
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 400L

    override fun getCableMin(): Double = 6.5 / 16.0
    override fun getCableMax(): Double = 9.5 / 16.0

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = GoldCableBlock::class.id(),
                result = GoldCableBlock::class.item(),
                resultCount = 4,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(Cutter::class.instance()),
                    Ingredient.of(ModTags.Compat.Items.PLATES_GOLD)
                )
            )
        }
    }
}

/**
 * 高压导线（铁质）。2048 EU/t，损耗 0.8 EU/格。碰撞箱较默认导线更粗。
 */
@ModBlock(name = "iron_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class IronCableBlock(settings: BlockBehaviour.Properties = defaultSettings()) : BaseCableBlock(settings), ITiered {
    override fun codec(): MapCodec<out BaseEntityBlock> = BlockBehaviour.simpleCodec { IronCableBlock() }

    override val tier: Int = 4
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 800L

    override fun getCableMin(): Double = 5.0 / 16.0
    override fun getCableMax(): Double = 11.0 / 16.0

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = IronCableBlock::class.id(),
                result = IronCableBlock::class.item(),
                resultCount = 2,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(Cutter::class.instance()),
                    Ingredient.of(ModTags.Compat.Items.PLATES_IRON)
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
    settings: BlockBehaviour.Properties = BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(0.5f).noCollission()
) : BaseCableBlock(settings), IInsulatedCable, ITiered {
    override fun codec(): MapCodec<out BaseEntityBlock> = BlockBehaviour.simpleCodec { GlassFibreCableBlock() }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, GlassFibreCableBlock::class.item(), 6)
                .pattern("GGG")
                .pattern("ESE")
                .pattern("GGG")
                .define('G', Items.GLASS)
                .define('E', EnergiumDust::class.instance())
                .define('S', Ingredient.of(ModTags.Compat.Items.DUSTS_SILVER))
                .unlockedBy(
                    getHasName(EnergiumDust::class.instance()),
                    has(EnergiumDust::class.instance())
                )
                .save(exporter, GlassFibreCableBlock::class.id())
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
class InsulatedCopperCableBlock(settings: BlockBehaviour.Properties = defaultSettings()) : BaseCableBlock(settings),
    IInsulatedCable, ITiered {
    override fun codec(): MapCodec<out BaseEntityBlock> = BlockBehaviour.simpleCodec { InsulatedCopperCableBlock() }

    override val tier: Int = 2
   override fun getTransferRate(): Long = nominalEuPerTick()
   override fun getEnergyLoss(): Long = 200L
  override val insulationLevel: Int = 2

   override fun getCableMin(): Double = 5.0 / 16.0
   override fun getCableMax(): Double = 11.0 / 16.0

   companion object {
       @RecipeProvider
       fun generateRecipes(exporter: RecipeOutput) {
           ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, InsulatedCopperCableBlock::class.item(), 1)
                .requires(CopperCableBlock::class.item())
                .requires(Ingredient.of(ModTags.Compat.Items.RUBBER))
                .unlockedBy(getHasName(CopperCableBlock::class.item()), has(CopperCableBlock::class.item()))
                .save(exporter, InsulatedCopperCableBlock::class.id())
            // 剪刀剥离绝缘层
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = InsulatedCopperCableBlock::class.recipeId("strip"),
                result = CopperCableBlock::class.item(),
                resultCount = 1,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(Cutter::class.instance()),
                    Ingredient.of(InsulatedCopperCableBlock::class.item())
                )
            )
        }
    }
}

/**
 * 绝缘锡质导线。32 EU/t，损耗 0.2 EU/格。1 倍绝缘（≤128）。
 */
@ModBlock(name = "insulated_tin_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class InsulatedTinCableBlock(settings: BlockBehaviour.Properties = defaultSettings()) : BaseCableBlock(settings),
    IInsulatedCable, ITiered {
    override fun codec(): MapCodec<out BaseEntityBlock> = BlockBehaviour.simpleCodec { InsulatedTinCableBlock() }

    override val tier: Int = 1
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 200L
    override val insulationLevel: Int = 2
    override fun getCableMin(): Double = 5.0 / 16.0
    override fun getCableMax(): Double = 11.0 / 16.0

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, InsulatedTinCableBlock::class.item(), 1)
                .requires(TinCableBlock::class.item())
                .requires(Ingredient.of(ModTags.Compat.Items.RUBBER))
                .unlockedBy(getHasName(TinCableBlock::class.item()), has(TinCableBlock::class.item()))
                .save(exporter, InsulatedTinCableBlock::class.id())
            // 剪刀剥离绝缘层
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = InsulatedTinCableBlock::class.recipeId("strip"),
                result = TinCableBlock::class.item(),
                resultCount = 1,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(Cutter::class.instance()),
                    Ingredient.of(InsulatedTinCableBlock::class.item())
                )
            )
        }
    }
}

/**
 * 绝缘金质导线。512 EU/t，损耗 0.4 EU/格。1 倍绝缘（≤128）。
 */
@ModBlock(name = "insulated_gold_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class InsulatedGoldCableBlock(settings: BlockBehaviour.Properties = defaultSettings()) : BaseCableBlock(settings),
    IInsulatedCable, ITiered {
    override fun codec(): MapCodec<out BaseEntityBlock> = BlockBehaviour.simpleCodec { InsulatedGoldCableBlock() }

    override val tier: Int = 3
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 400L
    override val insulationLevel: Int = 2

    override fun getCableMin(): Double = 5.5 / 16.0
    override fun getCableMax(): Double = 10.5 / 16.0

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, InsulatedGoldCableBlock::class.item(), 1)
                .requires(GoldCableBlock::class.item())
                .requires(Ingredient.of(ModTags.Compat.Items.RUBBER))
                .unlockedBy(getHasName(GoldCableBlock::class.item()), has(GoldCableBlock::class.item()))
                .save(exporter, InsulatedGoldCableBlock::class.id())
            // 剪刀剥离绝缘层
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = InsulatedGoldCableBlock::class.recipeId("strip"),
                result = GoldCableBlock::class.item(),
                resultCount = 1,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(Cutter::class.instance()),
                    Ingredient.of(InsulatedGoldCableBlock::class.item())
                )
            )
        }
    }
}

/**
 * 2x绝缘金质导线。512 EU/t，损耗 0.4 EU/格。2 倍绝缘（≤512）。
 */
@ModBlock(name = "double_insulated_gold_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class DoubleInsulatedGoldCableBlock(settings: BlockBehaviour.Properties = defaultSettings()) : BaseCableBlock(settings),
    IInsulatedCable, ITiered {
    override fun codec(): MapCodec<out BaseEntityBlock> = BlockBehaviour.simpleCodec { DoubleInsulatedGoldCableBlock() }

    override val tier: Int = 3
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 400L
    override val insulationLevel: Int = 3

    override fun getCableMin(): Double = 4.5 / 16.0
    override fun getCableMax(): Double = 11.5 / 16.0

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // 直接合成：金导线 + 2 橡胶
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, DoubleInsulatedGoldCableBlock::class.item(), 1)
                .requires(GoldCableBlock::class.item())
                .requires(Ingredient.of(ModTags.Compat.Items.RUBBER))
                .requires(Ingredient.of(ModTags.Compat.Items.RUBBER))
                .unlockedBy(getHasName(GoldCableBlock::class.item()), has(GoldCableBlock::class.item()))
                .save(exporter, DoubleInsulatedGoldCableBlock::class.id())
            // 递进合成：1x 绝缘金导线 + 1 橡胶
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, DoubleInsulatedGoldCableBlock::class.item(), 1)
                .requires(InsulatedGoldCableBlock::class.item())
                .requires(Ingredient.of(ModTags.Compat.Items.RUBBER))
                .unlockedBy(
                    getHasName(InsulatedGoldCableBlock::class.item()),
                    has(InsulatedGoldCableBlock::class.item())
                )
                .save(exporter, DoubleInsulatedGoldCableBlock::class.recipeId("from_insulated"))
            // 剪刀剥离绝缘层：2x → 1x
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = DoubleInsulatedGoldCableBlock::class.recipeId("strip"),
                result = InsulatedGoldCableBlock::class.item(),
                resultCount = 1,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(Cutter::class.instance()),
                    Ingredient.of(DoubleInsulatedGoldCableBlock::class.item())
                )
            )
        }
    }
}

/**
 * 绝缘高压导线。2048 EU/t，损耗 0.8 EU/格。1 倍绝缘（≤128）。
 */
@ModBlock(name = "insulated_iron_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class InsulatedIronCableBlock(settings: BlockBehaviour.Properties = defaultSettings()) : BaseCableBlock(settings),
    IInsulatedCable, ITiered {
    override fun codec(): MapCodec<out BaseEntityBlock> = BlockBehaviour.simpleCodec { InsulatedIronCableBlock() }

    override val tier: Int = 4
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 800L
    override val insulationLevel: Int = 2

    override fun getCableMin(): Double = 4.0 / 16.0
    override fun getCableMax(): Double = 12.0 / 16.0

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, InsulatedIronCableBlock::class.item(), 1)
                .requires(IronCableBlock::class.item())
                .requires(Ingredient.of(ModTags.Compat.Items.RUBBER))
                .unlockedBy(getHasName(IronCableBlock::class.item()), has(IronCableBlock::class.item()))
                .save(exporter, InsulatedIronCableBlock::class.id())
            // 剪刀剥离绝缘层
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = InsulatedIronCableBlock::class.recipeId("strip"),
                result = IronCableBlock::class.item(),
                resultCount = 1,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(Cutter::class.instance()),
                    Ingredient.of(InsulatedIronCableBlock::class.item())
                )
            )
        }
    }
}

/**
 * 2x绝缘高压导线。2048 EU/t，损耗 0.8 EU/格。2 倍绝缘（≤512）。
 */
@ModBlock(name = "double_insulated_iron_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class DoubleInsulatedIronCableBlock(settings: BlockBehaviour.Properties = defaultSettings()) : BaseCableBlock(settings),
    IInsulatedCable, ITiered {
    override fun codec(): MapCodec<out BaseEntityBlock> = BlockBehaviour.simpleCodec { DoubleInsulatedIronCableBlock() }

    override val tier: Int = 4
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 800L
    override val insulationLevel: Int = 3

    override fun getCableMin(): Double = 3.0 / 16.0
    override fun getCableMax(): Double = 13.0 / 16.0

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // 直接合成：高压导线 + 2 橡胶
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, DoubleInsulatedIronCableBlock::class.item(), 1)
                .requires(IronCableBlock::class.item())
                .requires(Ingredient.of(ModTags.Compat.Items.RUBBER))
                .requires(Ingredient.of(ModTags.Compat.Items.RUBBER))
                .unlockedBy(getHasName(IronCableBlock::class.item()), has(IronCableBlock::class.item()))
                .save(exporter, DoubleInsulatedIronCableBlock::class.id())
            // 递进合成：1x 绝缘高压导线 + 1 橡胶
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, DoubleInsulatedIronCableBlock::class.item(), 1)
                .requires(InsulatedIronCableBlock::class.item())
                .requires(Ingredient.of(ModTags.Compat.Items.RUBBER))
                .unlockedBy(
                    getHasName(InsulatedIronCableBlock::class.item()),
                    has(InsulatedIronCableBlock::class.item())
                )
                .save(exporter, DoubleInsulatedIronCableBlock::class.recipeId("from_insulated"))
            // 剪刀剥离绝缘层：2x → 1x
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = DoubleInsulatedIronCableBlock::class.recipeId("strip"),
                result = InsulatedIronCableBlock::class.item(),
                resultCount = 1,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(Cutter::class.instance()),
                    Ingredient.of(DoubleInsulatedIronCableBlock::class.item())
                )
            )
        }
    }
}

/**
 * 3x绝缘高压导线。2048 EU/t，损耗 0.8 EU/格。3 倍绝缘（≤2048）。
 */
@ModBlock(name = "triple_insulated_iron_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class TripleInsulatedIronCableBlock(settings: BlockBehaviour.Properties = defaultSettings()) : BaseCableBlock(settings),
    IInsulatedCable, ITiered {
    override fun codec(): MapCodec<out BaseEntityBlock> = BlockBehaviour.simpleCodec { TripleInsulatedIronCableBlock() }

    override val tier: Int = 4
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 800L
    override val insulationLevel: Int = 4

    override fun getCableMin(): Double = 2.0 / 16.0
    override fun getCableMax(): Double = 14.0 / 16.0

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // 直接合成：高压导线 + 3 橡胶
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, TripleInsulatedIronCableBlock::class.item(), 1)
                .requires(IronCableBlock::class.item())
                .requires(Ingredient.of(ModTags.Compat.Items.RUBBER))
                .requires(Ingredient.of(ModTags.Compat.Items.RUBBER))
                .requires(Ingredient.of(ModTags.Compat.Items.RUBBER))
                .unlockedBy(getHasName(IronCableBlock::class.item()), has(IronCableBlock::class.item()))
                .save(exporter, TripleInsulatedIronCableBlock::class.id())
            // 递进合成：2x 绝缘高压导线 + 1 橡胶
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, TripleInsulatedIronCableBlock::class.item(), 1)
                .requires(DoubleInsulatedIronCableBlock::class.item())
                .requires(Ingredient.of(ModTags.Compat.Items.RUBBER))
                .unlockedBy(
                    getHasName(DoubleInsulatedIronCableBlock::class.item()),
                    has(DoubleInsulatedIronCableBlock::class.item())
                )
                .save(exporter, TripleInsulatedIronCableBlock::class.recipeId("from_double_insulated"))
            // 剪刀剥离绝缘层：3x → 2x
            DamageToolShapelessRecipeDatagen.offer(
                exporter = exporter,
                recipeId = TripleInsulatedIronCableBlock::class.recipeId("strip"),
                result = DoubleInsulatedIronCableBlock::class.item(),
                resultCount = 1,
                ingredients = listOf(
                    DamageToolShapelessRecipeDatagen.toolIngredient(Cutter::class.instance()),
                    Ingredient.of(TripleInsulatedIronCableBlock::class.item())
                )
            )
        }
    }
}

/**
 * EU分流导线。红石信号达到可配置阈值时断开所有连接，并支持反相控制。
 * Tier 5（8192 EU/t），损耗 0.5 EU/格。
 */
@ModBlock(name = "splitter_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class SplitterCableBlock(settings: BlockBehaviour.Properties = defaultSettings()) : BaseCableBlock(settings), ITiered {
    override fun codec(): MapCodec<out BaseEntityBlock> = BlockBehaviour.simpleCodec { SplitterCableBlock() }

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.create("active")

        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SplitterCableBlock::class.item(), 1)
                .pattern("RCR")
                .pattern("LRL")
                .pattern("RCR")
                .define('R', Items.REDSTONE)
                .define('C', Circuit::class.instance())
                .define('L', Items.LEVER)
                .unlockedBy(getHasName(Circuit::class.instance()), has(Circuit::class.instance()))
                .save(exporter)
        }
    }

    override val tier: Int = 5
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 500L

    override fun getCableMin(): Double = 4.0 / 16.0
    override fun getCableMax(): Double = 12.0 / 16.0

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(ACTIVE)
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState {
        val active = ctx.level.getBestNeighborSignal(ctx.clickedPos) >= CableBlockEntity.DEFAULT_SPLITTER_THRESHOLD
        val superState = super.getStateForPlacement(ctx)
        return if (active) {
            var state = superState.setValue(ACTIVE, true)
            for (dir in Direction.values()) {
                state = state.setValue(propertyFor(dir), false)
            }
            state
        } else {
            superState.setValue(ACTIVE, false)
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun neighborChanged(
        state: BlockState, world: Level, pos: BlockPos,
        block: Block, sourcePos: BlockPos, notify: Boolean
    ) {
        if (world.isClient) {
            super.neighborChanged(state, world, pos, block, sourcePos, notify)
            return
        }
        refreshControlState(world, pos, state)
        super.neighborChanged(state, world, pos, block, sourcePos, notify)
    }

    /** 按方块实体中的阈值与反相配置刷新断开状态。 */
    fun refreshControlState(world: Level, pos: BlockPos, state: BlockState = world.getBlockState(pos)) {
        if (world.isClient || !state.`is`(this)) return
        val be = world.getBlockEntity(pos) as? CableBlockEntity
        val threshold = be?.splitterThreshold ?: CableBlockEntity.DEFAULT_SPLITTER_THRESHOLD
        val inverted = be?.splitterInverted ?: false
        val thresholdReached = world.getBestNeighborSignal(pos) >= threshold
        val shouldDisconnect = if (inverted) !thresholdReached else thresholdReached
        if (shouldDisconnect == state.getValue(ACTIVE)) return

        var newState = state.setValue(ACTIVE, shouldDisconnect)
        for (dir in Direction.values()) {
            newState = newState.setValue(propertyFor(dir), !shouldDisconnect && canConnect(world, pos, dir))
        }
        world.setBlock(pos, newState, Block.UPDATE_ALL)
        EnergyNetworkManager.invalidateAt(world, pos)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (world.isClient) return InteractionResult.SUCCESS
        val be = world.getBlockEntity(pos) as? CableBlockEntity ?: return InteractionResult.PASS
        player.openMenu(object : ExtendedScreenHandlerFactory<FriendlyByteBuf> {
            override fun getDisplayName(): Component = Component.translatable("block.ic2_120.splitter_cable")

            override fun getScreenOpeningData(player: ServerPlayer): FriendlyByteBuf {
                val buf = FriendlyByteBuf(Unpooled.buffer())
                    buf.writeBlockPos(pos)
                    buf.writeVarInt(be.splitterThreshold)
                    buf.writeBoolean(be.splitterInverted)
                return buf
            }

            override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player): AbstractContainerMenu {
                return SplitterCableScreenHandler(syncId, playerInventory, pos).also {
                    it.threshold = be.splitterThreshold
                    it.inverted = be.splitterInverted
                }
            }
        })
        return InteractionResult.SUCCESS
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun updateShape(
        state: BlockState, direction: Direction, neighborState: BlockState,
        world: LevelAccessor, pos: BlockPos, neighborPos: BlockPos
    ): BlockState {
        if (state.getValue(ACTIVE)) {
            if (state.getValue(BlockStateProperties.WATERLOGGED)) {
                world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world))
            }
            return state.setValue(propertyFor(direction), false)
        }
        return super.updateShape(state, direction, neighborState, world, pos, neighborPos)
    }
}

/**
 * EU限流导线。右键打开 GUI 设置限流值，限制经过该导线的最大传输速率。
 * Tier 5（8192 EU/t），损耗 0.5 EU/格。
 */
@ModBlock(name = "limiter_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "cables")
class LimiterCableBlock(settings: BlockBehaviour.Properties = defaultSettings()) : BaseCableBlock(settings), ITiered {
    override fun codec(): MapCodec<out BaseEntityBlock> = BlockBehaviour.simpleCodec { LimiterCableBlock() }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, LimiterCableBlock::class.item(), 1)
                .pattern(" C ")
                .pattern("RGR")
                .pattern(" C ")
                .define('C', Circuit::class.instance())
                .define('R', Items.REDSTONE)
                .define('G', GoldCableBlock::class.item())
                .unlockedBy(getHasName(GoldCableBlock::class.item()), has(GoldCableBlock::class.item()))
                .save(exporter)
        }
    }

    override val tier: Int = 5
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 500L

    override fun getCableMin(): Double = 4.0 / 16.0
    override fun getCableMax(): Double = 12.0 / 16.0

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (world.isClient) return InteractionResult.SUCCESS
        val be = world.getBlockEntity(pos) as? CableBlockEntity ?: return InteractionResult.PASS
        player.openMenu(object : ExtendedScreenHandlerFactory<FriendlyByteBuf> {
            override fun getDisplayName(): Component = Component.translatable("block.ic2_120.limiter_cable")
            override fun getScreenOpeningData(player: ServerPlayer): FriendlyByteBuf {
                val buf = FriendlyByteBuf(Unpooled.buffer())
                    buf.writeBlockPos(pos)
                    buf.writeLong(be.configuredLimit)
                return buf
            }
            override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player): AbstractContainerMenu {
                val handler = LimiterCableScreenHandler(syncId, playerInventory, pos)
                handler.limit = be.configuredLimit
                return handler
            }
        })
        return InteractionResult.SUCCESS
    }
}
