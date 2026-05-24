package ic2_120.content.sync

import ic2_120.content.TickLimitedSidedEnergyContainer
import ic2_120.content.syncs.SyncSchema
import net.minecraft.util.math.Direction

/**
 * 创造模式发电机的同步属性与能量存储。
 * 无缓存, 直接输出 512 EU/t。
 */
class CreativeGeneratorSync(
    schema: SyncSchema,
    private val currentTickProvider: () -> Long? = { null }
) : TickLimitedSidedEnergyContainer(
    baseCapacity = ENERGY_CAPACITY,
    maxInsertPerTick = 0L,
    maxExtractPerTick = MAX_EXTRACT,
    currentTickProvider = currentTickProvider
) {

    companion object {
        /** 最小缓存容量 512 EU（仅够 1 tick），等效无缓存直出 */
        const val ENERGY_CAPACITY = 512L
        /** 每 tick 最大输出 512 EU/t */
        const val MAX_EXTRACT = 512L
        /** 每 tick 生成 512 EU */
        const val GENERATION_RATE = 512L
        const val NBT_ENERGY_STORED = "EnergyStored"
    }

    private val flow = EnergyFlowSync(schema, this, useGeneratedAsInput = true)

    override fun getSideMaxInsert(side: Direction?): Long = 0L

    /** 六面均可输出 */
    override fun getSideMaxExtract(side: Direction?): Long = MAX_EXTRACT

    fun syncCurrentTickFlow() {
        flow.syncCurrentTickFlow()
    }

    fun getSyncedInsertedAmount(): Long = flow.getSyncedInsertedAmount()
    fun getSyncedExtractedAmount(): Long = flow.getSyncedExtractedAmount()
}
