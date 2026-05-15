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

    /**
     * 从 [RoutedItemStorage] 的 extractSlots 中提取 slot index 映射，
     * 用于判断哪些 handler slot 是输出/不可插入的。
     */
    fun isExtractOnlySlot(
        beSlotIndex: Int,
        itemStorage: RoutedItemStorage
    ): Boolean {
        // 如果一个 slot 只在 extractSlots 中、不在任何 insertRoute 中，则是只输出槽
        val inExtract = itemStorage.extractSlots.contains(beSlotIndex)
        val inInsert = itemStorage.insertRoutes.any { beSlotIndex in it.slotIndices }
        return inExtract && !inInsert
    }

    private fun insertFromRoute(stack: ItemStack, slot: Slot, maxPerSlot: Int?): Boolean {
        val slotStack = slot.stack
        // 直接用 route 的 maxPerSlot，不再依赖 SlotSpec
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

        if (!ItemStack.areItemsAndComponentsEqual(slotStack, stack)) return false

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
