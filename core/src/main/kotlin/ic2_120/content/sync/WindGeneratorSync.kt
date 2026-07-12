package ic2_120.content.sync

import ic2_120.content.TickLimitedSidedEnergyContainer
import ic2_120.content.syncs.SyncSchema
import net.minecraft.util.math.Direction

/**
 * 风力发电机同步属性与能量容器。
 *
 * 发电机制：方块 Y > 74 时固定发电 3 EU/t，否则不发电。
 */
class WindGeneratorSync(
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
        /** 整机总输出上限（EU/t） */
        const val MAX_EXTRACT = 20L
        const val NBT_ENERGY_STORED = "EnergyStored"
        /** 最小输出能量（EU），积累到此值后才输出，与 MAX_EXTRACT 相等以保证完整周期清零 */
        const val MIN_OUTPUT_ENERGY = MAX_EXTRACT
    }

    var energy by schema.int("Energy")
    /** 是否正在发电（供 GUI 显示） */
    var isGenerating by schema.int("IsGenerating")
    /** 发电状态：0=工作中，1=高度不足 */
    var status by schema.int("Status")
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
