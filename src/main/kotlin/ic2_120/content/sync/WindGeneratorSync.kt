package ic2_120.content.sync

import ic2_120.content.TickLimitedSidedEnergyContainer
import ic2_120.content.syncs.SyncSchema
import net.minecraft.util.math.Direction

/**
 * 风力发电机同步属性与能量容器。
 *
 * 发电机制：
 * - 以高度为动力，风力强度 0~30 每 128 tick 刷新
 * - 发电量 p = w * s * (h - 64) / 750，h = y - c（有效高度）
 * - w：晴天 1.0，雨天 1.2，雷雨 1.5
 * - 极限约 11.46 EU/t
 */
class WindGeneratorSync(
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
        /** 电力缓存容量（EU） */
        const val ENERGY_CAPACITY = 400L
        /** 整机总输出上限（EU/t），理论极限约 11.46 */
        const val MAX_EXTRACT = 20L
        const val NBT_ENERGY_STORED = "EnergyStored"
    }

    var energy by schema.int("Energy")
    /** 是否正在发电（供 GUI 显示） */
    var isGenerating by schema.int("IsGenerating")
    /** 当前输出速率（EU/t），供 GUI 显示 */
    var outputRate by schema.int("OutputRate")

    override fun getSideMaxInsert(side: Direction?): Long = 0L

    override fun getSideMaxExtract(side: Direction?): Long =
        if (side != getFacing()) MAX_EXTRACT else 0L

    override fun onEnergyCommitted() {
        energy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }
}
