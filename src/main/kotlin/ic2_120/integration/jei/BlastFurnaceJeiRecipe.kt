package ic2_120.integration.jei

import net.minecraft.item.ItemStack
import net.minecraft.recipe.Ingredient

/**
 * 高炉 JEI 配方
 */
data class BlastFurnaceJeiRecipe(
    val input: Ingredient,
    val steelOutput: ItemStack,
    val slagOutput: ItemStack
)
