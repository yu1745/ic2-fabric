package ic2_120.content.sync

import ic2_120.content.TickLimitedSidedEnergyContainer
import ic2_120.content.syncs.SyncSchema
import net.minecraft.util.math.Direction

/**
 * 太阳能发电机同步属性与能量容器。
 *
 * 发电机制：
 * - 仅主世界、白天（6:20~17:45）、无遮挡、无雨雪时发电
 * - 1 EU/t 输出，平均每天约 13050 EU
 */
class SolarGeneratorSync(
    schema: SyncSchema,
    private val getFacing: () -> Direction,
    private val currentTickProvider: () -> Long? = { null }
) : TickLimitedSidedEnergyContainer(
    baseCapacity = ENERGY_CAPACITY,
    maxInsertPerTick = 0L,
    maxExtractPerTick = MAX_EXTRACT,
    currentTickProvider = currentTickProvider
) {

    companion object {
        /** 电力缓存容量（EU） */
        const val ENERGY_CAPACITY = 400L
        /** 整机总输出（EU/t），1 EU/t */
        const val MAX_EXTRACT = 1L
        const val NBT_ENERGY_STORED = "EnergyStored"
        /** 每 tick 产生 EU */
        const val EU_PER_TICK = 1L
        /** 最小输出能量（EU），积累到此值后才输出，减少线损 */
        const val MIN_OUTPUT_ENERGY = 32L  // LV 等级单次输出量
    }

    var energy by schema.int("Energy")
    /** 是否正在发电（供 GUI 显示半圆球体颜色） */
    var isGenerating by schema.int("IsGenerating")
    private val flow = EnergyFlowSync(schema, this, useGeneratedAsInput = true)

    override fun getSideMaxInsert(side: Direction?): Long = 0L

    override fun getSideMaxExtract(side: Direction?): Long =
        if (side != getFacing()) {
            // 只有积累到最小输出能量后才输出，减少线损
            if (amount >= MIN_OUTPUT_ENERGY) MAX_EXTRACT else 0L
        } else 0L

    override fun onEnergyCommitted() {
        energy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    fun syncCurrentTickFlow() {
        flow.syncCurrentTickFlow()
    }

    fun getSyncedInsertedAmount(): Long = flow.getSyncedInsertedAmount()

    fun getSyncedExtractedAmount(): Long = flow.getSyncedExtractedAmount()
}

