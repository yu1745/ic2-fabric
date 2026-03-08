package ic2_120.content.sync

import ic2_120.content.TickLimitedEnergyStorage
import ic2_120.content.syncs.SyncSchema

/**
 * 金属成型机的同步属性与能量存储。
 */
class MetalFormerSync(
    schema: SyncSchema,
    currentTickProvider: () -> Long? = { null }
) : TickLimitedEnergyStorage(ENERGY_CAPACITY, MAX_INSERT, MAX_EXTRACT, currentTickProvider) {

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
        const val ENERGY_CAPACITY = 416L
        const val MAX_INSERT = 32L
        const val MAX_EXTRACT = 0L
        const val NBT_ENERGY_STORED = "EnergyStored"
        const val NBT_MODE = "Mode"
        const val PROGRESS_MAX = 130
        const val ENERGY_PER_TICK = 2L
    }

    var energy by schema.int("Energy")
    var progress by schema.int("Progress")
    var mode by schema.int("Mode")

    override fun onFinalCommit() {
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
}
