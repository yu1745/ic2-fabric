package ic2_120.content.item

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.item.Item

/** 焦炭 */
@ModItem(name = "coke", tab = CreativeTab.IC2_MATERIALS, group = "carbon_materials")
class Coke : Item(FabricItemSettings())
