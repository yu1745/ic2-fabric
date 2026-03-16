package ic2_120.content.item

import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.type
import net.minecraft.item.Item
import net.fabricmc.fabric.api.item.v1.FabricItemSettings

// ========== 外壳类 ==========

/** 青铜外壳 */
@ModItem(name = "bronze_casing", tab = CreativeTab.IC2_MATERIALS, group = "casing")
class BronzeCasing : Item(FabricItemSettings())

/** 铜质外壳 */
@ModItem(name = "copper_casing", tab = CreativeTab.IC2_MATERIALS, group = "casing")
class CopperCasing : Item(FabricItemSettings())

/** 黄金外壳 */
@ModItem(name = "gold_casing", tab = CreativeTab.IC2_MATERIALS, group = "casing")
class GoldCasing : Item(FabricItemSettings())

/** 铁质外壳 */
@ModItem(name = "iron_casing", tab = CreativeTab.IC2_MATERIALS, group = "casing")
class IronCasing : Item(FabricItemSettings())

/** 铅质外壳 */
@ModItem(name = "lead_casing", tab = CreativeTab.IC2_MATERIALS, group = "casing")
class LeadCasing : Item(FabricItemSettings())

/** 钢质外壳 */
@ModItem(name = "steel_casing", tab = CreativeTab.IC2_MATERIALS, group = "casing")
class SteelCasing : Item(FabricItemSettings())

/** 锡质外壳 */
@ModItem(name = "tin_casing", tab = CreativeTab.IC2_MATERIALS, group = "casing")
class TinCasing : Item(FabricItemSettings())
