package ic2_120_advanced_solar_addon.content.item

import net.minecraft.data.recipes.RecipeProvider.getHasName

import ic2_120.content.fluid.ModFluids
import ic2_120.content.item.*
import ic2_120.content.block.ReinforcedGlassBlock
import ic2_120.content.item.energy.ReBatteryItem
import ic2_120.content.recipes.ModTags
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.data.recipes.RecipeCategory
import java.util.function.Consumer
import net.minecraft.data.recipes.RecipeProvider.has

// i18n: item.ic2_120_advanced_solar_addon.sunnarium
// zh_cn: 阳光化合物
// en_us: Sunnarium
@ModItem(name = "sunnarium", tab = CreativeTab.IC2_SOLAR, group = "material")
class Sunnarium : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // 配方 1: UU物质 + 荧石粉
//            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Sunnarium::class.instance(), 1)
//                .pattern("UUU")
//                .pattern("GGG")
//                .pattern("UUU")
//                .define('U', ModFluids.UU_MATTER_BUCKET)
//                .define('G', Items.GLOWSTONE_DUST)
//                .unlockedBy(getHasName(ModFluids.UU_MATTER_BUCKET), has(ModFluids.UU_MATTER_BUCKET))
//                .save(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("sunnarium_from_uu"))

            // 配方 2: 9个小块阳光化合物
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Sunnarium::class.instance(), 1)
                .pattern("SSS")
                .pattern("SSS")
                .pattern("SSS")
                .define('S', SunnariumPart::class.instance())
                .unlockedBy(getHasName(SunnariumPart::class.instance()), has(SunnariumPart::class.instance()))
                .save(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("sunnarium_from_parts"))
        }
    }
}

// i18n: item.ic2_120_advanced_solar_addon.sunnarium_part
// zh_cn: 小块阳光化合物
// en_us: Sunnarium Part
@ModItem(name = "sunnarium_part", tab = CreativeTab.IC2_SOLAR, group = "material")
class SunnariumPart : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
//            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SunnariumPart::class.instance(), 1)
//                .pattern(" U ")
//                .pattern(" G ")
//                .pattern(" U ")
//                .define('U', ModFluids.UU_MATTER_BUCKET)
//                .define('G', Items.GLOWSTONE_DUST)
//                .unlockedBy(getHasName(ModFluids.UU_MATTER_BUCKET), has(ModFluids.UU_MATTER_BUCKET))
//                .save(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("sunnarium_part"))
        }
    }
}

// i18n: item.ic2_120_advanced_solar_addon.sunnarium_alloy
// zh_cn: 阳光合金
// en_us: Sunnarium Alloy
@ModItem(name = "sunnarium_alloy", tab = CreativeTab.IC2_SOLAR, group = "material")
class SunnariumAlloy : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SunnariumAlloy::class.instance(), 1)
                .pattern("III")
                .pattern("ISI")
                .pattern("III")
                .define('I', PhotovoltaicIridiumPlate::class.instance())
                .define('S', Sunnarium::class.instance())
                .unlockedBy(getHasName(PhotovoltaicIridiumPlate::class.instance()), has(PhotovoltaicIridiumPlate::class.instance()))
                .save(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("sunnarium_alloy"))
        }
    }
}

// i18n: item.ic2_120_advanced_solar_addon.photovoltaic_iridium_plate
// zh_cn: 光伏铱板
// en_us: Photovoltaic Iridium Plate
@ModItem(name = "photovoltaic_iridium_plate", tab = CreativeTab.IC2_SOLAR, group = "material")
class PhotovoltaicIridiumPlate : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // 与 core 强化铱板相同，仅将铱矿石替换为高级太阳能附属的铱锭
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PhotovoltaicIridiumPlate::class.instance(), 1)
                .pattern("IAI")
                .pattern("ADA")
                .pattern("IAI")
                .define('I', IridiumIngot::class.instance())
                .define('A', Alloy::class.instance())
                .define('D', Ingredient.of(ModTags.Compat.Items.GEMS_DIAMOND))
                .unlockedBy(getHasName(IridiumIngot::class.instance()), has(IridiumIngot::class.instance()))
                .save(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("photovoltaic_iridium_plate"))
        }
    }
}

