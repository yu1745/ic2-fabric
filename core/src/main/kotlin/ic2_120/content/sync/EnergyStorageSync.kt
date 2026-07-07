package ic2_120.content.sync

import ic2_120.content.TickLimitedSidedEnergyContainer
import ic2_120.content.energy.EnergyTier
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
    maxInsertPerTick = EnergyTier.euPerTickFromTier(tier),
    maxExtractPerTick = EnergyTier.euPerTickFromTier(tier),
    currentTickProvider = currentTickProvider
) {

   companion object {
       const val NBT_ENERGY_STORED = "EnergyStored"
       const val NBT_CHARGE_MODE = "ChargeMode"
       const val MODE_CHARGE = 0
       const val MODE_DISCHARGE = 1
   }

    private val maxRate = EnergyTier.euPerTickFromTier(tier)

   var energy by schema.int("Energy")
   private val flow = EnergyFlowSync(schema, this)
   /** 0 = 充电（机器→电池），1 = 放电（电池→机器） */
   var chargeMode by schema.int("ChargeMode")

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
