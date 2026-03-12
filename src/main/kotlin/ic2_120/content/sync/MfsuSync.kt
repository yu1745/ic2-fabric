package ic2_120.content.sync

import ic2_120.content.TickLimitedSidedEnergyContainer
import ic2_120.content.block.ITieredMachine
import ic2_120.content.syncs.SyncSchema
import net.minecraft.util.math.Direction

/**
 * MFSU 的同步属性与能量存储。
 * 容量 40M EU；输入/输出由 [ITieredMachine.euPerTickFromTier] 根据电压等级计算（MFSU 为等级 4）。
 * 整机输入仅正面可接、输出除正面外可接，多面共享。
 */
class MfsuSync(
    schema: SyncSchema,
    private val getFacing: () -> Direction,
    private val currentTickProvider: () -> Long? = { null },
    tier: Int = MFSU_TIER
) : TickLimitedSidedEnergyContainer(
    baseCapacity = ENERGY_CAPACITY,
    maxInsertPerTick = ITieredMachine.euPerTickFromTier(tier),
    maxExtractPerTick = ITieredMachine.euPerTickFromTier(tier),
    currentTickProvider = currentTickProvider
) {

    companion object {
        const val ENERGY_CAPACITY = 40_000_000L
        const val MFSU_TIER = 4
        const val NBT_ENERGY_STORED = "EnergyStored"
    }

    private val maxRate = ITieredMachine.euPerTickFromTier(tier)

    var energy by schema.int("Energy")
    private val flow = EnergyFlowSync(schema, this)

    override fun getSideMaxInsert(side: Direction?): Long {
        if (side == null) return maxRate
        return if (side == getFacing()) maxRate else 0L
    }

    override fun getSideMaxExtract(side: Direction?): Long {
        if (side == null) return maxRate
        return if (side != getFacing()) maxRate else 0L
    }

    override fun onEnergyCommitted() {
        energy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    /**
     * 在 tick 结束时调用，同步当前 tick 的实际输入/输出
     */
    fun syncCurrentTickFlow() {
        flow.syncCurrentTickFlow()
    }

    /** 获取同步的滤波后输入量（EU/t） */
    fun getSyncedInsertedAmount(): Long = flow.getSyncedInsertedAmount()

    /** 获取同步的滤波后输出量（EU/t） */
    fun getSyncedExtractedAmount(): Long = flow.getSyncedExtractedAmount()
}

