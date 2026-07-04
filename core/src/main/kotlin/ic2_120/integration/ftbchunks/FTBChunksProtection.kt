package ic2_120.integration.ftbchunks

import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.UUID

interface FTBChunksProtection {
    fun isProtected(world: World, pos: BlockPos, ownerUuid: UUID?, protectionType: String): Boolean
    fun isProtected(world: World, pos: BlockPos, actor: Entity?, protectionType: String): Boolean
    fun isExplosionProtected(world: World, pos: BlockPos, ownerUuid: UUID?): Boolean
}
