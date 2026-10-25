package ic2_120.content.recipes.compressor

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.core.HolderLookup
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeInput
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.resources.Identifier
import net.minecraft.world.level.Level
import ic2_120.content.recipes.ModMachineRecipes

class CompressorRecipe(
    private val id: Identifier,
    val ingredient: Ingredient,
    val inputCount: Int,
    val output: ItemStack,
    val containerReturn: ItemStack = ItemStack.EMPTY
) : ic2_120.content.recipes.LegacyMachineRecipe26<RecipeInput> {
    override fun matches(inventory: RecipeInput, world: Level): Boolean {
        val stack = inventory.getItem(0)
        return ingredient.test(stack) && stack.count >= inputCount
    }

    override fun assemble(inventory: RecipeInput): ItemStack {
        return output.copy()
    }

    fun canCraftInDimensions(width: Int, height: Int): Boolean = true

    fun getResultItem(registryManager: HolderLookup.Provider): ItemStack = output.copy()

    fun getId(): Identifier = id

    override fun getSerializer(): RecipeSerializer<CompressorRecipe> = CompressorRecipeSerializer.serializer

    override fun getType(): RecipeType<CompressorRecipe> = ModMachineRecipes.recipeType(CompressorRecipe::class)
}
