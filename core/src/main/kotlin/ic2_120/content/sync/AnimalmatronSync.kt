package ic2_120.content.sync

import ic2_120.content.UpgradeableTickLimitedSidedEnergyContainer
import ic2_120.content.syncs.SyncSchema

/**
 * 牲畜监管机同步数据
 *
 * 包含能量、流体和动物护理统计数据
 */
class AnimalmatronSync(
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
        const val ENERGY_CAPACITY = 10_000L
        const val MAX_INSERT = 32L
        const val MAX_EXTRACT = 0L
        const val NBT_ENERGY_STORED = "EnergyStored"

        const val WATER_TANK_CAPACITY_MB = 2_000
        const val WEED_EX_TANK_CAPACITY_MB = 2_000
    }

    var energy by schema.int("Energy")
    var energyCapacity by schema.int("EnergyCapacity", default = ENERGY_CAPACITY.toInt())
    var waterAmountMb by schema.int("WaterAmountMb")
    var weedExAmountMb by schema.int("WeedExAmountMb")

    // 统计数据
    var animalCount by schema.int("AnimalCount")
    var touchedThisRun by schema.int("TouchedThisRun")
    var foodConsumedThisRun by schema.int("FoodConsumedThisRun")
    var waterConsumedThisRun by schema.int("WaterConsumedThisRun")
    var weedExConsumedThisRun by schema.int("WeedExConsumedThisRun")
    var grewUpThisRun by schema.int("GrewUpThisRun")
    var canBreedNowThisRun by schema.int("CanBreedNowThisRun")
    var bredThisRun by schema.int("BredThisRun")

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
