package ic2_120.content.recipes.orewashing

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

/**
 * 洗矿机配方：粉碎矿石 + 水 → 多个输出
 *
 * 支持多输出（最多3个）和流体消耗
 */
class OreWashingRecipe(
    private val id: ResourceLocation,
    val ingredient: Ingredient,
    val outputItems: List<ItemStack>,
    /** 水消耗量（droplets） */
    val waterConsumptionDroplets: Long
) : Recipe<OreWashingRecipe.Input> {

    init {
        require(outputItems.size <= 3) { "OreWashingRecipe supports at most 3 outputs" }
        require(waterConsumptionDroplets > 0) { "Water consumption must be positive" }
    }

    override fun matches(inventory: Input, world: Level): Boolean {
        return ingredient.test(inventory.getItem(0))
    }

    override fun assemble(inventory: Input, lookup: HolderLookup.Provider): ItemStack {
        return outputItems.firstOrNull()?.copy() ?: ItemStack.EMPTY
    }

    override fun getSerializer(): RecipeSerializer<*> = ic2_120.content.recipes.ModMachineRecipes.recipeSerializer(OreWashingRecipe::class)

    override fun getType(): RecipeType<*> = ic2_120.content.recipes.ModMachineRecipes.recipeType(OreWashingRecipe::class)

    override fun canCraftInDimensions(width: Int, height: Int): Boolean = true

    override fun getResultItem(lookup: HolderLookup.Provider): ItemStack {
        return outputItems.firstOrNull()?.copy() ?: ItemStack.EMPTY
    }

    override fun getRemainingItems(inventory: Input): net.minecraft.core.NonNullList<ItemStack> {
        return net.minecraft.core.NonNullList.withSize(1, ItemStack.EMPTY)
    }

    /**
     * 获取所有输出（按顺序）
     */
    fun getOutputs(): List<ItemStack> = outputItems.map { it.copy() }

    /**
     * 简单输入容器，仅包含单个物品栈用于配方匹配
     */
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

    companion object {
        /**
         * 获取所有输出（安全版本，返回固定长度3的列表）
         */
        fun getAllOutputs(recipe: OreWashingRecipe): List<ItemStack> {
            val result = recipe.outputItems.map { it.copy() }.toMutableList()
            // 补齐到3个输出
            while (result.size < 3) {
                result.add(ItemStack.EMPTY)
            }
            return result
        }
    }
}
