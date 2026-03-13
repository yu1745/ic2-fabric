package ic2_120.client

import ic2_120.content.item.armor.NanoHelmet
import ic2_120.content.item.armor.QuantumChestplate
import ic2_120.content.item.armor.QuantumHelmet
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Formatting

/**
 * 护甲 Tooltip 客户端处理器
 *
 * 动态添加实际的按键绑定名称到 tooltip。
 */
@Environment(EnvType.CLIENT)
object ArmorTooltipHandler {

    fun register() {
        ItemTooltipCallback.EVENT.register { stack, context, type ->
            val item = stack.item

            // 处理纳米/量子头盔 - 添加夜视按键
            if (item is NanoHelmet || item is QuantumHelmet) {
                val visionKey = ArmorKeybinds.getVisionKey()
                val boundKeyName = visionKey.boundKeyLocalizedText.string
                type.add(Text.literal("夜视按键: ").formatted(Formatting.GRAY)
                    .append(Text.literal("Alt + ").formatted(Formatting.YELLOW))
                    .append(Text.literal(boundKeyName).formatted(Formatting.YELLOW)))
            }

            // 处理量子胸甲 - 添加飞行按键
            if (item is QuantumChestplate) {
                val flightKey = ArmorKeybinds.getFlightKey()
                val boundKeyName = flightKey.boundKeyLocalizedText.string
                type.add(Text.literal("飞行按键: ").formatted(Formatting.GRAY)
                    .append(Text.literal("Alt + ").formatted(Formatting.YELLOW))
                    .append(Text.literal(boundKeyName).formatted(Formatting.YELLOW)))
            }
        }
    }
}
