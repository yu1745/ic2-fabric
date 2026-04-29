package ic2_120.content.uu

import ic2_120.content.block.machines.PatternStorageBlockEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

fun findAdjacentPatternStorages(world: World, pos: BlockPos): List<PatternStorageBlockEntity> =
    Direction.entries.mapNotNull { side ->
        world.getBlockEntity(pos.offset(side)) as? PatternStorageBlockEntity
    }

fun findUniqueAdjacentPatternStorage(world: World, pos: BlockPos): PatternStorageBlockEntity? {
    val storages = findAdjacentPatternStorages(world, pos)
    return if (storages.size == 1) storages.first() else null
}
