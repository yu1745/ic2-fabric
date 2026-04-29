package ic2_120.integration.jei

import net.minecraft.item.ItemStack

/**
 * JEI 显示用的离心机配方
 *
 * @param input 输入物品（带数量）
 * @param minHeat 最低热量要求
 * @param outputs 多个输出物品（最多3个）
 */
data class CentrifugeJeiRecipe(
    val input: ItemStack,
    val minHeat: Int,
    val outputs: List<ItemStack>
)
