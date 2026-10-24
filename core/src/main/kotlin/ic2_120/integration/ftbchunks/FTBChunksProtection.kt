package ic2_120.integration.ftbchunks

import net.minecraft.world.entity.Entity
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import java.util.UUID

interface FTBChunksProtection {
    fun isProtected(world: Level, pos: BlockPos, ownerUuid: UUID?, protectionType: String): Boolean
    fun isProtected(world: Level, pos: BlockPos, actor: Entity?, protectionType: String): Boolean
    fun isExplosionProtected(world: Level, pos: BlockPos, ownerUuid: UUID?): Boolean
}
