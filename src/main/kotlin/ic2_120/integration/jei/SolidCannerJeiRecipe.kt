package ic2_120.integration.jei

import net.minecraft.item.ItemStack

/**
 * 固体装罐机 JEI 配方。
 * 槽位 0：主要材料（锡罐 / 空燃料棒）
 * 槽位 1：次要材料（食物 / 核燃料）
 */
data class SolidCannerJeiRecipe(
    val slot0: ItemStack,
    val slot1: ItemStack,
    val output: ItemStack
)
