package ic2_120.content.recipes.orewashing

import net.minecraft.item.ItemStack
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.Recipe
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.recipe.RecipeType
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.world.World

/**
 * 洗矿机配方：粉碎矿石 + 水 → 多个输出
 *
 * 支持多输出（最多3个）和流体消耗
 */
class OreWashingRecipe(
    private val id: Identifier,
    val ingredient: Ingredient,
    val outputItems: List<ItemStack>,
    val waterConsumptionMb: Long
) : Recipe<OreWashingRecipe.Input> {

    init {
        require(outputItems.size <= 3) { "OreWashingRecipe supports at most 3 outputs" }
        require(waterConsumptionMb > 0) { "Water consumption must be positive" }
    }

    override fun matches(inventory: Input, world: World): Boolean {
        return ingredient.test(inventory.getStack(0))
    }

    override fun craft(inventory: Input, registry: net.minecraft.registry.DynamicRegistryManager): ItemStack {
        return outputItems.firstOrNull()?.copy() ?: ItemStack.EMPTY
    }

    override fun getId(): Identifier = id

    override fun getSerializer(): RecipeSerializer<*> = ic2_120.content.recipes.ModMachineRecipes.recipeSerializer(OreWashingRecipe::class)

    override fun getType(): RecipeType<*> = ic2_120.content.recipes.ModMachineRecipes.recipeType(OreWashingRecipe::class)

    override fun fits(width: Int, height: Int): Boolean = true

    override fun getOutput(registry: net.minecraft.registry.DynamicRegistryManager): ItemStack {
        return outputItems.firstOrNull()?.copy() ?: ItemStack.EMPTY
    }

    override fun getRemainder(inventory: Input): net.minecraft.util.collection.DefaultedList<ItemStack> {
        return net.minecraft.util.collection.DefaultedList.ofSize(1, ItemStack.EMPTY)
    }

    /**
     * 获取所有输出（按顺序）
     */
    fun getOutputs(): List<ItemStack> = outputItems.map { it.copy() }

    /**
     * 简单输入容器，仅包含单个物品栈用于配方匹配
     */
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
