package ic2_120.content.block.nuclear

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant
import net.minecraft.item.ItemStack

/**
 * 将核反应堆的 Inventory 包装为 Fabric Transfer API 的 Storage<ItemVariant>。
 * 反应堆中每个物品堆叠上限为 1（组件不可堆叠），通过 isValid() 进行槽位验证。
 */
object ReactorItemStorageProvider {

    fun getStorage(reactor: NuclearReactorBlockEntity): Storage<ItemVariant> = ReactorItemStorage(reactor)

    fun getStorageForChamber(be: ReactorChamberBlockEntity): Storage<ItemVariant>? {
        val reactor = be.findAdjacentReactorPublic() ?: return null
        return ReactorItemStorage(reactor)
    }

    fun getStorageForAccessHatch(be: ReactorAccessHatchBlockEntity): Storage<ItemVariant>? {
        val reactor = be.getCentralReactorPublic() ?: return null
        return ReactorItemStorage(reactor)
    }
}

private class ReactorItemStorage(
    private val reactor: NuclearReactorBlockEntity
) : SnapshotParticipant<Unit>(), Storage<ItemVariant> {

    override fun createSnapshot() {}

    override fun readSnapshot(snapshot: Unit) {}

    override fun onFinalCommit() {
        reactor.markDirty()
    }

    override fun supportsInsertion(): Boolean = true

    override fun supportsExtraction(): Boolean = true

    override fun insert(resource: ItemVariant, maxAmount: Long, transaction: TransactionContext): Long {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount)
        if (maxAmount <= 0L) return 0L

        val probe = resource.toStack()
        var movedTotal = 0L

        // 反应堆物品不可堆叠，只放入空槽
        for (slot in 0 until reactor.size()) {
            if (movedTotal >= maxAmount) break
            if (!reactor.isValid(slot, probe)) continue
            if (!reactor.getStack(slot).isEmpty) continue
            updateSnapshots(transaction)
            reactor.setStack(slot, probe.copy())
            movedTotal += 1L
        }

        return movedTotal
    }

    override fun extract(resource: ItemVariant, maxAmount: Long, transaction: TransactionContext): Long {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount)
        if (maxAmount <= 0L) return 0L

        var remaining = maxAmount
        var extractedTotal = 0L

        for (slot in 0 until reactor.size()) {
            if (remaining <= 0L) break
            val stack = reactor.getStack(slot)
            if (stack.isEmpty) continue
            if (ItemVariant.of(stack) != resource) continue

            updateSnapshots(transaction)
            reactor.setStack(slot, ItemStack.EMPTY)
            remaining -= 1L
            extractedTotal += 1L
        }

        return extractedTotal
    }

    override fun iterator(): MutableIterator<StorageView<ItemVariant>> {
        val views = mutableListOf<StorageView<ItemVariant>>()
        for (slot in 0 until reactor.size()) {
            val stack = reactor.getStack(slot)
            if (stack.isEmpty) continue
            views.add(object : StorageView<ItemVariant> {
                override fun getResource(): ItemVariant = ItemVariant.of(reactor.getStack(slot))
                override fun getAmount(): Long = 1L
                override fun getCapacity(): Long = 1L
                override fun extract(resource: ItemVariant, maxAmount: Long, transaction: TransactionContext): Long =
                    this@ReactorItemStorage.extract(resource, maxAmount, transaction)
                override fun isResourceBlank(): Boolean = reactor.getStack(slot).isEmpty
            })
        }
        return views.iterator()
    }
}
