package ic2_120_industrial_upgrade.content.item

import ic2_120.compat.save

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120_advanced_solar_addon.content.item.EnrichedSunnariumAlloy
import ic2_120_industrial_upgrade.IC2IndustrialUpgrade
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.item.Item
import net.minecraft.data.recipes.RecipeCategory
import java.util.function.Consumer
import net.minecraft.data.recipes.RecipeProvider.getHasName
import ic2_120.compat.has

// ===== 富集阳光合金II enriched_sunnarium_alloy2 =====
// zh_cn: 光谱富集阳光合金  en_us: Spectral Enriched Sunnarium Alloy
@ModItem(name = "enriched_sunnarium_alloy2", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "material")
class EnrichedSunnariumAlloy2 : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // ABA：2×富集阳光合金 + 1×奇点核心
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, EnrichedSunnariumAlloy2::class.instance(), 1)
                .pattern("ABA")
                .define('A', EnrichedSunnariumAlloy::class.instance())
                .define('B', SingularCore::class.instance())
                .unlockedBy(getHasName(SingularCore::class.instance()), has(SingularCore::class.instance()))
                .save(exporter, IC2IndustrialUpgrade.id("enriched_sunnarium_alloy2"))
        }
    }
}

// ===== 富集阳光合金III enriched_sunnarium_alloy3 =====
// zh_cn: 奇点富集阳光合金  en_us: Singular Enriched Sunnarium Alloy
@ModItem(name = "enriched_sunnarium_alloy3", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "material")
class EnrichedSunnariumAlloy3 : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // ABA：2×富集阳光合金II + 1×光子锭
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, EnrichedSunnariumAlloy3::class.instance(), 1)
                .pattern("ABA")
                .define('A', EnrichedSunnariumAlloy2::class.instance())
                .define('B', PhotoniyIngot::class.instance())
                .unlockedBy(getHasName(PhotoniyIngot::class.instance()), has(PhotoniyIngot::class.instance()))
                .save(exporter, IC2IndustrialUpgrade.id("enriched_sunnarium_alloy3"))
        }
    }
}

// ===== 富集阳光合金IV enriched_sunnarium_alloy4 =====
// zh_cn: 质子富集阳光合金  en_us: Proton Enriched Sunnarium Alloy
@ModItem(name = "enriched_sunnarium_alloy4", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "material")
class EnrichedSunnariumAlloy4 : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // ABA：2×富集阳光合金 + 1×光谱核心
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, EnrichedSunnariumAlloy4::class.instance(), 1)
                .pattern("ABA")
                .define('A', EnrichedSunnariumAlloy::class.instance())
                .define('B', SpectralCore::class.instance())
                .unlockedBy(getHasName(SpectralCore::class.instance()), has(SpectralCore::class.instance()))
                .save(exporter, IC2IndustrialUpgrade.id("enriched_sunnarium_alloy4"))
        }
    }
}

// ===== 光子 photoniy =====
// zh_cn: 光子  en_us: Photoniy
// 注：1.4.0 由分子重组仪产出（铱锭 → 光子），配方在入口注册到 ASA 的 MTRecipes
@ModItem(name = "photoniy", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "material")
class Photoniy : Item(Item.Properties())

// ===== 光子锭 photoniy_ingot =====
// zh_cn: 光子锭  en_us: Photoniy Ingot
// 注：压缩机 9×光子 → 1×光子锭，配方在 RecipeProvider 统一注册
@ModItem(name = "photoniy_ingot", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "material")
class PhotoniyIngot : Item(Item.Properties())
