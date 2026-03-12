package ic2_120.content.sync

import ic2_120.content.UpgradeableTickLimitedSidedEnergyContainer
import ic2_120.content.syncs.SyncSchema

/**
 * 金属成型机的同步属性与能量存储。
 * 支持储能升级带来的额外容量、高压升级带来的输入速度。
 */
class MetalFormerSync(
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

    /**
     * 金属成型机加工模式
     */
    enum class Mode(val id: Int, val translationKey: String) {
        ROLLING(0, "mode.rolling"),      // 辊压模式
        CUTTING(1, "mode.cutting"),      // 切割模式
        EXTRUDING(2, "mode.extruding");  // 挤压模式

        companion object {
            fun fromId(id: Int): Mode = entries.firstOrNull { it.id == id } ?: ROLLING
        }
    }

    companion object {
        const val ENERGY_CAPACITY = 400L
        const val MAX_INSERT = 32L
        const val MAX_EXTRACT = 0L
        const val NBT_ENERGY_STORED = "EnergyStored"
        const val NBT_MODE = "Mode"
        const val PROGRESS_MAX = 200
        const val ENERGY_PER_TICK = 10L
    }

    var energy by schema.int("Energy")
    var progress by schema.int("Progress")
    var mode by schema.int("Mode")
    var energyCapacity by schema.int("EnergyCapacity", default = ENERGY_CAPACITY.toInt())
    private val flow = EnergyFlowSync(schema, this)

    override fun onEnergyCommitted() {
        energy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    fun getMode(): Mode = Mode.fromId(mode)

    fun setMode(newMode: Mode) {
        mode = newMode.id
    }

    fun cycleMode() {
        val currentMode = getMode()
        val nextMode = Mode.entries[(currentMode.ordinal + 1) % Mode.entries.size]
        setMode(nextMode)
    }

    /**
     * 在 tick 结束时调用，同步当前 tick 的实际输入/输出
     */
    fun syncCurrentTickFlow() {
        flow.syncCurrentTickFlow()
    }

    /** 获取同步的滤波后输入量（EU/t） */
    fun getSyncedInsertedAmount(): Long = flow.getSyncedInsertedAmount()

    /** 获取同步的滤波后输出量（EU/t） */
    fun getSyncedExtractedAmount(): Long = flow.getSyncedExtractedAmount()

    /** 获取同步的滤波后耗能量（EU/t） */
    fun getSyncedConsumedAmount(): Long = flow.getSyncedConsumedAmount()
}


