package ic2_120.content.sync

import ic2_120.content.UpgradeableTickLimitedSidedEnergyContainer
import ic2_120.content.sync.EnergyFlowSync
import ic2_120.content.syncs.SyncSchema

/**
 * 方块切割机的同步属性与能量存储。
 * 能量等级 1（LV），最大 32 EU/t，耗能 4 EU/t，切割 1 个方块约 22.5 秒（1800 EU）。
 */
class BlockCutterSync(
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
        const val ENERGY_CAPACITY = 3600L
        const val MAX_INSERT = 32L
        const val MAX_EXTRACT = 0L
        const val NBT_ENERGY_STORED = "EnergyStored"
        /** 加工所需 ticks（22.5 秒 = 450 ticks） */
        const val PROGRESS_MAX = 450
        const val ENERGY_PER_TICK = 4L
    }

    var energy by schema.int("Energy")
    var progress by schema.int("Progress")
    var energyCapacity by schema.int("EnergyCapacity", default = ENERGY_CAPACITY.toInt())
    /** 锯片硬度不够或缺失（1=是，0=否，客户端显示提示用） */
    var bladeTooWeak by schema.int("BladeTooWeak", default = 0)

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
