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
import java.util.ArrayDeque
import net.minecraft.registry.RegistryWrapper

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

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        for (i in 0..3) {
            extractedAt[i] = nbt.getLong("extracted_$i")
        }
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        for (i in 0..3) {
            nbt.putLong("extracted_$i", extractedAt[i])
        }
    }

    companion object {
        fun tick(world: World, pos: BlockPos, state: BlockState, be: RubberLogBlockEntity) {
            if (world.isClient) return
            val block = state.block
            if (block !is RubberLogBlock) return

            // Fallback：当 onBlockAdded 未触发时，仅为自然生成的原木初始化橡胶孔。
            if (state.get(RubberLogBlock.NATURAL) && RubberLogBlock.hasNoRubberFaces(state)) {
                world.setBlockState(pos, RubberLogBlock.initializeNaturalState(state, world.random))
                return
            }

            var hasRecoverableFace = false
            for (i in 0..3) {
                val face = faceOf(i)
                if (be.extractedAt[i] == 0L) continue
                if (block.getRubberState(state, face) != RubberFaceState.DRY) continue
                if (world.time - be.extractedAt[i] >= RECOVERY_TICKS) {
                    hasRecoverableFace = true
                    break
                }
            }

            if (!hasRecoverableFace || !hasConnectedRubberLeaves(world, pos)) return

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

        private fun hasConnectedRubberLeaves(world: World, startPos: BlockPos): Boolean {
            val queue = ArrayDeque<BlockPos>()
            val visited = HashSet<BlockPos>()

            queue.add(startPos)
            visited.add(startPos)

            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()

                for (direction in Direction.values()) {
                    val neighborPos = current.offset(direction)
                    val neighborState = world.getBlockState(neighborPos)
                    val neighborBlock = neighborState.block

                    if (neighborBlock is RubberLeavesBlock) {
                        return true
                    }

                    if (neighborBlock is RubberLogBlock && visited.add(neighborPos)) {
                        queue.add(neighborPos.toImmutable())
                    }
                }
            }

            return false
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
