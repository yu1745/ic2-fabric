package ic2_120.integration.jei

import net.minecraft.item.ItemStack
import net.minecraft.recipe.Ingredient

/**
 * 洗矿机 JEI 配方
 */
data class OreWashingJeiRecipe(
    val input: Ingredient,
    val outputs: List<ItemStack>,
    val waterConsumptionMb: Long
)
