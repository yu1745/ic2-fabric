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
}
