package ic2_120.compat

import net.minecraft.core.BlockPos
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import java.util.Optional

/** MC26 evaluate no longer accepts a nullable fallback. */
fun ContainerLevelAccess.blockEntityOrNull(): BlockEntity? =
    evaluate { level: Level, pos: BlockPos -> Optional.ofNullable(level.getBlockEntity(pos)) }
        .orElse(Optional.empty())
        .orElse(null)

fun <T : Any> ContainerLevelAccess.evaluateNullable(action: (Level, BlockPos) -> T?): T? =
    evaluate { level: Level, pos: BlockPos -> Optional.ofNullable(action(level, pos)) }
        .orElse(Optional.empty())
        .orElse(null)
