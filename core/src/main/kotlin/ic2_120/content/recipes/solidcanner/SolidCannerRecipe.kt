package ic2_120.content.recipes.solidcanner

import ic2_120.content.recipes.ModMachineRecipes
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.core.RegistryAccess
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level

/**
 * 固体装罐机配方。
 * 槽位 0：主要材料（锡罐 / 空燃料棒）
 * 槽位 1：次要材料（食物 / 核燃料）
 * 产出：满锡罐 / 燃料棒
 */
class SolidCannerRecipe(
    private val id: ResourceLocation,
    val slot0Ingredient: Ingredient,
    val slot0Count: Int,
    val slot1Ingredient: Ingredient,
    val slot1Count: Int,
    val output: ItemStack
) : Recipe<SimpleContainer> {

    companion object {
        private var slot1Cache: Set<Ingredient>? = null

        fun slot1Ingredients(world: net.minecraft.world.level.Level): Set<Ingredient> {
            slot1Cache?.let { return it }
            val recipeType = ModMachineRecipes.recipeType(SolidCannerRecipe::class)
            val result: Set<Ingredient> = world.recipeManager.listAllOfType(recipeType).mapTo(mutableSetOf()) { it.slot1Ingredient }
            slot1Cache = result
            return result
        }
    }

    override fun matches(inventory: SimpleContainer, world: Level): Boolean {
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

    override fun craft(inventory: SimpleContainer, registryManager: RegistryAccess): ItemStack {
        return output.copy()
    }

    override fun fits(width: Int, height: Int): Boolean = true

    override fun getOutput(registryManager: RegistryAccess): ItemStack = output.copy()

    override fun getId(): ResourceLocation = id

    override fun getSerializer(): RecipeSerializer<*> = ModMachineRecipes.recipeSerializer(SolidCannerRecipe::class)

    override fun getType(): RecipeType<*> = ModMachineRecipes.recipeType(SolidCannerRecipe::class)
}
