package ic2_120.content.recipes.extractor

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

class ExtractorRecipe(
    private val id: Identifier,
    val ingredient: Ingredient,
    val output: ItemStack
) : Recipe<SimpleInventory> {
    override fun matches(inventory: SimpleInventory, world: World): Boolean {
        return ingredient.test(inventory.getStack(0))
    }

    override fun craft(inventory: SimpleInventory, registryManager: DynamicRegistryManager): ItemStack {
        return output.copy()
    }

    override fun fits(width: Int, height: Int): Boolean = true

    override fun getOutput(registryManager: DynamicRegistryManager): ItemStack = output.copy()

    override fun getId(): Identifier = id

    override fun getSerializer(): RecipeSerializer<*> = ExtractorRecipeSerializer

    override fun getType(): RecipeType<*> = ModMachineRecipes.EXTRACTOR_TYPE
}
