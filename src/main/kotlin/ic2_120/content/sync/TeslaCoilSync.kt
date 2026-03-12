package ic2_120.content.sync

import ic2_120.content.TickLimitedSidedEnergyContainer
import ic2_120.content.sync.EnergyFlowSync
import ic2_120.content.syncs.SyncSchema

/**
 * 特斯拉线圈的同步属性与能量存储。
 * 不支持升级，使用简单的 TickLimitedSidedEnergyContainer。
 * 规格：5000 EU 容量，128 EU/t 输入（MV 等级），0 输出。
 * 需达到 5000 EU 才能工作，每秒工作一次。
 */
class TeslaCoilSync(
    schema: SyncSchema,
    currentTickProvider: () -> Long? = { null }
) : TickLimitedSidedEnergyContainer(ENERGY_CAPACITY, MAX_INSERT, MAX_EXTRACT, currentTickProvider) {

    companion object {
        const val ENERGY_CAPACITY = 5000L
        const val MAX_INSERT = 128L
        const val MAX_EXTRACT = 0L
        const val NBT_ENERGY_STORED = "EnergyStored"
        /** 每次放电消耗 128 EU */
        const val ENERGY_PER_SHOT = 128L
        /** 放电间隔（tick），每秒一次 */
        const val SHOT_INTERVAL = 20
        /** 正常工作所需最小能量（达到 5000 EU 才能工作） */
        const val MIN_ENERGY_TO_OPERATE = 5000L
    }

    var energy by schema.int("Energy")

    private val flow = EnergyFlowSync(schema, this)

    override fun onEnergyCommitted() {
        energy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    fun syncCurrentTickFlow() {
        flow.syncCurrentTickFlow()
    }

    fun getSyncedInsertedAmount(): Long = flow.getSyncedInsertedAmount()

    fun getSyncedExtractedAmount(): Long = flow.getSyncedExtractedAmount()

    fun getSyncedConsumedAmount(): Long = flow.getSyncedConsumedAmount()
}






