package ic2_120.content.sync

import ic2_120.content.TickLimitedSidedEnergyContainer
import ic2_120.content.syncs.SyncSchema

/**
 * 区块加载器的同步属性与能量存储。
 * 耗能：1 EU/tick 每区块，范围 1～25 区块可配置。
 */
class ChunkLoaderSync(
    schema: SyncSchema,
    currentTickProvider: () -> Long? = { null }
) : TickLimitedSidedEnergyContainer(ENERGY_CAPACITY, MAX_INSERT, MAX_EXTRACT, currentTickProvider) {

    companion object {
        const val ENERGY_CAPACITY = 10_000L  // 约 8 分钟满负载 (25 chunks)
        const val MAX_INSERT = 32L
        const val MAX_EXTRACT = 0L
        const val NBT_ENERGY_STORED = "EnergyStored"
        /** 每区块每 tick 耗能 (EU) */
        const val EU_PER_CHUNK_PER_TICK = 1L
        /** 范围选项对应的半径：0=1区块, 1=9区块, 2=25区块 */
        val RADIUS_TO_CHUNK_COUNT = intArrayOf(1, 9, 25)
    }

    var energy by schema.int("Energy")
    /** 加载范围：0=1区块, 1=9区块, 2=25区块 */
    var range by schema.int("Range", default = 2)
    var energyCapacity by schema.int("EnergyCapacity", default = ENERGY_CAPACITY.toInt())

    private val flow = EnergyFlowSync(schema, this)

    override fun onEnergyCommitted() {
        energy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    fun syncCurrentTickFlow() {
        flow.syncCurrentTickFlow()
    }

    fun getSyncedInsertedAmount(): Long = flow.getSyncedInsertedAmount()
    fun getSyncedConsumedAmount(): Long = flow.getSyncedConsumedAmount()

    /** 当前范围对应的区块数 */
    fun getChunkCount(): Int = RADIUS_TO_CHUNK_COUNT.getOrElse(range.coerceIn(0, 2)) { 25 }

    /** 当前范围对应的 ticket 半径 (0, 1, 2) */
    fun getTicketRadius(): Int = range.coerceIn(0, 2)
}
