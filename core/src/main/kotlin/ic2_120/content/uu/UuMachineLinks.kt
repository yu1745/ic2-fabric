package ic2_120.content.uu

import ic2_120.content.block.machines.PatternStorageBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level

fun findAdjacentPatternStorages(world: Level, pos: BlockPos): List<PatternStorageBlockEntity> =
    Direction.entries.mapNotNull { side ->
        world.getBlockEntity(pos.offset(side)) as? PatternStorageBlockEntity
    }

fun findUniqueAdjacentPatternStorage(world: Level, pos: BlockPos): PatternStorageBlockEntity? {
    val storages = findAdjacentPatternStorages(world, pos)
    return if (storages.size == 1) storages.first() else null
}
