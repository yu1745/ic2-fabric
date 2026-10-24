package ic2_120_advanced_solar_addon.content.sync

import ic2_120.content.TickLimitedSidedEnergyContainer
import ic2_120.content.energy.EnergyTier
import ic2_120.content.syncs.SyncSchema
import net.minecraft.util.math.Direction

class SolarPanelSync(
    schema: SyncSchema,
    capacity: Long,
    /** 每 tick 最大输出 EU/t（默认由 SolarPanelBlockEntity 按 tier 传入，变体可覆盖）。 */
    maxOutputPerTick: Long,
    private val getFacing: () -> Direction,
    currentTickProvider: () -> Long?
) : TickLimitedSidedEnergyContainer(
    baseCapacity = capacity,
    maxInsertPerTick = 0L,
    maxExtractPerTick = maxOutputPerTick,
    currentTickProvider = currentTickProvider
) {
    companion object {
        const val NBT_ENERGY = "Energy"
    }

    private val minOutput = maxOutputPerTick
    private val maxExtract = maxOutputPerTick

    var energy by schema.int("Energy")
    var capacitySync by schema.int("Capacity")
    var generationState by schema.int("GenState")
    var isGenerating by schema.int("IsGenerating")
    var dayPower by schema.int("DayPower")
    var nightPower by schema.int("NightPower")
    var maxOutput by schema.int("MaxOutput")
    var avgInserted by schema.intAveraged("AvgInserted")
    var avgExtracted by schema.intAveraged("AvgExtract")

    override fun getSideMaxInsert(side: Direction?): Long = 0L

    override fun getSideMaxExtract(side: Direction?): Long =
        if (amount >= minOutput) maxExtract else 0L

    override fun onEnergyCommitted() {
        val currentEnergy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
        if (energy != currentEnergy) energy = currentEnergy

        val currentCapacity = capacity.toInt().coerceIn(0, Int.MAX_VALUE)
        if (capacitySync != currentCapacity) capacitySync = currentCapacity
    }

    fun syncCurrentTickFlow() {
        finalizeFlowSnapshot()
        val generated = getLastGeneratedAmount().toInt()
        if (avgInserted != generated) avgInserted = generated

        val extracted = getLastExtractedAmount().toInt()
        if (avgExtracted != extracted) avgExtracted = extracted
    }
}

class QuantumGeneratorSync(
    schema: SyncSchema,
    tier: Int,
    private val getFacing: () -> Direction,
    currentTickProvider: () -> Long?
) : TickLimitedSidedEnergyContainer(
    baseCapacity = 1000000L,
    maxInsertPerTick = 0L,
    maxExtractPerTick = Long.MAX_VALUE,
    currentTickProvider = currentTickProvider
) {
    companion object {
        const val NBT_ENERGY = "Energy"
    }

    var energy by schema.int("Energy")
    var production by schema.int("Production")
    var tierLevel by schema.int("Tier")
    var isActive by schema.int("IsActive")
    var energyMac by schema.int("EnergyMac")
    var variable by schema.int("Variable")
    var avgInserted by schema.intAveraged("AvgInserted")
    var avgExtracted by schema.intAveraged("AvgExtract")

    override fun getSideMaxInsert(side: Direction?): Long = 0L

    override fun getSideMaxExtract(side: Direction?): Long =
        if (side != getFacing()) energyMac.toLong() else 0L

    override fun onEnergyCommitted() {
        energy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    fun syncCurrentTickFlow() {
        finalizeFlowSnapshot()
        avgInserted = getLastGeneratedAmount().toInt()
        avgExtracted = getLastExtractedAmount().toInt()
    }
}

class MolecularTransformerSync(
    schema: SyncSchema,
    tier: Int,
    private val getFacing: () -> Direction,
    currentTickProvider: () -> Long?,
    private val canAcceptEnergy: () -> Boolean,
    private val getRemainingEnergyNeeded: () -> Long
) : TickLimitedSidedEnergyContainer(
    baseCapacity = Long.MAX_VALUE,
    maxInsertPerTick = EnergyTier.euPerTickFromTier(tier),
    maxExtractPerTick = 0L,
    currentTickProvider = currentTickProvider
) {
    companion object {
        const val NBT_ENERGY = "Energy"
    }

    private val tierMaxInsert: Long = EnergyTier.euPerTickFromTier(tier)

    var energy by schema.int("Energy")
    var progress by schema.int("Progress")
    var requiredEnergy by schema.int("ReqEnergy")
    var inputItemId by schema.int("InputItemId")
    var outputItemId by schema.int("OutputItemId")
    var avgInserted by schema.intAveraged("AvgInserted")
    var avgExtracted by schema.intAveraged("AvgExtract")
    var avgConsumed by schema.intAveraged("AvgConsume")

    override fun getSideMaxInsert(side: Direction?): Long {
        if (!canAcceptEnergy()) return 0L
        val remaining = getRemainingEnergyNeeded()
        if (remaining <= 0) return 0L
        return minOf(tierMaxInsert, remaining)
    }

    override fun getSideMaxExtract(side: Direction?): Long = 0L

    override fun onEnergyCommitted() {
        energy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    fun syncCurrentTickFlow() {
        finalizeFlowSnapshot()
        avgInserted = getLastInsertedAmount().toInt()
        avgExtracted = getLastExtractedAmount().toInt()
        avgConsumed = getLastConsumedAmount().toInt()
    }
}
