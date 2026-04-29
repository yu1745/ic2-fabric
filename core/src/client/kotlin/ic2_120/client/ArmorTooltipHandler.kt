package ic2_120.client

import ic2_120.content.item.NightVisionGoggles
import ic2_120.content.item.armor.JetpackItem
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
 * 量子套/纳米套使用 Alt+N(夜视)、Alt+F(飞行)；
 * 夜视仪使用 Alt+N；
 * 喷气背包使用 Alt+M(模式切换)。
 */
@Environment(EnvType.CLIENT)
object ArmorTooltipHandler {

    fun register() {
        ItemTooltipCallback.EVENT.register { stack, context, type ->
            val item = stack.item

            // 喷气背包：M 键
            if (item is JetpackItem) {
                val key = ModeKeybinds.getModeKey()
                val name = key.boundKeyLocalizedText.string
                type.add(Text.literal("模式切换: ").formatted(Formatting.GRAY)
                    .append(Text.literal("Alt + ").formatted(Formatting.YELLOW))
                    .append(Text.literal(name).formatted(Formatting.YELLOW)))
            }

            // 夜视仪、纳米/量子头盔：N 键
            if (item is NightVisionGoggles || item is NanoHelmet || item is QuantumHelmet) {
                val key = ArmorKeybinds.getVisionKey()
                val name = key.boundKeyLocalizedText.string
                type.add(Text.literal("夜视按键: ").formatted(Formatting.GRAY)
                    .append(Text.literal("Alt + ").formatted(Formatting.YELLOW))
                    .append(Text.literal(name).formatted(Formatting.YELLOW)))
            }

            // 量子胸甲：F 键
            if (item is QuantumChestplate) {
                val key = ArmorKeybinds.getFlightKey()
                val name = key.boundKeyLocalizedText.string
                type.add(Text.literal("飞行按键: ").formatted(Formatting.GRAY)
                    .append(Text.literal("Alt + ").formatted(Formatting.YELLOW))
                    .append(Text.literal(name).formatted(Formatting.YELLOW)))
            }
        }
    }
}
