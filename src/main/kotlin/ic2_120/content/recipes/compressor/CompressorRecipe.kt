package ic2_120.content.recipes.compressor

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
import ic2_120.content.recipes.ModMachineRecipes

class CompressorRecipe(
    private val id: Identifier,
    val ingredient: Ingredient,
    val inputCount: Int,
    val output: ItemStack
) : Recipe<SingleStackRecipeInput> {
    override fun matches(input: SingleStackRecipeInput, world: World): Boolean {
        val stack = inventory.getStack(0)
        return ingredient.test(stack) && stack.count >= inputCount
    }

    override fun craft(inventory: RecipeInput, lookup: RegistryWrapper.WrapperLookup): ItemStack {
        return output.copy()
    }

    override fun fits(width: Int, height: Int): Boolean = true

    override fun getResult(lookup: RegistryWrapper.WrapperLookup): ItemStack = output.copy()
    override fun getSerializer(): RecipeSerializer<*> = CompressorRecipeSerializer

    override fun getType(): RecipeType<*> = ModMachineRecipes.recipeType(CompressorRecipe::class)
}