// i18n: item.ic2_120_advanced_solar_addon.irradiant_uranium
// zh_cn: 光辉铀锭
// en_us: Irradiant Uranium
@ModItem(name = "irradiant_uranium", tab = CreativeTab.IC2_SOLAR, group = "material")
class IrradiantUranium : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, IrradiantUranium::class.instance(), 1)
                .pattern(" G ")
                .pattern("GUG")
                .pattern(" G ")
                .define('G', Items.GLOWSTONE_DUST)
                .define('U', ic2_120.content.item.UraniumIngot::class.instance())
                .unlockedBy(getHasName(ic2_120.content.item.UraniumIngot::class.instance()), has(ic2_120.content.item.UraniumIngot::class.instance()))
                .save(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("irradiant_uranium"))
        }
    }
}

// i18n: item.ic2_120_advanced_solar_addon.enriched_sunnarium
// zh_cn: 富集阳光化合物
// en_us: Enriched Sunnarium
@ModItem(name = "enriched_sunnarium", tab = CreativeTab.IC2_SOLAR, group = "material")
class EnrichedSunnarium : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, EnrichedSunnarium::class.instance(), 1)
                .pattern("III")
                .pattern("ISI")
                .pattern("III")
                .define('I', IrradiantUranium::class.instance())
                .define('S', Sunnarium::class.instance())
                .unlockedBy(getHasName(IrradiantUranium::class.instance()), has(IrradiantUranium::class.instance()))
                .save(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("enriched_sunnarium"))
        }
    }
}

// i18n: item.ic2_120_advanced_solar_addon.enriched_sunnarium_alloy
// zh_cn: 富集阳光合金
// en_us: Enriched Sunnarium Alloy
@ModItem(name = "enriched_sunnarium_alloy", tab = CreativeTab.IC2_SOLAR, group = "material")
class EnrichedSunnariumAlloy : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, EnrichedSunnariumAlloy::class.instance(), 1)
                .pattern(" E ")
                .pattern("EAE")
                .pattern(" E ")
                .define('E', EnrichedSunnarium::class.instance())
                .define('A', SunnariumAlloy::class.instance())
                .unlockedBy(getHasName(EnrichedSunnarium::class.instance()), has(EnrichedSunnarium::class.instance()))
                .save(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("enriched_sunnarium_alloy"))
        }
    }
}

// i18n: item.ic2_120_advanced_solar_addon.irradiant_glass_pane
// zh_cn: 光辉玻璃板
// en_us: Irradiant Glass Pane
@ModItem(name = "irradiant_glass_pane", tab = CreativeTab.IC2_SOLAR, group = "material")
class IrradiantGlassPane : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, IrradiantGlassPane::class.instance(), 6)
                .pattern("GGG")
                .pattern("IXI")
                .pattern("GGG")
                .define('G', ReinforcedGlassBlock::class.instance())
                .define('I', IrradiantUranium::class.instance())
                .define('X', Items.GLOWSTONE_DUST)
                .unlockedBy(getHasName(ReinforcedGlassBlock::class.instance()), has(ReinforcedGlassBlock::class.instance()))
                .save(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("irradiant_glass_pane"))
        }
    }
}

// i18n: item.ic2_120_advanced_solar_addon.iridium_iron_plate
// zh_cn: 铱铁合金板
// en_us: Iridium Iron Plate
@ModItem(name = "iridium_iron_plate", tab = CreativeTab.IC2_SOLAR, group = "material")
class IridiumIronPlate : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // 配方 1: 铁锭 + 铱锭
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, IridiumIronPlate::class.instance(), 1)
                .pattern("III")
                .pattern("IOI")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('O', IridiumIngot::class.instance())
                .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
                .save(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("iridium_iron_plate"))

            // // 配方 2: 铁板 + 铱锭 (简单模式)
            // ShapedRecipeBuilder.shaped(RecipeCategory.MISC, IridiumIronPlate::class.instance(), 1)
            //     .pattern("III")
            //     .pattern("I I")
            //     .pattern("III")
            //     .define('I', IronPlate::class.instance())
            //     .unlockedBy(getHasName(IronPlate::class.instance()), has(IronPlate::class.instance()))
            //     .save(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("iridium_iron_plate_from_plate"))
        }
    }
}

