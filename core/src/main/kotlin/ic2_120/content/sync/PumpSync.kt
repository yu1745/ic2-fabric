package ic2_120.content.sync

import ic2_120.content.UpgradeableTickLimitedSidedEnergyContainer
import ic2_120.content.syncs.SyncSchema

class PumpSync(
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
        const val ENERGY_CAPACITY = 800L
        const val MAX_INSERT = 32L
        const val MAX_EXTRACT = 0L
        const val ENERGY_PER_BUCKET = 20L
        const val MB_PER_OPERATION = 1000
        const val NBT_ENERGY_STORED = "EnergyStored"
    }

    var energy by schema.int("Energy")
    var energyCapacity by schema.int("EnergyCapacity", default = ENERGY_CAPACITY.toInt())
    var fluidAmountMb by schema.int("FluidAmountMb")

    private val flow = EnergyFlowSync(schema, this)

    override fun onEnergyCommitted() {
        energy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    fun syncCurrentTickFlow() {
        flow.syncCurrentTickFlow()
    }

    fun getSyncedInsertedAmount(): Long = flow.getSyncedInsertedAmount()
    fun getSyncedConsumedAmount(): Long = flow.getSyncedConsumedAmount()
}
