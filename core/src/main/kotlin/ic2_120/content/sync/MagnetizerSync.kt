package ic2_120.content.sync

import ic2_120.content.TickLimitedSidedEnergyContainer
import ic2_120.content.syncs.SyncSchema

/**
 * 磁化机同步与能量容器。
 */
class MagnetizerSync(
    schema: SyncSchema,
    currentTickProvider: () -> Long? = { null },
    private val capacityBonusProvider: () -> Long = { 0L },
    private val maxInsertProvider: () -> Long = { MAX_INSERT }
) : TickLimitedSidedEnergyContainer(ENERGY_CAPACITY, MAX_INSERT, MAX_EXTRACT, currentTickProvider) {

    companion object {
        const val ENERGY_CAPACITY = 10_000L
        const val MAX_INSERT = 32L
        const val MAX_EXTRACT = 0L
        const val NBT_ENERGY_STORED = "EnergyStored"

        const val BASE_HEIGHT = 20
        const val HEIGHT_BONUS_PER_OVERCLOCKER = 4

        const val ENERGY_PER_PULSE_BASE = 100L
        const val ENERGY_PER_HEIGHT = 15L
        const val PULSE_INTERVAL_TICKS = 10
        const val PULSE_ACTIVE_TICKS = 10
    }

    var energy by schema.int("Energy")
    var energyCapacity by schema.int("EnergyCapacity", default = ENERGY_CAPACITY.toInt())
    var pulseCooldown by schema.int("PulseCooldown")
    var pulseTicksRemaining by schema.int("PulseTicksRemaining")
    var fenceCount by schema.int("FenceCount")
    var effectiveHeight by schema.int("EffectiveHeight", default = BASE_HEIGHT)
    var redstonePowered by schema.int("RedstonePowered")

    private val flow = EnergyFlowSync(schema, this)

    override fun onEnergyCommitted() {
        energy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    fun getEffectiveCapacity(): Long = (ENERGY_CAPACITY + capacityBonusProvider()).coerceAtLeast(ENERGY_CAPACITY)

    fun getEffectiveMaxInsertPerTick(): Long = maxInsertProvider().coerceAtLeast(1L)

    fun syncCurrentTickFlow() {
        flow.syncCurrentTickFlow()
    }

    fun getSyncedInsertedAmount(): Long = flow.getSyncedInsertedAmount()
    fun getSyncedConsumedAmount(): Long = flow.getSyncedConsumedAmount()
}

