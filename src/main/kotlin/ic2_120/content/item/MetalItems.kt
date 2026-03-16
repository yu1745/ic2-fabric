package ic2_120.content.item

import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.type
import net.minecraft.item.Item
import net.fabricmc.fabric.api.item.v1.FabricItemSettings

// 铜锭：使用原版 minecraft:copper_ingot，此处不再注册

/**
 * 锡锭。
 */
@ModItem(name = "tin_ingot", tab = CreativeTab.IC2_MATERIALS, group = "ingots")
class TinIngot : Item(FabricItemSettings())

/**
 * 青铜锭。
 */
@ModItem(name = "bronze_ingot", tab = CreativeTab.IC2_MATERIALS, group = "ingots")
class BronzeIngot : Item(FabricItemSettings())

/**
 * 橡胶。
 */
@ModItem(name = "rubber", tab = CreativeTab.IC2_MATERIALS, group = "materials")
class RubberItem : Item(FabricItemSettings())

