package ic2_120_advanced_solar_addon.content.sync

import ic2_120.content.TickLimitedSidedEnergyContainer
import ic2_120.content.energy.EnergyTier
import ic2_120.content.syncs.SyncSchema
import net.minecraft.util.math.Direction

class SolarPanelSync(
    schema: SyncSchema,
    capacity: Long,
    tier: Int,
    private val getFacing: () -> Direction,
    currentTickProvider: () -> Long?
) : TickLimitedSidedEnergyContainer(
    baseCapacity = capacity,
    maxInsertPerTick = 0L,
    maxExtractPerTick = EnergyTier.euPerTickFromTier(tier),
    currentTickProvider = currentTickProvider
) {
    companion object {
        const val NBT_ENERGY = "Energy"
    }

    private val minOutput = EnergyTier.euPerTickFromTier(tier)
    private val maxExtract = EnergyTier.euPerTickFromTier(tier)

    var energy by schema.int("Energy")
    var capacitySync by schema.int("Capacity")
    var generationState by schema.int("GenState")
    var isGenerating by schema.int("IsGenerating")
    var dayPower by schema.int("DayPower")
    var nightPower by schema.int("NightPower")
    var avgInserted by schema.intAveraged("AvgInserted")
    var avgExtracted by schema.intAveraged("AvgExtract")

    override fun getSideMaxInsert(side: Direction?): Long = 0L

    override fun getSideMaxExtract(side: Direction?): Long =
        if (amount >= minOutput) maxExtract else 0L

    override fun onEnergyCommitted() {
        energy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
        capacitySync = this@SolarPanelSync.capacity.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    fun syncCurrentTickFlow() {
        finalizeFlowSnapshot()
        avgInserted = getLastGeneratedAmount().toInt()
        avgExtracted = getLastExtractedAmount().toInt()
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
    maxExtractPerTick = EnergyTier.euPerTickFromTier(tier),
    currentTickProvider = currentTickProvider
) {
    companion object {
        const val NBT_ENERGY = "Energy"
    }

    var energy by schema.int("Energy")
    var production by schema.int("Production")
    var tierLevel by schema.int("Tier")
    var isActive by schema.int("IsActive")
    var avgInserted by schema.intAveraged("AvgInserted")
    var avgExtracted by schema.intAveraged("AvgExtract")

    private val maxExtract = EnergyTier.euPerTickFromTier(tier)

    override fun getSideMaxInsert(side: Direction?): Long = 0L

    override fun getSideMaxExtract(side: Direction?): Long =
        if (side != getFacing()) maxExtract else 0L

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
    currentTickProvider: () -> Long?
) : TickLimitedSidedEnergyContainer(
    baseCapacity = 10000000L,
    maxInsertPerTick = EnergyTier.euPerTickFromTier(tier),
    maxExtractPerTick = 0L,
    currentTickProvider = currentTickProvider
) {
    companion object {
        const val NBT_ENERGY = "Energy"
    }

    var energy by schema.int("Energy")
    var progress by schema.int("Progress")
    var requiredEnergy by schema.int("ReqEnergy")
    var avgInserted by schema.intAveraged("AvgInserted")
    var avgExtracted by schema.intAveraged("AvgExtract")
    var avgConsumed by schema.intAveraged("AvgConsume")

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
