package ic2_120.integration.jei

import net.minecraft.item.ItemStack

/** JEI 展示用：回收机为万能回收，仅作示意，非真实配方条目。 */
data class RecyclerJeiRecipe(
    val input: ItemStack,
    val output: ItemStack
)
