package ic2_120.content.sync

import ic2_120.content.TickLimitedSidedEnergyContainer
import ic2_120.content.syncs.SyncSchema
import net.minecraft.util.math.Direction

/**
 * MFSU 的同步属性与能量存储。
 * 容量 40M EU；整机 8192 EU/t 输入（仅正面可接）、8192 EU/t 输出（除正面外可接），多面共享。
 */
class MfsuSync(
    schema: SyncSchema,
    private val getFacing: () -> Direction,
    private val currentTickProvider: () -> Long? = { null }
) : TickLimitedSidedEnergyContainer(
    capacity = ENERGY_CAPACITY,
    maxInsertPerTick = MAX_INSERT,
    maxExtractPerTick = MAX_EXTRACT,
    currentTickProvider = currentTickProvider
) {

    companion object {
        const val ENERGY_CAPACITY = 40_000_000L
        const val MAX_INSERT = 8192L
        const val MAX_EXTRACT = 8192L
        const val NBT_ENERGY_STORED = "EnergyStored"
    }

    var energy by schema.int("Energy")

    override fun getSideMaxInsert(side: Direction?): Long {
        if (side == null) return MAX_INSERT
        return if (side == getFacing()) MAX_INSERT else 0L
    }

    override fun getSideMaxExtract(side: Direction?): Long {
        if (side == null) return MAX_EXTRACT
        return if (side != getFacing()) MAX_EXTRACT else 0L
    }

    override fun onEnergyCommitted() {
        energy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }
}
