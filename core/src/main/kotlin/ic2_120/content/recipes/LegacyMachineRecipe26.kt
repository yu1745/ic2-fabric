package ic2_120.content.recipes

import net.minecraft.world.item.crafting.PlacementInfo
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeBookCategories
import net.minecraft.world.item.crafting.RecipeBookCategory
import net.minecraft.world.item.crafting.RecipeInput

/** Shared 26.2 recipe-book metadata for IC2's machine-only recipes. */
interface LegacyMachineRecipe26<T : RecipeInput> : Recipe<T> {
    override fun showNotification(): Boolean = true
    override fun group(): String = ""
    override fun placementInfo(): PlacementInfo = PlacementInfo.NOT_PLACEABLE
    override fun recipeBookCategory(): RecipeBookCategory = RecipeBookCategories.CRAFTING_MISC
}
