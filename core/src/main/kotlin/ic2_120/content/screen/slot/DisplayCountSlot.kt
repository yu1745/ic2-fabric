package ic2_120.content.screen.slot

import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack

/**
 * Slot that keeps the backing inventory stack intact while exposing a safe count
 * to vanilla screen synchronization.
 */
class DisplayCountSlot(
    inventory: Container,
    index: Int,
    x: Int,
    y: Int,
    spec: SlotSpec,
    private val displayCount: Int = 1
) : PredicateSlot(inventory, index, x, y, spec) {

    override fun getItem(): ItemStack {
        val stack = container.getItem(index)
        if (stack.isEmpty || stack.count <= displayCount) return stack
        val copy = stack.copy()
        copy.count = displayCount
        return copy
    }

    /**
     * 使用真实库存数量计算剩余空间，避免 getItem() 返回假 count=1
     * 导致 Vanilla 误判槽位有空位而放入超出上限的物品。
     */
    override fun safeInsert(stack: ItemStack, count: Int): ItemStack {
        if (stack.isEmpty || !mayPlace(stack)) return stack
        val realStack = container.getItem(index)
        val slotMax = getMaxStackSize()
        val currentCount = realStack.count
        val space = slotMax - currentCount
        if (space <= 0) return stack
        val i = minOf(minOf(count, stack.count), space)
        if (i <= 0) return stack
        if (realStack.isEmpty) {
            this.set(stack.split(i))
        } else if (ItemStack.isSameItemSameComponents(realStack, stack)) {
            stack.shrink(i)
            realStack.grow(i)
            this.set(realStack)
        }
        return stack
    }
}
