package ic2_120.integration.jei

import net.minecraft.item.ItemStack
import net.minecraft.recipe.Ingredient

/**
 * 金属成型机 JEI 配方 - 辊压模式
 */
data class MetalFormerRollingJeiRecipe(
    val input: Ingredient,
    val output: ItemStack
)

/**
 * 金属成型机 JEI 配方 - 切割模式
 */
data class MetalFormerCuttingJeiRecipe(
    val input: Ingredient,
    val output: ItemStack
)

/**
 * 金属成型机 JEI 配方 - 挤压模式
 */
data class MetalFormerExtrudingJeiRecipe(
    val input: Ingredient,
    val output: ItemStack
)
