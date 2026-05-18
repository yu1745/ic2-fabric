package ic2_120.content.sync

import ic2_120.content.TickLimitedSidedEnergyContainer
import ic2_120.content.syncs.SyncSchema

/**
 * 冷凝器同步数据。
 *
 * 参考 ic2_origin 数值：
 * - EU 存储: 100,000 EU
 * - 蒸汽罐: 100,000 mB
 * - 蒸馏水罐: 1,000 mB
 * - 被动冷却: 100 mB/t
 * - 每个散热口: +100 mB/t, 消耗 2 EU/t
 * - 蒸馏水产率: 100 mB / 10000 进度
 */
class CondenserSync(
    schema: SyncSchema,
    private val getFacing: () -> net.minecraft.util.math.Direction,
    currentTickProvider: () -> Long? = { null }
) : TickLimitedSidedEnergyContainer(
    baseCapacity = ENERGY_CAPACITY,
    maxInsertPerTick = MAX_INSERT,
    maxExtractPerTick = MAX_EXTRACT,
    currentTickProvider = currentTickProvider
) {
    companion object {
        const val ENERGY_CAPACITY = 100_000L
        const val MAX_INSERT = 512L
        const val MAX_EXTRACT = 0L
        const val NBT_ENERGY_STORED = "EnergyStored"

        const val STEAM_TANK_CAPACITY = 100_000L
        const val WATER_TANK_CAPACITY = 1_000L

        /** 被动冷却速率 (mB/t) */
        const val PASSIVE_COOLING = 100
        /** 每个散热口的额外冷却 (mB/t) */
        const val COOLING_PER_VENT = 100
        /** 每个散热口的 EU 消耗 (EU/t) */
        const val EU_PER_VENT = 2
        /** 最大散热口数 */
        const val MAX_VENTS = 4
        /** 蒸馏水产出进度上限 */
        const val PROGRESS_MAX = 10_000
    }

    var energy by schema.int("Energy")
    var steamAmount by schema.int("SteamAmount")
    var waterAmount by schema.int("WaterAmount")
    var progress by schema.int("Progress")
    var ventCount by schema.int("VentCount")
    var coolingRate by schema.int("CoolingRate")

    private val flow = EnergyFlowSync(schema, this, useGeneratedAsInput = false)

    override fun onEnergyCommitted() {
        energy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    fun syncCurrentTickFlow() {
        flow.syncCurrentTickFlow()
    }

    fun getSyncedConsumedAmount(): Long = flow.getSyncedConsumedAmount()
}
