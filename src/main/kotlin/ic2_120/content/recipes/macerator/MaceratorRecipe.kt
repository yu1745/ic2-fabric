package ic2_120.content.recipes.macerator

import ic2_120.content.recipes.ModMachineRecipes
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.Recipe
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.recipe.RecipeType
import net.minecraft.recipe.input.SingleStackRecipeInput
import net.minecraft.registry.RegistryWrapper
import net.minecraft.util.Identifier
import net.minecraft.world.World

class MaceratorRecipe(
    private val id: Identifier,
    val ingredient: Ingredient,
    val output: ItemStack
) : Recipe<SingleStackRecipeInput> {
    override fun matches(input: SingleStackRecipeInput, world: World): Boolean {
        return ingredient.test(inventory.getStack(0))
    }

    override fun craft(inventory: RecipeInput, lookup: RegistryWrapper.WrapperLookup): ItemStack {
        return output.copy()
    }

    override fun fits(width: Int, height: Int): Boolean = true

    override fun getResult(lookup: RegistryWrapper.WrapperLookup): ItemStack = output.copy()
    override fun getSerializer(): RecipeSerializer<*> = ModMachineRecipes.recipeSerializer(MaceratorRecipe::class)

    override fun getType(): RecipeType<*> = ModMachineRecipes.recipeType(MaceratorRecipe::class)
}
