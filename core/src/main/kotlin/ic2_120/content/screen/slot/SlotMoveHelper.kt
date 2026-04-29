package ic2_120.content.screen.slot

import net.minecraft.item.ItemStack
import net.minecraft.screen.slot.Slot

/**
 * 基于槽位规则的通用移动工具，用于 quickMove 复用。
 */
object SlotMoveHelper {
    /**
     * 按给定顺序尝试把 [stack] 放入目标槽位列表。
     * 返回是否至少移动了 1 个物品。
     */
    fun insertIntoTargets(stack: ItemStack, targets: List<SlotTarget>): Boolean {
        if (stack.isEmpty) return false
        var movedAny = false
        for (target in targets) {
            if (stack.isEmpty) break
            if (insertIntoSingleTarget(stack, target)) movedAny = true
        }
        return movedAny
    }

    private fun insertIntoSingleTarget(stack: ItemStack, target: SlotTarget): Boolean {
        val slot = target.slot
        if (!slot.canInsert(stack)) return false

        val slotStack = slot.stack
        val slotLimit = minOf(target.spec.maxItemCount, slot.maxItemCount, stack.maxCount)
        if (slotLimit <= 0) return false

        if (slotStack.isEmpty) {
            val moveCount = minOf(slotLimit, stack.count)
            if (moveCount <= 0) return false
            val moved = stack.copy()
            moved.count = moveCount
            slot.stack = moved
            stack.decrement(moveCount)
            slot.markDirty()
            return true
        }

        if (!ItemStack.canCombine(slotStack, stack)) return false

        val space = (slotLimit - slotStack.count).coerceAtLeast(0)
        if (space <= 0) return false

        val moveCount = minOf(space, stack.count)
        if (moveCount <= 0) return false
        slotStack.increment(moveCount)
        stack.decrement(moveCount)
        slot.markDirty()
        return true
    }
}

data class SlotTarget(
    val slot: Slot,
    val spec: SlotSpec
)
