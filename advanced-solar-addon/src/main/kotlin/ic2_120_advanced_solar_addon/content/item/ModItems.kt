package ic2_120_advanced_solar_addon.content.item

import ic2_120.content.fluid.ModFluids
import ic2_120.content.item.*
import ic2_120.content.block.ReinforcedGlassBlock
import ic2_120.content.item.energy.ReBatteryItem
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.book.RecipeCategory
import java.util.function.Consumer

// i18n: item.ic2_120_advanced_solar_addon.sunnarium
// zh_cn: 阳光化合物
// en_us: Sunnarium
@ModItem(name = "sunnarium", tab = CreativeTab.IC2_SOLAR, group = "material")
class Sunnarium : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            // 配方 1: UU物质 + 荧石粉
//            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, Sunnarium::class.instance(), 1)
//                .pattern("UUU")
//                .pattern("GGG")
//                .pattern("UUU")
//                .input('U', ModFluids.UU_MATTER_BUCKET)
//                .input('G', Items.GLOWSTONE_DUST)
//                .criterion(hasItem(ModFluids.UU_MATTER_BUCKET), conditionsFromItem(ModFluids.UU_MATTER_BUCKET))
//                .offerTo(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("sunnarium_from_uu"))

            // 配方 2: 9个小块阳光化合物
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, Sunnarium::class.instance(), 1)
                .pattern("SSS")
                .pattern("SSS")
                .pattern("SSS")
                .input('S', SunnariumPart::class.instance())
                .criterion(hasItem(SunnariumPart::class.instance()), conditionsFromItem(SunnariumPart::class.instance()))
                .offerTo(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("sunnarium_from_parts"))
        }
    }
}

// i18n: item.ic2_120_advanced_solar_addon.sunnarium_part
// zh_cn: 小块阳光化合物
// en_us: Sunnarium Part
@ModItem(name = "sunnarium_part", tab = CreativeTab.IC2_SOLAR, group = "material")
class SunnariumPart : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
//            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, SunnariumPart::class.instance(), 1)
//                .pattern(" U ")
//                .pattern(" G ")
//                .pattern(" U ")
//                .input('U', ModFluids.UU_MATTER_BUCKET)
//                .input('G', Items.GLOWSTONE_DUST)
//                .criterion(hasItem(ModFluids.UU_MATTER_BUCKET), conditionsFromItem(ModFluids.UU_MATTER_BUCKET))
//                .offerTo(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("sunnarium_part"))
        }
    }
}

// i18n: item.ic2_120_advanced_solar_addon.sunnarium_alloy
// zh_cn: 阳光合金
// en_us: Sunnarium Alloy
@ModItem(name = "sunnarium_alloy", tab = CreativeTab.IC2_SOLAR, group = "material")
class SunnariumAlloy : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, SunnariumAlloy::class.instance(), 1)
                .pattern("III")
                .pattern("ISI")
                .pattern("III")
                .input('I', IridiumPlate::class.instance())
                .input('S', Sunnarium::class.instance())
                .criterion(hasItem(IridiumPlate::class.instance()), conditionsFromItem(IridiumPlate::class.instance()))
                .offerTo(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("sunnarium_alloy"))
        }
    }
}

// i18n: item.ic2_120_advanced_solar_addon.irradiant_uranium
// zh_cn: 光辉铀锭
// en_us: Irradiant Uranium
@ModItem(name = "irradiant_uranium", tab = CreativeTab.IC2_SOLAR, group = "material")
class IrradiantUranium : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, IrradiantUranium::class.instance(), 1)
                .pattern(" G ")
                .pattern("GUG")
                .pattern(" G ")
                .input('G', Items.GLOWSTONE_DUST)
                .input('U', ic2_120.content.item.UraniumIngot::class.instance())
                .criterion(hasItem(ic2_120.content.item.UraniumIngot::class.instance()), conditionsFromItem(ic2_120.content.item.UraniumIngot::class.instance()))
                .offerTo(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("irradiant_uranium"))
        }
    }
}

