package ic2_120.content.item

import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.type
import net.minecraft.item.Item
import net.fabricmc.fabric.api.item.v1.FabricItemSettings

// ========== 锭（已存在：copper_ingot, tin_ingot, bronze_ingot 在 MetalItems.kt） ==========

/** 合金锭 */
@ModItem(name = "mixed_metal_ingot", tab = CreativeTab.IC2_MATERIALS, group = "ingots")
class MixedMetalIngot : Item(FabricItemSettings())

/** 铅锭 */
@ModItem(name = "lead_ingot", tab = CreativeTab.IC2_MATERIALS, group = "ingots")
class LeadIngot : Item(FabricItemSettings())

/** 银锭 */
@ModItem(name = "silver_ingot", tab = CreativeTab.IC2_MATERIALS, group = "ingots")
class SilverIngot : Item(FabricItemSettings())

/** 钢锭 */
@ModItem(name = "steel_ingot", tab = CreativeTab.IC2_MATERIALS, group = "ingots")
class SteelIngot : Item(FabricItemSettings())

/** 精炼铁锭 */
@ModItem(name = "refined_iron_ingot", tab = CreativeTab.IC2_MATERIALS, group = "ingots")
class RefinedIronIngot : Item(FabricItemSettings())

/** 铀锭 */
@ModItem(name = "uranium_ingot", tab = CreativeTab.IC2_MATERIALS, group = "ingots")
class UraniumIngot : Item(FabricItemSettings())

// ========== 粗金属（raw，冶炼粗矿得） ==========

/** 粗铅 */
@ModItem(name = "raw_lead", tab = CreativeTab.IC2_MATERIALS, group = "raw_metals")
class RawLead : Item(FabricItemSettings())

/** 粗锡 */
@ModItem(name = "raw_tin", tab = CreativeTab.IC2_MATERIALS, group = "raw_metals")
class RawTin : Item(FabricItemSettings())

/** 粗铀 */
@ModItem(name = "raw_uranium", tab = CreativeTab.IC2_MATERIALS, group = "raw_metals")
class RawUranium : Item(FabricItemSettings())

// ========== 粉碎矿石（打粉机产物） ==========

@ModItem(name = "crushed_copper", tab = CreativeTab.IC2_MATERIALS, group = "crushed_ore")
class CrushedCopper : Item(FabricItemSettings())

@ModItem(name = "crushed_gold", tab = CreativeTab.IC2_MATERIALS, group = "crushed_ore")
class CrushedGold : Item(FabricItemSettings())

@ModItem(name = "crushed_iron", tab = CreativeTab.IC2_MATERIALS, group = "crushed_ore")
class CrushedIron : Item(FabricItemSettings())

@ModItem(name = "crushed_lead", tab = CreativeTab.IC2_MATERIALS, group = "crushed_ore")
class CrushedLead : Item(FabricItemSettings())

@ModItem(name = "crushed_silver", tab = CreativeTab.IC2_MATERIALS, group = "crushed_ore")
class CrushedSilver : Item(FabricItemSettings())

@ModItem(name = "crushed_tin", tab = CreativeTab.IC2_MATERIALS, group = "crushed_ore")
class CrushedTin : Item(FabricItemSettings())

@ModItem(name = "crushed_uranium", tab = CreativeTab.IC2_MATERIALS, group = "crushed_ore")
class CrushedUranium : Item(FabricItemSettings())

// ========== 纯净的粉碎矿石（洗矿机产物） ==========

@ModItem(name = "purified_copper", tab = CreativeTab.IC2_MATERIALS, group = "purified_ore")
class PurifiedCopper : Item(FabricItemSettings())

@ModItem(name = "purified_gold", tab = CreativeTab.IC2_MATERIALS, group = "purified_ore")
class PurifiedGold : Item(FabricItemSettings())

@ModItem(name = "purified_iron", tab = CreativeTab.IC2_MATERIALS, group = "purified_ore")
class PurifiedIron : Item(FabricItemSettings())

@ModItem(name = "purified_lead", tab = CreativeTab.IC2_MATERIALS, group = "purified_ore")
class PurifiedLead : Item(FabricItemSettings())

@ModItem(name = "purified_silver", tab = CreativeTab.IC2_MATERIALS, group = "purified_ore")
class PurifiedSilver : Item(FabricItemSettings())

@ModItem(name = "purified_tin", tab = CreativeTab.IC2_MATERIALS, group = "purified_ore")
class PurifiedTin : Item(FabricItemSettings())

@ModItem(name = "purified_uranium", tab = CreativeTab.IC2_MATERIALS, group = "purified_ore")
class PurifiedUranium : Item(FabricItemSettings())
