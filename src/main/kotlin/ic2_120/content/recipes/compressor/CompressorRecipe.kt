package ic2_120.content.recipes.compressor

import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.Recipe
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.recipe.RecipeType
import net.minecraft.registry.DynamicRegistryManager
import net.minecraft.util.Identifier
import net.minecraft.world.World
import ic2_120.content.recipes.ModMachineRecipes

class CompressorRecipe(
    private val id: Identifier,
    val ingredient: Ingredient,
    val inputCount: Int,
    val output: ItemStack
) : Recipe<SimpleInventory> {
    override fun matches(inventory: SimpleInventory, world: World): Boolean {
        val stack = inventory.getStack(0)
        return ingredient.test(stack) && stack.count >= inputCount
    }

    override fun craft(inventory: SimpleInventory, registryManager: DynamicRegistryManager): ItemStack {
        return output.copy()
    }

    override fun fits(width: Int, height: Int): Boolean = true

    override fun getOutput(registryManager: DynamicRegistryManager): ItemStack = output.copy()

    override fun getId(): Identifier = id

    override fun getSerializer(): RecipeSerializer<*> = CompressorRecipeSerializer

    override fun getType(): RecipeType<*> = ModMachineRecipes.COMPRESSOR_TYPE
}
