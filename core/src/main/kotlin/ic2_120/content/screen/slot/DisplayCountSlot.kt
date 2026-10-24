package ic2_120.content.screen.slot

import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack

/**
 * Slot that keeps the backing inventory stack intact while exposing a safe count
 * to vanilla screen synchronization.
 */
class DisplayCountSlot(
    inventory: Inventory,
    index: Int,
    x: Int,
    y: Int,
    spec: SlotSpec,
    private val displayCount: Int = 1
) : PredicateSlot(inventory, index, x, y, spec) {

    override fun getStack(): ItemStack {
        val stack = inventory.getStack(index)
        if (stack.isEmpty || stack.count <= displayCount) return stack
        val copy = stack.copy()
        copy.count = displayCount
        return copy
    }

    /**
     * 使用真实库存数量计算剩余空间，避免 getStack() 返回假 count=1
     * 导致 Vanilla 误判槽位有空位而放入超出上限的物品。
     */
    override fun insertStack(stack: ItemStack, count: Int): ItemStack {
        if (stack.isEmpty || !canInsert(stack)) return stack
        val realStack = inventory.getStack(index)
        val slotMax = getMaxItemCount()
        val currentCount = realStack.count
        val space = slotMax - currentCount
        if (space <= 0) return stack
        val i = minOf(minOf(count, stack.count), space)
        if (i <= 0) return stack
        if (realStack.isEmpty) {
            this.setStack(stack.split(i))
        } else if (ItemStack.canCombine(realStack, stack)) {
            stack.decrement(i)
            realStack.increment(i)
            this.setStack(realStack)
        }
        return stack
    }
}
