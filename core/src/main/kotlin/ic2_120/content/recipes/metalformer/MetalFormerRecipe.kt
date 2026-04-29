package ic2_120.content.recipes.metalformer

import ic2_120.content.recipes.ModMachineRecipes
import net.minecraft.item.ItemStack
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.Recipe
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.recipe.RecipeType
import net.minecraft.util.Identifier
import net.minecraft.world.World

/**
 * 金属成型机配方基类
 */
sealed class MetalFormerRecipe(
    @JvmField val recipeId: Identifier,
    val ingredient: Ingredient,
    val output: ItemStack
) : Recipe<MetalFormerRecipe.Input> {

    override fun matches(inventory: Input, world: World): Boolean {
        return ingredient.test(inventory.getStack(0))
    }

    override fun craft(inventory: Input, registry: net.minecraft.registry.DynamicRegistryManager): ItemStack {
        return output.copy()
    }

    override fun getId(): Identifier = recipeId

    override fun fits(width: Int, height: Int): Boolean = true

    override fun getOutput(registry: net.minecraft.registry.DynamicRegistryManager): ItemStack = output.copy()

    override fun getRemainder(inventory: Input): net.minecraft.util.collection.DefaultedList<ItemStack> {
        return net.minecraft.util.collection.DefaultedList.ofSize(1, ItemStack.EMPTY)
    }

    class Input(val inputStack: ItemStack) : net.minecraft.inventory.Inventory {
        override fun getStack(slot: Int): ItemStack = if (slot == 0) inputStack else ItemStack.EMPTY
        override fun size(): Int = 1
        override fun isEmpty(): Boolean = inputStack.isEmpty
        override fun clear() {}
        override fun setStack(slot: Int, stack: ItemStack) {}
        override fun removeStack(slot: Int): ItemStack = ItemStack.EMPTY
        override fun removeStack(slot: Int, amount: Int): ItemStack = ItemStack.EMPTY
        override fun markDirty() {}
        override fun canPlayerUse(player: net.minecraft.entity.player.PlayerEntity): Boolean = true
    }

    override fun getSerializer(): RecipeSerializer<*> = ModMachineRecipes.recipeSerializer(MetalFormerRecipe::class)

    override fun getType(): RecipeType<*> = ModMachineRecipes.recipeType(MetalFormerRecipe::class)

    companion object {
        fun getOutput(recipe: MetalFormerRecipe): ItemStack = recipe.output.copy()
    }
}

/**
 * 辊压模式配方：锭 -> 板，板 -> 外壳
 */
class RollingRecipe(
    recipeId: Identifier,
    ingredient: Ingredient,
    output: ItemStack
) : MetalFormerRecipe(recipeId, ingredient, output)

/**
 * 切割模式配方：板 -> 导线，外壳 -> 货币
 */
class CuttingRecipe(
    recipeId: Identifier,
    ingredient: Ingredient,
    output: ItemStack
) : MetalFormerRecipe(recipeId, ingredient, output)

/**
 * 挤压模式配方：锭/板/块 -> 导线或组件
 */
class ExtrudingRecipe(
    recipeId: Identifier,
    ingredient: Ingredient,
    output: ItemStack
) : MetalFormerRecipe(recipeId, ingredient, output)
