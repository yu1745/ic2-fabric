package ic2_120.content.item

import ic2_120.Ic2_120
import ic2_120.registry.CreativeTab
import ic2_120.registry.instance
import ic2_120.registry.type
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.type
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.util.Identifier
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import java.util.function.Consumer

// ========== 粉尘类 ==========

/** 青铜粉 */
@ModItem(name = "bronze_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts")
class BronzeDust : Item(FabricItemSettings())

/** 粘土粉 */
@ModItem(name = "clay_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts")
class ClayDust : Item(FabricItemSettings())

/** 煤粉 */
@ModItem(name = "coal_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts")
class CoalDust : Item(FabricItemSettings())

/** 湿煤粉 */
@ModItem(name = "coal_fuel_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts")
class CoalFuelDust : Item(FabricItemSettings()) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, CoalFuelDust::class.instance(), 1)
                .input(CoalDust::class.instance())
                .input(Items.WATER_BUCKET)
                .criterion(hasItem(CoalDust::class.instance()), conditionsFromItem(CoalDust::class.instance()))
                .offerTo(exporter, Identifier(Ic2_120.MOD_ID, "coal_fuel_dust_from_bucket"))

            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, CoalFuelDust::class.instance(), 1)
                .input(CoalDust::class.instance())
                .input(WaterCell::class.instance())
                .criterion(hasItem(CoalDust::class.instance()), conditionsFromItem(CoalDust::class.instance()))
                .offerTo(exporter, Identifier(Ic2_120.MOD_ID, "coal_fuel_dust_from_cell"))
        }
    }
}

/** 铜粉 */
@ModItem(name = "copper_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts")
class CopperDust : Item(FabricItemSettings())

/** 钻石粉 */
@ModItem(name = "diamond_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts")
class DiamondDust : Item(FabricItemSettings())

/** 能量水晶粉 */
@ModItem(name = "energium_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts")
class EnergiumDust : Item(FabricItemSettings())

/** 金粉 */
@ModItem(name = "gold_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts")
class GoldDust : Item(FabricItemSettings())

/** 铁粉 */
@ModItem(name = "iron_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts")
class IronDust : Item(FabricItemSettings())

/** 青金石粉 */
@ModItem(name = "lapis_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts")
class LapisDust : Item(FabricItemSettings())

/** 铅粉 */
@ModItem(name = "lead_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts")
class LeadDust : Item(FabricItemSettings())

/** 锂粉 */
@ModItem(name = "lithium_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts")
class LithiumDust : Item(FabricItemSettings())

/** 黑曜石粉 */
@ModItem(name = "obsidian_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts")
class ObsidianDust : Item(FabricItemSettings())

/** 二氧化硅粉 */
@ModItem(name = "silicon_dioxide_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts")
class SiliconDioxideDust : Item(FabricItemSettings())

/** 银粉 */
@ModItem(name = "silver_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts")
class SilverDust : Item(FabricItemSettings())

/** 石粉 */
@ModItem(name = "stone_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts")
class StoneDust : Item(FabricItemSettings())

/** 硫粉 */
@ModItem(name = "sulfur_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts")
class SulfurDust : Item(FabricItemSettings())

/** 锡粉 */
@ModItem(name = "tin_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts")
class TinDust : Item(FabricItemSettings())

/** 氢氧化锡粉 */
@ModItem(name = "hydrated_tin_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts")
class HydratedTinDust : Item(FabricItemSettings())

/** 地狱岩粉 */
@ModItem(name = "netherrack_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts")
class NetherrackDust : Item(FabricItemSettings())

/** 末影珍珠粉 */
@ModItem(name = "ender_pearl_dust", tab = CreativeTab.IC2_MATERIALS, group = "dusts")
class EnderPearlDust : Item(FabricItemSettings())

// ========== 小撮粉尘类 ==========

/** 小撮青铜粉 */
@ModItem(name = "small_bronze_dust", tab = CreativeTab.IC2_MATERIALS, group = "small_dusts")
class SmallBronzeDust : Item(FabricItemSettings())

/** 小撮铜粉 */
@ModItem(name = "small_copper_dust", tab = CreativeTab.IC2_MATERIALS, group = "small_dusts")
class SmallCopperDust : Item(FabricItemSettings())

/** 小撮金粉 */
@ModItem(name = "small_gold_dust", tab = CreativeTab.IC2_MATERIALS, group = "small_dusts")
class SmallGoldDust : Item(FabricItemSettings())

/** 小撮铁粉 */
@ModItem(name = "small_iron_dust", tab = CreativeTab.IC2_MATERIALS, group = "small_dusts")
class SmallIronDust : Item(FabricItemSettings())

/** 小撮青金石粉 */
@ModItem(name = "small_lapis_dust", tab = CreativeTab.IC2_MATERIALS, group = "small_dusts")
class SmallLapisDust : Item(FabricItemSettings())

/** 小撮铅粉 */
@ModItem(name = "small_lead_dust", tab = CreativeTab.IC2_MATERIALS, group = "small_dusts")
class SmallLeadDust : Item(FabricItemSettings())

/** 小撮锂粉 */
@ModItem(name = "small_lithium_dust", tab = CreativeTab.IC2_MATERIALS, group = "small_dusts")
class SmallLithiumDust : Item(FabricItemSettings())

/** 小撮黑曜石粉 */
@ModItem(name = "small_obsidian_dust", tab = CreativeTab.IC2_MATERIALS, group = "small_dusts")
class SmallObsidianDust : Item(FabricItemSettings())

/** 小撮银粉 */
@ModItem(name = "small_silver_dust", tab = CreativeTab.IC2_MATERIALS, group = "small_dusts")
class SmallSilverDust : Item(FabricItemSettings())

/** 小撮硫粉 */
@ModItem(name = "small_sulfur_dust", tab = CreativeTab.IC2_MATERIALS, group = "small_dusts")
class SmallSulfurDust : Item(FabricItemSettings())

/** 小撮锡粉 */
@ModItem(name = "small_tin_dust", tab = CreativeTab.IC2_MATERIALS, group = "small_dusts")
class SmallTinDust : Item(FabricItemSettings())
