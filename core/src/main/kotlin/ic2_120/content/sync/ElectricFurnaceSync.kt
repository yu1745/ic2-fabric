package ic2_120.content.sync

import ic2_120.content.UpgradeableTickLimitedSidedEnergyContainer
import ic2_120.content.sync.EnergyFlowSync
import ic2_120.content.syncs.SyncSchema

/**
 * 电炉的同步属性与能量存储。
 * 支持储能升级带来的额外容量、高压升级带来的输入速度。
 */
class ElectricFurnaceSync(
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
        /** 电力缓存容量，与 IC2 实验版电炉一致（416 EU） */
        const val ENERGY_CAPACITY = 416L
        const val MAX_INSERT = 32L
        /** 对外不允许提取（电缆等不能从电炉拉电）；内部消耗在 BlockEntity.tick 中直接扣减 amount */
        const val MAX_EXTRACT = 0L
        const val NBT_ENERGY_STORED = "EnergyStored"
        /** 烧炼完成所需进度（与 IC2 实验版电炉一致：6.5 秒 = 130 tick） */
        const val PROGRESS_MAX = 130
        /** 每 tick 消耗能量（EU），与 IC2 实验版一致：3 EU/t，整炉 130 tick 共 390 EU */
        const val ENERGY_PER_TICK = 3L
    }

    var energy by schema.int("Energy")
    var progress by schema.int("Progress")
    var energyCapacity by schema.int("EnergyCapacity", default = ENERGY_CAPACITY.toInt())
    /** 累积经验值 × 10（用于 GUI 显示，整数部分+一位小数） */
    var experienceDisplay by schema.int("Experience")

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





