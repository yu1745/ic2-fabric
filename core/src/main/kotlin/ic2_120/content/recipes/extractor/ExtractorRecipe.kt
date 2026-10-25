package ic2_120.content.recipes.extractor

import net.minecraft.core.HolderLookup

import net.minecraft.server.level.ServerPlayer


import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.item.crafting.RecipeInput
import net.minecraft.resources.Identifier
import net.minecraft.world.level.Level
import ic2_120.content.recipes.ModMachineRecipes

class ExtractorRecipe(
    private val id: Identifier,
    val ingredient: Ingredient,
    val output: ItemStack
) : ic2_120.content.recipes.LegacyMachineRecipe26<RecipeInput> {
    override fun matches(input: RecipeInput, world: Level): Boolean {
        return ingredient.test(input.getItem(0))
    }

    override fun assemble(input: RecipeInput): ItemStack {
        return output.copy()
    }

    fun canCraftInDimensions(width: Int, height: Int): Boolean = true

    fun getResultItem(provider: HolderLookup.Provider): ItemStack = output.copy()
    override fun getSerializer(): RecipeSerializer<ExtractorRecipe> = ExtractorRecipeSerializer.serializer

    override fun getType(): RecipeType<ExtractorRecipe> = ModMachineRecipes.recipeType(ExtractorRecipe::class)
}
