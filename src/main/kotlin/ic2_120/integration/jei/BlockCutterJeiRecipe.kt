package ic2_120.integration.jei

import net.minecraft.item.ItemStack
import net.minecraft.recipe.Ingredient

/**
 * 方块切割机 JEI 配方
 */
data class BlockCutterJeiRecipe(
    val input: Ingredient,
    val inputCount: Int,
    val output: ItemStack
)
