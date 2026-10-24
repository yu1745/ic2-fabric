package ic2_120.content.storage

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant
import net.minecraft.item.ItemStack

data class ItemInsertRoute(
    val slotIndices: IntArray,
    val matcher: (ItemStack) -> Boolean,
    val maxPerSlot: Int? = null
)

class RoutedItemStorage(
    private val inventory: MutableList<ItemStack>,
    private val maxCountPerStackProvider: () -> Int,
    private val slotValidator: (Int, ItemStack) -> Boolean,
    val insertRoutes: List<ItemInsertRoute>,
    val extractSlots: IntArray,
    private val markDirty: () -> Unit
) : SnapshotParticipant<MutableList<ItemStack>>(), SlottedStorage<ItemVariant> {

    /**
     * Immutable routing metadata derived once when this storage is constructed.  The old
     * implementation rebuilt this relation with `insertRoutes.filter { slot in ... }` for
     * every sided-inventory query and every single-slot storage operation.  Route order is
     * retained, including the order in which a route first mentions a slot.
     */
    private data class SlotRouting(
        val routes: List<ItemInsertRoute>,
        val isExtractable: Boolean,
        val explicitMaxPerSlot: Int?
    )

    private val extractableSlotSet: Set<Int> = extractSlots.toHashSet()

    private val routingBySlot: Map<Int, SlotRouting> = run {
        val routesBySlot = LinkedHashMap<Int, MutableList<ItemInsertRoute>>()
        for (route in insertRoutes) {
            for (slot in route.slotIndices) {
                val routes = routesBySlot.getOrPut(slot) { ArrayList(1) }
                // A repeated slot in one route must not make the derived per-slot route
                // list repeat that route.  The original `filter` returned it only once.
                if (routes.none { it === route }) routes.add(route)
            }
        }

        // Add extraction-only slots as well, preserving the original visible-slot order:
        // all insertion route slots first, followed by extractSlots.
        for (slot in extractSlots) routesBySlot.putIfAbsent(slot, ArrayList(0))

        val result = LinkedHashMap<Int, SlotRouting>(routesBySlot.size)
        for ((slot, routes) in routesBySlot) {
            var explicitMax: Int? = null
            for (route in routes) {
                val max = route.maxPerSlot ?: continue
                val currentMax = explicitMax
                if (currentMax == null || max < currentMax) explicitMax = max
            }
            result[slot] = SlotRouting(
                routes = routes.toList(),
                isExtractable = slot in extractableSlotSet,
                explicitMaxPerSlot = explicitMax
            )
        }
        result
    }

    val visibleSlots: IntArray = routingBySlot.keys.toIntArray()

    /**
     * 为每个对外可见槽位预建的 SlottedStorage 单槽视图（含空槽），构成固定槽位表。
     *
     * 这是修复“被 Sinytra Connector / ForgifiedFabricAPI 桥接成 Forge IItemHandler 后，
     * 纯抽取槽（如复制机输出槽）的产物抽不动”的关键：FFAPI 对 [Storage]（非 SlottedStorage）
     * 会用 ItemStorageItemHandler，它在构造时一次性遍历 iterator() 固化非空槽快照并全局缓存，
     * 首次快照时为空的输出槽会被永久漏掉。改为 SlottedStorage 后，FFAPI 改用
     * SlottedItemStorageItemHandler，getSlots()/getStackInSlot()/extractItem() 全部实时按固定
     * 槽位查询，空槽（输出槽）永远占据一个槽位，产物出现即可被发现和抽取。
     */
    private val slots: List<Slot> = List(visibleSlots.size) { Slot(visibleSlots[it]) }
    private val cachedViews: MutableList<StorageView<ItemVariant>> =
        ArrayList<StorageView<ItemVariant>>(slots.size).apply { for (slot in slots) add(slot) }

    override fun getSlotCount(): Int = slots.size

    override fun getSlot(slot: Int): SingleSlotStorage<ItemVariant> {
        if (slot !in slots.indices) throw IndexOutOfBoundsException("Slot $slot 越界，槽位数=${slots.size}")
        return slots[slot]
    }

    /**
     * 判断指定 slot 是否允许从侧面插入指定物品（供 SidedInventory.canInsert 委托）。
     */
    fun canInsertFromSide(slot: Int, stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        val routesForSlot = routingBySlot[slot]?.routes ?: emptyList()
        if (routesForSlot.isEmpty()) return false
        return slotValidator(slot, stack) && routesForSlot.any { it.matcher(stack) }
    }

    override fun createSnapshot(): MutableList<ItemStack> = inventory.map { it.copy() }.toMutableList()

    override fun readSnapshot(snapshot: MutableList<ItemStack>) {
        for (i in inventory.indices) {
            inventory[i] = snapshot.getOrElse(i) { ItemStack.EMPTY }.copy()
        }
    }

    override fun onFinalCommit() {
        markDirty()
    }

    override fun supportsInsertion(): Boolean = insertRoutes.isNotEmpty()

    override fun supportsExtraction(): Boolean = extractSlots.isNotEmpty()

    override fun insert(resource: ItemVariant, maxAmount: Long, transaction: TransactionContext): Long {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount)
        if (maxAmount <= 0L || insertRoutes.isEmpty()) return 0L

        val probe = resource.toStack()
        var remaining = maxAmount
        var movedTotal = 0L

        for (route in insertRoutes) {
            if (remaining <= 0L) break
            if (!route.matcher(probe)) continue

            for (slot in route.slotIndices) {
                if (remaining <= 0L) break
                if (slot !in inventory.indices) continue
                val existing = inventory[slot]
                if (existing.isEmpty) continue
                if (!slotValidator(slot, probe) || ItemVariant.of(existing) != resource) continue
                val limit = slotLimit(route, existing)
                val room = (limit - existing.count).coerceAtLeast(0)
                if (room <= 0) continue
                val moved = minOf(remaining, room.toLong())
                if (moved <= 0L) continue
                updateSnapshots(transaction)
                existing.increment(moved.toInt())
                remaining -= moved
                movedTotal += moved
            }

            for (slot in route.slotIndices) {
                if (remaining <= 0L) break
                if (slot !in inventory.indices) continue
                val existing = inventory[slot]
                if (!existing.isEmpty) continue
                if (!slotValidator(slot, probe)) continue
                val limit = slotLimit(route, probe)
                if (limit <= 0) continue
                val moved = minOf(remaining, limit.toLong())
                if (moved <= 0L) continue
                updateSnapshots(transaction)
                val inserted = probe.copy()
                inserted.count = moved.toInt()
                inventory[slot] = inserted
                remaining -= moved
                movedTotal += moved
            }
        }

        return movedTotal
    }

    override fun extract(resource: ItemVariant, maxAmount: Long, transaction: TransactionContext): Long {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount)
        if (maxAmount <= 0L || extractSlots.isEmpty()) return 0L

        var remaining = maxAmount
        var extractedTotal = 0L

        for (slot in extractSlots) {
            if (remaining <= 0L) break
            val stack = inventory.getOrElse(slot) { ItemStack.EMPTY }
            if (stack.isEmpty) continue
            if (ItemVariant.of(stack) != resource) continue

            val extracted = minOf(remaining, stack.count.toLong())
            if (extracted <= 0L) continue
            updateSnapshots(transaction)
            if (extracted >= stack.count.toLong()) {
                inventory[slot] = ItemStack.EMPTY
            } else {
                stack.decrement(extracted.toInt())
            }
            remaining -= extracted
            extractedTotal += extracted
        }

        return extractedTotal
    }

    /**
     * 返回固定槽位表（含空槽）的视图迭代器，槽位稳定、不随状态漂移。
     * 空槽以 blank resource / amount 0 的 view 暴露，便于物流系统发现后续出现的物品。
     */
    override fun iterator(): MutableIterator<StorageView<ItemVariant>> = cachedViews.iterator()

    /**
     * 从路由规则派生指定 BE slot 的 [ic2_120.content.screen.slot.SlotSpec]。
     *
     * - canInsert: slotValidator AND 至少一条 route 的 matcher 通过
     * - maxItemCount: 所有覆盖该 slot 的 route 的 maxPerSlot 中的最小值；无 route 则 64
     * - canTake: slot 在 extractSlots 中，或 slot 在某条 insertRoute 的 slotIndices 中（玩家应能取回放入的物品）
     */
    fun deriveSlotSpec(beSlotIndex: Int): ic2_120.content.screen.slot.SlotSpec {
        val routing = routingBySlot[beSlotIndex]
        val routesForSlot = routing?.routes ?: emptyList()
        val isInsertable = routesForSlot.isNotEmpty()
        val isExtractable = routing?.isExtractable == true || isInsertable

        val canInsert: (ItemStack) -> Boolean = if (isInsertable) {
            { stack -> slotValidator(beSlotIndex, stack) && routesForSlot.any { it.matcher(stack) } }
        } else {
            { false }
        }

        val maxItemCount = routing?.explicitMaxPerSlot ?: 64

        val canTake: (net.minecraft.entity.player.PlayerEntity) -> Boolean = if (isExtractable) {
            { true }
        } else {
            { false }
        }

        return ic2_120.content.screen.slot.SlotSpec(
            maxItemCount = maxItemCount,
            canInsert = canInsert,
            canTake = canTake
        )
    }

    private fun slotLimit(route: ItemInsertRoute, stack: ItemStack): Int {
        route.maxPerSlot?.let { return it }
        return minOf(maxCountPerStackProvider(), stack.maxCount)
    }

    /**
     * 单个对外可见槽位的 SlottedStorage 单槽视图。所有读写都实时操作底层 [inventory] 并受
     * [insertRoutes] / [extractSlots] 约束：仅当本槽被某条 route 覆盖且 matcher+slotValidator 通过时
     * 允许插入，仅当本槽在 [extractSlots] 中时允许抽取。空槽也作为有效 view 暴露
     * （blank resource、amount 0、容量按可插入性给出上界）。
     */
    private inner class Slot(val beIndex: Int) : SingleSlotStorage<ItemVariant> {
        init {
            check(beIndex in inventory.indices) { "Slot 索引 $beIndex 越界，inventory size=${inventory.size}" }
        }

        private val routing: SlotRouting = routingBySlot[beIndex]
            ?: SlotRouting(emptyList(), isExtractable = false, explicitMaxPerSlot = null)
        private fun currentStack(): ItemStack = inventory[beIndex]

        /**
         * 本槽已有物品或给定 probe 时的最大堆叠：取覆盖本槽且 matcher 通过的 route 的
         * [ItemInsertRoute.maxPerSlot] 中最严格者，无显式限制时取 min(maxCountPerStack, maxCount)。
         */
        private fun perSlotLimit(stack: ItemStack): Int {
            var strict: Int? = null
            for (route in routing.routes) {
                if (!route.matcher(stack)) continue
                val max = route.maxPerSlot ?: continue
                val currentStrict = strict
                if (currentStrict == null || max < currentStrict) strict = max
            }
            return strict ?: minOf(maxCountPerStackProvider(), stack.maxCount)
        }

        /**
         * 空槽（blank resource）的容量上界：可插入槽取覆盖 route 的最严格 maxPerSlot（无则 maxCountPerStack）；
         * 纯抽取槽（无 route 覆盖）返回 0，表明该槽不接受任何插入。
         */
        private fun emptyCapacity(): Int {
            if (routing.routes.isEmpty()) return 0
            return routing.explicitMaxPerSlot ?: maxCountPerStackProvider()
        }

        override fun getResource(): ItemVariant {
            val stack = currentStack()
            return if (stack.isEmpty) ItemVariant.blank() else ItemVariant.of(stack)
        }

        override fun getAmount(): Long = currentStack().count.toLong()

        override fun getCapacity(): Long {
            val stack = currentStack()
            return if (stack.isEmpty) emptyCapacity().toLong() else perSlotLimit(stack).toLong()
        }

        override fun isResourceBlank(): Boolean = currentStack().isEmpty

        override fun supportsInsertion(): Boolean = routing.routes.isNotEmpty()

        override fun supportsExtraction(): Boolean = routing.isExtractable

        override fun insert(resource: ItemVariant, maxAmount: Long, transaction: TransactionContext): Long {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount)
            if (maxAmount <= 0L) return 0L
            val routes = routing.routes
            if (routes.isEmpty()) return 0L
            val probe = resource.toStack()
            if (!slotValidator(beIndex, probe)) return 0L
            var matched = false
            var strictLimit: Int? = null
            for (route in routes) {
                if (!route.matcher(probe)) continue
                matched = true
                val max = route.maxPerSlot ?: continue
                val currentStrict = strictLimit
                if (currentStrict == null || max < currentStrict) strictLimit = max
            }
            if (!matched) return 0L
            val limit = strictLimit ?: minOf(maxCountPerStackProvider(), probe.maxCount)
            val existing = inventory[beIndex]
            if (existing.isEmpty) {
                val moved = minOf(maxAmount, limit.toLong()).coerceAtLeast(0L)
                if (moved <= 0L) return 0L
                updateSnapshots(transaction)
                val placed = probe.copy()
                placed.count = moved.toInt()
                inventory[beIndex] = placed
                return moved
            }
            if (ItemVariant.of(existing) != resource) return 0L
            val room = (limit - existing.count).coerceAtLeast(0)
            if (room <= 0) return 0L
            val moved = minOf(maxAmount, room.toLong())
            if (moved <= 0L) return 0L
            updateSnapshots(transaction)
            existing.increment(moved.toInt())
            return moved
        }

        override fun extract(resource: ItemVariant, maxAmount: Long, transaction: TransactionContext): Long {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount)
            if (maxAmount <= 0L || !routing.isExtractable) return 0L
            val stack = currentStack()
            if (stack.isEmpty) return 0L
            if (ItemVariant.of(stack) != resource) return 0L
            val moved = minOf(maxAmount, stack.count.toLong()).coerceAtLeast(0L)
            if (moved <= 0L) return 0L
            updateSnapshots(transaction)
            if (moved >= stack.count.toLong()) {
                inventory[beIndex] = ItemStack.EMPTY
            } else {
                stack.decrement(moved.toInt())
            }
            return moved
        }
    }
}
