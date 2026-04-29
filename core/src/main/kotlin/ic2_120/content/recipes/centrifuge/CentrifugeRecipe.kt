package ic2_120.content.recipes.centrifuge

import ic2_120.content.recipes.ModMachineRecipes
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.Recipe
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.recipe.RecipeType
import net.minecraft.registry.DynamicRegistryManager
import net.minecraft.util.Identifier
import net.minecraft.world.World

/**
 * 热能离心机配方 - 支持多输出
 *
 * @param id 配方ID
 * @param ingredient 输入成分
 * @param inputCount 输入物品数量需求
 * @param minHeat 最低热量要求
 * @param outputs 多个输出物品（最多3个）
 */
class CentrifugeRecipe(
    private val id: Identifier,
    val ingredient: Ingredient,
    val inputCount: Int,
    val minHeat: Int,
    val outputs: List<ItemStack>
) : Recipe<SimpleInventory> {
    init {
        require(outputs.size <= 3) { "Centrifuge recipes can have at most 3 outputs" }
        require(outputs.isNotEmpty()) { "Centrifuge recipes must have at least 1 output" }
    }

    override fun matches(inventory: SimpleInventory, world: World): Boolean {
        val stack = inventory.getStack(0)
        return ingredient.test(stack) && stack.count >= inputCount
    }

    override fun craft(inventory: SimpleInventory, registryManager: DynamicRegistryManager): ItemStack {
        // 返回第一个输出作为主要输出（实际机器逻辑会处理所有输出）
        return outputs.first().copy()
    }

    override fun fits(width: Int, height: Int): Boolean = true

    override fun getOutput(registryManager: DynamicRegistryManager): ItemStack {
        // 返回第一个输出作为主要输出
        return outputs.first().copy()
    }

    /**
     * 获取所有输出物品
     */
    fun getAllOutputs(): List<ItemStack> = outputs.map { it.copy() }

    override fun getId(): Identifier = id

    override fun getSerializer(): RecipeSerializer<*> = ModMachineRecipes.recipeSerializer(CentrifugeRecipe::class)

    override fun getType(): RecipeType<*> = ModMachineRecipes.recipeType(CentrifugeRecipe::class)
}
