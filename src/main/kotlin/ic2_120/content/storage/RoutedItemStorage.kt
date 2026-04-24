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
    private val insertRoutes: List<ItemInsertRoute>,
    private val extractSlots: IntArray,
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

    private fun isSlotAvailableForInsert(slot: Int, stack: ItemStack): Boolean {
        if (slot !in inventory.indices) return false
        return slotValidator(slot, stack)
    }

    private fun slotLimit(route: ItemInsertRoute, stack: ItemStack): Int {
        route.maxPerSlot?.let { return it }
        return minOf(maxCountPerStackProvider(), stack.maxCount)
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
