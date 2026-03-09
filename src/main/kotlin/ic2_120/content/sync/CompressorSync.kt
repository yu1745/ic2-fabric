package ic2_120.content.sync

import ic2_120.content.UpgradeableTickLimitedEnergyStorage
import ic2_120.content.syncs.SyncSchema

/**
 * 压缩机的同步属性与能量存储。
 * 支持储能升级带来的额外容量、高压升级带来的输入速度。
 */
class CompressorSync(
    schema: SyncSchema,
    currentTickProvider: () -> Long? = { null },
    capacityBonusProvider: () -> Long = { 0L },
    maxInsertPerTickProvider: (() -> Long)? = null
) : UpgradeableTickLimitedEnergyStorage(
    ENERGY_CAPACITY,
    capacityBonusProvider,
    MAX_INSERT,
    MAX_EXTRACT,
    currentTickProvider,
    maxInsertPerTickProvider
) {

    companion object {
        /** 基础容量 600 EU，可完成一次完整压缩 */
        const val ENERGY_CAPACITY = 600L
        const val MAX_INSERT = 32L
        const val MAX_EXTRACT = 0L
        const val NBT_ENERGY_STORED = "EnergyStored"
        /** 加工耗时 15 秒 = 300 tick */
        const val PROGRESS_MAX = 300
        /** 功率 2 EU/t，单次压缩共需 600 EU */
        const val ENERGY_PER_TICK = 2L
    }

    var energy by schema.int("Energy")
    var progress by schema.int("Progress")
    var energyCapacity by schema.int("EnergyCapacity", default = ENERGY_CAPACITY.toInt())

    /** 当前有效的每 tick 输入上限（含高压升级），供 BlockEntity 调用 */
    fun getMaxInsertPerTick(): Long = getEffectiveMaxInsertPerTick()

    override fun onFinalCommit() {
        energy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }
}
