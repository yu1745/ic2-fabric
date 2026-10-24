package ic2_120_industrial_upgrade.content.item

import net.minecraft.data.recipes.RecipeProvider.getHasName

import ic2_120.content.block.ReinforcedGlassBlock
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120_industrial_upgrade.IC2IndustrialUpgrade
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory
import java.util.function.Consumer
import net.minecraft.data.recipes.RecipeProvider.has

/**
 * 透镜系物品（对应 1.4.0 CraftingThings.sunlinse/nightlinse/rainlinse）。
 * 用于把太阳能发电机升级为 Sun/Rain 变体（配方见各面板变体的 @RecipeProvider）。
 * 原版 sunlinse/nightlinse 无配方，本组配方为新设计（强化玻璃聚光），rainlinse 沿用原版 USU。
 */

// i18n: item.ic2_120_industrial_upgrade.sunlinse
// zh_cn: 日光镜  en_us: Sun Linse
@ModItem(name = "sunlinse", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "material")
class SunLinse : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // 十字：强化玻璃聚萤石之光
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SunLinse::class.instance(), 1)
                .pattern(" G ")
                .pattern("GSG")
                .pattern(" G ")
                .define('G', Items.GLOWSTONE_DUST)
                .define('S', ReinforcedGlassBlock::class.item())
                .unlockedBy(getHasName(Items.GLOWSTONE_DUST), has(Items.GLOWSTONE_DUST))
                .save(exporter, IC2IndustrialUpgrade.id("sunlinse"))
        }
    }
}

// i18n: item.ic2_120_industrial_upgrade.nightlinse
// zh_cn: 夜光镜  en_us: Night Linse
@ModItem(name = "nightlinse", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "material")
class NightLinse : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // 十字：强化玻璃聚煤之暗
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, NightLinse::class.instance(), 1)
                .pattern(" C ")
                .pattern("CSC")
                .pattern(" C ")
                .define('C', Items.COAL)
                .define('S', ReinforcedGlassBlock::class.item())
                .unlockedBy(getHasName(Items.COAL), has(Items.COAL))
                .save(exporter, IC2IndustrialUpgrade.id("nightlinse"))
        }
    }
}

// i18n: item.ic2_120_industrial_upgrade.rainlinse
// zh_cn: 雨镜  en_us: Rain Linse
@ModItem(name = "rainlinse", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "material")
class RainLinse : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // 原版配方：2×日光镜 + 1×夜光镜
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RainLinse::class.instance(), 1)
                .pattern("USU")
                .define('U', SunLinse::class.instance())
                .define('S', NightLinse::class.instance())
                .unlockedBy(getHasName(SunLinse::class.instance()), has(SunLinse::class.instance()))
                .save(exporter, IC2IndustrialUpgrade.id("rainlinse"))
        }
    }
}
