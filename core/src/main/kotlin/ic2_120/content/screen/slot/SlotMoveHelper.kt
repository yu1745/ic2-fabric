package ic2_120.content.screen.slot

import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import net.minecraft.item.ItemStack
import net.minecraft.screen.slot.Slot

/**
 * 基于槽位规则的通用移动工具，用于 quickMove 复用。
 */
object SlotMoveHelper {

    /**
     * 基于 [RoutedItemStorage] 的路由规则，将 [stack] 按 route 优先级插入机器槽位。
     *
     * 使用 [beSlotToHandlerIndex] 将 BlockEntity 的 slot index 映射为 ScreenHandler 的 slot index，
     * 从 [handlerSlots] 中获取实际的 Slot 对象。同时复用 Route 自带的 matcher 和 maxPerSlot，
     * 绕过 Slot.canInsert / Slot.getMaxItemCount（它们可能持有旧规则），
     * 直接以 RoutedItemStorage 作为唯一数据源。
     *
     * @return 是否至少移动了 1 个物品。
     */
    fun insertFromRoutes(
        stack: ItemStack,
        itemStorage: RoutedItemStorage,
        insertRoutes: List<ItemInsertRoute>,
        beSlotToHandlerIndex: Map<Int, Int>,
        handlerSlots: List<Slot>,
    ): Boolean {
        if (stack.isEmpty) return false
        var movedAny = false

        for (route in insertRoutes) {
            if (stack.isEmpty) break
            if (!route.matcher(stack)) continue

            val maxPerSlot = route.maxPerSlot
            for (beSlot in route.slotIndices) {
                if (stack.isEmpty) break
                val handlerIdx = beSlotToHandlerIndex[beSlot] ?: continue
                val slot = handlerSlots[handlerIdx]
                if (insertFromRoute(stack, slot, maxPerSlot)) movedAny = true
            }
        }
        return movedAny
    }

    private fun insertFromRoute(stack: ItemStack, slot: Slot, maxPerSlot: Int?): Boolean {
        val slotStack = slot.stack
        val effectiveLimit = maxPerSlot ?: slot.maxItemCount
        val slotLimit = minOf(effectiveLimit, stack.maxCount)
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
