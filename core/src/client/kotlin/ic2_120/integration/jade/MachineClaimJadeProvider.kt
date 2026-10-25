package ic2_120.integration.jade

import ic2_120.content.block.IClaimSensitive
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.resources.Identifier
import snownee.jade.api.BlockAccessor
import snownee.jade.api.IBlockComponentProvider
import snownee.jade.api.IServerDataProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.config.IPluginConfig

/**
 * 通用领地保护诊断 Jade 提供者。
 *
 * 注册到 BlockEntity / Block 基类，对实现 [IClaimSensitive] 的方块实体生效。
 * 当机器当前操作目标被 FTB Chunks 领地保护阻止时，在 Jade HUD 显示红色警告，
 * 帮助玩家理解"为什么机器不工作"。
 */
object MachineClaimJadeProvider : IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    private val CLAIM_BLOCKED = Identifier.fromNamespaceAndPath("ic2_120", "claim_blocked")

    override fun appendServerData(data: CompoundTag, accessor: BlockAccessor) {
        val be = accessor.blockEntity as? IClaimSensitive ?: return
        val world = accessor.level
        val pos = accessor.position
        val state = accessor.blockState
        val blocked = be.claimBlockedTargets(world, pos, state)
        if (blocked.isEmpty()) {
            data.putBoolean("claimBlocked", false)
        } else {
            data.putBoolean("claimBlocked", true)
            data.putInt("claimBlockedCount", blocked.size)
            val first = blocked.first()
            data.putInt("bx", first.x - pos.x)
            data.putInt("by", first.y - pos.y)
            data.putInt("bz", first.z - pos.z)
        }
    }

    override fun appendTooltip(tooltip: ITooltip, accessor: BlockAccessor, config: IPluginConfig) {
        if (!accessor.serverData.getBooleanOr("claimBlocked", false)) return
        val count = accessor.serverData.getIntOr("claimBlockedCount", 0)
        val dx = accessor.serverData.getIntOr("bx", 0)
        val dy = accessor.serverData.getIntOr("by", 0)
        val dz = accessor.serverData.getIntOr("bz", 0)
        tooltip.add(
            Component.translatable("ic2_120.jade.claim_blocked", count, dx, dy, dz)
                .setStyle(Style.EMPTY.withColor(ChatFormatting.RED))
        )
    }

    override fun getUid(): Identifier = CLAIM_BLOCKED
}
