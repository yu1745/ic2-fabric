package ic2_120.content.recipes.compressor

import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.core.RegistryAccess
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level
import ic2_120.content.recipes.ModMachineRecipes

class CompressorRecipe(
    private val id: ResourceLocation,
    val ingredient: Ingredient,
    val inputCount: Int,
    val output: ItemStack,
    val containerReturn: ItemStack = ItemStack.EMPTY
) : Recipe<SimpleContainer> {
    override fun matches(inventory: SimpleContainer, world: Level): Boolean {
        val stack = inventory.getStack(0)
        return ingredient.test(stack) && stack.count >= inputCount
    }

    override fun craft(inventory: SimpleContainer, registryManager: RegistryAccess): ItemStack {
        return output.copy()
    }

    override fun fits(width: Int, height: Int): Boolean = true

    override fun getOutput(registryManager: RegistryAccess): ItemStack = output.copy()

    override fun getId(): ResourceLocation = id

    override fun getSerializer(): RecipeSerializer<*> = CompressorRecipeSerializer

    override fun getType(): RecipeType<*> = ModMachineRecipes.recipeType(CompressorRecipe::class)
}
