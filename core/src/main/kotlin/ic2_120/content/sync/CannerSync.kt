package ic2_120.content.sync

import ic2_120.content.UpgradeableTickLimitedSidedEnergyContainer
import ic2_120.content.syncs.SyncSchema

/**
 * 流体/固体装罐机的同步属性与能量存储。
 * 规格：10 秒/次，4 EU/t，单次 800 EU，最大输入 32 EU/t (LV)
 * 复合流体装罐与固体装罐功能。
 */
class CannerSync(
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
    enum class Mode(val id: Int, val label: String) {
        BOTTLE_SOLID(0, "固体装罐"),
        EMPTY_LIQUID(1, "排出"),
        BOTTLE_LIQUID(2, "灌入"),
        ENRICH_LIQUID(3, "流体混合");

        companion object {
            fun fromId(id: Int): Mode = entries.firstOrNull { it.id == id } ?: EMPTY_LIQUID
        }
    }

    companion object {
        const val CANNER_TIER = 1
        /** 基础容量 800 EU，可完成一次完整装罐/倒出 */
        const val ENERGY_CAPACITY = 832L
        const val MAX_INSERT = 32L
        const val MAX_EXTRACT = 0L
        const val NBT_ENERGY_STORED = "EnergyStored"
        /** 加工耗时 10 秒 = 200 tick */
        const val PROGRESS_MAX = 200
        /** 功率 4 EU/t，单次操作共需 800 EU */
        const val ENERGY_PER_TICK = 4L
    }

    var energy by schema.int("Energy")
    var progress by schema.int("Progress")
    var energyCapacity by schema.int("EnergyCapacity", default = ENERGY_CAPACITY.toInt())
    var mode by schema.int("Mode", default = Mode.EMPTY_LIQUID.id)
    var leftFluidAmountMb by schema.int("LeftFluidAmount", default = 0)
    var leftFluidCapacityMb by schema.int("LeftFluidCapacity", default = 10000)
    var rightFluidAmountMb by schema.int("RightFluidAmount", default = 0)
    var rightFluidCapacityMb by schema.int("RightFluidCapacity", default = 10000)

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

    fun getMode(): Mode = Mode.fromId(mode)

    fun setMode(newMode: Mode) {
        mode = newMode.id
    }

    fun cycleMode() {
        val current = getMode()
        val next = Mode.entries[(current.ordinal + 1) % Mode.entries.size]
        setMode(next)
    }
}
