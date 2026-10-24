package ic2_120.content.item

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.content.recipes.ModTags
import ic2_120.registry.CreativeTab
import ic2_120.registry.instance
import ic2_120.registry.recipeId
import ic2_120.registry.annotation.ModItem
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.data.recipes.ShapelessRecipeBuilder
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.data.recipes.RecipeCategory
import java.util.function.Consumer
import ic2_120.registry.annotation.RecipeProvider

// ========== 粉尘类 ==========

/** 青铜粉 */
@ModItem(name = "bronze_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/bronze"])
class BronzeDust : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BronzeDust::class.instance(), 1).pattern("SSS")
                .pattern("SSS").pattern("SSS").define('S', Ingredient.of(ModTags.Compat.Items.SMALL_DUSTS_BRONZE)).unlockedBy(
                    has(SmallBronzeDust::class.instance()), has(SmallBronzeDust::class.instance())
                ).save(exporter, BronzeDust::class.recipeId("from_small"))

            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, SmallBronzeDust::class.instance(), 9)
                .requires(Ingredient.of(ModTags.Compat.Items.DUSTS_BRONZE))
                .unlockedBy(has(BronzeDust::class.instance()), has(BronzeDust::class.instance()))
                .save(exporter, SmallBronzeDust::class.recipeId("from_normal"))

            // 合成配方：3铜粉 + 1锡粉 = 4青铜粉
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, BronzeDust::class.instance(), 4)
                .requires(Ingredient.of(ModTags.Compat.Items.DUSTS_COPPER))
                .requires(Ingredient.of(ModTags.Compat.Items.DUSTS_COPPER))
                .requires(Ingredient.of(ModTags.Compat.Items.DUSTS_COPPER))
                .requires(Ingredient.of(ModTags.Compat.Items.DUSTS_TIN))
                .unlockedBy(has(CopperDust::class.instance()), has(CopperDust::class.instance()))
                .save(exporter, BronzeDust::class.recipeId("from_copper_and_tin"))

            SimpleCookingRecipeBuilder.createSmelting(
                Ingredient.of(ModTags.Compat.Items.DUSTS_BRONZE),
                RecipeCategory.MISC,
                BronzeIngot::class.instance(),
                0.1f,
                200
            )
                .unlockedBy(has(BronzeDust::class.instance()), has(BronzeDust::class.instance()))
                .save(exporter, BronzeDust::class.recipeId("to_bronze_ingot_smelting"))
        }
    }
}

/** 粘土粉 */
@ModItem(name = "clay_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/clay"])
class ClayDust : Item(Item.Properties())

/** 煤粉 */
@ModItem(name = "coal_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/coal"])
class CoalDust : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            // 熔炉配方：湿煤粉加热成煤粉 (200 tick = 10秒)
            SimpleCookingRecipeBuilder.createSmelting(
                net.minecraft.world.item.crafting.Ingredient.ofItems(CoalFuelDust::class.instance()),
                RecipeCategory.MISC,
                CoalDust::class.instance(),
                0.1f,
                200
            ).unlockedBy(has(CoalFuelDust::class.instance()), has(CoalFuelDust::class.instance()))
                .save(exporter, CoalDust::class.recipeId("from_coal_fuel_dust"))
        }
    }
}

/** 湿煤粉 */
@ModItem(name = "coal_fuel_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts")
class CoalFuelDust : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, CoalFuelDust::class.instance(), 1)
                .requires(CoalDust::class.instance()).requires(Items.WATER_BUCKET)
                .unlockedBy(has(CoalDust::class.instance()), has(CoalDust::class.instance()))
                .save(exporter, CoalFuelDust::class.recipeId("from_bucket"))

            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, CoalFuelDust::class.instance(), 1)
                .requires(CoalDust::class.instance()).requires(WaterCell::class.instance())
                .unlockedBy(has(CoalDust::class.instance()), has(CoalDust::class.instance()))
                .save(exporter, CoalFuelDust::class.recipeId("from_cell"))
        }
    }
}

