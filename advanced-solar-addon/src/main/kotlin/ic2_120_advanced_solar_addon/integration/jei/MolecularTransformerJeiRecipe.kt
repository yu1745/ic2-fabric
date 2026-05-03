package ic2_120_advanced_solar_addon.integration.jei

import net.minecraft.item.ItemStack

data class MolecularTransformerJeiRecipe(
    val input: ItemStack,
    val output: ItemStack,
    val energy: Long
)
