package ic2_120.content.recipes.macerator

import ic2_120.content.recipes.ModMachineRecipes
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.core.HolderLookup
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeInput
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level

class MaceratorRecipe(
    private val id: ResourceLocation,
    val ingredient: Ingredient,
    val output: ItemStack,
    val inputCount: Int = 1
) : Recipe<RecipeInput> {
    override fun matches(inventory: RecipeInput, world: Level): Boolean {
        val stack = inventory.getItem(0)
        return ingredient.test(stack) && stack.count >= inputCount
    }

    override fun assemble(inventory: RecipeInput, registryManager: HolderLookup.Provider): ItemStack {
        return output.copy()
    }

    override fun canCraftInDimensions(width: Int, height: Int): Boolean = true

    override fun getResultItem(registryManager: HolderLookup.Provider): ItemStack = output.copy()

    fun getId(): ResourceLocation = id

    override fun getSerializer(): RecipeSerializer<*> = ModMachineRecipes.recipeSerializer(MaceratorRecipe::class)

    override fun getType(): RecipeType<*> = ModMachineRecipes.recipeType(MaceratorRecipe::class)
}