// i18n: item.ic2_120_advanced_solar_addon.enriched_sunnarium
// zh_cn: 富集阳光化合物
// en_us: Enriched Sunnarium
@ModItem(name = "enriched_sunnarium", tab = CreativeTab.IC2_SOLAR, group = "material")
class EnrichedSunnarium : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, EnrichedSunnarium::class.instance(), 1)
                .pattern("III")
                .pattern("ISI")
                .pattern("III")
                .input('I', IrradiantUranium::class.instance())
                .input('S', Sunnarium::class.instance())
                .criterion(hasItem(IrradiantUranium::class.instance()), conditionsFromItem(IrradiantUranium::class.instance()))
                .offerTo(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("enriched_sunnarium"))
        }
    }
}

// i18n: item.ic2_120_advanced_solar_addon.enriched_sunnarium_alloy
// zh_cn: 富集阳光合金
// en_us: Enriched Sunnarium Alloy
@ModItem(name = "enriched_sunnarium_alloy", tab = CreativeTab.IC2_SOLAR, group = "material")
class EnrichedSunnariumAlloy : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, EnrichedSunnariumAlloy::class.instance(), 1)
                .pattern(" E ")
                .pattern("EAE")
                .pattern(" E ")
                .input('E', EnrichedSunnarium::class.instance())
                .input('A', SunnariumAlloy::class.instance())
                .criterion(hasItem(EnrichedSunnarium::class.instance()), conditionsFromItem(EnrichedSunnarium::class.instance()))
                .offerTo(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("enriched_sunnarium_alloy"))
        }
    }
}

// i18n: item.ic2_120_advanced_solar_addon.irradiant_glass_pane
// zh_cn: 光辉玻璃板
// en_us: Irradiant Glass Pane
@ModItem(name = "irradiant_glass_pane", tab = CreativeTab.IC2_SOLAR, group = "material")
class IrradiantGlassPane : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, IrradiantGlassPane::class.instance(), 1)
                .pattern("GGG")
                .pattern("GIG")
                .pattern("GGG")
                .input('G', ReinforcedGlassBlock::class.instance())
                .input('I', IrradiantUranium::class.instance())
                .criterion(hasItem(ReinforcedGlassBlock::class.instance()), conditionsFromItem(ReinforcedGlassBlock::class.instance()))
                .offerTo(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("irradiant_glass_pane"))
        }
    }
}

// i18n: item.ic2_120_advanced_solar_addon.iridium_iron_plate
// zh_cn: 铱铁合金板
// en_us: Iridium Iron Plate
@ModItem(name = "iridium_iron_plate", tab = CreativeTab.IC2_SOLAR, group = "material")
class IridiumIronPlate : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            // 配方 1: 铁锭 + 铱锭
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, IridiumIronPlate::class.instance(), 1)
                .pattern("III")
                .pattern("IOI")
                .pattern("III")
                .input('I', Items.IRON_INGOT)
                .input('O', IridiumIngot::class.instance())
                .criterion(hasItem(Items.IRON_INGOT), conditionsFromItem(Items.IRON_INGOT))
                .offerTo(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("iridium_iron_plate"))

            // // 配方 2: 铁板 + 铱锭 (简单模式)
            // ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, IridiumIronPlate::class.instance(), 1)
            //     .pattern("III")
            //     .pattern("I I")
            //     .pattern("III")
            //     .input('I', IronPlate::class.instance())
            //     .criterion(hasItem(IronPlate::class.instance()), conditionsFromItem(IronPlate::class.instance()))
            //     .offerTo(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("iridium_iron_plate_from_plate"))
        }
    }
}

// i18n: item.ic2_120_advanced_solar_addon.reinforced_iridium_iron_plate
// zh_cn: 强化铱铁合金板
// en_us: Reinforced Iridium Iron Plate
@ModItem(name = "reinforced_iridium_iron_plate", tab = CreativeTab.IC2_SOLAR, group = "material")
class ReinforcedIridiumIronPlate : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ReinforcedIridiumIronPlate::class.instance(), 1)
                .pattern("ACA")
                .pattern("CPC")
                .pattern("ACA")
                .input('A', Alloy::class.instance())
                .input('C', CarbonPlate::class.instance())
                .input('P', IridiumIronPlate::class.instance())
                .criterion(hasItem(IridiumIronPlate::class.instance()), conditionsFromItem(IridiumIronPlate::class.instance()))
                .offerTo(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("reinforced_iridium_iron_plate"))
        }
    }
}

