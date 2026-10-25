package ic2_120_industrial_upgrade.content.item

import ic2_120.compat.save

import net.minecraft.data.recipes.RecipeProvider.getHasName

import ic2_120.content.item.IridiumOreItem
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120_advanced_solar_addon.content.item.EnrichedSunnariumAlloy
import ic2_120_advanced_solar_addon.content.item.PhotovoltaicIridiumPlate
import ic2_120_advanced_solar_addon.content.item.QuantumCore
import ic2_120_industrial_upgrade.IC2IndustrialUpgrade
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory
import java.util.function.Consumer
import ic2_120.compat.has

// ===== 光谱核心 spectral_core =====
// zh_cn: 光谱核心  en_us: Spectral Core
@ModItem(name = "spectral_core", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "component")
class SpectralCore : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // CBC/CAC/CBC：1×量子核心 + 4×光子 + 4×分光器
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, SpectralCore::class.instance(), 1)
                .pattern("CBC")
                .pattern("CAC")
                .pattern("CBC")
                .define('A', QuantumCore::class.instance())
                .define('B', Photoniy::class.instance())
                .define('C', SolarSplitter::class.instance())
                .unlockedBy(getHasName(QuantumCore::class.instance()), has(QuantumCore::class.instance()))
                .save(exporter, IC2IndustrialUpgrade.id("spectral_core"))
        }
    }
}

// ===== 质子核心 proton_core =====
// zh_cn: 质子核心  en_us: Proton Core
@ModItem(name = "proton_core", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "component")
class ProtonCore : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // +形：4×质子 + 2×富集阳光合金IV + 1×光谱核心
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, ProtonCore::class.instance(), 1)
                .pattern(" B ")
                .pattern("ACA")
                .pattern(" B ")
                .define('A', EnrichedSunnariumAlloy4::class.instance())
                .define('B', Proton::class.instance())
                .define('C', SpectralCore::class.instance())
                .unlockedBy(getHasName(SpectralCore::class.instance()), has(SpectralCore::class.instance()))
                .save(exporter, IC2IndustrialUpgrade.id("proton_core"))
        }
    }
}

// ===== 奇点核心 singular_core =====
// zh_cn: 奇点核心  en_us: Singular Core
@ModItem(name = "singular_core", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "component")
class SingularCore : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // ABA/DCD/ABA：4×末影量子组件 + 1×光子锭 + 2×光伏铱板 + 2×富集阳光合金
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, SingularCore::class.instance(), 1)
                .pattern("ABA")
                .pattern("DCD")
                .pattern("ABA")
                .define('A', EnderQuantumComponent::class.instance())
                .define('B', PhotovoltaicIridiumPlate::class.instance())
                .define('C', PhotoniyIngot::class.instance())
                .define('D', EnrichedSunnariumAlloy::class.instance())
                .unlockedBy(getHasName(EnderQuantumComponent::class.instance()), has(EnderQuantumComponent::class.instance()))
                .save(exporter, IC2IndustrialUpgrade.id("singular_core"))
        }
    }
}

// ===== 量子核心II quant_core2 =====
// zh_cn: 量子核心II  en_us: Quantum Core II
@ModItem(name = "quant_core2", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "component")
class QuantCore2 : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // ABA：2×富集阳光合金III + 1×奇点核心
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, QuantCore2::class.instance(), 1)
                .pattern("ABA")
                .define('A', EnrichedSunnariumAlloy3::class.instance())
                .define('B', SingularCore::class.instance())
                .unlockedBy(getHasName(SingularCore::class.instance()), has(SingularCore::class.instance()))
                .save(exporter, IC2IndustrialUpgrade.id("quant_core2"))
        }
    }
}

// ===== 量子核心I quant_core1 =====
// zh_cn: 量子核心I  en_us: Quantum Core I
@ModItem(name = "quant_core1", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "component")
class QuantCore1 : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // ABA：2×富集阳光合金II + 1×量子核心II
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, QuantCore1::class.instance(), 1)
                .pattern("ABA")
                .define('A', EnrichedSunnariumAlloy2::class.instance())
                .define('B', QuantCore2::class.instance())
                .unlockedBy(getHasName(QuantCore2::class.instance()), has(QuantCore2::class.instance()))
                .save(exporter, IC2IndustrialUpgrade.id("quant_core1"))
        }
    }
}

// ===== 中子核心 neutron_core =====
// zh_cn: 中子核心  en_us: Neutron Core
@ModItem(name = "neutron_core", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "component")
class NeutronCore : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // +形：4×中子 + 1×量子核心II
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, NeutronCore::class.instance(), 1)
                .pattern(" A ")
                .pattern("ABA")
                .pattern(" A ")
                .define('A', Neutron::class.instance())
                .define('B', QuantCore2::class.instance())
                .unlockedBy(getHasName(QuantCore2::class.instance()), has(QuantCore2::class.instance()))
                .save(exporter, IC2IndustrialUpgrade.id("neutron_core"))
        }
    }
}

// ===== 末影量子组件 ender_quantum_component =====
// zh_cn: 末影量子组件  en_us: Ender Quantum Component
@ModItem(name = "ender_quantum_component", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "component")
class EnderQuantumComponent : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // ABA/BCB/ABA：4×光伏铱板 + 4×末影之眼 + 1×下界之星
            // （1.4.0 原用 IC2 强化铱板，按计划全部替换为光伏铱板）
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, EnderQuantumComponent::class.instance(), 1)
                .pattern("ABA")
                .pattern("BCB")
                .pattern("ABA")
                .define('A', PhotovoltaicIridiumPlate::class.instance())
                .define('B', Items.ENDER_EYE)
                .define('C', Items.NETHER_STAR)
                .unlockedBy(getHasName(Items.NETHER_STAR), has(Items.NETHER_STAR))
                .save(exporter, IC2IndustrialUpgrade.id("ender_quantum_component"))
        }
    }
}
