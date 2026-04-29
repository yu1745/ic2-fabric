package ic2_120.content.item

import ic2_120.content.recipes.ModTags
import ic2_120.registry.CreativeTab
import ic2_120.registry.instance
import ic2_120.registry.recipeId
import ic2_120.registry.annotation.ModItem
import net.minecraft.data.server.recipe.CookingRecipeJsonBuilder
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.book.RecipeCategory
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import ic2_120.registry.annotation.RecipeProvider

// ========== 粉尘类 ==========

/** 青铜粉 */
@ModItem(name = "bronze_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/bronze"])
class BronzeDust : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, BronzeDust::class.instance(), 1).pattern("SSS")
                .pattern("SSS").pattern("SSS").input('S', SmallBronzeDust::class.instance()).criterion(
                    hasItem(SmallBronzeDust::class.instance()), conditionsFromItem(SmallBronzeDust::class.instance())
                ).offerTo(exporter, BronzeDust::class.recipeId("from_small"))

            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, SmallBronzeDust::class.instance(), 9)
                .input(BronzeDust::class.instance())
                .criterion(hasItem(BronzeDust::class.instance()), conditionsFromItem(BronzeDust::class.instance()))
                .offerTo(exporter, SmallBronzeDust::class.recipeId("from_normal"))

            // 合成配方：3铜粉 + 1锡粉 = 4青铜粉
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, BronzeDust::class.instance(), 4)
                .input(CopperDust::class.instance())
                .input(CopperDust::class.instance())
                .input(CopperDust::class.instance())
                .input(TinDust::class.instance())
                .criterion(hasItem(CopperDust::class.instance()), conditionsFromItem(CopperDust::class.instance()))
                .offerTo(exporter, BronzeDust::class.recipeId("from_copper_and_tin"))

            CookingRecipeJsonBuilder.createSmelting(
                Ingredient.ofItems(BronzeDust::class.instance()),
                RecipeCategory.MISC,
                BronzeIngot::class.instance(),
                0.1f,
                200
            )
                .criterion(hasItem(BronzeDust::class.instance()), conditionsFromItem(BronzeDust::class.instance()))
                .offerTo(exporter, BronzeDust::class.recipeId("to_bronze_ingot_smelting"))
        }
    }
}

/** 粘土粉 */
@ModItem(name = "clay_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/clay"])
class ClayDust : Item(Item.Settings())

/** 煤粉 */
@ModItem(name = "coal_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/coal"])
class CoalDust : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            // 熔炉配方：湿煤粉加热成煤粉 (200 tick = 10秒)
            CookingRecipeJsonBuilder.createSmelting(
                net.minecraft.recipe.Ingredient.ofItems(CoalFuelDust::class.instance()),
                RecipeCategory.MISC,
                CoalDust::class.instance(),
                0.1f,
                200
            ).criterion(hasItem(CoalFuelDust::class.instance()), conditionsFromItem(CoalFuelDust::class.instance()))
                .offerTo(exporter, CoalDust::class.recipeId("from_coal_fuel_dust"))
        }
    }
}

/** 湿煤粉 */
@ModItem(name = "coal_fuel_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts")
class CoalFuelDust : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, CoalFuelDust::class.instance(), 1)
                .input(CoalDust::class.instance()).input(Items.WATER_BUCKET)
                .criterion(hasItem(CoalDust::class.instance()), conditionsFromItem(CoalDust::class.instance()))
                .offerTo(exporter, CoalFuelDust::class.recipeId("from_bucket"))

            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, CoalFuelDust::class.instance(), 1)
                .input(CoalDust::class.instance()).input(WaterCell::class.instance())
                .criterion(hasItem(CoalDust::class.instance()), conditionsFromItem(CoalDust::class.instance()))
                .offerTo(exporter, CoalFuelDust::class.recipeId("from_cell"))
        }
    }
}

/** 铜粉 */
@ModItem(name = "copper_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/copper"])
class CopperDust : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, CopperDust::class.instance(), 1).pattern("SSS")
                .pattern("SSS").pattern("SSS").input('S', SmallCopperDust::class.instance()).criterion(
                    hasItem(SmallCopperDust::class.instance()), conditionsFromItem(SmallCopperDust::class.instance())
                ).offerTo(exporter, CopperDust::class.recipeId("from_small"))

            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, SmallCopperDust::class.instance(), 9)
                .input(CopperDust::class.instance())
                .criterion(hasItem(CopperDust::class.instance()), conditionsFromItem(CopperDust::class.instance()))
                .offerTo(exporter, SmallCopperDust::class.recipeId("from_normal"))

            CookingRecipeJsonBuilder.createSmelting(
                Ingredient.ofItems(CopperDust::class.instance()),
                RecipeCategory.MISC,
                Items.COPPER_INGOT,
                0.1f,
                200
            )
                .criterion(hasItem(CopperDust::class.instance()), conditionsFromItem(CopperDust::class.instance()))
                .offerTo(exporter, CopperDust::class.recipeId("to_copper_ingot_smelting"))
        }
    }
}

