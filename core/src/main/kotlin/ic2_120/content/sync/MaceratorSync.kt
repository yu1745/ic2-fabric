package ic2_120.content.sync

import ic2_120.content.UpgradeableTickLimitedSidedEnergyContainer
import ic2_120.content.sync.EnergyFlowSync
import ic2_120.content.syncs.SyncSchema

/**
 * 打粉机的同步属性与能量存储。
 * 支持储能升级带来的额外容量、高压升级带来的输入速度。
 */
class MaceratorSync(
    schema: SyncSchema,
    currentTickProvider: () -> Long? = { null },
    capacityBonusProvider: () -> Long = { 0L },
    maxInsertPerTickProvider: (() -> Long)? = null
) : UpgradeableTickLimitedSidedEnergyContainer(
    ENERGY_CAPACITY,
    capacityBonusProvider,
    MAX_INSERT,
    MAX_EXTRACT,
    currentTickProvider,
    maxInsertPerTickProvider
) {

    companion object {
        const val ENERGY_CAPACITY = 416L
        const val MAX_INSERT = 32L
        const val MAX_EXTRACT = 0L
        const val NBT_ENERGY_STORED = "EnergyStored"
        const val PROGRESS_MAX = 130
        const val ENERGY_PER_TICK = 2L
    }

    var energy by schema.int("Energy")
    var progress by schema.int("Progress")  // 加工进度 0..PROGRESS_MAX
    var energyCapacity by schema.int("EnergyCapacity", default = ENERGY_CAPACITY.toInt())

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






