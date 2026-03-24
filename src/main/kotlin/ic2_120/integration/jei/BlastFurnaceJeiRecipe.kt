package ic2_120.integration.jei

import net.minecraft.item.ItemStack

/**
 * 高炉 JEI 配方
 */
data class BlastFurnaceJeiRecipe(
    val input: ItemStack,
    val steelOutput: ItemStack,
    val slagOutput: ItemStack
)
