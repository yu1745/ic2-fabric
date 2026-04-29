package ic2_120.content.sync

import ic2_120.content.UpgradeableTickLimitedSidedEnergyContainer
import ic2_120.content.syncs.SyncSchema

class UuScannerSync(
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
        const val UU_SCANNER_TIER = 3
        const val ENERGY_CAPACITY = 200_000L
        const val MAX_INSERT = 512L
        const val MAX_EXTRACT = 0L
        const val NBT_ENERGY_STORED = "EnergyStored"
        const val SCAN_DURATION_TICKS = 20 * 165
        const val ENERGY_PER_TICK = 256L
        const val TOTAL_ENERGY_PER_SCAN = ENERGY_PER_TICK * SCAN_DURATION_TICKS
        const val PROGRESS_MAX = SCAN_DURATION_TICKS

        const val STATUS_IDLE = 0
        const val STATUS_NO_STORAGE = 1
        const val STATUS_NO_INPUT = 2
        const val STATUS_NOT_WHITELISTED = 3
        const val STATUS_NO_ENERGY = 4
        const val STATUS_SCANNING = 5
        const val STATUS_COMPLETE = 6
    }

    var energy by schema.int("Energy")
    var progress by schema.int("Progress")
    var energyCapacity by schema.int("EnergyCapacity", default = ENERGY_CAPACITY.toInt())
    var status by schema.int("Status", default = STATUS_IDLE)
    var currentCostUb by schema.int("CurrentCostUb", default = 0)

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
