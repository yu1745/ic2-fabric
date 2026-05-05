package ic2_120.client

import ic2_120.content.item.ElectricJetpack
import ic2_120.content.item.NightVisionGoggles
import ic2_120.content.item.armor.JetpackItem
import ic2_120.content.item.armor.NanoHelmet
import ic2_120.content.item.armor.QuantumBoots
import ic2_120.content.item.armor.QuantumChestplate
import ic2_120.content.item.armor.QuantumHelmet
import ic2_120.content.item.armor.QuantumLeggings
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
 * 喷气背包使用 Alt+M(飞行开关)。
 */
@Environment(EnvType.CLIENT)
object ArmorTooltipHandler {

    fun register() {
        ItemTooltipCallback.EVENT.register { stack, context, type ->
            val item = stack.item

            // 喷气背包：M 键
            if (item is JetpackItem || item is ElectricJetpack) {
                val key = ModeKeybinds.getModeKey()
                val name = key.boundKeyLocalizedText.string
                type.add(Text.literal("飞行开关: ").formatted(Formatting.GRAY)
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

            // 量子护腿：L 键
            if (item is QuantumLeggings) {
                val key = ArmorKeybinds.getSpeedKey()
                val name = key.boundKeyLocalizedText.string
                type.add(Text.literal("神行按键: ").formatted(Formatting.GRAY)
                    .append(Text.literal("Alt + ").formatted(Formatting.YELLOW))
                    .append(Text.literal(name).formatted(Formatting.YELLOW)))
            }

            // 量子靴子：J 键
            if (item is QuantumBoots) {
                val key = ArmorKeybinds.getJumpKey()
                val name = key.boundKeyLocalizedText.string
                type.add(Text.literal("大跳按键: ").formatted(Formatting.GRAY)
                    .append(Text.literal("Alt + ").formatted(Formatting.YELLOW))
                    .append(Text.literal(name).formatted(Formatting.YELLOW)))
            }
        }
    }
}
