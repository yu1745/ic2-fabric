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
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting

/**
 * 护甲 Tooltip 客户端处理器
 *
 * 量子套/纳米套使用 Alt+N(夜视)；
 * 夜视仪使用 Alt+N；
 * 飞行（量子胸甲/喷气背包）使用双击空格触发。
 */
@Environment(EnvType.CLIENT)
object ArmorTooltipHandler {

    fun register() {
        ItemTooltipCallback.EVENT.register { stack, context, type ->
            val item = stack.item

            // 喷气背包：双击空格
            if (item is JetpackItem || item is ElectricJetpack) {
                type.add(Component.translatable("tooltip.ic2_120.armor.key.flight_double_tap").formatted(ChatFormatting.GRAY))
            }

            // 夜视仪、纳米/量子头盔：N 键
            if (item is NightVisionGoggles || item is NanoHelmet || item is QuantumHelmet) {
                val key = ArmorKeybinds.getVisionKey()
                val name = key.boundKeyLocalizedText.string
                type.add(Component.translatable("tooltip.ic2_120.armor.key.vision").formatted(ChatFormatting.GRAY)
                    .append(Component.literal("Alt + ").formatted(ChatFormatting.YELLOW))
                    .append(Component.literal(name).formatted(ChatFormatting.YELLOW)))
            }

            // 量子胸甲：双击空格
            if (item is QuantumChestplate) {
                type.add(Component.translatable("tooltip.ic2_120.armor.key.flight_double_tap").formatted(ChatFormatting.GRAY))
            }

            // 量子护腿：L 键
            if (item is QuantumLeggings) {
                val key = ArmorKeybinds.getSpeedKey()
                val name = key.boundKeyLocalizedText.string
                type.add(Component.translatable("tooltip.ic2_120.armor.key.speed").formatted(ChatFormatting.GRAY)
                    .append(Component.literal("Alt + ").formatted(ChatFormatting.YELLOW))
                    .append(Component.literal(name).formatted(ChatFormatting.YELLOW)))
            }

            // 量子靴子：J 键
            if (item is QuantumBoots) {
                val key = ArmorKeybinds.getJumpKey()
                val name = key.boundKeyLocalizedText.string
                type.add(Component.translatable("tooltip.ic2_120.armor.key.jump").formatted(ChatFormatting.GRAY)
                    .append(Component.literal("Alt + ").formatted(ChatFormatting.YELLOW))
                    .append(Component.literal(name).formatted(ChatFormatting.YELLOW)))
            }
        }
    }
}
