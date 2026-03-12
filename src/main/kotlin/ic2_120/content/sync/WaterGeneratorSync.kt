package ic2_120.content.sync

import ic2_120.content.TickLimitedSidedEnergyContainer
import ic2_120.content.syncs.SyncSchema
import net.minecraft.util.math.Direction

/**
 * 水力发电机同步属性与能量容器。
 *
 * 发电机制：
 * - 水桶：500 EU 总量，1 EU/t 速率
 * - 周围 3x3x3 水方块：每个水方块 +0.01 EU/t（常见水塔约 0.25 EU/t）
 */
class WaterGeneratorSync(
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
        const val ENERGY_CAPACITY = 10_000L
        /** 整机总输出（EU/t） */
        const val MAX_EXTRACT = 20L
        const val NBT_ENERGY_STORED = "EnergyStored"
        /** 水桶发电量（EU） */
        const val EU_PER_WATER_BUCKET = 500
        /** 水桶发电速率（EU/t） */
        const val EU_PER_TICK_FROM_BUCKET = 1
        /** 1 桶水燃烧时长（500 ticks = 25s） */
        const val BURN_TICKS_PER_BUCKET = 25 * 20
        /** 水罐每 tick 产能（EU） */
        const val EU_PER_BURN_TICK = 1L
        /** 周围每个水方块增加的 EU/t（0.01，用百分之一表示） */
        const val EU_PER_TICK_PER_WATER_BLOCK_CENT = 1  // 1/100 = 0.01 EU/t
    }

    var energy by schema.int("Energy")
    /** 水储量（mB），供 GUI 显示 */
    var waterAmountMb by schema.int("WaterAmountMb")
    private val flow = EnergyFlowSync(schema, this, useGeneratedAsInput = true)

    override fun getSideMaxInsert(side: Direction?): Long = 0L

    override fun getSideMaxExtract(side: Direction?): Long =
        if (side != getFacing()) MAX_EXTRACT else 0L

    override fun onEnergyCommitted() {
        energy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    fun syncCurrentTickFlow() {
        flow.syncCurrentTickFlow()
    }

    fun getSyncedInsertedAmount(): Long = flow.getSyncedInsertedAmount()

    fun getSyncedExtractedAmount(): Long = flow.getSyncedExtractedAmount()
}

