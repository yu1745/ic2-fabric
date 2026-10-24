package ic2_120.content.recipes.metalformer

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

/**
 * 金属成型机配方基类
 */
sealed class MetalFormerRecipe(
    @JvmField val recipeId: ResourceLocation,
    val ingredient: Ingredient,
    val output: ItemStack
) : Recipe<MetalFormerRecipe.Input> {

    override fun matches(inventory: Input, world: Level): Boolean {
        return ingredient.test(inventory.getItem(0))
    }

    override fun assemble(inventory: Input, lookup: HolderLookup.Provider): ItemStack {
        return output.copy()
    }

    fun getId(): ResourceLocation = recipeId

    override fun canCraftInDimensions(width: Int, height: Int): Boolean = true

    override fun getResultItem(lookup: HolderLookup.Provider): ItemStack = output.copy()

    override fun getRemainingItems(inventory: Input): net.minecraft.core.NonNullList<ItemStack> {
        return net.minecraft.core.NonNullList.withSize(1, ItemStack.EMPTY)
    }

    class Input(val inputStack: ItemStack) : RecipeInput, net.minecraft.world.Container {
        override fun getItem(slot: Int): ItemStack = if (slot == 0) inputStack else ItemStack.EMPTY
        override fun size(): Int = 1
        override fun getContainerSize(): Int = 1
        override fun isEmpty(): Boolean = inputStack.isEmpty
        override fun clearContent() {}
        override fun setItem(slot: Int, stack: ItemStack) {}
        override fun removeItemNoUpdate(slot: Int): ItemStack = ItemStack.EMPTY
        override fun removeItem(slot: Int, amount: Int): ItemStack = ItemStack.EMPTY
        override fun setChanged() {}
        override fun stillValid(player: net.minecraft.world.entity.player.Player): Boolean = true
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
    recipeId: ResourceLocation,
    ingredient: Ingredient,
    output: ItemStack
) : MetalFormerRecipe(recipeId, ingredient, output)

/**
 * 切割模式配方：板 -> 导线，外壳 -> 货币
 */
class CuttingRecipe(
    recipeId: ResourceLocation,
    ingredient: Ingredient,
    output: ItemStack
) : MetalFormerRecipe(recipeId, ingredient, output)

/**
 * 挤压模式配方：锭/板/块 -> 导线或组件
 */
class ExtrudingRecipe(
    recipeId: ResourceLocation,
    ingredient: Ingredient,
    output: ItemStack
) : MetalFormerRecipe(recipeId, ingredient, output)
