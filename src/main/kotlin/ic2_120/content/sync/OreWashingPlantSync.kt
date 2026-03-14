package ic2_120.content.sync

import ic2_120.content.UpgradeableTickLimitedSidedEnergyContainer
import ic2_120.content.syncs.SyncSchema
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants

class OreWashingPlantSync(
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
        const val ENERGY_CAPACITY = 16000L      // 基础容量 1.6W EU
        const val MAX_INSERT = 32L             // 最大输入
        const val MAX_EXTRACT = 0L             // 最大输出（0=不输出）
        const val NBT_ENERGY_STORED = "EnergyStored"
        const val NBT_WATER_AMOUNT = "WaterAmount"
        const val PROGRESS_MAX = 500           // 加工所需 ticks（25秒 @ 20 ticks/s）
        const val ENERGY_PER_TICK = 16L        // 每 tick 耗能（总计 8000 EU = 25秒 * 16 EU/t）
        const val WATER_PER_OPERATION = FluidConstants.BUCKET  // 每次加工消耗1桶水
    }

    var energy by schema.int("Energy")
    var progress by schema.int("Progress")
    var energyCapacity by schema.int("EnergyCapacity", default = ENERGY_CAPACITY.toInt())
    var waterAmountMb by schema.int("WaterAmount", default = 0)  // 以 mB 为单位显示

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