/** 铜粉 */
@ModItem(name = "copper_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/copper"])
class CopperDust : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CopperDust::class.instance(), 1).pattern("SSS")
                .pattern("SSS").pattern("SSS").define('S', Ingredient.of(ModTags.Compat.Items.SMALL_DUSTS_COPPER)).unlockedBy(
                    has(SmallCopperDust::class.instance()), has(SmallCopperDust::class.instance())
                ).save(exporter, CopperDust::class.recipeId("from_small"))

            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, SmallCopperDust::class.instance(), 9)
                .requires(Ingredient.of(ModTags.Compat.Items.DUSTS_COPPER))
                .unlockedBy(has(CopperDust::class.instance()), has(CopperDust::class.instance()))
                .save(exporter, SmallCopperDust::class.recipeId("from_normal"))

            SimpleCookingRecipeBuilder.createSmelting(
                Ingredient.of(ModTags.Compat.Items.DUSTS_COPPER),
                RecipeCategory.MISC,
                Items.COPPER_INGOT,
                0.1f,
                200
            )
                .unlockedBy(has(CopperDust::class.instance()), has(CopperDust::class.instance()))
                .save(exporter, CopperDust::class.recipeId("to_copper_ingot_smelting"))
        }
    }
}

/** 钻石粉 */
@ModItem(name = "diamond_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/diamond"])
class DiamondDust : Item(Item.Properties())

/** 能量水晶粉 */
@ModItem(name = "energium_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts")
class EnergiumDust : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            // 交叉摆法：4个钻石粉 + 5个红石粉
            // R D R
            // D R D
            // R D R
            // D=钻石粉, R=红石粉
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, EnergiumDust::class.instance(), 9)
                .pattern("RDR").pattern("DRD").pattern("RDR")
                .define('D', Ingredient.of(ModTags.Compat.Items.DUSTS_DIAMOND))
                .define('R', Ingredient.of(ModTags.Compat.Items.DUSTS_REDSTONE))
                .unlockedBy(has(DiamondDust::class.instance()), has(DiamondDust::class.instance()))
                .save(exporter, EnergiumDust::class.recipeId("from_crafting"))
        }
    }
}

/** 金粉 */
@ModItem(name = "gold_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/gold"])
class GoldDust : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, GoldDust::class.instance(), 1).pattern("SSS")
                .pattern("SSS").pattern("SSS").define('S', Ingredient.of(ModTags.Compat.Items.SMALL_DUSTS_GOLD)).unlockedBy(
                    has(SmallGoldDust::class.instance()), has(SmallGoldDust::class.instance())
                ).save(exporter, GoldDust::class.recipeId("from_small"))

            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, SmallGoldDust::class.instance(), 9)
                .requires(Ingredient.of(ModTags.Compat.Items.DUSTS_GOLD))
                .unlockedBy(has(GoldDust::class.instance()), has(GoldDust::class.instance()))
                .save(exporter, SmallGoldDust::class.recipeId("from_normal"))

            SimpleCookingRecipeBuilder.createSmelting(
                Ingredient.of(ModTags.Compat.Items.DUSTS_GOLD),
                RecipeCategory.MISC,
                Items.GOLD_INGOT,
                0.1f,
                200
            )
                .unlockedBy(has(GoldDust::class.instance()), has(GoldDust::class.instance()))
                .save(exporter, GoldDust::class.recipeId("to_gold_ingot_smelting"))
        }
    }
}

/** 铁粉 */
@ModItem(name = "iron_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/iron"])
class IronDust : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, IronDust::class.instance(), 1).pattern("SSS")
                .pattern("SSS").pattern("SSS").define('S', Ingredient.of(ModTags.Compat.Items.SMALL_DUSTS_IRON)).unlockedBy(
                    has(SmallIronDust::class.instance()), has(SmallIronDust::class.instance())
                ).save(exporter, IronDust::class.recipeId("from_small"))

            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, SmallIronDust::class.instance(), 9)
                .requires(Ingredient.of(ModTags.Compat.Items.DUSTS_IRON))
                .unlockedBy(has(IronDust::class.instance()), has(IronDust::class.instance()))
                .save(exporter, SmallIronDust::class.recipeId("from_normal"))

            SimpleCookingRecipeBuilder.createSmelting(
                Ingredient.of(ModTags.Compat.Items.DUSTS_IRON),
                RecipeCategory.MISC,
                Items.IRON_INGOT,
                0.1f,
                200
            )
                .unlockedBy(has(IronDust::class.instance()), has(IronDust::class.instance()))
                .save(exporter, IronDust::class.recipeId("to_iron_ingot_smelting"))
        }
    }
}

