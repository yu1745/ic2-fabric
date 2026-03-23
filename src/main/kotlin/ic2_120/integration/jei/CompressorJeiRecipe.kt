package ic2_120.integration.jei

import net.minecraft.item.ItemStack
import net.minecraft.recipe.Ingredient

data class CompressorJeiRecipe(
    val input: Ingredient,
    val inputCount: Int,
    val output: ItemStack
)