// i18n: item.ic2_120_advanced_solar_addon.reinforced_iridium_iron_plate
// zh_cn: 强化铱铁合金板
// en_us: Reinforced Iridium Iron Plate
@ModItem(name = "reinforced_iridium_iron_plate", tab = CreativeTab.IC2_SOLAR, group = "material")
class ReinforcedIridiumIronPlate : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ReinforcedIridiumIronPlate::class.instance(), 1)
                .pattern("ACA")
                .pattern("CPC")
                .pattern("ACA")
                .define('A', Alloy::class.instance())
                .define('C', CarbonPlate::class.instance())
                .define('P', IridiumIronPlate::class.instance())
                .unlockedBy(getHasName(IridiumIronPlate::class.instance()), has(IridiumIronPlate::class.instance()))
                .save(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("reinforced_iridium_iron_plate"))
        }
    }
}

// i18n: item.ic2_120_advanced_solar_addon.irradiant_reinforced_plate
// zh_cn: 光辉铱铁合金板
// en_us: Irradiant Reinforced Plate
@ModItem(name = "irradiant_reinforced_plate", tab = CreativeTab.IC2_SOLAR, group = "material")
class IrradiantReinforcedPlate : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, IrradiantReinforcedPlate::class.instance(), 1)
                .pattern("RPR")
                .pattern("LIL")
                .pattern("RDR")
                .define('R', Items.REDSTONE)
                .define('D', Items.DIAMOND)
                .define('L', Items.LAPIS_LAZULI)
                .define('P', SunnariumPart::class.instance())
                .define('I', ReinforcedIridiumIronPlate::class.instance())
                .unlockedBy(getHasName(SunnariumPart::class.instance()), has(SunnariumPart::class.instance()))
                .save(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("irradiant_reinforced_plate"))
        }
    }
}

// i18n: item.ic2_120_advanced_solar_addon.iridium_ingot
// zh_cn: 铱锭
// en_us: Iridium Ingot
@ModItem(name = "iridium_ingot", tab = CreativeTab.IC2_SOLAR, group = "material")
class IridiumIngot : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            // // 配方 1: 8个铱矿石杆
            // ShapedRecipeBuilder.shaped(RecipeCategory.MISC, IridiumIngot::class.instance(), 1)
            //     .pattern("III")
            //     .pattern("I I")
            //     .pattern("III")
            //     .define('I', IridiumOreItem::class.instance())
            //     .unlockedBy(getHasName(IridiumOreItem::class.instance()), has(IridiumOreItem::class.instance()))
            //     .save(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("iridium_ingot_from_ore"))

           // // 配方 3: 强化铱板 + 充电电池
            // ShapedRecipeBuilder.shaped(RecipeCategory.MISC, IridiumIngot::class.instance(), 1)
            //     .pattern(" I ")
            //     .pattern("   ")
            //     .pattern(" B ")
            //     .define('I', IridiumPlate::class.instance())
            //     .define('B', ReBatteryItem::class.instance())
            //     .unlockedBy(getHasName(IridiumPlate::class.instance()), has(IridiumPlate::class.instance()))
            //     .save(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("iridium_ingot_from_plate_battery"))
        }
    }
}

// i18n: item.ic2_120_advanced_solar_addon.mt_core
// zh_cn: 分子重组核心
// en_us: MT Core
@ModItem(name = "mt_core", tab = CreativeTab.IC2_SOLAR, group = "component")
class MtCore : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, MtCore::class.instance(), 1)
                .pattern("GNG")
                .pattern("G G")
                .pattern("GNG")
                .define('G', IrradiantGlassPane::class.instance())
                .define('N', ThickNeutronReflectorItem::class.instance())
                .unlockedBy(getHasName(IrradiantGlassPane::class.instance()), has(IrradiantGlassPane::class.instance()))
                .save(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("mt_core"))
        }
    }
}

// i18n: item.ic2_120_advanced_solar_addon.quantum_core
// zh_cn: 量子核心
// en_us: Quantum Core
@ModItem(name = "quantum_core", tab = CreativeTab.IC2_SOLAR, group = "component")
class QuantumCore : Item(Item.Properties()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, QuantumCore::class.instance(), 1)
                .pattern("ESE")
                .pattern("SNS")
                .pattern("ESE")
                .define('E', EnrichedSunnariumAlloy::class.instance())
                .define('S', Items.NETHER_STAR)
                .define('N', Items.ENDER_EYE)
                .unlockedBy(getHasName(Items.NETHER_STAR), has(Items.NETHER_STAR))
                .save(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("quantum_core"))
        }
    }
}