/** 青金石粉 */
@ModItem(name = "lapis_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/lapis"])
class LapisDust : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, LapisDust::class.instance(), 1).pattern("SSS")
                .pattern("SSS").pattern("SSS").define('S', Ingredient.of(ModTags.Compat.Items.SMALL_DUSTS_LAPIS)).unlockedBy(
                    has(SmallLapisDust::class.instance()), has(SmallLapisDust::class.instance())
                ).save(exporter, LapisDust::class.recipeId("from_small"))

            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, SmallLapisDust::class.instance(), 9)
                .requires(Ingredient.of(ModTags.Compat.Items.DUSTS_LAPIS))
                .unlockedBy(has(LapisDust::class.instance()), has(LapisDust::class.instance()))
                .save(exporter, SmallLapisDust::class.recipeId("from_normal"))
        }
    }
}

/** 铅粉 */
@ModItem(name = "lead_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/lead"])
class LeadDust : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, LeadDust::class.instance(), 1).pattern("SSS")
                .pattern("SSS").pattern("SSS").define('S', Ingredient.of(ModTags.Compat.Items.SMALL_DUSTS_LEAD)).unlockedBy(
                    has(SmallLeadDust::class.instance()), has(SmallLeadDust::class.instance())
                ).save(exporter, LeadDust::class.recipeId("from_small"))

            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, SmallLeadDust::class.instance(), 9)
                .requires(Ingredient.of(ModTags.Compat.Items.DUSTS_LEAD))
                .unlockedBy(has(LeadDust::class.instance()), has(LeadDust::class.instance()))
                .save(exporter, SmallLeadDust::class.recipeId("from_normal"))

            SimpleCookingRecipeBuilder.createSmelting(
                Ingredient.of(ModTags.Compat.Items.DUSTS_LEAD),
                RecipeCategory.MISC,
                LeadIngot::class.instance(),
                0.1f,
                200
            )
                .unlockedBy(has(LeadDust::class.instance()), has(LeadDust::class.instance()))
                .save(exporter, LeadDust::class.recipeId("to_lead_ingot_smelting"))
        }
    }
}

/** 锂粉 */
@ModItem(name = "lithium_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/lithium"])
class LithiumDust : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, LithiumDust::class.instance(), 1).pattern("SSS")
                .pattern("SSS").pattern("SSS").define('S', Ingredient.of(ModTags.Compat.Items.SMALL_DUSTS_LITHIUM)).unlockedBy(
                    has(SmallLithiumDust::class.instance()), has(SmallLithiumDust::class.instance())
                ).save(exporter, LithiumDust::class.recipeId("from_small"))

            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, SmallLithiumDust::class.instance(), 9)
                .requires(Ingredient.of(ModTags.Compat.Items.DUSTS_LITHIUM))
                .unlockedBy(has(LithiumDust::class.instance()), has(LithiumDust::class.instance()))
                .save(exporter, SmallLithiumDust::class.recipeId("from_normal"))
        }
    }
}

/** 黑曜石粉 */
@ModItem(name = "obsidian_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/obsidian"])
class ObsidianDust : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ObsidianDust::class.instance(), 1).pattern("SSS")
                .pattern("SSS").pattern("SSS").define('S', Ingredient.of(ModTags.Compat.Items.SMALL_DUSTS_OBSIDIAN)).unlockedBy(
                    has(SmallObsidianDust::class.instance()),
                    has(SmallObsidianDust::class.instance())
                ).save(exporter, ObsidianDust::class.recipeId("from_small"))

            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, SmallObsidianDust::class.instance(), 9)
                .requires(Ingredient.of(ModTags.Compat.Items.DUSTS_OBSIDIAN))
                .unlockedBy(has(ObsidianDust::class.instance()), has(ObsidianDust::class.instance()))
                .save(exporter, SmallObsidianDust::class.recipeId("from_normal"))
        }
    }
}

