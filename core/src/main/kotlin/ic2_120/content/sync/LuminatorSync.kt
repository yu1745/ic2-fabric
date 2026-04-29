package ic2_120.content.sync

import ic2_120.content.TickLimitedSidedEnergyContainer
import ic2_120.content.sync.EnergyFlowSync
import ic2_120.content.energy.EnergyTier
import ic2_120.content.syncs.SyncSchema

/**
 * 日光灯的能量缓冲。无容量、不可充电，仅从电缆取电，断电即关。
 * 电压等级 5（不限制电压），5 EU/s 消耗（每 4 tick 消耗 1 EU）。
 * 最小缓冲 1 EU，确保断电后 4 tick 内熄灭。
 */
class LuminatorSync(
    schema: SyncSchema,
    currentTickProvider: () -> Long? = { null }
) : TickLimitedSidedEnergyContainer(ENERGY_CAPACITY, MAX_INSERT, MAX_EXTRACT, currentTickProvider) {

    companion object {
        /** 最小缓冲，无储能，断电即关 */
        const val ENERGY_CAPACITY = 1L
        /** 电压等级 5：8192 EU/t 输入上限 */
        val MAX_INSERT: Long = EnergyTier.euPerTickFromTier(5)
        const val MAX_EXTRACT = 0L
        const val NBT_ENERGY_STORED = "EnergyStored"
        /** 每 4 tick 消耗 1 EU，即 5 EU/s */
        const val ENERGY_PER_CYCLE = 1L
        const val CYCLE_TICKS = 4
    }

    var energy by schema.int("Energy")

    private val flow = EnergyFlowSync(schema, this)

    override fun onEnergyCommitted() {
        energy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    fun syncCurrentTickFlow() {
        flow.syncCurrentTickFlow()
    }

    fun getSyncedInsertedAmount(): Long = flow.getSyncedInsertedAmount()

    fun getSyncedExtractedAmount(): Long = flow.getSyncedExtractedAmount()
}




