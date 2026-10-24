package ic2_120.integration.ftbchunks
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.world.entity.Entity
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import java.util.UUID
import kotlin.math.ceil
import net.minecraft.world.entity.player.Player
import net.minecraft.server.level.ServerPlayer

/**
 * FTB Chunks 领地保护集成（外观层）。
 *
 * 本类不包含任何 FTB Chunks API 的直接引用。
 * 实际保护检查通过 Fabric entrypoint 发现 [FTBChunksProtection] 实现完成，
 * 确保 FTB Chunks 未安装时不会触发 ClassNotFoundException。
 */
object ClaimProtection {

    const val EDIT_BLOCK = "EDIT_BLOCK"
    const val EDIT_FLUID = "EDIT_FLUID"
    const val INTERACT_BLOCK = "INTERACT_BLOCK"
    const val RIGHT_CLICK_ITEM = "RIGHT_CLICK_ITEM"
    const val INTERACT_ENTITY = "INTERACT_ENTITY"
    const val ATTACK_NONLIVING_ENTITY = "ATTACK_NONLIVING_ENTITY"

    private val impl: FTBChunksProtection? by lazy {
        if (!FabricLoader.getInstance().isModLoaded("ftbchunks")) null
        else FabricLoader.getInstance()
            .getEntrypoints("ic2_120:ftbchunks_protection", FTBChunksProtection::class.java)
            .firstOrNull()
    }

    @JvmStatic
    fun isProtected(world: Level, pos: BlockPos, ownerUuid: UUID? = null, protectionType: String = EDIT_BLOCK): Boolean =
        impl?.isProtected(world, pos, ownerUuid, protectionType) == true

    @JvmStatic
    fun isProtected(world: Level, pos: BlockPos, actor: Entity?, protectionType: String = EDIT_BLOCK): Boolean =
        impl?.isProtected(world, pos, actor, protectionType) == true

    /**
     * 爆炸专用保护检查（FTB Chunks canExplosionsDamageTerrain）。
     * 返回 true 表示该位置受保护，爆炸不应破坏此处方块。
     * claim 是 chunk 粒度，调用方应按 chunk 缓存结果以优化批量场景（如核爆炸）。
     */
    @JvmStatic
    fun isExplosionProtected(world: Level, pos: BlockPos, ownerUuid: UUID? = null): Boolean =
        impl?.isExplosionProtected(world, pos, ownerUuid) == true

    /**
     * Preflight every position before a multi-block operation starts.  Positions are
     * deliberately checked as a snapshot so callers cannot accidentally break the
     * center block before discovering a denied edge block.
     */
    @JvmStatic
    fun allAllowed(world: Level, positions: Iterable<BlockPos>, player: Player?, protectionType: String = EDIT_BLOCK): Boolean {
        val serverPlayer = player as? ServerPlayer
        if (serverPlayer != null) {
            return positions.all { !isProtected(world, it, serverPlayer, protectionType) }
        }
        val owner = player?.uuid
        return positions.all { !isProtected(world, it, owner, protectionType) }
    }

    @JvmStatic
    fun allAllowedUuid(world: Level, positions: Iterable<BlockPos>, ownerUuid: UUID?, protectionType: String = EDIT_BLOCK): Boolean =
        positions.all { !isProtected(world, it, ownerUuid, protectionType) }

    @JvmStatic
    fun allExplosionAllowed(world: Level, positions: Iterable<BlockPos>, ownerUuid: UUID?): Boolean =
        positions.all { !isExplosionProtected(world, it, ownerUuid) }

    @JvmStatic
    fun explosionCubeAllowed(world: Level, center: BlockPos, radius: Float, ownerUuid: UUID?): Boolean {
        val r = ceil(radius.toDouble()).toInt().coerceAtLeast(1)
        val positions = BlockPos.betweenClosed(
            center.x - r, center.y - r, center.z - r,
            center.x + r, center.y + r, center.z + r
        ).map { it.immutable() }.toList()
        return allExplosionAllowed(world, positions, ownerUuid)
    }
}
