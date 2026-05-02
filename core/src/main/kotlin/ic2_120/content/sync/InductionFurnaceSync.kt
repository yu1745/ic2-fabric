package ic2_120.content.sync

import ic2_120.content.UpgradeableTickLimitedSidedEnergyContainer
import ic2_120.content.sync.EnergyFlowSync
import ic2_120.content.syncs.SyncSchema

class InductionFurnaceSync(
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
        const val ENERGY_CAPACITY = 1600L
        const val MAX_INSERT = 128L
        const val MAX_EXTRACT = 0L
        const val NBT_ENERGY_STORED = "EnergyStored"
        const val EU_PER_OPERATION = 150L
        const val BASE_TICKS_PER_OPERATION = 10.0
        const val MAX_HEAT_ENERGY_PER_TICK = 1L
        const val MIN_HEAT_THRESHOLD = 1
        const val HEAT_MAX = 10000
        const val HEAT_CHANGE_PER_TICK = 5
    }

    var energy by schema.int("Energy")
    var progressSlot0 by schema.int("Progress0")
    var progressSlot1 by schema.int("Progress1")
    var heat by schema.int("Heat")
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
