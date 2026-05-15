package ic2_120.content.storage

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
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
) : SnapshotParticipant<MutableList<ItemStack>>(), Storage<ItemVariant> {

    private val visibleSlots: IntArray = linkedSetOf<Int>().apply {
        for (route in insertRoutes) {
            for (slot in route.slotIndices) add(slot)
        }
        for (slot in extractSlots) add(slot)
    }.toIntArray()

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
                if (!isSlotAvailableForInsert(slot, probe)) continue
                val existing = inventory[slot]
                if (existing.isEmpty || ItemVariant.of(existing) != resource) continue
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
                if (!isSlotAvailableForInsert(slot, probe)) continue
                val existing = inventory[slot]
                if (!existing.isEmpty) continue
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

    override fun iterator(): MutableIterator<StorageView<ItemVariant>> {
        val views = mutableListOf<StorageView<ItemVariant>>()
        for (slot in visibleSlots) {
            val stack = inventory.getOrElse(slot) { ItemStack.EMPTY }
            if (stack.isEmpty) continue
            views.add(object : StorageView<ItemVariant> {
                override fun getResource(): ItemVariant = ItemVariant.of(inventory[slot])

                override fun getAmount(): Long = inventory[slot].count.toLong()

                override fun getCapacity(): Long {
                    val current = inventory[slot]
                    return if (current.isEmpty) {
                        0L
                    } else {
                        slotLimitForView(slot, current).toLong()
                    }
                }

                override fun extract(resource: ItemVariant, maxAmount: Long, transaction: TransactionContext): Long =
                    this@RoutedItemStorage.extract(resource, maxAmount, transaction)

                override fun isResourceBlank(): Boolean = inventory[slot].isEmpty
            })
        }
        return views.iterator()
    }

    /**
     * 从路由规则派生指定 BE slot 的 [ic2_120.content.screen.slot.SlotSpec]。
     *
     * - canInsert: slotValidator AND 至少一条 route 的 matcher 通过
     * - maxItemCount: 所有覆盖该 slot 的 route 的 maxPerSlot 中的最小值；无 route 则 64
     * - canTake: slot 在 extractSlots 中，或 slot 在某条 insertRoute 的 slotIndices 中（玩家应能取回放入的物品）
     */
    fun deriveSlotSpec(beSlotIndex: Int): ic2_120.content.screen.slot.SlotSpec {
        val routesForSlot = insertRoutes.filter { beSlotIndex in it.slotIndices }
        val isInsertable = routesForSlot.isNotEmpty()
        val isExtractable = beSlotIndex in extractSlots || isInsertable

        val canInsert: (ItemStack) -> Boolean = if (isInsertable) {
            { stack -> slotValidator(beSlotIndex, stack) && routesForSlot.any { it.matcher(stack) } }
        } else {
            { false }
        }

        val maxItemCount = if (routesForSlot.isEmpty()) {
            64
        } else {
            routesForSlot.mapNotNull { it.maxPerSlot }.minOrNull() ?: 64
        }

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

    private fun isSlotAvailableForInsert(slot: Int, stack: ItemStack): Boolean {
        if (slot !in inventory.indices) return false
        return slotValidator(slot, stack)
    }

    private fun slotLimit(route: ItemInsertRoute, stack: ItemStack): Int {
        route.maxPerSlot?.let { return it }
        return minOf(maxCountPerStackProvider(), stack.maxCount)
    }

    /**
     * 根据路由规则为指定 BlockEntity 槽位推导 [SlotSpec]。
     * - 若该槽位出现在某条 insertRoute 中，则 canInsert 使用该 route 的 matcher，
     *   maxItemCount 取该 route 的 maxPerSlot（未设置则默认 64）。
     * - 若该槽位不在任何 insertRoute 中，则 canInsert 返回 false（纯输出槽）。
     * - canTake 始终为 true。
     */
    fun deriveSlotSpec(beSlotIndex: Int): ic2_120.content.screen.slot.SlotSpec {
        val matchingRoutes = insertRoutes.filter { beSlotIndex in it.slotIndices }
        if (matchingRoutes.isEmpty()) {
            // 纯输出/不可插入槽位
            return ic2_120.content.screen.slot.SlotSpec(
                canInsert = { false },
                canTake = { true }
            )
        }
        val combinedMatcher: (ItemStack) -> Boolean = { stack ->
            matchingRoutes.any { it.matcher(stack) }
        }
        val maxPerSlot = matchingRoutes.mapNotNull { it.maxPerSlot }.minOrNull() ?: 64
        return ic2_120.content.screen.slot.SlotSpec(
            maxItemCount = maxPerSlot,
            canInsert = combinedMatcher,
            canTake = { true }
        )
    }

    private fun slotLimitForView(slot: Int, stack: ItemStack): Int {
        var maxRouteLimit: Int? = null
        for (route in insertRoutes) {
            if (slot !in route.slotIndices) continue
            if (!route.matcher(stack)) continue
            val limit = route.maxPerSlot ?: continue
            maxRouteLimit = if (maxRouteLimit == null) limit else maxOf(maxRouteLimit, limit)
        }
        return maxRouteLimit ?: minOf(maxCountPerStackProvider(), stack.maxCount)
    }
}
