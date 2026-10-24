package ic2_120.content.recipes.centrifuge

import net.minecraft.core.HolderLookup

import net.minecraft.server.level.ServerPlayer


import ic2_120.content.recipes.ModMachineRecipes
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.item.crafting.RecipeInput
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level

class CentrifugeRecipe(
    private val id: ResourceLocation,
    val ingredient: Ingredient,
    val inputCount: Int,
    val minHeat: Int,
    val outputs: List<ItemStack>
) : Recipe<RecipeInput> {
    init {
        require(outputs.size <= 3) { "Centrifuge recipes can have at most 3 outputs" }
        require(outputs.isNotEmpty()) { "Centrifuge recipes must have at least 1 output" }
    }

    override fun matches(input: RecipeInput, world: Level): Boolean {
        val stack = input.getItem(0)
        return ingredient.test(stack) && stack.count >= inputCount
    }

    override fun assemble(input: RecipeInput, provider: HolderLookup.Provider): ItemStack {
        return outputs.first().copy()
    }

    override fun canCraftInDimensions(width: Int, height: Int): Boolean = true

    override fun getResultItem(provider: HolderLookup.Provider): ItemStack {
        return outputs.first().copy()
    }

    fun getAllOutputs(): List<ItemStack> = outputs.map { it.copy() }
    override fun getSerializer(): RecipeSerializer<*> = ModMachineRecipes.recipeSerializer(CentrifugeRecipe::class)

    override fun getType(): RecipeType<*> = ModMachineRecipes.recipeType(CentrifugeRecipe::class)
}
