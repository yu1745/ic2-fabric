package ic2_120.content.sync

import ic2_120.content.TickLimitedEnergyStorage
import ic2_120.content.syncs.SyncSchema

/**
 * 提取机的同步属性与能量存储。
 */
class ExtractorSync(
    schema: SyncSchema,
    currentTickProvider: () -> Long? = { null }
) : TickLimitedEnergyStorage(ENERGY_CAPACITY, MAX_INSERT, MAX_EXTRACT, currentTickProvider) {

    companion object {
        const val ENERGY_CAPACITY = 800L
        const val MAX_INSERT = 32L
        const val MAX_EXTRACT = 0L
        const val NBT_ENERGY_STORED = "EnergyStored"
        /** 加工耗时 20 秒 = 400 tick */
        const val PROGRESS_MAX = 400
        /** 功率 2 EU/t，单次提取共需 800 EU */
        const val ENERGY_PER_TICK = 2L
    }

    var energy by schema.int("Energy")
    var progress by schema.int("Progress")

    override fun onFinalCommit() {
        energy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }
}