/** 二氧化硅粉 */
@ModItem(name = "silicon_dioxide_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/silicon_dioxide"])
class SiliconDioxideDust : Item(Item.Properties())

/** 银粉 */
@ModItem(name = "silver_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/silver"])
class SilverDust : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SilverDust::class.instance(), 1).pattern("SSS")
                .pattern("SSS").pattern("SSS").define('S', Ingredient.of(ModTags.Compat.Items.SMALL_DUSTS_SILVER)).unlockedBy(
                    has(SmallSilverDust::class.instance()), has(SmallSilverDust::class.instance())
                ).save(exporter, SilverDust::class.recipeId("from_small"))

            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, SmallSilverDust::class.instance(), 9)
                .requires(Ingredient.of(ModTags.Compat.Items.DUSTS_SILVER))
                .unlockedBy(has(SilverDust::class.instance()), has(SilverDust::class.instance()))
                .save(exporter, SmallSilverDust::class.recipeId("from_normal"))

            SimpleCookingRecipeBuilder.createSmelting(
                Ingredient.of(ModTags.Compat.Items.DUSTS_SILVER),
                RecipeCategory.MISC,
                SilverIngot::class.instance(),
                0.1f,
                200
            )
                .unlockedBy(has(SilverDust::class.instance()), has(SilverDust::class.instance()))
                .save(exporter, SilverDust::class.recipeId("to_silver_ingot_smelting"))
        }
    }
}

/** 石粉 */
@ModItem(name = "stone_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/stone"])
class StoneDust : Item(Item.Properties())

/** 硫粉 */
@ModItem(name = "sulfur_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/sulfur"])
class SulfurDust : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SulfurDust::class.instance(), 1).pattern("SSS")
                .pattern("SSS").pattern("SSS").define('S', Ingredient.of(ModTags.Compat.Items.SMALL_DUSTS_SULFUR)).unlockedBy(
                    has(SmallSulfurDust::class.instance()), has(SmallSulfurDust::class.instance())
                ).save(exporter, SulfurDust::class.recipeId("from_small"))

            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, SmallSulfurDust::class.instance(), 9)
                .requires(Ingredient.of(ModTags.Compat.Items.DUSTS_SULFUR))
                .unlockedBy(has(SulfurDust::class.instance()), has(SulfurDust::class.instance()))
                .save(exporter, SmallSulfurDust::class.recipeId("from_normal"))
        }
    }
}

/** 锡粉 */
@ModItem(name = "tin_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/tin"])
class TinDust : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TinDust::class.instance(), 1).pattern("SSS")
                .pattern("SSS").pattern("SSS").define('S', Ingredient.of(ModTags.Compat.Items.SMALL_DUSTS_TIN))
                .unlockedBy(has(SmallTinDust::class.instance()), has(SmallTinDust::class.instance()))
                .save(exporter, TinDust::class.recipeId("from_small"))

            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, SmallTinDust::class.instance(), 9)
                .requires(Ingredient.of(ModTags.Compat.Items.DUSTS_TIN))
                .unlockedBy(has(TinDust::class.instance()), has(TinDust::class.instance()))
                .save(exporter, SmallTinDust::class.recipeId("from_normal"))

            SimpleCookingRecipeBuilder.createSmelting(
                Ingredient.of(ModTags.Compat.Items.DUSTS_TIN),
                RecipeCategory.MISC,
                TinIngot::class.instance(),
                0.1f,
                200
            )
                .unlockedBy(has(TinDust::class.instance()), has(TinDust::class.instance()))
                .save(exporter, TinDust::class.recipeId("to_tin_ingot_smelting"))
        }
    }
}

