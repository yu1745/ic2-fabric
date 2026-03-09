package ic2_120.content.item

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.item.Item

// ========== 地形转换模板类 ==========

@ModItem(name = "blank_tfbp", tab = CreativeTab.IC2_MATERIALS, group = "tfbp")
class BlankTfbpItem : Item(FabricItemSettings())

@ModItem(name = "chilling_tfbp", tab = CreativeTab.IC2_MATERIALS, group = "tfbp")
class ChillingTfbpItem : Item(FabricItemSettings())

@ModItem(name = "cultivation_tfbp", tab = CreativeTab.IC2_MATERIALS, group = "tfbp")
class CultivationTfbpItem : Item(FabricItemSettings())

@ModItem(name = "desertification_tfbp", tab = CreativeTab.IC2_MATERIALS, group = "tfbp")
class DesertificationTfbpItem : Item(FabricItemSettings())

@ModItem(name = "flatification_tfbp", tab = CreativeTab.IC2_MATERIALS, group = "tfbp")
class FlatificationTfbpItem : Item(FabricItemSettings())

@ModItem(name = "irrigation_tfbp", tab = CreativeTab.IC2_MATERIALS, group = "tfbp")
class IrrigationTfbpItem : Item(FabricItemSettings())

@ModItem(name = "mushroom_tfbp", tab = CreativeTab.IC2_MATERIALS, group = "tfbp")
class MushroomTfbpItem : Item(FabricItemSettings())
