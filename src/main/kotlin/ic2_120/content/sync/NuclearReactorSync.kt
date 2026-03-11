package ic2_120.content.sync

import ic2_120.content.TickLimitedEnergyStorage
import ic2_120.content.block.ITieredMachine
import ic2_120.content.syncs.SyncSchema
import net.minecraft.util.math.Direction

/**
 * 核反应堆同步属性与能量容器。
 *
 * 电力等级 5，最大输出 8192 EU/t。
 * 能量缓冲容量较大，供反应堆持续发电输出。
 */
class NuclearReactorSync(
    schema: SyncSchema,
    private val getFacing: () -> Direction,
    private val currentTickProvider: () -> Long? = { null }
) : TickLimitedEnergyStorage(
    capacity = ENERGY_CAPACITY,
    maxInsertPerTick = 0L,
    maxExtract = MAX_EXTRACT,
    currentTickProvider = currentTickProvider
) {

    companion object {
        /** 电力等级 5 */
        const val REACTOR_TIER = 5
        /** 电力缓存容量 100000 EU */
        const val ENERGY_CAPACITY = 100_000L
        /** 整机每 tick 最大输出（8192 EU/t，tier 5） */
        val MAX_EXTRACT = ITieredMachine.euPerTickFromTier(REACTOR_TIER)
        const val NBT_ENERGY_STORED = "EnergyStored"
        const val NBT_HEAT_STORED = "HeatStored"
        /** 基础槽位（无反应仓时） */
        const val BASE_SLOTS = 27
        /** 每个相邻反应仓增加的槽位 */
        const val SLOTS_PER_CHAMBER = 9
        /** 热量/堆温上限 10000 */
        const val HEAT_CAPACITY = 10_000
        /** 堆温 > 4000 时方块着火 */
        const val HEAT_FIRE_THRESHOLD = 4_000
        /** 堆温 > 5000 时水蒸发 */
        const val HEAT_EVAPORATE_THRESHOLD = 5_000
        /** 堆温 > 7000 时生物受伤（防化服可挡） */
        const val HEAT_DAMAGE_THRESHOLD = 7_000
        /** 堆温 > 8500 时方块变岩浆 */
        const val HEAT_LAVA_THRESHOLD = 8_500
        /** 堆温 >= 10000 时爆炸 */
        const val HEAT_EXPLODE_THRESHOLD = 10_000
        /** 每 output 点对应的 EU 数（与 IC2 平衡，output 每脉冲 +1） */
        const val EU_PER_OUTPUT = 100
    }

    var energy by schema.int("Energy")
    /** 当前有效槽位数量（27 + 相邻反应仓数 * 9），用于 GUI 与槽位校验 */
    var capacity1 by schema.int("Capacity", default = BASE_SLOTS)
    /** 反应堆温度/贮存热量（0–10000），贮存的热量即堆温 */
    var temperature by schema.int("Temperature", default = 0)
    /** 发电速度（EU/t） */
    var outputRate by schema.int("OutputRate", default = 0)
    /** 总产热 */
    var totalHeatProduced by schema.int("TotalHeatProduced", default = 0)
    /** 总散热 */
    var totalHeatDissipated by schema.int("TotalHeatDissipated", default = 0)

    // override fun getSideMaxInsert(side: Direction?): Long = 0L
    // /** 正面不输出；其余面可输出 */
    // override fun getSideMaxExtract(side: Direction?): Long =
    //     if (side != getFacing()) MAX_EXTRACT else 0L

    // override fun onEnergyCommitted() {
    //     energy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
    // }
}
