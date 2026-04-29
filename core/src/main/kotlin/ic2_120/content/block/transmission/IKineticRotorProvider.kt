package ic2_120.content.block.transmission

import net.minecraft.item.ItemStack

interface IKineticRotorProvider {
    fun getRotorStack(): ItemStack
    fun getRotorRadius(stack: ItemStack): Float
}