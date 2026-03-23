package ic2_120.content.item

import ic2_120.Ic2_120
import ic2_120.registry.CreativeTab
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.type
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.type
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.util.Identifier
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import java.util.function.Consumer

// ========== 碳材料类 ==========

@ModItem(name = "carbon_fibre", tab = CreativeTab.IC2_MATERIALS, group = "carbon_materials")
class CarbonFibre : Item(FabricItemSettings())

@ModItem(name = "carbon_mesh", tab = CreativeTab.IC2_MATERIALS, group = "carbon_materials")
class CarbonMesh : Item(FabricItemSettings())

@ModItem(name = "carbon_plate", tab = CreativeTab.IC2_MATERIALS, group = "carbon_materials")
class CarbonPlate : Item(FabricItemSettings())

@ModItem(name = "wooden_rotor_blade", tab = CreativeTab.IC2_MATERIALS, group = "rotor_blades")
class WoodenRotorBlade : Item(FabricItemSettings())

@ModItem(name = "bronze_rotor_blade", tab = CreativeTab.IC2_MATERIALS, group = "rotor_blades")
class BronzeRotorBlade : Item(FabricItemSettings())

@ModItem(name = "iron_rotor_blade", tab = CreativeTab.IC2_MATERIALS, group = "rotor_blades")
class IronRotorBlade : Item(FabricItemSettings())

@ModItem(name = "steel_rotor_blade", tab = CreativeTab.IC2_MATERIALS, group = "rotor_blades")
class SteelRotorBlade : Item(FabricItemSettings())

@ModItem(name = "carbon_rotor_blade", tab = CreativeTab.IC2_MATERIALS, group = "rotor_blades")
class CarbonRotorBlade : Item(FabricItemSettings())

@ModItem(name = "wooden_rotor", tab = CreativeTab.IC2_MATERIALS, group = "rotors")
class WoodenRotor : Item(FabricItemSettings())

@ModItem(name = "iron_rotor", tab = CreativeTab.IC2_MATERIALS, group = "rotors")
class IronRotor : Item(FabricItemSettings())

@ModItem(name = "steel_rotor", tab = CreativeTab.IC2_MATERIALS, group = "rotors")
class SteelRotor : Item(FabricItemSettings())

@ModItem(name = "carbon_rotor", tab = CreativeTab.IC2_MATERIALS, group = "rotors")
class CarbonRotor : Item(FabricItemSettings())

// ========== 电路与机械部件 ==========

@ModItem(name = "circuit", tab = CreativeTab.IC2_MATERIALS, group = "circuits")
class Circuit : Item(FabricItemSettings())

@ModItem(name = "advanced_circuit", tab = CreativeTab.IC2_MATERIALS, group = "circuits")
class AdvancedCircuit : Item(FabricItemSettings())

@ModItem(name = "alloy", tab = CreativeTab.IC2_MATERIALS, group = "circuits")
class Alloy : Item(FabricItemSettings())

@ModItem(name = "iridium", tab = CreativeTab.IC2_MATERIALS, group = "circuits")
class IridiumPlate : Item(FabricItemSettings())

@ModItem(name = "coil", tab = CreativeTab.IC2_MATERIALS, group = "circuits")
class Coil : Item(FabricItemSettings())

@ModItem(name = "electric_motor", tab = CreativeTab.IC2_MATERIALS, group = "circuits")
class ElectricMotor : Item(FabricItemSettings())

@ModItem(name = "heat_conductor", tab = CreativeTab.IC2_MATERIALS, group = "circuits")
class HeatConductor : Item(FabricItemSettings())

@ModItem(name = "copper_boiler", tab = CreativeTab.IC2_MATERIALS, group = "circuits")
class CopperBoiler : Item(FabricItemSettings())

// ========== 能源球类 ==========

@ModItem(name = "coal_ball", tab = CreativeTab.IC2_MATERIALS, group = "energy_balls")
class CoalBall : Item(FabricItemSettings()) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, CoalBall::class.instance(), 1)
                .pattern("DCD").pattern("CFC").pattern("DCD")
                .input('D', CoalDust::class.instance())
                .input('C', CoalDust::class.instance())
                .input('F', Items.FLINT)
                .criterion(hasItem(CoalDust::class.instance()), conditionsFromItem(CoalDust::class.instance()))
                .offerTo(exporter, CoalBall::class.id())
        }
    }
}

@ModItem(name = "coal_chunk", tab = CreativeTab.IC2_MATERIALS, group = "energy_balls")
class CoalChunk : Item(FabricItemSettings()) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, Items.COAL_BLOCK, 1)
                .pattern("C C").pattern("COC").pattern("C C")
                .input('C', CoalChunk::class.instance())
                .input('O', Items.OBSIDIAN)
                .criterion(hasItem(CoalChunk::class.instance()), conditionsFromItem(CoalChunk::class.instance()))
                .offerTo(exporter, CoalChunk::class.id())
        }
    }
}

@ModItem(name = "industrial_diamond", tab = CreativeTab.IC2_MATERIALS, group = "energy_balls")
class IndustrialDiamond : Item(FabricItemSettings())

@ModItem(name = "plant_ball", tab = CreativeTab.IC2_MATERIALS, group = "energy_balls")
class PlantBall : Item(FabricItemSettings())

@ModItem(name = "compressed_plants", tab = CreativeTab.IC2_MATERIALS, group = "energy_balls")
class CompressedPlants : Item(FabricItemSettings())

@ModItem(name = "bio_chaff", tab = CreativeTab.IC2_MATERIALS, group = "energy_balls")
class BioChaff : Item(FabricItemSettings())

@ModItem(name = "compressed_hydrated_coal", tab = CreativeTab.IC2_MATERIALS, group = "energy_balls")
class CompressedHydratedCoal : Item(FabricItemSettings())

@ModItem(name = "scrap", tab = CreativeTab.IC2_MATERIALS, group = "energy_balls")
class Scrap : Item(FabricItemSettings())

@ModItem(name = "scrap_box", tab = CreativeTab.IC2_MATERIALS, group = "energy_balls")
class ScrapBox : Item(FabricItemSettings())

// ========== 柄与涡轮类 ==========

@ModItem(name = "steam_turbine_blade", tab = CreativeTab.IC2_MATERIALS, group = "rotor_blades")
class SteamTurbineBlade : Item(FabricItemSettings())

@ModItem(name = "steam_turbine", tab = CreativeTab.IC2_MATERIALS, group = "rotors")
class SteamTurbine : Item(FabricItemSettings())

@ModItem(name = "jetpack_attachment_plate", tab = CreativeTab.IC2_MATERIALS, group = "circuits")
class JetpackAttachmentPlate : Item(FabricItemSettings())

// ========== 货币与特殊物品 ==========

@ModItem(name = "resin", tab = CreativeTab.IC2_MATERIALS, group = "misc")
class Resin : Item(FabricItemSettings())

@ModItem(name = "slag", tab = CreativeTab.IC2_MATERIALS, group = "misc")
class Slag : Item(FabricItemSettings())

@ModItem(name = "iodine", tab = CreativeTab.IC2_MATERIALS, group = "misc")
class Iodine : Item(FabricItemSettings())