/** 钻石粉 */
@ModItem(name = "diamond_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/diamond"])
class DiamondDust : Item(Item.Settings())

/** 能量水晶粉 */
@ModItem(name = "energium_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts")
class EnergiumDust : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            // 交叉摆法：4个钻石粉 + 5个红石粉
            // D R D
            // R D R
            // D R D
            // D=钻石粉, R=红石粉
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, EnergiumDust::class.instance(), 9)
                .pattern("DRD").pattern("RDR").pattern("DRD")
                .input('D', Ingredient.fromTag(ModTags.Compat.Items.DUSTS_DIAMOND))
                .input('R', Ingredient.fromTag(ModTags.Compat.Items.DUSTS_REDSTONE))
                .criterion(hasItem(DiamondDust::class.instance()), conditionsFromItem(DiamondDust::class.instance()))
                .offerTo(exporter, EnergiumDust::class.recipeId("from_crafting"))
        }
    }
}

/** 金粉 */
@ModItem(name = "gold_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/gold"])
class GoldDust : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, GoldDust::class.instance(), 1).pattern("SSS")
                .pattern("SSS").pattern("SSS").input('S', SmallGoldDust::class.instance()).criterion(
                    hasItem(SmallGoldDust::class.instance()), conditionsFromItem(SmallGoldDust::class.instance())
                ).offerTo(exporter, GoldDust::class.recipeId("from_small"))

            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, SmallGoldDust::class.instance(), 9)
                .input(GoldDust::class.instance())
                .criterion(hasItem(GoldDust::class.instance()), conditionsFromItem(GoldDust::class.instance()))
                .offerTo(exporter, SmallGoldDust::class.recipeId("from_normal"))

            CookingRecipeJsonBuilder.createSmelting(
                Ingredient.ofItems(GoldDust::class.instance()),
                RecipeCategory.MISC,
                Items.GOLD_INGOT,
                0.1f,
                200
            )
                .criterion(hasItem(GoldDust::class.instance()), conditionsFromItem(GoldDust::class.instance()))
                .offerTo(exporter, GoldDust::class.recipeId("to_gold_ingot_smelting"))
        }
    }
}

/** 铁粉 */
@ModItem(name = "iron_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/iron"])
class IronDust : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, IronDust::class.instance(), 1).pattern("SSS")
                .pattern("SSS").pattern("SSS").input('S', SmallIronDust::class.instance()).criterion(
                    hasItem(SmallIronDust::class.instance()), conditionsFromItem(SmallIronDust::class.instance())
                ).offerTo(exporter, IronDust::class.recipeId("from_small"))

            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, SmallIronDust::class.instance(), 9)
                .input(IronDust::class.instance())
                .criterion(hasItem(IronDust::class.instance()), conditionsFromItem(IronDust::class.instance()))
                .offerTo(exporter, SmallIronDust::class.recipeId("from_normal"))

            CookingRecipeJsonBuilder.createSmelting(
                Ingredient.ofItems(IronDust::class.instance()),
                RecipeCategory.MISC,
                Items.IRON_INGOT,
                0.1f,
                200
            )
                .criterion(hasItem(IronDust::class.instance()), conditionsFromItem(IronDust::class.instance()))
                .offerTo(exporter, IronDust::class.recipeId("to_iron_ingot_smelting"))
        }
    }
}

/** 青金石粉 */
@ModItem(name = "lapis_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/lapis"])
class LapisDust : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, LapisDust::class.instance(), 1).pattern("SSS")
                .pattern("SSS").pattern("SSS").input('S', SmallLapisDust::class.instance()).criterion(
                    hasItem(SmallLapisDust::class.instance()), conditionsFromItem(SmallLapisDust::class.instance())
                ).offerTo(exporter, LapisDust::class.recipeId("from_small"))

            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, SmallLapisDust::class.instance(), 9)
                .input(LapisDust::class.instance())
                .criterion(hasItem(LapisDust::class.instance()), conditionsFromItem(LapisDust::class.instance()))
                .offerTo(exporter, SmallLapisDust::class.recipeId("from_normal"))
        }
    }
}

