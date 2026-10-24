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
        // 直接读槽位背后的真实库存（而非 slot.stack）：DisplayCountSlot.getStack
        // 在真实数量超过展示上限时返回 count 被篡改的副本，若对其 increment/decrement
        // 增量会落在副本上而源被扣减——物品凭空消失。
        // 注意必须用 slot.index（库存索引）而非 slot.id（handler 槽位列表序号）：
        // 两者在高级采矿机等跳过中间槽位的 GUI 布局中错位，用 id 判空会读到别的槽，
        // 而写入（setStack）仍走 index，导致新物品直接覆写已有物品（吞升级）。
        val realStack = slot.inventory.getStack(slot.index)
        // 源槽守卫：目标槽即源槽（同一实例）时拒绝——同实例合并是历史复制 bug 的根因。
        if (realStack === stack) return false
        val effectiveLimit = maxPerSlot ?: slot.maxItemCount
        val slotLimit = effectiveLimit
        if (slotLimit <= 0) return false

        if (realStack.isEmpty) {
            val moveCount = minOf(slotLimit, stack.count)
            if (moveCount <= 0) return false
            val moved = stack.copy()
            moved.count = moveCount
            slot.stack = moved
            stack.decrement(moveCount)
            slot.markDirty()
            return true
        }

        if (!ItemStack.canCombine(realStack, stack)) return false

        val space = (slotLimit - realStack.count).coerceAtLeast(0)
        if (space <= 0) return false

        val moveCount = minOf(space, stack.count)
        if (moveCount <= 0) return false
        realStack.increment(moveCount)
        stack.decrement(moveCount)
        slot.markDirty()
        return true
    }
}
