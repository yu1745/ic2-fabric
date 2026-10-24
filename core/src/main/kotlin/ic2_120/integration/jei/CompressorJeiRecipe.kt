package ic2_120.integration.jei

import net.minecraft.world.item.ItemStack

data class CompressorJeiRecipe(
    val input: List<ItemStack>,
    val output: ItemStack,
    val containerReturn: ItemStack = ItemStack.EMPTY
)
