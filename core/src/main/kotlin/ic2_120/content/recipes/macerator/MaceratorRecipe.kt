package ic2_120.content.recipes.macerator

import ic2_120.content.recipes.ModMachineRecipes
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.core.RegistryAccess
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level

class MaceratorRecipe(
    private val id: ResourceLocation,
    val ingredient: Ingredient,
    val output: ItemStack,
    val inputCount: Int = 1
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

    override fun getSerializer(): RecipeSerializer<*> = ModMachineRecipes.recipeSerializer(MaceratorRecipe::class)

    override fun getType(): RecipeType<*> = ModMachineRecipes.recipeType(MaceratorRecipe::class)
}
