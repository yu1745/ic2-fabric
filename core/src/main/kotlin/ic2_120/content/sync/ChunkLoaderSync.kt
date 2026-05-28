package ic2_120.content.sync

import ic2_120.content.TickLimitedSidedEnergyContainer
import ic2_120.content.syncs.SyncSchema
import net.minecraft.util.math.ChunkPos

class ChunkLoaderSync(
    schema: SyncSchema,
    currentTickProvider: () -> Long? = { null }
) : TickLimitedSidedEnergyContainer(ENERGY_CAPACITY, MAX_INSERT, MAX_EXTRACT, currentTickProvider) {

    companion object {
        const val ENERGY_CAPACITY = 10_000L
        const val MAX_INSERT = 32L
        const val MAX_EXTRACT = 0L
        const val NBT_ENERGY_STORED = "EnergyStored"
        const val EU_PER_CHUNK_PER_TICK = 1L
        const val GRID_SIZE = 9
        const val CHUNK_COUNT = GRID_SIZE * GRID_SIZE
        const val CENTER_INDEX = GRID_SIZE / 2 * GRID_SIZE + GRID_SIZE / 2
        const val MAX_LOADED_CHUNKS = 25

        private const val DEFAULT_BITMASK_0 = 0
        private const val DEFAULT_BITMASK_1 = 1 shl (CENTER_INDEX - 32)
        private const val DEFAULT_BITMASK_2 = 0
    }

    var energy by schema.int("Energy")
    var chunkBitmask0 by schema.int("ChunkBitmask0", default = DEFAULT_BITMASK_0)
    var chunkBitmask1 by schema.int("ChunkBitmask1", default = DEFAULT_BITMASK_1)
    var chunkBitmask2 by schema.int("ChunkBitmask2", default = DEFAULT_BITMASK_2)
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

    fun getChunkCount(): Int = countChunkBits()

    fun getChunkPositions(center: ChunkPos): List<ChunkPos> {
        val positions = mutableListOf<ChunkPos>()
        val half = GRID_SIZE / 2
        for (i in 0 until CHUNK_COUNT) {
            if (isChunkEnabled(i)) {
                val row = i / GRID_SIZE
                val col = i % GRID_SIZE
                positions.add(ChunkPos(center.x + col - half, center.z + row - half))
            }
        }
        return positions
    }

    fun isChunkEnabled(index: Int): Boolean {
        if (index !in 0 until CHUNK_COUNT) return false
        return when {
            index < 32 -> (chunkBitmask0 and (1 shl index)) != 0
            index < 64 -> (chunkBitmask1 and (1 shl (index - 32))) != 0
            else -> (chunkBitmask2 and (1 shl (index - 64))) != 0
        }
    }

    fun toggleChunkBit(index: Int) {
        if (index !in 0 until CHUNK_COUNT) return
        val mask = 1 shl when {
            index < 32 -> index
            index < 64 -> index - 32
            else -> index - 64
        }
        when {
            index < 32 -> chunkBitmask0 = chunkBitmask0 xor mask
            index < 64 -> chunkBitmask1 = chunkBitmask1 xor mask
            else -> chunkBitmask2 = chunkBitmask2 xor mask
        }
    }

    private fun countChunkBits(): Int {
        var count = 0
        var v = chunkBitmask0; while (v != 0) { count++; v = v and (v - 1) }
        v = chunkBitmask1; while (v != 0) { count++; v = v and (v - 1) }
        v = chunkBitmask2; while (v != 0) { count++; v = v and (v - 1) }
        return count
    }
}