/** 氢氧化锡粉 */
@ModItem(name = "hydrated_tin_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts")
class HydratedTinDust : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            // 锡粉 + 水桶 -> 氢氧化锡粉 (返回空桶)
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, HydratedTinDust::class.instance(), 1)
                .requires(Ingredient.of(ModTags.Compat.Items.DUSTS_TIN)).requires(Items.WATER_BUCKET)
                .unlockedBy(has(TinDust::class.instance()), has(TinDust::class.instance()))
                .save(exporter, HydratedTinDust::class.recipeId("from_bucket"))

            // 锡粉 + 水单元 -> 氢氧化锡粉 (返回空单元)
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, HydratedTinDust::class.instance(), 1)
                .requires(Ingredient.of(ModTags.Compat.Items.DUSTS_TIN)).requires(WaterCell::class.instance())
                .unlockedBy(has(TinDust::class.instance()), has(TinDust::class.instance()))
                .save(exporter, HydratedTinDust::class.recipeId("from_cell"))
        }
    }
}

/** 地狱岩粉 */
@ModItem(name = "netherrack_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/netherrack"])
class NetherrackDust : Item(Item.Properties())

/** 末影珍珠粉 */
@ModItem(name = "ender_pearl_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts", materialTags = ["dusts/ender_pearl"])
class EnderPearlDust : Item(Item.Properties())

// ========== 小撮粉尘类 ==========

/** 小撮青铜粉 */
@ModItem(name = "small_bronze_dust", tab = CreativeTab.IC2_MATERIALS, group = "small_dusts", materialTags = ["small_dusts/bronze"])
class SmallBronzeDust : Item(Item.Properties())

/** 小撮铜粉 */
@ModItem(name = "small_copper_dust", tab = CreativeTab.IC2_MATERIALS, group = "small_dusts", materialTags = ["small_dusts/copper"])
class SmallCopperDust : Item(Item.Properties())

/** 小撮金粉 */
@ModItem(name = "small_gold_dust", tab = CreativeTab.IC2_MATERIALS, group = "small_dusts", materialTags = ["small_dusts/gold"])
class SmallGoldDust : Item(Item.Properties())

/** 小撮铁粉 */
@ModItem(name = "small_iron_dust", tab = CreativeTab.IC2_MATERIALS, group = "small_dusts", materialTags = ["small_dusts/iron"])
class SmallIronDust : Item(Item.Properties())

/** 小撮青金石粉 */
@ModItem(name = "small_lapis_dust", tab = CreativeTab.IC2_MATERIALS, group = "small_dusts", materialTags = ["small_dusts/lapis"])
class SmallLapisDust : Item(Item.Properties())

/** 小撮铅粉 */
@ModItem(name = "small_lead_dust", tab = CreativeTab.IC2_MATERIALS, group = "small_dusts", materialTags = ["small_dusts/lead"])
class SmallLeadDust : Item(Item.Properties())

/** 小撮锂粉 */
@ModItem(name = "small_lithium_dust", tab = CreativeTab.IC2_MATERIALS, group = "small_dusts", materialTags = ["small_dusts/lithium"])
class SmallLithiumDust : Item(Item.Properties())

/** 小撮黑曜石粉 */
@ModItem(name = "small_obsidian_dust", tab = CreativeTab.IC2_MATERIALS, group = "small_dusts", materialTags = ["small_dusts/obsidian"])
class SmallObsidianDust : Item(Item.Properties())

/** 小撮银粉 */
@ModItem(name = "small_silver_dust", tab = CreativeTab.IC2_MATERIALS, group = "small_dusts", materialTags = ["small_dusts/silver"])
class SmallSilverDust : Item(Item.Properties())

/** 小撮硫粉 */
@ModItem(name = "small_sulfur_dust", tab = CreativeTab.IC2_MATERIALS, group = "small_dusts", materialTags = ["small_dusts/sulfur"])
class SmallSulfurDust : Item(Item.Properties())

/** 小撮锡粉 */
@ModItem(name = "small_tin_dust", tab = CreativeTab.IC2_MATERIALS, group = "small_dusts", materialTags = ["small_dusts/tin"])
class SmallTinDust : Item(Item.Properties())
