package ic2_120.integration.ftbchunks

import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.UUID

/**
 * FTB Chunks 领地保护集成（外观层）。
 *
 * 本类不包含任何 FTB Chunks API 的直接引用，
 * 实际保护检查通过 [ClaimProtectionImpl] 延迟加载完成，
 * 确保 FTB Chunks 未安装时不会触发 ClassNotFoundException。
 */
object ClaimProtection {

    private val logger = LoggerFactory.getLogger("IC2-ClaimProtection")

    init {
        val loaded = FabricLoader.getInstance().isModLoaded("ftbchunks")
        logger.info("[IC2] FTB Chunks integration: {}", if (loaded) "enabled" else "disabled (mod not present)")
    }

    /** 保护类型常量，对应 [dev.ftb.mods.ftbchunks.api.Protection] 枚举值 */
    const val EDIT_BLOCK = "EDIT_BLOCK"
    const val EDIT_FLUID = "EDIT_FLUID"
    const val INTERACT_BLOCK = "INTERACT_BLOCK"
    const val RIGHT_CLICK_ITEM = "RIGHT_CLICK_ITEM"
    const val INTERACT_ENTITY = "INTERACT_ENTITY"
    const val ATTACK_NONLIVING_ENTITY = "ATTACK_NONLIVING_ENTITY"

    private val impl: ClaimProtectionImpl? by lazy {
        if (!FabricLoader.getInstance().isModLoaded("ftbchunks")) return@lazy null
        runCatching {
            Class.forName("ic2_120.integration.ftbchunks.ClaimProtectionImpl")
                .getDeclaredConstructor()
                .newInstance() as ClaimProtectionImpl
        }.getOrNull()
    }

    fun isProtected(world: World, pos: BlockPos, ownerUuid: UUID? = null, protectionType: String = EDIT_BLOCK): Boolean {
        return impl?.isProtected(world, pos, ownerUuid, protectionType) == true
    }

    fun isProtected(world: World, pos: BlockPos, actor: net.minecraft.entity.Entity?, protectionType: String = EDIT_BLOCK): Boolean {
        return impl?.isProtected(world, pos, actor, protectionType) == true
    }
}