/** 铅粉 */
@ModItem(name = "lead_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/lead"])
class LeadDust : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, LeadDust::class.instance(), 1).pattern("SSS")
                .pattern("SSS").pattern("SSS").input('S', SmallLeadDust::class.instance()).criterion(
                    hasItem(SmallLeadDust::class.instance()), conditionsFromItem(SmallLeadDust::class.instance())
                ).offerTo(exporter, LeadDust::class.recipeId("from_small"))

            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, SmallLeadDust::class.instance(), 9)
                .input(LeadDust::class.instance())
                .criterion(hasItem(LeadDust::class.instance()), conditionsFromItem(LeadDust::class.instance()))
                .offerTo(exporter, SmallLeadDust::class.recipeId("from_normal"))

            CookingRecipeJsonBuilder.createSmelting(
                Ingredient.ofItems(LeadDust::class.instance()),
                RecipeCategory.MISC,
                LeadIngot::class.instance(),
                0.1f,
                200
            )
                .criterion(hasItem(LeadDust::class.instance()), conditionsFromItem(LeadDust::class.instance()))
                .offerTo(exporter, LeadDust::class.recipeId("to_lead_ingot_smelting"))
        }
    }
}

/** 锂粉 */
@ModItem(name = "lithium_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/lithium"])
class LithiumDust : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, LithiumDust::class.instance(), 1).pattern("SSS")
                .pattern("SSS").pattern("SSS").input('S', SmallLithiumDust::class.instance()).criterion(
                    hasItem(SmallLithiumDust::class.instance()), conditionsFromItem(SmallLithiumDust::class.instance())
                ).offerTo(exporter, LithiumDust::class.recipeId("from_small"))

            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, SmallLithiumDust::class.instance(), 9)
                .input(LithiumDust::class.instance())
                .criterion(hasItem(LithiumDust::class.instance()), conditionsFromItem(LithiumDust::class.instance()))
                .offerTo(exporter, SmallLithiumDust::class.recipeId("from_normal"))
        }
    }
}

/** 黑曜石粉 */
@ModItem(name = "obsidian_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/obsidian"])
class ObsidianDust : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ObsidianDust::class.instance(), 1).pattern("SSS")
                .pattern("SSS").pattern("SSS").input('S', SmallObsidianDust::class.instance()).criterion(
                    hasItem(SmallObsidianDust::class.instance()),
                    conditionsFromItem(SmallObsidianDust::class.instance())
                ).offerTo(exporter, ObsidianDust::class.recipeId("from_small"))

            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, SmallObsidianDust::class.instance(), 9)
                .input(ObsidianDust::class.instance())
                .criterion(hasItem(ObsidianDust::class.instance()), conditionsFromItem(ObsidianDust::class.instance()))
                .offerTo(exporter, SmallObsidianDust::class.recipeId("from_normal"))
        }
    }
}

/** 二氧化硅粉 */
@ModItem(name = "silicon_dioxide_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/silicon_dioxide"])
class SiliconDioxideDust : Item(Item.Settings())

/** 银粉 */
@ModItem(name = "silver_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/silver"])
class SilverDust : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, SilverDust::class.instance(), 1).pattern("SSS")
                .pattern("SSS").pattern("SSS").input('S', SmallSilverDust::class.instance()).criterion(
                    hasItem(SmallSilverDust::class.instance()), conditionsFromItem(SmallSilverDust::class.instance())
                ).offerTo(exporter, SilverDust::class.recipeId("from_small"))

            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, SmallSilverDust::class.instance(), 9)
                .input(SilverDust::class.instance())
                .criterion(hasItem(SilverDust::class.instance()), conditionsFromItem(SilverDust::class.instance()))
                .offerTo(exporter, SmallSilverDust::class.recipeId("from_normal"))

            CookingRecipeJsonBuilder.createSmelting(
                Ingredient.ofItems(SilverDust::class.instance()),
                RecipeCategory.MISC,
                SilverIngot::class.instance(),
                0.1f,
                200
            )
                .criterion(hasItem(SilverDust::class.instance()), conditionsFromItem(SilverDust::class.instance()))
                .offerTo(exporter, SilverDust::class.recipeId("to_silver_ingot_smelting"))
        }
    }
}

/** 石粉 */
@ModItem(name = "stone_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/stone"])
class StoneDust : Item(Item.Settings())

/** 硫粉 */
@ModItem(name = "sulfur_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/sulfur"])
class SulfurDust : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, SulfurDust::class.instance(), 1).pattern("SSS")
                .pattern("SSS").pattern("SSS").input('S', SmallSulfurDust::class.instance()).criterion(
                    hasItem(SmallSulfurDust::class.instance()), conditionsFromItem(SmallSulfurDust::class.instance())
                ).offerTo(exporter, SulfurDust::class.recipeId("from_small"))

            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, SmallSulfurDust::class.instance(), 9)
                .input(SulfurDust::class.instance())
                .criterion(hasItem(SulfurDust::class.instance()), conditionsFromItem(SulfurDust::class.instance()))
                .offerTo(exporter, SmallSulfurDust::class.recipeId("from_normal"))
        }
    }
}

