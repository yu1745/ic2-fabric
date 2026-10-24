package ic2_120.content.recipes.extractor

import net.minecraft.core.HolderLookup

import net.minecraft.server.level.ServerPlayer


import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.item.crafting.RecipeInput
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level
import ic2_120.content.recipes.ModMachineRecipes

class ExtractorRecipe(
    private val id: ResourceLocation,
    val ingredient: Ingredient,
    val output: ItemStack
) : Recipe<RecipeInput> {
    override fun matches(input: RecipeInput, world: Level): Boolean {
        return ingredient.test(input.getItem(0))
    }

    override fun assemble(input: RecipeInput, provider: HolderLookup.Provider): ItemStack {
        return output.copy()
    }

    override fun canCraftInDimensions(width: Int, height: Int): Boolean = true

    override fun getResultItem(provider: HolderLookup.Provider): ItemStack = output.copy()
    override fun getSerializer(): RecipeSerializer<*> = ExtractorRecipeSerializer

    override fun getType(): RecipeType<*> = ModMachineRecipes.recipeType(ExtractorRecipe::class)
}
