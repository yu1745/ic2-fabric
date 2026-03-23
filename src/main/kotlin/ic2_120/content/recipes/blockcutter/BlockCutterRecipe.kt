package ic2_120.content.recipes.blockcutter

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
 * 方块切割机配方
 *
 * 特殊机制：
 * - 刀片硬度检查：锯片硬度必须大于材料硬度
 * - 双配方系统：同一输入可能有多个配方（通过inputCount区分）
 *
 * @param id 配方ID
 * @param ingredient 输入成分
 * @param inputCount 输入数量（1或2，用于区分双配方）
 * @param materialHardness 材料硬度（用于刀片检查）
 * @param output 输出物品
 */
class BlockCutterRecipe(
    private val id: Identifier,
    val ingredient: Ingredient,
    val inputCount: Int,
    val materialHardness: Float,
    val output: ItemStack
) : Recipe<SimpleInventory> {
    init {
        require(inputCount == 1 || inputCount == 2) { "BlockCutter inputCount must be 1 or 2" }
    }

    override fun matches(inventory: SimpleInventory, world: World): Boolean {
        val stack = inventory.getStack(0)
        return ingredient.test(stack) && stack.count >= inputCount
    }

    override fun craft(inventory: SimpleInventory, registryManager: DynamicRegistryManager): ItemStack {
        return output.copy()
    }

    override fun fits(width: Int, height: Int): Boolean = true

    override fun getOutput(registryManager: DynamicRegistryManager): ItemStack = output.copy()

    override fun getId(): Identifier = id

    override fun getSerializer(): RecipeSerializer<*> = ModMachineRecipes.BLOCK_CUTTER_SERIALIZER

    override fun getType(): RecipeType<*> = ModMachineRecipes.BLOCK_CUTTER_TYPE

    /**
     * 检查刀片硬度是否足够
     * @param bladeHardness 锯片硬度
     * @return true 如果锯片足够硬
     */
    fun isBladeSufficient(bladeHardness: Float): Boolean {
        return bladeHardness > materialHardness
    }
}
