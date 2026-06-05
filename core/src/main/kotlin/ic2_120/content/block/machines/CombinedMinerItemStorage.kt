package ic2_120.content.block.machines

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext

/**
 * 组合两个 Storage<ItemVariant>，对外暴露为一个统一的存储。
 * insert 按顺序尝试两个子存储，extract 同理。
 */
class CombinedMinerItemStorage(
    private val general: Storage<ItemVariant>,
    private val pipe: Storage<ItemVariant>
) : Storage<ItemVariant> {

    override fun supportsInsertion(): Boolean = general.supportsInsertion() || pipe.supportsInsertion()

    override fun supportsExtraction(): Boolean = general.supportsExtraction() || pipe.supportsExtraction()

    override fun insert(resource: ItemVariant, maxAmount: Long, transaction: TransactionContext): Long {
        var remaining = maxAmount
        var total = 0L
        for (storage in listOf(pipe, general)) {
            if (remaining <= 0L) break
            val moved = storage.insert(resource, remaining, transaction)
            remaining -= moved
            total += moved
        }
        return total
    }

    override fun extract(resource: ItemVariant, maxAmount: Long, transaction: TransactionContext): Long {
        var remaining = maxAmount
        var total = 0L
        for (storage in listOf(pipe, general)) {
            if (remaining <= 0L) break
            val moved = storage.extract(resource, remaining, transaction)
            remaining -= moved
            total += moved
        }
        return total
    }

    override fun iterator(): MutableIterator<StorageView<ItemVariant>> {
        val views = mutableListOf<StorageView<ItemVariant>>()
        for (storage in listOf(pipe, general)) {
            for (view in storage) {
                if (!view.isResourceBlank) {
                    views.add(view)
                }
            }
        }
        return views.iterator()
    }
}
