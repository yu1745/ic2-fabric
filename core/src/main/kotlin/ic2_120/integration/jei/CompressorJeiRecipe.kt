package ic2_120.integration.jei

import net.minecraft.item.ItemStack

data class CompressorJeiRecipe(
    val input: List<ItemStack>,
    val output: ItemStack,
    val containerReturn: ItemStack = ItemStack.EMPTY
)
