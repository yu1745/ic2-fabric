package ic2_120.integration.ftbchunks

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import java.util.UUID

/**
 * FTB Chunks has no 26.2 API release yet. This entrypoint is loaded only when
 * ftbchunks is installed; deny destructive interactions until its API can be
 * queried on 26.2. Without ftbchunks, ClaimProtection never loads this class.
 */
class ClaimProtectionImpl : FTBChunksProtection {
    override fun isExplosionProtected(world: Level, pos: BlockPos, ownerUuid: UUID?): Boolean = !world.isClientSide

    override fun isProtected(world: Level, pos: BlockPos, ownerUuid: UUID?, protectionType: String): Boolean = !world.isClientSide

    override fun isProtected(world: Level, pos: BlockPos, actor: Entity?, protectionType: String): Boolean = !world.isClientSide
}
