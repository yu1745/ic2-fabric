package ic2_120.integration.jei

import net.minecraft.item.ItemStack
import net.minecraft.recipe.Ingredient

data class ExtractorJeiRecipe(
    val input: Ingredient,
    val output: ItemStack
)
