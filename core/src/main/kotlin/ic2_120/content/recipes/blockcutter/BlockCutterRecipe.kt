package ic2_120.content.recipes.blockcutter

import net.minecraft.core.HolderLookup

import net.minecraft.server.level.ServerPlayer


import ic2_120.content.recipes.ModMachineRecipes
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.item.crafting.RecipeInput
import net.minecraft.resources.Identifier
import net.minecraft.world.level.Level

class BlockCutterRecipe(
    private val id: Identifier,
    val ingredient: Ingredient,
    val inputCount: Int,
    val materialHardness: Float,
    val output: ItemStack
) : ic2_120.content.recipes.LegacyMachineRecipe26<RecipeInput> {
    init {
        require(inputCount == 1 || inputCount == 2) { "BlockCutter inputCount must be 1 or 2" }
    }

    override fun matches(input: RecipeInput, world: Level): Boolean {
        val stack = input.getItem(0)
        return ingredient.test(stack) && stack.count >= inputCount
    }

    override fun assemble(input: RecipeInput): ItemStack {
        return output.copy()
    }

    fun canCraftInDimensions(width: Int, height: Int): Boolean = true

    fun getResultItem(provider: HolderLookup.Provider): ItemStack = output.copy()
    override fun getSerializer(): RecipeSerializer<BlockCutterRecipe> = ModMachineRecipes.recipeSerializer(BlockCutterRecipe::class)

    override fun getType(): RecipeType<BlockCutterRecipe> = ModMachineRecipes.recipeType(BlockCutterRecipe::class)

    fun isBladeSufficient(bladeHardness: Float): Boolean {
        return bladeHardness > materialHardness
    }
}
