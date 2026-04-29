package ic2_120.content.sync

import ic2_120.content.TickLimitedSidedEnergyContainer
import ic2_120.content.syncs.SyncSchema
import net.minecraft.util.math.Direction

class TeleporterSync(
    schema: SyncSchema,
    currentTickProvider: () -> Long? = { null },
    private val capacityBonusProvider: () -> Long = { 0L },
    private val maxInsertProvider: () -> Long = { MAX_INSERT }
) : TickLimitedSidedEnergyContainer(
    ENERGY_CAPACITY,
    maxInsertProvider(),
    MAX_EXTRACT,
    currentTickProvider
) {

    companion object {
        const val ENERGY_CAPACITY = 100_000L
        const val MAX_INSERT = 2048L
        const val MAX_EXTRACT = 0L
        const val NBT_ENERGY_STORED = "EnergyStored"

        const val BASE_COST = 2000L
        const val COST_PER_BLOCK = 12L
        const val BASE_COOLDOWN_TICKS = 40
    }

    var energy by schema.int("Energy")
    var energyCapacity by schema.int("EnergyCapacity", default = ENERGY_CAPACITY.toInt())
    var targetSet by schema.int("TargetSet")
    var targetX by schema.int("TargetX")
    var targetY by schema.int("TargetY")
    var targetZ by schema.int("TargetZ")
    var cooldown by schema.int("Cooldown")
    var charging by schema.int("Charging")
    var chargeProgress by schema.int("ChargeProgress")
    var chargeMax by schema.int("ChargeMax")
    var teleportRange by schema.int("TeleportRange", default = 1)

    private val flow = EnergyFlowSync(schema, this)

    override val capacity: Long
        get() = (ENERGY_CAPACITY + capacityBonusProvider()).coerceAtLeast(1L)

    override fun getSideMaxInsert(side: Direction?): Long = maxInsertProvider().coerceAtLeast(0L)

    override fun getSideMaxExtract(side: Direction?): Long = 0L

    fun getEffectiveCapacity(): Long = capacity

    override fun onEnergyCommitted() {
        energy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
        energyCapacity = getEffectiveCapacity().toInt().coerceIn(1, Int.MAX_VALUE)
    }

    fun syncCurrentTickFlow() {
        flow.syncCurrentTickFlow()
    }

    fun getSyncedInsertedAmount(): Long = flow.getSyncedInsertedAmount()
    fun getSyncedConsumedAmount(): Long = flow.getSyncedConsumedAmount()
}
