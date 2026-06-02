package ic2_120.integration.jei

import net.minecraft.item.ItemStack

/**
 * 金属成型机 JEI 配方 - 辊压模式
 */
data class MetalFormerRollingJeiRecipe(
    val input: List<ItemStack>,
    val output: ItemStack
)

/**
 * 金属成型机 JEI 配方 - 切割模式
 */
data class MetalFormerCuttingJeiRecipe(
    val input: List<ItemStack>,
    val output: ItemStack
)

/**
 * 金属成型机 JEI 配方 - 挤压模式
 */
data class MetalFormerExtrudingJeiRecipe(
    val input: List<ItemStack>,
    val output: ItemStack
)
