package ic2_120.content.sync

import ic2_120.content.TickLimitedSidedEnergyContainer
import ic2_120.content.syncs.SyncSchema
import net.minecraft.util.math.Direction

/**
 * 火力发电机的同步属性与能量存储。
 * 燃烧燃料产生 EU，可被相邻方块（电缆等）提取。
 */
class GeneratorSync(
    schema: SyncSchema,
    private val getFacing: () -> Direction,
    private val currentTickProvider: () -> Long? = { null }
) : TickLimitedSidedEnergyContainer(
    capacity = ENERGY_CAPACITY,
    maxInsertPerTick = 0L,
    maxExtractPerTick = MAX_EXTRACT,
    currentTickProvider = currentTickProvider
) {

    companion object {
        /** 电力缓存容量 4000 EU（= 1 煤，与 IC2 Experimental 一致） */
        const val ENERGY_CAPACITY = 4_000L
        /** 整机每 tick 最大输出 10 EU/t（多面共享，与 IC2 Experimental 一致） */
        const val MAX_EXTRACT = 10L
        const val NBT_ENERGY_STORED = "EnergyStored"
        /** 燃料燃烧进度条最大值 */
        const val BURN_TIME_MAX = 100
        /** 每 tick 燃烧进度消耗（用于 GUI 显示） */
        const val BURN_PROGRESS_PER_TICK = 1
        /** 每 tick 产生 EU（与 IC2 Experimental 一致：10 EU/t，1 煤 400 tick → 4000 EU） */
        const val EU_PER_BURN_TICK = 10.0
        /** 相对原版熔炉燃烧时间的除数，使 1 煤 = 400 tick（1600/4） */
        const val BURN_TICKS_DIVISOR = 4
    }

    var energy by schema.int("Energy")
    /** 当前燃料剩余燃烧时间（tick） */
    var burnTime by schema.int("BurnTime")
    /** 当前燃料总燃烧时间（tick），用于 GUI 进度条 */
    var totalBurnTime by schema.int("TotalBurnTime")

    override fun getSideMaxInsert(side: Direction?): Long = 0L
    /** 正面不输出；其余面可输出，整机总输出由基类限制为 MAX_EXTRACT/tick（多面共享）。 */
    override fun getSideMaxExtract(side: Direction?): Long =
        if (side != getFacing()) MAX_EXTRACT else 0L

    override fun onEnergyCommitted() {
        energy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }
}
