package ic2_120_industrial_upgrade.content.item

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120_advanced_solar_addon.content.item.EnrichedSunnariumAlloy
import ic2_120_industrial_upgrade.IC2IndustrialUpgrade
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.item.Item
import net.minecraft.data.recipes.RecipeCategory
import java.util.function.Consumer

// ===== 富集阳光合金II enriched_sunnarium_alloy2 =====
// zh_cn: 光谱富集阳光合金  en_us: Spectral Enriched Sunnarium Alloy
@ModItem(name = "enriched_sunnarium_alloy2", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "material")
class EnrichedSunnariumAlloy2 : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            // ABA：2×富集阳光合金 + 1×奇点核心
            ShapedRecipeBuilder.create(RecipeCategory.MISC, EnrichedSunnariumAlloy2::class.instance(), 1)
                .pattern("ABA")
                .input('A', EnrichedSunnariumAlloy::class.instance())
                .input('B', SingularCore::class.instance())
                .criterion(hasItem(SingularCore::class.instance()), conditionsFromItem(SingularCore::class.instance()))
                .offerTo(exporter, IC2IndustrialUpgrade.id("enriched_sunnarium_alloy2"))
        }
    }
}

// ===== 富集阳光合金III enriched_sunnarium_alloy3 =====
// zh_cn: 奇点富集阳光合金  en_us: Singular Enriched Sunnarium Alloy
@ModItem(name = "enriched_sunnarium_alloy3", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "material")
class EnrichedSunnariumAlloy3 : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            // ABA：2×富集阳光合金II + 1×光子锭
            ShapedRecipeBuilder.create(RecipeCategory.MISC, EnrichedSunnariumAlloy3::class.instance(), 1)
                .pattern("ABA")
                .input('A', EnrichedSunnariumAlloy2::class.instance())
                .input('B', PhotoniyIngot::class.instance())
                .criterion(hasItem(PhotoniyIngot::class.instance()), conditionsFromItem(PhotoniyIngot::class.instance()))
                .offerTo(exporter, IC2IndustrialUpgrade.id("enriched_sunnarium_alloy3"))
        }
    }
}

// ===== 富集阳光合金IV enriched_sunnarium_alloy4 =====
// zh_cn: 质子富集阳光合金  en_us: Proton Enriched Sunnarium Alloy
@ModItem(name = "enriched_sunnarium_alloy4", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "material")
class EnrichedSunnariumAlloy4 : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeOutput>) {
            // ABA：2×富集阳光合金 + 1×光谱核心
            ShapedRecipeBuilder.create(RecipeCategory.MISC, EnrichedSunnariumAlloy4::class.instance(), 1)
                .pattern("ABA")
                .input('A', EnrichedSunnariumAlloy::class.instance())
                .input('B', SpectralCore::class.instance())
                .criterion(hasItem(SpectralCore::class.instance()), conditionsFromItem(SpectralCore::class.instance()))
                .offerTo(exporter, IC2IndustrialUpgrade.id("enriched_sunnarium_alloy4"))
        }
    }
}

// ===== 光子 photoniy =====
// zh_cn: 光子  en_us: Photoniy
// 注：1.4.0 由分子重组仪产出（铱锭 → 光子），配方在入口注册到 ASA 的 MTRecipes
@ModItem(name = "photoniy", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "material")
class Photoniy : Item(Item.Settings())

// ===== 光子锭 photoniy_ingot =====
// zh_cn: 光子锭  en_us: Photoniy Ingot
// 注：压缩机 9×光子 → 1×光子锭，配方在 RecipeProvider 统一注册
@ModItem(name = "photoniy_ingot", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "material")
class PhotoniyIngot : Item(Item.Settings())
