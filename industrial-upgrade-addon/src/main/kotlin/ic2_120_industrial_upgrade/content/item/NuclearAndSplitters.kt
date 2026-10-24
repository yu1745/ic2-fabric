package ic2_120_industrial_upgrade.content.item

import ic2_120.content.block.ReinforcedGlassBlock
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120_advanced_solar_addon.content.item.IrradiantGlassPane
import ic2_120_industrial_upgrade.IC2IndustrialUpgrade
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import java.util.function.Consumer

// ===== 质子碎片 proton_shard =====
// zh_cn: 质子碎片  en_us: Proton Shard
// 注：压缩机 1×钚 → 1×质子碎片，配方在 RecipeProvider 统一注册
@ModItem(name = "proton_shard", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "material")
class ProtonShard : Item(Item.Settings())

// ===== 质子 proton =====
// zh_cn: 质子  en_us: Proton
// 注：压缩机 18×质子碎片 → 1×质子
@ModItem(name = "proton", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "material")
class Proton : Item(Item.Settings())

// ===== 中子碎片 neutron_shard =====
// zh_cn: 中子碎片  en_us: Neutron Shard
// 注：压缩机 1×中子流体桶 → 1×中子碎片（返还空桶）
@ModItem(name = "neutron_shard", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "material")
class NeutronShard : Item(Item.Settings())

// ===== 中子 neutron =====
// zh_cn: 中子  en_us: Neutron
// 注：压缩机 9×中子碎片 → 1×中子
@ModItem(name = "neutron", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "material")
class Neutron : Item(Item.Settings())

// ===== 红色组件 red_component =====
// zh_cn: 红色组件  en_us: Red Component
@ModItem(name = "red_component", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "component")
class RedComponent : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            // AAA/BBB/AAA：5×防爆玻璃 + 3×红石
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, RedComponent::class.instance(), 1)
                .pattern("AAA")
                .pattern("BBB")
                .pattern("AAA")
                .input('A', ReinforcedGlassBlock::class.item())
                .input('B', Items.REDSTONE)
                .criterion(hasItem(Items.REDSTONE), conditionsFromItem(Items.REDSTONE))
                .offerTo(exporter, IC2IndustrialUpgrade.id("red_component"))
        }
    }
}

// ===== 蓝色组件 blue_component =====
// zh_cn: 蓝色组件  en_us: Blue Component
@ModItem(name = "blue_component", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "component")
class BlueComponent : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            // AAA/BBB/AAA：5×防爆玻璃 + 3×青金石
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, BlueComponent::class.instance(), 1)
                .pattern("AAA")
                .pattern("BBB")
                .pattern("AAA")
                .input('A', ReinforcedGlassBlock::class.item())
                .input('B', Items.LAPIS_LAZULI)
                .criterion(hasItem(Items.LAPIS_LAZULI), conditionsFromItem(Items.LAPIS_LAZULI))
                .offerTo(exporter, IC2IndustrialUpgrade.id("blue_component"))
        }
    }
}

// ===== 绿色组件 green_component =====
// zh_cn: 绿色组件  en_us: Green Component
@ModItem(name = "green_component", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "component")
class GreenComponent : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            // A  ：1×光辉玻璃板
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, GreenComponent::class.instance(), 1)
                .pattern("A  ")
                .input('A', IrradiantGlassPane::class.instance())
                .criterion(hasItem(IrradiantGlassPane::class.instance()), conditionsFromItem(IrradiantGlassPane::class.instance()))
                .offerTo(exporter, IC2IndustrialUpgrade.id("green_component"))
        }
    }
}

// ===== 分光器 solar_splitter =====
// zh_cn: 分光器  en_us: Solar Splitter
@ModItem(name = "solar_splitter", tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "component")
class SolarSplitter : Item(Item.Settings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            // ABC/ABC/ABC：3×红色 + 3×绿色 + 3×蓝色
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, SolarSplitter::class.instance(), 1)
                .pattern("ABC")
                .pattern("ABC")
                .pattern("ABC")
                .input('A', RedComponent::class.instance())
                .input('B', GreenComponent::class.instance())
                .input('C', BlueComponent::class.instance())
                .criterion(hasItem(RedComponent::class.instance()), conditionsFromItem(RedComponent::class.instance()))
                .offerTo(exporter, IC2IndustrialUpgrade.id("solar_splitter"))
        }
    }
}
