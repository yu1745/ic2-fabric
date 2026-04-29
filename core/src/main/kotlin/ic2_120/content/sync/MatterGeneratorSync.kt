package ic2_120.content.sync

import ic2_120.content.UpgradeableTickLimitedSidedEnergyContainer
import ic2_120.content.syncs.SyncSchema

class MatterGeneratorSync(
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
        const val MATTER_GENERATOR_TIER = 3
        const val ENERGY_CAPACITY = 4_000_000L
        const val MAX_INSERT = 512L
        const val MAX_EXTRACT = 0L
        const val NBT_ENERGY_STORED = "EnergyStored"
        const val PROGRESS_MAX = 1_954
        const val TANK_CAPACITY_MB = 10_000
        const val BASE_EU_PER_MB = 1_000_000L
        const val SCRAP_EU_PER_MB = 166_667L
        const val SCRAP_PER_MB = 34
    }

    var energy by schema.int("Energy")
    var progress by schema.int("Progress")
    var energyCapacity by schema.int("EnergyCapacity", default = ENERGY_CAPACITY.toInt())
    var fluidAmountMb by schema.int("FluidAmount", default = 0)
    var fluidCapacityMb by schema.int("FluidCapacity", default = TANK_CAPACITY_MB)
    var mode by schema.int("Mode", default = 0)

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
