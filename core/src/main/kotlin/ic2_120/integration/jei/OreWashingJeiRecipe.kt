package ic2_120.integration.jei

import net.minecraft.item.ItemStack

/**
 * 洗矿机 JEI 配方
 */
data class OreWashingJeiRecipe(
    val input: ItemStack,
    val outputs: List<ItemStack>,
    val waterConsumptionMb: Long
)
