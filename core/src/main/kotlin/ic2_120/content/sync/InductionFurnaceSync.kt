package ic2_120.content.sync

import ic2_120.content.TickLimitedSidedEnergyContainer
import ic2_120.content.sync.EnergyFlowSync
import ic2_120.content.syncs.SyncSchema

/**
 * 感应炉的同步属性与能量存储。
 */
class InductionFurnaceSync(
    schema: SyncSchema,
    currentTickProvider: () -> Long? = { null }
) : TickLimitedSidedEnergyContainer(
    ENERGY_CAPACITY,
    MAX_INSERT,
    MAX_EXTRACT,
    currentTickProvider
) {

    companion object {
        /** 能量缓存容量 */
        const val ENERGY_CAPACITY = 1600L
        /** 最大输入（MV, 128 EU/t） */
        const val MAX_INSERT = 128L
        /** 最大输出（不输出） */
        const val MAX_EXTRACT = 0L
        const val NBT_ENERGY_STORED = "EnergyStored"

        /** 每个物品所需总能量（EU），热量 100% 时 */
        const val EU_PER_OPERATION = 150L

        /** 基准加工时间（ticks），热量 100% 时 */
        const val BASE_TICKS_PER_OPERATION = 10.0

        /** 热量 100% 时每 tick 消耗能量（加热维持） */
        const val MAX_HEAT_ENERGY_PER_TICK = 1L

        /** 热量最小阈值，低于此值不加工（存储值） */
        const val MIN_HEAT_THRESHOLD = 1
        /** 热量最大百分比（存储值 = 实际% × 100） */
        const val HEAT_MAX = 10000
        /** 热量上升/下降速度（存储值 per tick），100秒从0到100% */
        const val HEAT_CHANGE_PER_TICK = 5
    }

    var energy by schema.int("Energy")
    /** 槽 0 加工进度（ticks） */
    var progressSlot0 by schema.int("Progress0")
    /** 槽 1 加工进度（ticks） */
    var progressSlot1 by schema.int("Progress1")
    /** 热量百分比（0-100） */
    var heat by schema.int("Heat")
    var energyCapacity by schema.int("EnergyCapacity", default = ENERGY_CAPACITY.toInt())

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
