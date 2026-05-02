package ic2_120.content.sync

import ic2_120.content.UpgradeableTickLimitedSidedEnergyContainer
import ic2_120.content.syncs.SyncSchema

/**
 * 流体装罐机的同步属性与能量存储。
 * 规格：5 秒/桶，2 EU/t，单次 200 EU，最大输入 32 EU/t (LV)
 */
class FluidCannerSync(
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
        const val FLUID_CANNER_TIER = 1
        /** 基础容量 200 EU，可完成一次完整装罐/倒出 */
        const val ENERGY_CAPACITY = 250L
        const val MAX_INSERT = 32L
        const val MAX_EXTRACT = 0L
        const val NBT_ENERGY_STORED = "EnergyStored"
        /** 加工耗时 5 秒 = 100 tick */
        const val PROGRESS_MAX = 100
        /** 功率 2 EU/t，单次操作共需 200 EU */
        const val ENERGY_PER_TICK = 2L
    }

    var energy by schema.int("Energy")
    var progress by schema.int("Progress")
    var energyCapacity by schema.int("EnergyCapacity", default = ENERGY_CAPACITY.toInt())
    var fluidAmountMb by schema.int("FluidAmount", default = 0)
    var fluidCapacityMb by schema.int("FluidCapacity", default = 10000)

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
