package ic2_120.integration.jade

import ic2_120.content.block.machines.ElectricHeatGeneratorBlockEntity
import ic2_120.content.sync.ElectricHeatGeneratorSync
import ic2_120.content.upgrade.IRedstoneControlSupport
import ic2_120.content.upgrade.RedstoneControlComponent
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.resources.ResourceLocation
import snownee.jade.api.BlockAccessor
import snownee.jade.api.IBlockComponentProvider
import snownee.jade.api.IServerDataProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.config.IPluginConfig

/**
 * 电力加热机 Jade 提示。
 *
 * 显示内容：
 * - 缓存能量 / 容量
 * - 当前产热（HU/t，滤波后）
 * - 线圈数量与最大产热
 * - 当机器因红石逻辑当前无法运行时，显示红色警告，帮助玩家理解"为什么不发热"
 */
object ElectricHeatGeneratorJadeProvider : IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    private val ELECTRIC_HEAT = ResourceLocation.fromNamespaceAndPath("ic2_120", "electric_heat_generator")

    override fun appendServerData(data: CompoundTag, accessor: BlockAccessor) {
        val be = accessor.blockEntity as? ElectricHeatGeneratorBlockEntity ?: return
        val world = accessor.level
        val pos = accessor.position

        data.putLong("eu", be.sync.amount.coerceAtLeast(0L))
        data.putLong("euCap", ElectricHeatGeneratorSync.ENERGY_CAPACITY)
        data.putLong("generatedHeat", be.sync.getSyncedGeneratedHeat().coerceAtLeast(0L))
        data.putInt("coilCount", be.getCoilCount())
        data.putBoolean("redstoneInverted", be.redstoneInverted)
        data.putBoolean("redstonePowered", world.hasNeighborSignal(pos))
        // 复用机器自身使用的判定逻辑，保证与运行行为完全一致
        data.putBoolean("redstoneAllowsRun", RedstoneControlComponent.canRun(world, pos, be))
    }

    override fun appendHoverText(tooltip: ITooltip, accessor: BlockAccessor, config: IPluginConfig) {
        if (!accessor.serverData.contains("eu")) return
        val data = accessor.serverData

        val eu = data.getLong("eu")
        val euCap = data.getLong("euCap")
        val generatedHeat = data.getLong("generatedHeat")
        val coilCount = data.getInt("coilCount")
        val redstoneInverted = data.getBoolean("redstoneInverted")
        val redstonePowered = data.getBoolean("redstonePowered")
        val redstoneAllowsRun = data.getBoolean("redstoneAllowsRun")

        tooltip.add(Component.translatable("ic2_120.jade.electric_heat.buffer", eu, euCap))
        tooltip.add(Component.translatable("ic2_120.jade.electric_heat.generating", generatedHeat))
        tooltip.add(Component.translatable("ic2_120.jade.electric_heat.coils", coilCount))

        if (!redstoneAllowsRun) {
            // 机器因红石逻辑停机：根据模式给出对应原因
            val reasonKey = if (redstoneInverted) {
                // 反转模式：有信号时停机
                "ic2_120.jade.electric_heat.stopped_inverted"
            } else {
                // 默认模式：无信号时停机
                "ic2_120.jade.electric_heat.stopped_no_signal"
            }
            tooltip.add(
                Component.translatable(reasonKey)
                    .setStyle(Style.EMPTY.withColor(ChatFormatting.RED))
            )
        } else {
            tooltip.add(
                Component.translatable("ic2_120.jade.electric_heat.running")
                    .setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN))
            )
        }
    }

    override fun getUid(): ResourceLocation = ELECTRIC_HEAT
}
