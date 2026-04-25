package ic2_120.content.recipes.solidcanner

import ic2_120.content.recipes.ModMachineRecipes
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.Recipe
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.recipe.RecipeType
import net.minecraft.recipe.input.SingleStackRecipeInput
import net.minecraft.registry.RegistryWrapper
import net.minecraft.util.Identifier
import net.minecraft.world.World

/**
 * 固体装罐机配方。
 * 槽位 0：主要材料（锡罐 / 空燃料棒）
 * 槽位 1：次要材料（食物 / 核燃料）
 * 产出：满锡罐 / 燃料棒
 */
class SolidCannerRecipe(
    private val id: Identifier,
    val slot0Ingredient: Ingredient,
    val slot0Count: Int,
    val slot1Ingredient: Ingredient,
    val slot1Count: Int,
    val output: ItemStack
) : Recipe<SingleStackRecipeInput> {

    override fun matches(input: SingleStackRecipeInput, world: World): Boolean {
        if (inventory.size() < 2) return false
        val slot0 = inventory.getStack(0)
        val slot1 = inventory.getStack(1)
        if (slot0.isEmpty || slot1.isEmpty) return false
        if (slot0.count < slot0Count) return false
        if (!slot0Ingredient.test(slot0)) return false
        if (slot1.count < slot1Count) return false
        if (!slot1Ingredient.test(slot1)) return false
        return true
    }

    override fun craft(inventory: RecipeInput, lookup: RegistryWrapper.WrapperLookup): ItemStack {
        return output.copy()
    }

    override fun fits(width: Int, height: Int): Boolean = true

    override fun getResult(lookup: RegistryWrapper.WrapperLookup): ItemStack = output.copy()
    override fun getSerializer(): RecipeSerializer<*> = ModMachineRecipes.recipeSerializer(SolidCannerRecipe::class)

    override fun getType(): RecipeType<*> = ModMachineRecipes.recipeType(SolidCannerRecipe::class)
}
