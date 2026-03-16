package ic2_120.content.item

import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.type
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.item.Item

// ========== 植物种子类 ==========

@ModItem(name = "fertilizer", tab = CreativeTab.IC2_MATERIALS, group = "crops")
class Fertilizer : Item(FabricItemSettings())

@ModItem(name = "grin_powder", tab = CreativeTab.IC2_MATERIALS, group = "crops")
class GrinPowder : Item(FabricItemSettings())

@ModItem(name = "hops", tab = CreativeTab.IC2_MATERIALS, group = "crops")
class Hops : Item(FabricItemSettings())

@ModItem(name = "weed", tab = CreativeTab.IC2_MATERIALS, group = "crops")
class Weed : Item(FabricItemSettings())

@ModItem(name = "terra_wart", tab = CreativeTab.IC2_MATERIALS, group = "crops")
class TerraWart : Item(FabricItemSettings())

@ModItem(name = "coffee_beans", tab = CreativeTab.IC2_MATERIALS, group = "crops")
class CoffeeBeans : Item(FabricItemSettings())

@ModItem(name = "coffee_powder", tab = CreativeTab.IC2_MATERIALS, group = "crops")
class CoffeePowder : Item(FabricItemSettings())

/** 面粉（小麦提取） */
@ModItem(name = "flour", tab = CreativeTab.IC2_MATERIALS, group = "crops")
class Flour : Item(FabricItemSettings())

// ========== 饮料杯类 ==========

@ModItem(name = "empty_mug", tab = CreativeTab.IC2_MATERIALS, group = "mugs")
class EmptyMug : Item(FabricItemSettings())

@ModItem(name = "coffee_mug", tab = CreativeTab.IC2_MATERIALS, group = "mugs")
class CoffeeMug : Item(FabricItemSettings())

@ModItem(name = "cold_coffee_mug", tab = CreativeTab.IC2_MATERIALS, group = "mugs")
class ColdCoffeeMug : Item(FabricItemSettings())

@ModItem(name = "dark_coffee_mug", tab = CreativeTab.IC2_MATERIALS, group = "mugs")
class DarkCoffeeMug : Item(FabricItemSettings())
