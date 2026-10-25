package ic2_120.content.recipes.macerator

import ic2_120.content.recipes.ModMachineRecipes
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.core.HolderLookup
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeInput
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.resources.Identifier
import net.minecraft.world.level.Level

class MaceratorRecipe(
    private val id: Identifier,
    val ingredient: Ingredient,
    val output: ItemStack,
    val inputCount: Int = 1
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

    override fun getSerializer(): RecipeSerializer<MaceratorRecipe> = ModMachineRecipes.recipeSerializer(MaceratorRecipe::class)

    override fun getType(): RecipeType<MaceratorRecipe> = ModMachineRecipes.recipeType(MaceratorRecipe::class)
}
