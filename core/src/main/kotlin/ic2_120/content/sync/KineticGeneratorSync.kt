package ic2_120.content.sync

import ic2_120.content.TickLimitedSidedEnergyContainer
import ic2_120.content.syncs.SyncSchema
import net.minecraft.util.math.Direction

/**
 * 动能发电机同步属性与能量容器。
 *
 * 机器从传动网络拉取 KU，并按 4KU=1EU 转换为 EU。
 */
class KineticGeneratorSync(
    schema: SyncSchema,
    private val getFacing: () -> Direction,
    private val currentTickProvider: () -> Long? = { null }
) : TickLimitedSidedEnergyContainer(
    baseCapacity = ENERGY_CAPACITY,
    maxInsertPerTick = 0L,
    maxExtractPerTick = MAX_EXTRACT_EU,
    currentTickProvider = currentTickProvider
) {

    companion object {
        const val ENERGY_CAPACITY = 10_000L
        const val MAX_EXTRACT_EU = 512L
        const val MAX_INPUT_KU = 2_048
        const val KU_PER_EU = 4
        const val NBT_ENERGY_STORED = "EnergyStored"
    }

    var energy by schema.int("Energy")
    var isGenerating by schema.int("IsGenerating")
    var currentKu by schema.int("CurrentKu")
    var outputEu by schema.int("OutputEu")
    private val flow = EnergyFlowSync(schema, this, useGeneratedAsInput = true)

    override fun getSideMaxInsert(side: Direction?): Long = 0L

    override fun getSideMaxExtract(side: Direction?): Long =
        if (side != getFacing()) MAX_EXTRACT_EU else 0L

    override fun onEnergyCommitted() {
        energy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    fun syncCurrentTickFlow() {
        flow.syncCurrentTickFlow()
    }
}