// i18n: item.ic2_120_advanced_solar_addon.irradiant_reinforced_plate
// zh_cn: 光辉铱铁合金板
// en_us: Irradiant Reinforced Plate
@ModItem(name = "irradiant_reinforced_plate", tab = CreativeTab.IC2_SOLAR, group = "material")
class IrradiantReinforcedPlate : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, IrradiantReinforcedPlate::class.instance(), 1)
                .pattern("RDR")
                .pattern("LPL")
                .pattern("RIR")
                .input('R', Items.REDSTONE)
                .input('D', Items.DIAMOND)
                .input('L', Items.LAPIS_LAZULI)
                .input('P', SunnariumPart::class.instance())
                .input('I', ReinforcedIridiumIronPlate::class.instance())
                .criterion(hasItem(SunnariumPart::class.instance()), conditionsFromItem(SunnariumPart::class.instance()))
                .offerTo(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("irradiant_reinforced_plate"))
        }
    }
}

// i18n: item.ic2_120_advanced_solar_addon.iridium_ingot
// zh_cn: 铱锭
// en_us: Iridium Ingot
@ModItem(name = "iridium_ingot", tab = CreativeTab.IC2_SOLAR, group = "material")
class IridiumIngot : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            // // 配方 1: 8个铱矿石杆
            // ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, IridiumIngot::class.instance(), 1)
            //     .pattern("III")
            //     .pattern("I I")
            //     .pattern("III")
            //     .input('I', IridiumOreItem::class.instance())
            //     .criterion(hasItem(IridiumOreItem::class.instance()), conditionsFromItem(IridiumOreItem::class.instance()))
            //     .offerTo(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("iridium_ingot_from_ore"))

            // 配方 2: 铱矿石
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, IridiumIngot::class.instance(), 1)
                .pattern("   ")
                .pattern(" O ")
                .pattern("   ")
                .input('O', IridiumOreItem::class.instance())
                .criterion(hasItem(IridiumOreItem::class.instance()), conditionsFromItem(IridiumOreItem::class.instance()))
                .offerTo(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("iridium_ingot_from_ore"))

            // // 配方 3: 强化铱板 + 充电电池
            // ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, IridiumIngot::class.instance(), 1)
            //     .pattern(" I ")
            //     .pattern("   ")
            //     .pattern(" B ")
            //     .input('I', IridiumPlate::class.instance())
            //     .input('B', ReBatteryItem::class.instance())
            //     .criterion(hasItem(IridiumPlate::class.instance()), conditionsFromItem(IridiumPlate::class.instance()))
            //     .offerTo(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("iridium_ingot_from_plate_battery"))
        }
    }
}

// i18n: item.ic2_120_advanced_solar_addon.mt_core
// zh_cn: 分子重组核心
// en_us: MT Core
@ModItem(name = "mt_core", tab = CreativeTab.IC2_SOLAR, group = "component")
class MtCore : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, MtCore::class.instance(), 1)
                .pattern("GNG")
                .pattern("G G")
                .pattern("GNG")
                .input('G', IrradiantGlassPane::class.instance())
                .input('N', ThickNeutronReflectorItem::class.instance())
                .criterion(hasItem(IrradiantGlassPane::class.instance()), conditionsFromItem(IrradiantGlassPane::class.instance()))
                .offerTo(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("mt_core"))
        }
    }
}

// i18n: item.ic2_120_advanced_solar_addon.quantum_core
// zh_cn: 量子核心
// en_us: Quantum Core
@ModItem(name = "quantum_core", tab = CreativeTab.IC2_SOLAR, group = "component")
class QuantumCore : Item(FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, QuantumCore::class.instance(), 1)
                .pattern("ESE")
                .pattern("SNS")
                .pattern("ESE")
                .input('E', EnrichedSunnariumAlloy::class.instance())
                .input('S', Items.NETHER_STAR)
                .input('N', Items.ENDER_EYE)
                .criterion(hasItem(Items.NETHER_STAR), conditionsFromItem(Items.NETHER_STAR))
                .offerTo(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("quantum_core"))
        }
    }
}
