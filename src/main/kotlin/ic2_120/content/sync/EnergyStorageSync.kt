package ic2_120.content.sync

import ic2_120.content.TickLimitedSidedEnergyContainer
import ic2_120.content.block.ITieredMachine
import ic2_120.content.syncs.SyncSchema
import net.minecraft.util.math.Direction

/**
 * 储电盒的同步属性与能量存储。
 * 四个等级（BatBox/CESU/MFE/MFSU）共用此类，通过 tier 和 capacity 参数区分。
 * 整机：除正面外五面可输入，仅正面可输出。
 */
class EnergyStorageSync(
    schema: SyncSchema,
    private val getFacing: () -> Direction,
    private val currentTickProvider: () -> Long? = { null },
    tier: Int,
    capacity: Long
) : TickLimitedSidedEnergyContainer(
    baseCapacity = capacity,
    maxInsertPerTick = ITieredMachine.euPerTickFromTier(tier),
    maxExtractPerTick = ITieredMachine.euPerTickFromTier(tier),
    currentTickProvider = currentTickProvider
) {

    companion object {
        const val NBT_ENERGY_STORED = "EnergyStored"
    }

    private val maxRate = ITieredMachine.euPerTickFromTier(tier)

    var energy by schema.int("Energy")
    private val flow = EnergyFlowSync(schema, this)

    override fun getSideMaxInsert(side: Direction?): Long {
        if (side == null) return maxRate
        return if (side != getFacing()) maxRate else 0L
    }

    override fun getSideMaxExtract(side: Direction?): Long {
        if (side == null) return maxRate
        return if (side == getFacing()) maxRate else 0L
    }

    override fun onEnergyCommitted() {
        energy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    fun syncCurrentTickFlow() {
        flow.syncCurrentTickFlow()
    }

    fun getSyncedInsertedAmount(): Long = flow.getSyncedInsertedAmount()
    fun getSyncedExtractedAmount(): Long = flow.getSyncedExtractedAmount()
}
