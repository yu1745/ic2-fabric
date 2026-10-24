package ic2_120.integration.jei

import net.minecraft.world.item.ItemStack

data class ExtractorJeiRecipe(
    val input: List<ItemStack>,
    val output: ItemStack
)
