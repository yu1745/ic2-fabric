package ic2_120.client

import ic2_120.content.entity.LaserMode
import ic2_120.content.item.MiningLaserItem
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.client.MinecraftClient
import net.minecraft.client.util.InputUtil
import net.minecraft.text.Text
import net.minecraft.util.Formatting
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

            val modeKey = ModeKeybinds.getModeKey()
            val boundKeyName = modeKey.boundKeyLocalizedText.string

            lines.add(
                Text.translatable("tooltip.ic2_120.mining_laser.current_mode")
                    .formatted(Formatting.GRAY)
                    .append(Text.translatable(mode.translationKey).formatted(Formatting.AQUA))
            )

            lines.add(
                Text.translatable("tooltip.ic2_120.mining_laser.remaining_shots", shots)
                    .formatted(Formatting.GRAY)
            )

            lines.add(
                Text.translatable("tooltip.ic2_120.mining_laser.toggle_mode_prefix")
                    .formatted(Formatting.GRAY)
                    .append(Text.literal(boundKeyName).formatted(Formatting.YELLOW))
                    .append(Text.translatable("tooltip.ic2_120.mining_laser.toggle_mode_suffix").formatted(Formatting.YELLOW))
            )

            val shift = isShiftDown()
            if (!shift) {
                lines.add(Text.translatable(mode.descriptionKey).formatted(Formatting.DARK_GRAY))
                lines.add(
                    Text.translatable("tooltip.ic2_120.mining_laser.shift_hint")
                        .formatted(Formatting.DARK_GRAY)
                )
            } else {
                lines.add(Text.translatable("tooltip.ic2_120.mining_laser.modes_header").formatted(Formatting.GOLD))
                for (m in LaserMode.entries) {
                    lines.add(
                        Text.literal("• ")
                            .formatted(Formatting.GRAY)
                            .append(Text.translatable(m.translationKey).formatted(Formatting.AQUA))
                            .append(Text.literal(" — ").formatted(Formatting.DARK_GRAY))
                            .append(Text.translatable(m.descriptionKey).formatted(Formatting.GRAY))
                    )
                }
            }
        }
    }

    private fun isShiftDown(): Boolean {
        val handle = MinecraftClient.getInstance().window.handle
        return InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_SHIFT) ||
            InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_RIGHT_SHIFT)
    }
}
