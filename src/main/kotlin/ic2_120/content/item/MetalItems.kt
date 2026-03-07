package ic2_120.content.item

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import net.minecraft.item.Item
import net.fabricmc.fabric.api.item.v1.FabricItemSettings

/**
 * 铜锭。
 */
@ModItem(name = "copper_ingot", tab = CreativeTab.IC2_MATERIALS)
class CopperIngot : Item(FabricItemSettings())

/**
 * 锡锭。
 */
@ModItem(name = "tin_ingot", tab = CreativeTab.IC2_MATERIALS)
class TinIngot : Item(FabricItemSettings())

/**
 * 青铜锭。
 */
@ModItem(name = "bronze_ingot", tab = CreativeTab.IC2_MATERIALS)
class BronzeIngot : Item(FabricItemSettings())

/**
 * 橡胶。
 */
@ModItem(name = "rubber", tab = CreativeTab.IC2_MATERIALS)
class RubberItem : Item(FabricItemSettings())
