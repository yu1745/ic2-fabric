package ic2_120.content.sync

import ic2_120.content.TickLimitedSidedEnergyContainer
import ic2_120.content.syncs.SyncSchema
import net.minecraft.util.math.Direction

/**
 * 电力加热机能量容器。
 * Tier 4，1w EU 缓存，只进不出。
 */
class ElectricHeatGeneratorSync(
    schema: SyncSchema,
    private val currentTickProvider: () -> Long? = { null },
    private val heatFlow: HeatFlowSync? = null
) : TickLimitedSidedEnergyContainer(
    baseCapacity = ENERGY_CAPACITY,
    maxInsertPerTick = MAX_INSERT,
    maxExtractPerTick = 0L,
    currentTickProvider = currentTickProvider
) {
    companion object {
        const val ENERGY_CAPACITY = 10_000L
        const val MAX_INSERT = 2048L
        const val NBT_ENERGY_STORED = "EnergyStored"
    }

    var energy by schema.int("Energy")
    private val flow = EnergyFlowSync(schema, this)

    override fun getSideMaxInsert(side: Direction?): Long = MAX_INSERT
    override fun getSideMaxExtract(side: Direction?): Long = 0L

    override fun onEnergyCommitted() {
        energy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    fun syncCurrentTickFlow() {
        flow.syncCurrentTickFlow()
    }

    /** 获取同步的滤波后产热速率（HU/t） */
    fun getSyncedGeneratedHeat(): Long = heatFlow?.getSyncedGeneratedHeat() ?: 0L

    /** 获取同步的滤波后输出速率（HU/t） */
    fun getSyncedOutputHeat(): Long = heatFlow?.getSyncedOutputHeat() ?: 0L
}

