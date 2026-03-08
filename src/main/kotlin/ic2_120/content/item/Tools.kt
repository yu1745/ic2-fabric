package ic2_120.content.item

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.fabricmc.fabric.api.item.v1.FabricItemSettings

// ========== 工具类 ==========

/** 锻造锤 - 将锭锻造成板，将板锻造成外壳 */
@ModItem(name = "forge_hammer", tab = CreativeTab.IC2_TOOLS)
class ForgeHammer : Item(FabricItemSettings().maxDamage(80)) {
    override fun getRecipeRemainder(stack: ItemStack): ItemStack {
        val result = stack.copy()
        if (result.damage < result.maxDamage - 1) {
            result.damage += 1
            return result
        }
        return ItemStack.EMPTY
    }
}

/** 板材切割剪刀 - 将板材切割成导线 */
@ModItem(name = "cutter", tab = CreativeTab.IC2_TOOLS)
class Cutter : Item(FabricItemSettings().maxDamage(60)) {
    override fun getRecipeRemainder(stack: ItemStack): ItemStack {
        val result = stack.copy()
        if (result.damage < result.maxDamage - 1) {
            result.damage += 1
            return result
        }
        return ItemStack.EMPTY
    }
}
