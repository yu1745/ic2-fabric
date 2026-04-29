package ic2_120.content.recipes.centrifuge

import ic2_120.content.recipes.ModMachineRecipes
import net.minecraft.item.ItemStack
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.Recipe
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.recipe.RecipeType
import net.minecraft.recipe.input.RecipeInput
import net.minecraft.registry.RegistryWrapper
import net.minecraft.util.Identifier
import net.minecraft.world.World

class CentrifugeRecipe(
    private val id: Identifier,
    val ingredient: Ingredient,
    val inputCount: Int,
    val minHeat: Int,
    val outputs: List<ItemStack>
) : Recipe<RecipeInput> {
    init {
        require(outputs.size <= 3) { "Centrifuge recipes can have at most 3 outputs" }
        require(outputs.isNotEmpty()) { "Centrifuge recipes must have at least 1 output" }
    }

    override fun matches(input: RecipeInput, world: World): Boolean {
        val stack = input.getStackInSlot(0)
        return ingredient.test(stack) && stack.count >= inputCount
    }

    override fun craft(input: RecipeInput, lookup: RegistryWrapper.WrapperLookup): ItemStack {
        return outputs.first().copy()
    }

    override fun fits(width: Int, height: Int): Boolean = true

    override fun getResult(lookup: RegistryWrapper.WrapperLookup): ItemStack {
        return outputs.first().copy()
    }

    fun getAllOutputs(): List<ItemStack> = outputs.map { it.copy() }
    override fun getSerializer(): RecipeSerializer<*> = ModMachineRecipes.recipeSerializer(CentrifugeRecipe::class)

    override fun getType(): RecipeType<*> = ModMachineRecipes.recipeType(CentrifugeRecipe::class)
}
