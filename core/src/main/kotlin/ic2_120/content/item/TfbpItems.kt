package ic2_120.content.item

import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.type
import net.minecraft.item.Item

// ========== 地形转换模板类 ==========

@ModItem(name = "blank_tfbp", tab = CreativeTab.IC2_MATERIALS, group = "tfbp")
class BlankTfbpItem : Item(Item.Settings())

@ModItem(name = "chilling_tfbp", tab = CreativeTab.IC2_MATERIALS, group = "tfbp")
class ChillingTfbpItem : Item(Item.Settings())

@ModItem(name = "cultivation_tfbp", tab = CreativeTab.IC2_MATERIALS, group = "tfbp")
class CultivationTfbpItem : Item(Item.Settings())

@ModItem(name = "desertification_tfbp", tab = CreativeTab.IC2_MATERIALS, group = "tfbp")
class DesertificationTfbpItem : Item(Item.Settings())

@ModItem(name = "flatification_tfbp", tab = CreativeTab.IC2_MATERIALS, group = "tfbp")
class FlatificationTfbpItem : Item(Item.Settings())

@ModItem(name = "irrigation_tfbp", tab = CreativeTab.IC2_MATERIALS, group = "tfbp")
class IrrigationTfbpItem : Item(Item.Settings())

@ModItem(name = "mushroom_tfbp", tab = CreativeTab.IC2_MATERIALS, group = "tfbp")
class MushroomTfbpItem : Item(Item.Settings())
