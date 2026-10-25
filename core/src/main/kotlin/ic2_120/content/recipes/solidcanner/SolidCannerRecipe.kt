package ic2_120.content.recipes.solidcanner

import ic2_120.compat.recipeManager
import ic2_120.content.recipes.ModMachineRecipes
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.core.RegistryAccess
import net.minecraft.resources.Identifier
import net.minecraft.world.level.Level

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
) : ic2_120.content.recipes.LegacyMachineRecipe26<SolidCannerRecipe.Input> {

    companion object {
        private var slot1Cache: Set<Ingredient>? = null

        fun slot1Ingredients(world: net.minecraft.world.level.Level): Set<Ingredient> {
            slot1Cache?.let { return it }
            val result: Set<Ingredient> = world.recipeManager
                .recipes.mapNotNull { it.value as? SolidCannerRecipe }
                .mapTo(mutableSetOf()) { it.slot1Ingredient }
            slot1Cache = result
            return result
        }
    }

    override fun matches(inventory: Input, world: Level): Boolean {
        if (inventory.size() < 2) return false
        val slot0 = inventory.getItem(0)
        val slot1 = inventory.getItem(1)
        if (slot0.isEmpty || slot1.isEmpty) return false
        if (slot0.count < slot0Count) return false
        if (!slot0Ingredient.test(slot0)) return false
        if (slot1.count < slot1Count) return false
        if (!slot1Ingredient.test(slot1)) return false
        return true
    }

    override fun assemble(inventory: Input): ItemStack {
        return output.copy()
    }

    fun canCraftInDimensions(width: Int, height: Int): Boolean = true

    fun getResultItem(lookup: net.minecraft.core.HolderLookup.Provider): ItemStack = output.copy()

    override fun getSerializer(): RecipeSerializer<SolidCannerRecipe> = ModMachineRecipes.recipeSerializer(SolidCannerRecipe::class)

    override fun getType(): RecipeType<SolidCannerRecipe> = ModMachineRecipes.recipeType(SolidCannerRecipe::class)

    /** 配方输入：锡罐 + 食物两槽（用 mojmap RecipeInput 取代 SimpleContainer）。 */
    class Input(val tinCan: ItemStack, val food: ItemStack) : net.minecraft.world.item.crafting.RecipeInput {
        override fun getItem(slot: Int): ItemStack = if (slot == 0) tinCan else food
        override fun size(): Int = 2
    }
}
