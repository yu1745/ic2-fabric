package ic2_120.content.item

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import net.minecraft.item.Item
import net.fabricmc.fabric.api.item.v1.FabricItemSettings

// ========== 金属成型机制品 ==========

@ModItem(name = "tin_can", tab = CreativeTab.IC2_MATERIALS, group = "parts")
class EmptyTinCanItem : Item(FabricItemSettings())

@ModItem(name = "filled_tin_can", tab = CreativeTab.IC2_MATERIALS, group = "parts")
class FilledTinCanItem : Item(FabricItemSettings())

@ModItem(name = "small_power_unit", tab = CreativeTab.IC2_MATERIALS, group = "parts")
class SmallPowerUnitItem : Item(FabricItemSettings())

@ModItem(name = "power_unit", tab = CreativeTab.IC2_MATERIALS, group = "parts")
class PowerUnitItem : Item(FabricItemSettings())

@ModItem(name = "fuel_rod", tab = CreativeTab.IC2_MATERIALS, group = "parts")
class EmptyFuelRodItem : Item(FabricItemSettings())

@ModItem(name = "iron_shaft", tab = CreativeTab.IC2_MATERIALS, group = "parts")
class ToolHandleIronItem : Item(FabricItemSettings())

@ModItem(name = "steel_shaft", tab = CreativeTab.IC2_MATERIALS, group = "parts")
class ToolHandleSteelItem : Item(FabricItemSettings())

@ModItem(name = "coin", tab = CreativeTab.IC2_MATERIALS, group = "parts")
class IndustrialCurrencyItem : Item(FabricItemSettings())
