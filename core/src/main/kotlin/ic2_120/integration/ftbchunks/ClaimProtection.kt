package ic2_120.integration.ftbchunks
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.UUID

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

    fun isProtected(world: World, pos: BlockPos, ownerUuid: UUID? = null, protectionType: String = EDIT_BLOCK): Boolean =
        impl?.isProtected(world, pos, ownerUuid, protectionType) == true

    fun isProtected(world: World, pos: BlockPos, actor: Entity?, protectionType: String = EDIT_BLOCK): Boolean =
        impl?.isProtected(world, pos, actor, protectionType) == true

    /**
     * 爆炸专用保护检查（FTB Chunks canExplosionsDamageTerrain）。
     * 返回 true 表示该位置受保护，爆炸不应破坏此处方块。
     * claim 是 chunk 粒度，调用方应按 chunk 缓存结果以优化批量场景（如核爆炸）。
     */
    fun isExplosionProtected(world: World, pos: BlockPos, ownerUuid: UUID? = null): Boolean =
        impl?.isExplosionProtected(world, pos, ownerUuid) == true
}
