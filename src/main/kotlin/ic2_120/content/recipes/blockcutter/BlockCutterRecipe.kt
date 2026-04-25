package ic2_120.content.recipes.blockcutter

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

class BlockCutterRecipe(
    private val id: Identifier,
    val ingredient: Ingredient,
    val inputCount: Int,
    val materialHardness: Float,
    val output: ItemStack
) : Recipe<RecipeInput> {
    init {
        require(inputCount == 1 || inputCount == 2) { "BlockCutter inputCount must be 1 or 2" }
    }

    override fun matches(input: RecipeInput, world: World): Boolean {
        val stack = input.getStackInSlot(0)
        return ingredient.test(stack) && stack.count >= inputCount
    }

    override fun craft(input: RecipeInput, lookup: RegistryWrapper.WrapperLookup): ItemStack {
        return output.copy()
    }

    override fun fits(width: Int, height: Int): Boolean = true

    override fun getResult(lookup: RegistryWrapper.WrapperLookup): ItemStack = output.copy()
    override fun getSerializer(): RecipeSerializer<*> = ModMachineRecipes.recipeSerializer(BlockCutterRecipe::class)

    override fun getType(): RecipeType<*> = ModMachineRecipes.recipeType(BlockCutterRecipe::class)

    fun isBladeSufficient(bladeHardness: Float): Boolean {
        return bladeHardness > materialHardness
    }
}
