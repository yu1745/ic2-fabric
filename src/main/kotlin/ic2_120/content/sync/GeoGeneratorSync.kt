package ic2_120.content.sync

import ic2_120.content.TickLimitedSidedEnergyContainer
import ic2_120.content.block.GeoGeneratorBlock
import ic2_120.content.block.machines.GeoGeneratorBlockEntity
import ic2_120.content.syncs.SyncSchema
import net.minecraft.util.math.Direction

/**
 * 地热发电机同步属性与能量容器。
 */
class GeoGeneratorSync(
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
        const val ENERGY_CAPACITY = 10_000L
        /** 整机总输出（EU/t） */
        const val MAX_EXTRACT = 20L
        /** 每 tick 产能（EU） */
        const val EU_PER_BURN_TICK = 20L
        /** 1 桶岩浆燃烧时长（25s = 500 ticks） */
        const val BURN_TICKS_PER_BUCKET = 25 * 20
        const val NBT_ENERGY_STORED = "EnergyStored"
    }

    var energy by schema.int("Energy")
    var lavaAmountMb by schema.int("LavaAmountMb")
    private val flow = EnergyFlowSync(schema, this, useGeneratedAsInput = true)

    override fun getSideMaxInsert(side: Direction?): Long = 0L

    override fun getSideMaxExtract(side: Direction?): Long =
        if (side != getFacing()) MAX_EXTRACT else 0L

    override fun onEnergyCommitted() {
        energy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    fun syncCurrentTickFlow() {
        flow.syncCurrentTickFlow()
    }

    fun getSyncedInsertedAmount(): Long = flow.getSyncedInsertedAmount()

    fun getSyncedExtractedAmount(): Long = flow.getSyncedExtractedAmount()
}

