package ic2_120.integration.ftbchunks

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI
import dev.ftb.mods.ftbchunks.api.Protection
import dev.ftb.mods.ftblibrary.math.ChunkDimPos
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.UUID

/**
 * FTB Chunks 领地保护的实际实现。
 *
 * 此类包含所有 FTB Chunks API 的直接引用，
 * 仅在 [ClaimProtection] 通过 Class.forName 延迟加载时才会被 JVM 解析，
 * 确保 FTB Chunks 未安装时不会触发 ClassNotFoundException。
 */
class ClaimProtectionImpl {

    /**
     * 爆炸专用保护检查：按 chunk 粒度查询，使用 FTB Chunks 的 canExplosionsDamageTerrain()。
     * 返回 true 表示该位置受保护（爆炸不应破坏此处的方块）。
     *
     * 性能说明：claim 是 chunk 粒度的，同一 chunk 内所有方块共享同一个判定，
     * 调用方应按 chunk 缓存结果（见 NuclearExplosion 的 chunkLevelCache）。
     */
    fun isExplosionProtected(world: World, pos: BlockPos, ownerUuid: UUID?): Boolean {
        if (world.isClient) return false
        val api = FTBChunksAPI.api()
        if (!api.isManagerLoaded) return false
        val manager = api.manager
        val chunkDimPos = ChunkDimPos(world, pos)
        val claimedChunk = manager.getChunk(chunkDimPos) ?: return false

        val teamData = claimedChunk.teamData

        // 1) 如果该 team 允许爆炸破坏地形，直接放行
        if (teamData.canExplosionsDamageTerrain()) return false

        // 2) 不允许爆炸破坏：判断 owner 是否有权限（owner 是 team 成员则放行自己的反应堆爆炸）
        if (ownerUuid != null) {
            if (teamData.isTeamMember(ownerUuid)) return false
            if (teamData.isAlly(ownerUuid)) return false
        }
        return true
    }

    fun isProtected(world: World, pos: BlockPos, ownerUuid: UUID?, protectionType: String): Boolean {
        if (world.isClient) return false
        val protection = parseProtection(protectionType) ?: return false
        return checkProtection(world, pos, ownerUuid, protection)
    }

    fun isProtected(world: World, pos: BlockPos, actor: net.minecraft.entity.Entity?, protectionType: String): Boolean {
        if (world.isClient) return false
        val protection = parseProtection(protectionType) ?: return false
        return checkProtectionWithActor(world, pos, actor, protection)
    }

    private fun parseProtection(type: String): Protection? {
        return try {
            Protection::class.java.getDeclaredField(type).get(null) as? Protection
        } catch (_: Exception) {
            null
        }
    }

    private fun checkProtection(world: World, pos: BlockPos, ownerUuid: UUID?, protection: Protection): Boolean {
        val api = FTBChunksAPI.api()
        if (!api.isManagerLoaded) return false
        val manager = api.manager

        val server = world.server ?: return false
        val ownerPlayer: ServerPlayerEntity? = ownerUuid?.let { server.playerManager.getPlayer(it) }

        if (ownerPlayer != null) {
            return manager.shouldPreventInteraction(
                ownerPlayer, Hand.MAIN_HAND, pos,
                protection, null
            )
        }

        return checkOfflineProtection(manager, world, pos, ownerUuid, protection)
    }

    private fun checkProtectionWithActor(world: World, pos: BlockPos, actor: net.minecraft.entity.Entity?, protection: Protection): Boolean {
        val api = FTBChunksAPI.api()
        if (!api.isManagerLoaded) return false
        val manager = api.manager

        if (actor is ServerPlayerEntity) {
            return manager.shouldPreventInteraction(
                actor, Hand.MAIN_HAND, pos,
                protection, null
            )
        }

        val ownerUuid = (actor as? net.minecraft.entity.projectile.ProjectileEntity)?.owner
            ?.uuid
        return checkOfflineProtection(manager, world, pos, ownerUuid, protection)
    }

    private fun checkOfflineProtection(
        manager: dev.ftb.mods.ftbchunks.api.ClaimedChunkManager,
        world: World,
        pos: BlockPos,
        ownerUuid: UUID?,
        protection: Protection
    ): Boolean {
        val chunkDimPos = ChunkDimPos(world, pos)
        val claimedChunk = manager.getChunk(chunkDimPos) ?: return false

        if (ownerUuid == null) return true

        val teamData = claimedChunk.teamData
        val property = when (protection) {
            Protection.INTERACT_BLOCK, Protection.RIGHT_CLICK_ITEM ->
                dev.ftb.mods.ftbchunks.api.FTBChunksProperties.BLOCK_INTERACT_MODE
            Protection.INTERACT_ENTITY ->
                dev.ftb.mods.ftbchunks.api.FTBChunksProperties.ENTITY_INTERACT_MODE
            Protection.ATTACK_NONLIVING_ENTITY ->
                dev.ftb.mods.ftbchunks.api.FTBChunksProperties.NONLIVING_ENTITY_ATTACK_MODE
            else ->
                dev.ftb.mods.ftbchunks.api.FTBChunksProperties.BLOCK_EDIT_MODE
        }
        val mode = teamData.team.getProperty(property)

        if (mode == dev.ftb.mods.ftbteams.api.property.PrivacyMode.PUBLIC) return false
        if (mode == dev.ftb.mods.ftbteams.api.property.PrivacyMode.ALLIES) return !teamData.isAlly(ownerUuid)
        return !teamData.team.getRankForPlayer(ownerUuid).isOwner
    }
}
