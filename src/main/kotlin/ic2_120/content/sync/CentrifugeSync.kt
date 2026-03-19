package ic2_120.content.sync

import ic2_120.content.UpgradeableTickLimitedSidedEnergyContainer
import ic2_120.content.sync.EnergyFlowSync
import ic2_120.content.syncs.SyncSchema

/**
 * 热能离心机同步数据。
 *
 * 规格（来自 IC2 文档）：
 * - 电压：48 EU/t 最低（Tier 2/MV），无升级时最大 128 EU/t
 * - 加工耗能：48 EU/t
 * - 加热耗能：1 EU/t（不受超频影响）
 * - 总耗能：49 EU/t
 * - 加工时间：25 秒 = 500 ticks
 * - 每周期总能量：24,500 EU
 * - 热量最大值：5000
 * - 加热速率：1 EU/t
 */
class CentrifugeSync(
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
        const val CENTRIFUGE_TIER = 2
        const val ENERGY_CAPACITY = 10_000L  // 约 200 秒缓冲
        const val MAX_INSERT = 128L
        const val MAX_EXTRACT = 0L
        const val NBT_ENERGY_STORED = "EnergyStored"
        const val PROGRESS_MAX = 500  // 25 秒
        const val ENERGY_PER_TICK_PROCESSING = 48L
        const val ENERGY_PER_TICK_HEATING = 1L
        const val HEAT_MAX = 5000
        const val HEAT_RATE_PER_TICK = 1  // 加热速率，不受超频影响
    }

    var energy by schema.int("Energy")
    var progress by schema.int("Progress")
    var energyCapacity by schema.int("EnergyCapacity", default = ENERGY_CAPACITY.toInt())
    var heat by schema.int("Heat", default = 0)

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
