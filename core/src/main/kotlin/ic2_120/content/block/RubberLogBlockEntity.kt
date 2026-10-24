package ic2_120.content.block

import net.minecraft.core.HolderLookup
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.nbt.CompoundTag
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

/**
 * 橡胶原木方块实体。仅记录各面提取时间戳供 Jade 显示，不参与恢复逻辑
 * （恢复由 [RubberLogBlock.randomTick] 1/7 概率完成）。
 */
@ModBlockEntity(block = RubberLogBlock::class)
class RubberLogBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state) {

    private val extractedAt = LongArray(4)

    constructor(pos: BlockPos, state: BlockState) : this(
        RubberLogBlockEntity::class.type(),
        pos,
        state
    )

    fun setExtractedTime(face: Direction, worldTime: Long) {
        val i = indexOf(face)
        if (i >= 0) {
            extractedAt[i] = worldTime
            setChanged()
        }
    }

    fun getExtractedAt(face: Direction): Long {
        val i = indexOf(face)
        return if (i >= 0) extractedAt[i] else 0L
    }

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        for (i in 0..3) {
            extractedAt[i] = nbt.getLong("extracted_$i")
        }
    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        for (i in 0..3) {
            nbt.putLong("extracted_$i", extractedAt[i])
        }
    }

    companion object {
        private fun indexOf(face: Direction): Int = when (face) {
            Direction.NORTH -> 0
            Direction.SOUTH -> 1
            Direction.EAST -> 2
            Direction.WEST -> 3
            else -> -1
        }
    }
}
