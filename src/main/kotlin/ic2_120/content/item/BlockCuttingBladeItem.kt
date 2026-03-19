package ic2_120.content.item

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

@ModItem(name = "iron_block_cutting_blade", tab = CreativeTab.IC2_MATERIALS)
class IronBlockCuttingBladeItem : Item(FabricItemSettings()), IBlockCuttingBlade {
    override fun getBladeHardness(stack: ItemStack): Float = 5.0f
}

@ModItem(name = "steel_block_cutting_blade", tab = CreativeTab.IC2_MATERIALS)
class SteelBlockCuttingBladeItem : Item(FabricItemSettings()), IBlockCuttingBlade {
    override fun getBladeHardness(stack: ItemStack): Float = 6.0f
}

@ModItem(name = "diamond_block_cutting_blade", tab = CreativeTab.IC2_MATERIALS)
class DiamondBlockCuttingBladeItem : Item(FabricItemSettings()), IBlockCuttingBlade {
    override fun getBladeHardness(stack: ItemStack): Float = 50.0f
}
