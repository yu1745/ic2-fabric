package ic2_120.content.sync

import ic2_120.content.UpgradeableTickLimitedSidedEnergyContainer
import ic2_120.content.syncs.SyncSchema

class ReplicatorSync(
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
        const val REPLICATOR_TIER = 3
        const val ENERGY_CAPACITY = 400_000L
        const val MAX_INSERT = 512L
        const val MAX_EXTRACT = 0L
        const val NBT_ENERGY_STORED = "EnergyStored"
        const val TANK_CAPACITY_MB = 16_000
        const val BASE_UB_PER_SECOND = 100
        const val BASE_UB_PER_TICK = 5
        const val ENERGY_PER_TICK = 512L

        const val MODE_SINGLE = 0
        const val MODE_CONTINUOUS = 1

        const val STATUS_IDLE = 0
        const val STATUS_NO_REDSTONE = 1
        const val STATUS_NO_STORAGE = 2
        const val STATUS_NO_TEMPLATE = 3
        const val STATUS_NO_FLUID = 4
        const val STATUS_NO_OUTPUT = 5
        const val STATUS_NO_ENERGY = 6
        const val STATUS_RUNNING = 7
        const val STATUS_COMPLETE = 8
    }

    var energy by schema.int("Energy")
    var energyCapacity by schema.int("EnergyCapacity", default = ENERGY_CAPACITY.toInt())
    var fluidAmountMb by schema.int("FluidAmountMb")
    var fluidCapacityMb by schema.int("FluidCapacityMb", default = TANK_CAPACITY_MB)
    var progressUb by schema.int("ProgressUb")
    var progressMaxUb by schema.int("ProgressMaxUb")
    var mode by schema.int("Mode", default = MODE_SINGLE)
    var status by schema.int("Status", default = STATUS_IDLE)
    var currentCostUb by schema.int("CurrentCostUb")

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
