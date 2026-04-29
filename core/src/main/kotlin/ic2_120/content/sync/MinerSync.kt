package ic2_120.content.sync

import ic2_120.content.UpgradeableTickLimitedSidedEnergyContainer
import ic2_120.content.syncs.SyncSchema

class MinerSync(
    schema: SyncSchema,
    currentTickProvider: () -> Long? = { null },
    capacityProvider: () -> Long,
    maxInsertPerTickProvider: (() -> Long)? = null
) : UpgradeableTickLimitedSidedEnergyContainer(
    BASE_ENERGY_CAPACITY,
    { (capacityProvider() - BASE_ENERGY_CAPACITY).coerceAtLeast(0L) },
    MAX_INSERT,
    MAX_EXTRACT,
    currentTickProvider,
    maxInsertPerTickProvider
) {
    companion object {
        const val BASE_ENERGY_CAPACITY = 10000L
        const val MAX_INSERT = 512L
        const val MAX_EXTRACT = 0L
        const val SCAN_ENERGY_PER_STEP = 64L
        const val DRILL_ENERGY_PER_BREAK = 500L
        const val DIAMOND_OR_IRIDIUM_ENERGY_PER_BREAK = 1500L
        const val SILK_TOUCH_MULTIPLIER = 10L
        const val NBT_ENERGY_STORED = "EnergyStored"
    }

    var energy by schema.int("Energy")
    var energyCapacity by schema.int("EnergyCapacity", default = BASE_ENERGY_CAPACITY.toInt())
    var running by schema.int("Running", default = 1)
    var mode by schema.int("Mode", default = 1) // 0=whitelist, 1=blacklist
    var silkTouch by schema.int("SilkTouch", default = 0)
    var cursorX by schema.int("CursorX")
    var cursorY by schema.int("CursorY")
    var cursorZ by schema.int("CursorZ")
    var progressTicks by schema.int("ProgressTicks")

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
