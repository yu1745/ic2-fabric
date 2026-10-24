package ic2_120.client

import ic2_120.content.item.NightVisionGoggles
import ic2_120.content.item.armor.NanoHelmet
import ic2_120.content.item.armor.QuantumBoots
import ic2_120.content.item.armor.QuantumHelmet
import ic2_120.content.item.armor.QuantumLeggings
import ic2_120.content.network.NetworkManager
import io.netty.buffer.Unpooled
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.KeyMapping
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.network.FriendlyByteBuf
import org.lwjgl.glfw.GLFW

/**
 * 量子套 / 纳米套专用按键（夜视、神行、大跳）
 *
 * - Alt+N：纳米/量子头盔夜视
 * - Alt+L：量子护腿神行
 * - Alt+J：量子靴子大跳
 *
 * 飞行（量子胸甲/喷气背包）复用原版创造飞行的双击空格触发。
 * 手持工具的模式切换使用 Alt+右键；盔甲功能不共用该交互。
 */
@Environment(EnvType.CLIENT)
object ArmorKeybinds {
    private val toggleVisionKey = KeyBindingHelper.registerKeyBinding(
        KeyMapping(
            "key.ic2_120.toggle_vision",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            "category.ic2_120.ic2"
        )
    )

    private val toggleSpeedKey = KeyBindingHelper.registerKeyBinding(
        KeyMapping(
            "key.ic2_120.toggle_speed",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_L,
            "category.ic2_120.ic2"
        )
    )

    private val toggleJumpKey = KeyBindingHelper.registerKeyBinding(
        KeyMapping(
            "key.ic2_120.toggle_jump",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "category.ic2_120.ic2"
        )
    )

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val player = minecraft.player ?: return@register

            while (toggleVisionKey.consumeClick()) {
                if (!isAltDown(client)) continue

                // 优先级 1：头盔 → 夜视眼镜
                val helmet = player.getItemBySlot(EquipmentSlot.HEAD)
                if (helmet.item is NightVisionGoggles) {
                    ClientPlayNetworking.send(ToggleNightVisionGogglesPayload)
                    return@register
                }

                // 优先级 2：头盔 → 纳米/量子头盔夜视
                if (helmet.item is NanoHelmet || helmet.item is QuantumHelmet) {
                    ClientPlayNetworking.send(ToggleNanoVisionPayload)
                }
            }

            while (toggleSpeedKey.consumeClick()) {
                if (!isAltDown(client)) continue

                val legs = player.getItemBySlot(EquipmentSlot.LEGS)
                if (legs.item is QuantumLeggings) {
                    ClientPlayNetworking.send(ToggleQuantumLeggingsSpeedPayload)
                }
            }

            while (toggleJumpKey.consumeClick()) {
                if (!isAltDown(client)) continue

                val feet = player.getItemBySlot(EquipmentSlot.FEET)
                if (feet.item is QuantumBoots) {
                    ClientPlayNetworking.send(ToggleQuantumBootsJumpPayload)
                }
            }
        }
    }

    private fun isAltDown(client: net.minecraft.client.Minecraft): Boolean {
        val window = minecraft.getWindow().handle
        return InputConstants..isDown.takeIf { it } != null || window, GLFW.GLFW_KEY_LEFT_ALT) ||
               InputConstants..isDown.takeIf { it } != null || window, GLFW.GLFW_KEY_RIGHT_ALT)
    }

    fun getVisionKey(): KeyMapping = toggleVisionKey
    fun getSpeedKey(): KeyMapping = toggleSpeedKey
    fun getJumpKey(): KeyMapping = toggleJumpKey
}
