package ic2_120.client

import ic2_120.content.entity.LaserMode
import ic2_120.content.item.MiningLaserItem
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.client.Minecraft
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import org.lwjgl.glfw.GLFW

/**
 * 采矿镭射枪：当前模式、绑定键切换提示、按当前模式单次耗能估算剩余发射次数；
 * 按住 Shift 显示全部模式名称与说明。
 */
@Environment(EnvType.CLIENT)
object MiningLaserTooltipHandler {

    fun register() {
        ItemTooltipCallback.EVENT.register { stack, _, type, lines ->
            if (stack.item !is MiningLaserItem) return@register
            val laser = stack.item as MiningLaserItem
            val mode = MiningLaserItem.getMode(stack)
            val energy = laser.getEnergy(stack)
            val cost = mode.energyCost
            val shots = if (cost > 0) energy / cost else 0L

            lines.add(
                Component.translatable("tooltip.ic2_120.mining_laser.current_mode")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.translatable(mode.translationKey).withStyle(ChatFormatting.AQUA))
            )

            lines.add(
                Component.translatable("tooltip.ic2_120.mining_laser.remaining_shots", shots)
                    .withStyle(ChatFormatting.GRAY)
            )

            lines.add(
                Component.translatable("tooltip.ic2_120.mining_laser.toggle_mode_prefix")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.translatable("tooltip.ic2_120.foam_sprayer.alt_right_click").withStyle(ChatFormatting.YELLOW))
            )

            val shift = isShiftDown()
            if (!shift) {
                lines.add(Component.translatable(mode.descriptionKey).withStyle(ChatFormatting.DARK_GRAY))
                lines.add(
                    Component.translatable("tooltip.ic2_120.mining_laser.shift_hint")
                        .withStyle(ChatFormatting.DARK_GRAY)
                )
            } else {
                lines.add(Component.translatable("tooltip.ic2_120.mining_laser.modes_header").withStyle(ChatFormatting.GOLD))
                for (m in LaserMode.entries) {
                    lines.add(
                        Component.literal("• ")
                            .withStyle(ChatFormatting.GRAY)
                            .append(Component.translatable(m.translationKey).withStyle(ChatFormatting.AQUA))
                            .append(Component.literal(" — ").withStyle(ChatFormatting.DARK_GRAY))
                            .append(Component.translatable(m.descriptionKey).withStyle(ChatFormatting.GRAY))
                    )
                }
            }
        }
    }

    private fun isShiftDown(): Boolean {
        val handle = Minecraft.getInstance().window.handle
        return InputConstants.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_SHIFT) ||
            InputConstants.isKeyPressed(handle, GLFW.GLFW_KEY_RIGHT_SHIFT)
    }
}
