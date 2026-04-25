package ic2_120.content.recipes.blastfurnace

import net.minecraft.item.ItemStack
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.Recipe
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.recipe.RecipeType
import net.minecraft.recipe.input.RecipeInput
import net.minecraft.registry.RegistryWrapper
import net.minecraft.util.Identifier
import net.minecraft.world.World

class BlastFurnaceRecipe(
    private val id: Identifier,
    val ingredient: Ingredient,
    val steelOutput: ItemStack,
    val slagOutput: ItemStack
) : Recipe<BlastFurnaceRecipe.Input> {

    override fun matches(input: Input, world: World): Boolean {
        return ingredient.test(input.getStackInSlot(0))
    }

    override fun craft(input: Input, lookup: RegistryWrapper.WrapperLookup): ItemStack {
        return steelOutput.copy()
    }

    override fun getSerializer(): RecipeSerializer<*> = ic2_120.content.recipes.ModMachineRecipes.recipeSerializer(BlastFurnaceRecipe::class)

    override fun getType(): RecipeType<*> = ic2_120.content.recipes.ModMachineRecipes.recipeType(BlastFurnaceRecipe::class)

    override fun fits(width: Int, height: Int): Boolean = true

    override fun getResult(lookup: RegistryWrapper.WrapperLookup): ItemStack = steelOutput.copy()

    override fun getRemainder(input: Input): net.minecraft.util.collection.DefaultedList<ItemStack> {
        return net.minecraft.util.collection.DefaultedList.ofSize(1, ItemStack.EMPTY)
    }

    class Input(val inputStack: ItemStack) : RecipeInput {
        override fun getStackInSlot(slot: Int): ItemStack = if (slot == 0) inputStack else ItemStack.EMPTY

        override fun getSize(): Int = 1
    }

    companion object {
        fun getSteelOutput(recipe: BlastFurnaceRecipe): ItemStack = recipe.steelOutput.copy()

        fun getSlagOutput(recipe: BlastFurnaceRecipe): ItemStack = recipe.slagOutput.copy()
    }
}
