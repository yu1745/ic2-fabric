package ic2_120_industrial_upgrade.content.item

import ic2_120.content.item.IridiumOreItem
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120_advanced_solar_addon.content.item.EnrichedSunnariumAlloy
import ic2_120_advanced_solar_addon.content.item.PhotovoltaicIridiumPlate
import ic2_120_advanced_solar_addon.content.item.QuantumCore
import ic2_120_industrial_upgrade.IC2IndustrialUpgrade
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import java.util.function.Consumer

// ===== 光谱核心 spectral_core =====
// zh_cn: 光谱核心  en_us: Spectral Core
@ModItem(name = "spectral_core", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "component")
class SpectralCore : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            // CBC/CAC/CBC：1×量子核心 + 4×光子 + 4×分光器
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, SpectralCore::class.instance(), 1)
                .pattern("CBC")
                .pattern("CAC")
                .pattern("CBC")
                .input('A', QuantumCore::class.instance())
                .input('B', Photoniy::class.instance())
                .input('C', SolarSplitter::class.instance())
                .criterion(hasItem(QuantumCore::class.instance()), conditionsFromItem(QuantumCore::class.instance()))
                .offerTo(exporter, IC2IndustrialUpgrade.id("spectral_core"))
        }
    }
}

// ===== 质子核心 proton_core =====
// zh_cn: 质子核心  en_us: Proton Core
@ModItem(name = "proton_core", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "component")
class ProtonCore : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            // +形：4×质子 + 2×富集阳光合金IV + 1×光谱核心
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ProtonCore::class.instance(), 1)
                .pattern(" B ")
                .pattern("ACA")
                .pattern(" B ")
                .input('A', EnrichedSunnariumAlloy4::class.instance())
                .input('B', Proton::class.instance())
                .input('C', SpectralCore::class.instance())
                .criterion(hasItem(SpectralCore::class.instance()), conditionsFromItem(SpectralCore::class.instance()))
                .offerTo(exporter, IC2IndustrialUpgrade.id("proton_core"))
        }
    }
}

// ===== 奇点核心 singular_core =====
// zh_cn: 奇点核心  en_us: Singular Core
@ModItem(name = "singular_core", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "component")
class SingularCore : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            // ABA/DCD/ABA：4×末影量子组件 + 1×光子锭 + 2×光伏铱板 + 2×富集阳光合金
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, SingularCore::class.instance(), 1)
                .pattern("ABA")
                .pattern("DCD")
                .pattern("ABA")
                .input('A', EnderQuantumComponent::class.instance())
                .input('B', PhotovoltaicIridiumPlate::class.instance())
                .input('C', PhotoniyIngot::class.instance())
                .input('D', EnrichedSunnariumAlloy::class.instance())
                .criterion(hasItem(EnderQuantumComponent::class.instance()), conditionsFromItem(EnderQuantumComponent::class.instance()))
                .offerTo(exporter, IC2IndustrialUpgrade.id("singular_core"))
        }
    }
}

// ===== 量子核心II quant_core2 =====
// zh_cn: 量子核心II  en_us: Quantum Core II
@ModItem(name = "quant_core2", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "component")
class QuantCore2 : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            // ABA：2×富集阳光合金III + 1×奇点核心
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, QuantCore2::class.instance(), 1)
                .pattern("ABA")
                .input('A', EnrichedSunnariumAlloy3::class.instance())
                .input('B', SingularCore::class.instance())
                .criterion(hasItem(SingularCore::class.instance()), conditionsFromItem(SingularCore::class.instance()))
                .offerTo(exporter, IC2IndustrialUpgrade.id("quant_core2"))
        }
    }
}

// ===== 量子核心I quant_core1 =====
// zh_cn: 量子核心I  en_us: Quantum Core I
@ModItem(name = "quant_core1", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "component")
class QuantCore1 : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            // ABA：2×富集阳光合金II + 1×量子核心II
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, QuantCore1::class.instance(), 1)
                .pattern("ABA")
                .input('A', EnrichedSunnariumAlloy2::class.instance())
                .input('B', QuantCore2::class.instance())
                .criterion(hasItem(QuantCore2::class.instance()), conditionsFromItem(QuantCore2::class.instance()))
                .offerTo(exporter, IC2IndustrialUpgrade.id("quant_core1"))
        }
    }
}

// ===== 中子核心 neutron_core =====
// zh_cn: 中子核心  en_us: Neutron Core
@ModItem(name = "neutron_core", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "component")
class NeutronCore : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            // +形：4×中子 + 1×量子核心II
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, NeutronCore::class.instance(), 1)
                .pattern(" A ")
                .pattern("ABA")
                .pattern(" A ")
                .input('A', Neutron::class.instance())
                .input('B', QuantCore2::class.instance())
                .criterion(hasItem(QuantCore2::class.instance()), conditionsFromItem(QuantCore2::class.instance()))
                .offerTo(exporter, IC2IndustrialUpgrade.id("neutron_core"))
        }
    }
}

// ===== 末影量子组件 ender_quantum_component =====
// zh_cn: 末影量子组件  en_us: Ender Quantum Component
@ModItem(name = "ender_quantum_component", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "component")
class EnderQuantumComponent : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            // ABA/BCB/ABA：4×光伏铱板 + 4×末影之眼 + 1×下界之星
            // （1.4.0 原用 IC2 强化铱板，按计划全部替换为光伏铱板）
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, EnderQuantumComponent::class.instance(), 1)
                .pattern("ABA")
                .pattern("BCB")
                .pattern("ABA")
                .input('A', PhotovoltaicIridiumPlate::class.instance())
                .input('B', Items.ENDER_EYE)
                .input('C', Items.NETHER_STAR)
                .criterion(hasItem(Items.NETHER_STAR), conditionsFromItem(Items.NETHER_STAR))
                .offerTo(exporter, IC2IndustrialUpgrade.id("ender_quantum_component"))
        }
    }
}
