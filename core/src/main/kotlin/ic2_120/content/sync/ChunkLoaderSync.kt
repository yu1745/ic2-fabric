package ic2_120.content.sync

import ic2_120.content.TickLimitedSidedEnergyContainer
import ic2_120.content.syncs.SyncSchema
import net.minecraft.util.math.ChunkPos

/**
 * 区块加载器的同步属性与能量存储。
 * 耗能：1 EU/tick 每区块，使用 25 位 bitmask 逐区块控制加载。
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
        /** 默认 bitmask：25 个区块全选 */
        const val DEFAULT_BITMASK = (1 shl 25) - 1
        /** 网格尺寸 5×5 */
        const val GRID_SIZE = 5
        /** 总区块数 */
        const val CHUNK_COUNT = GRID_SIZE * GRID_SIZE
    }

    var energy by schema.int("Energy")
    /** 区块加载 bitmask，每位代表一个区块 (bit index = row * 5 + col) */
    var chunkBitmask by schema.int("ChunkBitmask", default = DEFAULT_BITMASK)
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

    /** 当前 bitmask 中已选中的区块数 */
    fun getChunkCount(): Int = chunkBitmask.countOneBits()

    /** 根据 bitmask 和中心区块位置，返回所有需要加载的区块坐标 */
    fun getChunkPositions(center: ChunkPos): List<ChunkPos> {
        val positions = mutableListOf<ChunkPos>()
        for (i in 0 until CHUNK_COUNT) {
            if ((chunkBitmask and (1 shl i)) != 0) {
                val row = i / GRID_SIZE
                val col = i % GRID_SIZE
                positions.add(ChunkPos(center.x + col - 2, center.z + row - 2))
            }
        }
        return positions
    }

    /** 检查指定索引的区块是否选中 */
    fun isChunkEnabled(index: Int): Boolean =
        index in 0 until CHUNK_COUNT && (chunkBitmask and (1 shl index)) != 0
}
