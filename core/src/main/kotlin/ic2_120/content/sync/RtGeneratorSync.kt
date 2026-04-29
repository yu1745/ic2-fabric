package ic2_120.content.sync

import ic2_120.content.TickLimitedSidedEnergyContainer
import ic2_120.content.syncs.SyncSchema
import net.minecraft.util.math.Direction

/**
 * 放射性同位素温差发电机（RTG）同步属性与能量容器。
 *
 * 发电机制：
 * - 需放入放射性同位素燃料靶丸（rtg_pellet），靶丸无限耐久不消耗
 * - 发电量由靶丸数量决定：1→1, 2→2, 3→4, 4→8, 5→16, 6→32 EU/t
 */
class RtGeneratorSync(
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
        /** 电力缓存容量 20000 EU */
        const val ENERGY_CAPACITY = 20_000L
        /** 整机总输出（EU/t），6 颗靶丸时最大 32 EU/t */
        const val MAX_EXTRACT = 32L
        const val NBT_ENERGY_STORED = "EnergyStored"

        /** 根据靶丸数量计算发电量（EU/t）：1→1, 2→2, 3→4, 4→8, 5→16, 6→32 */
        fun euPerTickFromPelletCount(count: Int): Int =
            when (count.coerceIn(0, 6)) {
                0 -> 0
                1 -> 1
                2 -> 2
                3 -> 4
                4 -> 8
                5 -> 16
                6 -> 32
                else -> 0
            }
    }

    var energy by schema.int("Energy")
    private val flow = EnergyFlowSync(schema, this, useGeneratedAsInput = true)

    override fun getSideMaxInsert(side: Direction?): Long = 0L

    override fun getSideMaxExtract(side: Direction?): Long =
        if (side != getFacing()) MAX_EXTRACT else 0L

    override fun onEnergyCommitted() {
        energy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    /**
     * 在 tick 结束时调用，同步当前 tick 的实际输入/输出
     * 发电机不支持输入：输入字段用于存储发电速度
     */
    fun syncCurrentTickFlow() {
        flow.syncCurrentTickFlow()
    }

    /** 获取同步的滤波后输入量（EU/t，发电速度） */
    fun getSyncedInsertedAmount(): Long = flow.getSyncedInsertedAmount()

    /** 获取同步的滤波后输出量（EU/t） */
    fun getSyncedExtractedAmount(): Long = flow.getSyncedExtractedAmount()
}


