package ic2_120.content.recipes.blastfurnace

import net.minecraft.core.HolderLookup
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeInput
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.Level

/**
 * 高炉配方：铁质材料 → 钢锭 + 炉渣
 *
 * 配方处理物品转换，压缩空气在运行时由机器单独消耗（每1000 ticks 1瓶）。
 */
class BlastFurnaceRecipe(
    private val id: ResourceLocation,
    val ingredient: Ingredient,
    val steelOutput: ItemStack,
    val slagOutput: ItemStack
) : Recipe<BlastFurnaceRecipe.Input> {

    override fun matches(inventory: Input, world: Level): Boolean {
        return ingredient.test(inventory.getItem(0))
    }

    fun craft(inventory: Input, registry: HolderLookup.Provider): ItemStack {
        return steelOutput.copy()
    }

    override fun assemble(inventory: Input, registry: HolderLookup.Provider): ItemStack {
        return steelOutput.copy()
    }

    override fun getResultItem(registry: HolderLookup.Provider): ItemStack = steelOutput.copy()

    fun getId(): ResourceLocation = id

    override fun getSerializer(): RecipeSerializer<*> = ic2_120.content.recipes.ModMachineRecipes.recipeSerializer(BlastFurnaceRecipe::class)

    override fun getType(): RecipeType<*> = ic2_120.content.recipes.ModMachineRecipes.recipeType(BlastFurnaceRecipe::class)

    override fun canCraftInDimensions(width: Int, height: Int): Boolean = true

    /**
     * 简单输入容器，仅包含单个物品栈用于配方匹配
     */
    class Input(val inputStack: ItemStack) : RecipeInput {
        override fun getItem(slot: Int): ItemStack = if (slot == 0) inputStack else ItemStack.EMPTY

        override fun size(): Int = 1
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