/** 锡粉 */
@ModItem(name = "tin_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/tin"])
class TinDust : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, TinDust::class.instance(), 1).pattern("SSS")
                .pattern("SSS").pattern("SSS").input('S', SmallTinDust::class.instance())
                .criterion(hasItem(SmallTinDust::class.instance()), conditionsFromItem(SmallTinDust::class.instance()))
                .offerTo(exporter, TinDust::class.recipeId("from_small"))

            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, SmallTinDust::class.instance(), 9)
                .input(TinDust::class.instance())
                .criterion(hasItem(TinDust::class.instance()), conditionsFromItem(TinDust::class.instance()))
                .offerTo(exporter, SmallTinDust::class.recipeId("from_normal"))

            CookingRecipeJsonBuilder.createSmelting(
                Ingredient.ofItems(TinDust::class.instance()),
                RecipeCategory.MISC,
                TinIngot::class.instance(),
                0.1f,
                200
            )
                .criterion(hasItem(TinDust::class.instance()), conditionsFromItem(TinDust::class.instance()))
                .offerTo(exporter, TinDust::class.recipeId("to_tin_ingot_smelting"))
        }
    }
}

/** 氢氧化锡粉 */
@ModItem(name = "hydrated_tin_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts")
class HydratedTinDust : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            // 锡粉 + 水桶 -> 氢氧化锡粉 (返回空桶)
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, HydratedTinDust::class.instance(), 1)
                .input(TinDust::class.instance()).input(Items.WATER_BUCKET)
                .criterion(hasItem(TinDust::class.instance()), conditionsFromItem(TinDust::class.instance()))
                .offerTo(exporter, HydratedTinDust::class.recipeId("from_bucket"))

            // 锡粉 + 水单元 -> 氢氧化锡粉 (返回空单元)
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, HydratedTinDust::class.instance(), 1)
                .input(TinDust::class.instance()).input(WaterCell::class.instance())
                .criterion(hasItem(TinDust::class.instance()), conditionsFromItem(TinDust::class.instance()))
                .offerTo(exporter, HydratedTinDust::class.recipeId("from_cell"))
        }
    }
}

/** 地狱岩粉 */
@ModItem(name = "netherrack_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/netherrack"])
class NetherrackDust : Item(Item.Settings())

/** 末影珍珠粉 */
@ModItem(name = "ender_pearl_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/ender_pearl"])
class EnderPearlDust : Item(Item.Settings())

// ========== 小撮粉尘类 ==========

/** 小撮青铜粉 */
@ModItem(name = "small_bronze_dust", tab = CreativeTab.IC2_MATERIALS, group = "small_dusts")
class SmallBronzeDust : Item(Item.Settings())

/** 小撮铜粉 */
@ModItem(name = "small_copper_dust", tab = CreativeTab.IC2_MATERIALS, group = "small_dusts")
class SmallCopperDust : Item(Item.Settings())

/** 小撮金粉 */
@ModItem(name = "small_gold_dust", tab = CreativeTab.IC2_MATERIALS, group = "small_dusts")
class SmallGoldDust : Item(Item.Settings())

/** 小撮铁粉 */
@ModItem(name = "small_iron_dust", tab = CreativeTab.IC2_MATERIALS, group = "small_dusts")
class SmallIronDust : Item(Item.Settings())

/** 小撮青金石粉 */
@ModItem(name = "small_lapis_dust", tab = CreativeTab.IC2_MATERIALS, group = "small_dusts")
class SmallLapisDust : Item(Item.Settings())

/** 小撮铅粉 */
@ModItem(name = "small_lead_dust", tab = CreativeTab.IC2_MATERIALS, group = "small_dusts")
class SmallLeadDust : Item(Item.Settings())

/** 小撮锂粉 */
@ModItem(name = "small_lithium_dust", tab = CreativeTab.IC2_MATERIALS, group = "small_dusts")
class SmallLithiumDust : Item(Item.Settings())

/** 小撮黑曜石粉 */
@ModItem(name = "small_obsidian_dust", tab = CreativeTab.IC2_MATERIALS, group = "small_dusts")
class SmallObsidianDust : Item(Item.Settings())

/** 小撮银粉 */
@ModItem(name = "small_silver_dust", tab = CreativeTab.IC2_MATERIALS, group = "small_dusts")
class SmallSilverDust : Item(Item.Settings())

/** 小撮硫粉 */
@ModItem(name = "small_sulfur_dust", tab = CreativeTab.IC2_MATERIALS, group = "small_dusts")
class SmallSulfurDust : Item(Item.Settings())

/** 小撮锡粉 */
@ModItem(name = "small_tin_dust", tab = CreativeTab.IC2_MATERIALS, group = "small_dusts")
class SmallTinDust : Item(Item.Settings())
