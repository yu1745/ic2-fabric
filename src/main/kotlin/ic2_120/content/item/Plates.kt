package ic2_120.content.item

import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.type
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.item.Item

// ========== 板类（金属成型机切割：1 锭 -> 1 板；青金石/黑曜石见配方） ==========

@ModItem(name = "bronze_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates")
class BronzePlate : Item(FabricItemSettings())

@ModItem(name = "copper_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates")
class CopperPlate : Item(FabricItemSettings())

@ModItem(name = "gold_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates")
class GoldPlate : Item(FabricItemSettings())

@ModItem(name = "iron_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates")
class IronPlate : Item(FabricItemSettings())

@ModItem(name = "lapis_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates")
class LapisPlate : Item(FabricItemSettings())

@ModItem(name = "lead_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates")
class LeadPlate : Item(FabricItemSettings())

@ModItem(name = "obsidian_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates")
class ObsidianPlate : Item(FabricItemSettings())

@ModItem(name = "steel_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates")
class SteelPlate : Item(FabricItemSettings())

@ModItem(name = "tin_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates")
class TinPlate : Item(FabricItemSettings())

// ========== 致密板类（压缩机：9 板 -> 1 致密板） ==========

@ModItem(name = "dense_bronze_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseBronzePlate : Item(FabricItemSettings())

@ModItem(name = "dense_copper_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseCopperPlate : Item(FabricItemSettings())

@ModItem(name = "dense_gold_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseGoldPlate : Item(FabricItemSettings())

@ModItem(name = "dense_iron_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseIronPlate : Item(FabricItemSettings())

@ModItem(name = "dense_lapis_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseLapisPlate : Item(FabricItemSettings())

@ModItem(name = "dense_lead_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseLeadPlate : Item(FabricItemSettings())

@ModItem(name = "dense_obsidian_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseObsidianPlate : Item(FabricItemSettings())

@ModItem(name = "dense_steel_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseSteelPlate : Item(FabricItemSettings())

@ModItem(name = "dense_tin_plate", tab = CreativeTab.IC2_MATERIALS, group = "dense_plates")
class DenseTinPlate : Item(FabricItemSettings())
