package ic2_120_industrial_upgrade.content.item

import ic2_120.content.block.ReinforcedGlassBlock
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120_industrial_upgrade.IC2IndustrialUpgrade
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory
import java.util.function.Consumer

/**
 * 透镜系物品（对应 1.4.0 CraftingThings.sunlinse/nightlinse/rainlinse）。
 * 用于把太阳能发电机升级为 Sun/Rain 变体（配方见各面板变体的 @RecipeProvider）。
 * 原版 sunlinse/nightlinse 无配方，本组配方为新设计（强化玻璃聚光），rainlinse 沿用原版 USU。
 */

// i18n: item.ic2_120_industrial_upgrade.sunlinse
// zh_cn: 日光镜  en_us: Sun Linse
@ModItem(name = "sunlinse", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "material")
class SunLinse : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            // 十字：强化玻璃聚萤石之光
            ShapedRecipeBuilder.create(RecipeCategory.MISC, SunLinse::class.instance(), 1)
                .pattern(" G ")
                .pattern("GSG")
                .pattern(" G ")
                .input('G', Items.GLOWSTONE_DUST)
                .input('S', ReinforcedGlassBlock::class.item())
                .criterion(hasItem(Items.GLOWSTONE_DUST), conditionsFromItem(Items.GLOWSTONE_DUST))
                .offerTo(exporter, IC2IndustrialUpgrade.id("sunlinse"))
        }
    }
}

// i18n: item.ic2_120_industrial_upgrade.nightlinse
// zh_cn: 夜光镜  en_us: Night Linse
@ModItem(name = "nightlinse", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "material")
class NightLinse : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            // 十字：强化玻璃聚煤之暗
            ShapedRecipeBuilder.create(RecipeCategory.MISC, NightLinse::class.instance(), 1)
                .pattern(" C ")
                .pattern("CSC")
                .pattern(" C ")
                .input('C', Items.COAL)
                .input('S', ReinforcedGlassBlock::class.item())
                .criterion(hasItem(Items.COAL), conditionsFromItem(Items.COAL))
                .offerTo(exporter, IC2IndustrialUpgrade.id("nightlinse"))
        }
    }
}

// i18n: item.ic2_120_industrial_upgrade.rainlinse
// zh_cn: 雨镜  en_us: Rain Linse
@ModItem(name = "rainlinse", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "material")
class RainLinse : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            // 原版配方：2×日光镜 + 1×夜光镜
            ShapedRecipeBuilder.create(RecipeCategory.MISC, RainLinse::class.instance(), 1)
                .pattern("USU")
                .input('U', SunLinse::class.instance())
                .input('S', NightLinse::class.instance())
                .criterion(hasItem(SunLinse::class.instance()), conditionsFromItem(SunLinse::class.instance()))
                .offerTo(exporter, IC2IndustrialUpgrade.id("rainlinse"))
        }
    }
}
