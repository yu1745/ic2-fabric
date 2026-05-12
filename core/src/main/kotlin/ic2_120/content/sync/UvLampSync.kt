package ic2_120.content.sync

import ic2_120.content.UpgradeableTickLimitedSidedEnergyContainer
import ic2_120.content.energy.EnergyTier
import ic2_120.content.syncs.SyncSchema

/**
 * 紫外线灯能量缓冲。
 * 能量等级由超频升级数量动态决定（tier = overclockerCount + 1）。
 * 输入上限跟随等级，不允许输出。
 */
class UvLampSync(
    schema: SyncSchema,
    currentTickProvider: () -> Long? = { null },
    maxInsertPerTickProvider: () -> Long = { EnergyTier.euPerTickFromTier(5) }
) : UpgradeableTickLimitedSidedEnergyContainer(
    ENERGY_CAPACITY,
    { 0L },
    EnergyTier.euPerTickFromTier(5),
    MAX_EXTRACT,
    currentTickProvider,
    maxInsertPerTickProvider
) {
    companion object {
        const val ENERGY_CAPACITY = 10_000L
        const val MAX_EXTRACT = 0L
        const val NBT_ENERGY_STORED = "EnergyStored"
    }

    var energy by schema.int("Energy")
    var energyCapacity by schema.int("EnergyCapacity", default = ENERGY_CAPACITY.toInt())
    var growthMultiplier by schema.int("GrowthMultiplier")

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
