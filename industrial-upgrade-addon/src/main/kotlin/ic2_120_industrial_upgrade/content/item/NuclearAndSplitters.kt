package ic2_120_industrial_upgrade.content.item

import ic2_120.compat.save

import net.minecraft.data.recipes.RecipeProvider.getHasName

import ic2_120.content.block.ReinforcedGlassBlock
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120_advanced_solar_addon.content.item.IrradiantGlassPane
import ic2_120_industrial_upgrade.IC2IndustrialUpgrade
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory
import java.util.function.Consumer
import ic2_120.compat.has

// ===== 质子碎片 proton_shard =====
// zh_cn: 质子碎片  en_us: Proton Shard
// 注：压缩机 1×钚 → 1×质子碎片，配方在 RecipeProvider 统一注册
@ModItem(name = "proton_shard", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "material")
class ProtonShard : Item(Item.Properties())

// ===== 质子 proton =====
// zh_cn: 质子  en_us: Proton
// 注：压缩机 18×质子碎片 → 1×质子
@ModItem(name = "proton", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "material")
class Proton : Item(Item.Properties())

// ===== 中子碎片 neutron_shard =====
// zh_cn: 中子碎片  en_us: Neutron Shard
// 注：压缩机 1×中子流体桶 → 1×中子碎片（返还空桶）
@ModItem(name = "neutron_shard", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "material")
class NeutronShard : Item(Item.Properties())

// ===== 中子 neutron =====
// zh_cn: 中子  en_us: Neutron
// 注：压缩机 9×中子碎片 → 1×中子
@ModItem(name = "neutron", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "material")
class Neutron : Item(Item.Properties())

// ===== 红色组件 red_component =====
// zh_cn: 红色组件  en_us: Red Component
@ModItem(name = "red_component", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "component")
class RedComponent : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // AAA/BBB/AAA：5×防爆玻璃 + 3×红石
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, RedComponent::class.instance(), 1)
                .pattern("AAA")
                .pattern("BBB")
                .pattern("AAA")
                .define('A', ReinforcedGlassBlock::class.item())
                .define('B', Items.REDSTONE)
                .unlockedBy(getHasName(Items.REDSTONE), has(Items.REDSTONE))
                .save(exporter, IC2IndustrialUpgrade.id("red_component"))
        }
    }
}

// ===== 蓝色组件 blue_component =====
// zh_cn: 蓝色组件  en_us: Blue Component
@ModItem(name = "blue_component", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "component")
class BlueComponent : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // AAA/BBB/AAA：5×防爆玻璃 + 3×青金石
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, BlueComponent::class.instance(), 1)
                .pattern("AAA")
                .pattern("BBB")
                .pattern("AAA")
                .define('A', ReinforcedGlassBlock::class.item())
                .define('B', Items.LAPIS_LAZULI)
                .unlockedBy(getHasName(Items.LAPIS_LAZULI), has(Items.LAPIS_LAZULI))
                .save(exporter, IC2IndustrialUpgrade.id("blue_component"))
        }
    }
}

// ===== 绿色组件 green_component =====
// zh_cn: 绿色组件  en_us: Green Component
@ModItem(name = "green_component", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "component")
class GreenComponent : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // A  ：1×光辉玻璃板
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, GreenComponent::class.instance(), 1)
                .pattern("A  ")
                .define('A', IrradiantGlassPane::class.instance())
                .unlockedBy(getHasName(IrradiantGlassPane::class.instance()), has(IrradiantGlassPane::class.instance()))
                .save(exporter, IC2IndustrialUpgrade.id("green_component"))
        }
    }
}

// ===== 分光器 solar_splitter =====
// zh_cn: 分光器  en_us: Solar Splitter
@ModItem(name = "solar_splitter", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "component")
class SolarSplitter : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // ABC/ABC/ABC：3×红色 + 3×绿色 + 3×蓝色
            ShapedRecipeBuilder.shaped(net.minecraft.core.registries.BuiltInRegistries.ITEM, RecipeCategory.MISC, SolarSplitter::class.instance(), 1)
                .pattern("ABC")
                .pattern("ABC")
                .pattern("ABC")
                .define('A', RedComponent::class.instance())
                .define('B', GreenComponent::class.instance())
                .define('C', BlueComponent::class.instance())
                .unlockedBy(getHasName(RedComponent::class.instance()), has(RedComponent::class.instance()))
                .save(exporter, IC2IndustrialUpgrade.id("solar_splitter"))
        }
    }
}
