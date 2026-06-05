package ic2_120.content.block.machines

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage
import net.minecraft.item.ItemStack

/**
 * 采矿管道专用 Storage，容量 [capacity]，不依赖 ItemStack.getMaxCount()。
 * 与 RoutedItemStorage 组合后通过 @RegisterItemStorage 暴露给 Fabric Transfer API。
 */
class PipeSlotStorage(
    private val inventory: MutableList<ItemStack>,
    private val slotIndex: Int,
    private val capacity: Int,
    private val pipeItem: () -> net.minecraft.item.Item,
    private val markDirty: () -> Unit
) : SingleStackStorage() {

    override fun getStack(): ItemStack = inventory.getOrElse(slotIndex) { ItemStack.EMPTY }

    override fun setStack(stack: ItemStack) {
        inventory[slotIndex] = stack
    }

    override fun getCapacity(variant: ItemVariant): Int = if (variant.isOf(pipeItem())) capacity else 0

    override fun canInsert(variant: ItemVariant): Boolean {
        if (!variant.isOf(pipeItem())) return false
        val current = getStack()
        if (current.isEmpty) return true
        return current.count < capacity
    }

    override fun canExtract(variant: ItemVariant): Boolean = variant.isOf(pipeItem()) && !getStack().isEmpty

    override fun onFinalCommit() {
        markDirty()
    }
}
