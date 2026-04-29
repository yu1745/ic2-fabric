package ic2_120.content.recipes.blastfurnace

import net.minecraft.item.ItemStack
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.Recipe
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.recipe.RecipeType
import net.minecraft.recipe.book.CraftingRecipeCategory
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.world.World

/**
 * 高炉配方：铁质材料 → 钢锭 + 炉渣
 *
 * 配方处理物品转换，压缩空气在运行时由机器单独消耗（每1000 ticks 1瓶）。
 */
class BlastFurnaceRecipe(
    private val id: Identifier,
    val ingredient: Ingredient,
    val steelOutput: ItemStack,
    val slagOutput: ItemStack
) : Recipe<BlastFurnaceRecipe.Input> {

    override fun matches(inventory: Input, world: World): Boolean {
        return ingredient.test(inventory.getStack(0))
    }

    override fun craft(inventory: Input, registry: net.minecraft.registry.DynamicRegistryManager): ItemStack {
        return steelOutput.copy()
    }

    override fun getId(): Identifier = id

    override fun getSerializer(): RecipeSerializer<*> = ic2_120.content.recipes.ModMachineRecipes.recipeSerializer(BlastFurnaceRecipe::class)

    override fun getType(): RecipeType<*> = ic2_120.content.recipes.ModMachineRecipes.recipeType(BlastFurnaceRecipe::class)

    override fun fits(width: Int, height: Int): Boolean = true

    override fun getOutput(registry: net.minecraft.registry.DynamicRegistryManager): ItemStack = steelOutput.copy()

    override fun getRemainder(inventory: Input): net.minecraft.util.collection.DefaultedList<ItemStack> {
        return net.minecraft.util.collection.DefaultedList.ofSize(1, ItemStack.EMPTY)
    }

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
         * 获取钢锭输出
         */
        fun getSteelOutput(recipe: BlastFurnaceRecipe): ItemStack = recipe.steelOutput.copy()

        /**
         * 获取炉渣输出
         */
        fun getSlagOutput(recipe: BlastFurnaceRecipe): ItemStack = recipe.slagOutput.copy()
    }
}
