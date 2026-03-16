package ic2_120.content.block

import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

/** 1 MC 天 = 24000 tick */
private const val RECOVERY_TICKS = 24000L

/**
 * 橡胶原木方块实体。记录各面提取时间，1 MC 天后将 DRY 恢复为 WET。
 */
@ModBlockEntity(block = RubberLogBlock::class)
class RubberLogBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state) {

    /** 各面提取时的世界时间，0 表示未提取或已恢复 */
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
            markDirty()
        }
    }

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        for (i in 0..3) {
            extractedAt[i] = nbt.getLong("extracted_$i")
        }
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        for (i in 0..3) {
            nbt.putLong("extracted_$i", extractedAt[i])
        }
    }

    companion object {
        fun tick(world: World, pos: BlockPos, state: BlockState, be: RubberLogBlockEntity) {
            if (world.isClient) return
            val block = state.block
            if (block !is RubberLogBlock) return

            // Fallback：当 onBlockAdded 未被调用时（世界生成、创造放置）在首次 tick 时初始化橡胶孔
            if (state.get(RubberLogBlock.RUBBER_NORTH) == RubberFaceState.NONE &&
                state.get(RubberLogBlock.RUBBER_SOUTH) == RubberFaceState.NONE &&
                state.get(RubberLogBlock.RUBBER_EAST) == RubberFaceState.NONE &&
                state.get(RubberLogBlock.RUBBER_WEST) == RubberFaceState.NONE
            ) {
                val faces = listOf(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)
                val r = world.random.nextInt(14)
                val count = when {
                    r < 11 -> 0
                    r < 13 -> 1
                    else -> 2
                }
                val indices = (0..3).toMutableList()
                for (i in 0 until count) {
                    val j = i + world.random.nextInt(4 - i)
                    indices[i] = indices[j].also { indices[j] = indices[i] }
                }
                var newState = state
                for (i in 0 until count) {
                    newState = newState.with(RubberLogBlock.propFor(faces[indices[i]]), RubberFaceState.WET)
                }
                world.setBlockState(pos, newState)
                return
            }

            var newState = state
            var changed = false
            for (i in 0..3) {
                val face = faceOf(i)
                if (be.extractedAt[i] == 0L) continue
                if (block.getRubberState(state, face) != RubberFaceState.DRY) continue
                if (world.time - be.extractedAt[i] >= RECOVERY_TICKS) {
                    newState = newState.with(RubberLogBlock.propFor(face), RubberFaceState.WET)
                    be.extractedAt[i] = 0L
                    changed = true
                }
            }
            if (changed) {
                world.setBlockState(pos, newState)
                be.markDirty()
            }
        }

        private fun indexOf(face: Direction): Int = when (face) {
            Direction.NORTH -> 0
            Direction.SOUTH -> 1
            Direction.EAST -> 2
            Direction.WEST -> 3
            else -> -1
        }

        private fun faceOf(i: Int): Direction = when (i) {
            0 -> Direction.NORTH
            1 -> Direction.SOUTH
            2 -> Direction.EAST
            3 -> Direction.WEST
            else -> Direction.NORTH
        }
    }
}
