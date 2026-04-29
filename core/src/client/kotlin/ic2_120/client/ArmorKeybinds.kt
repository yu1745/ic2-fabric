package ic2_120.client

import ic2_120.content.item.NightVisionGoggles
import ic2_120.content.item.armor.NanoHelmet
import ic2_120.content.item.armor.QuantumChestplate
import ic2_120.content.item.armor.QuantumHelmet
import ic2_120.content.network.ToggleNightVisionGogglesPayload
import ic2_120.content.network.ToggleNanoVisionPayload
import ic2_120.content.network.ToggleQuantumFlightPayload
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.entity.EquipmentSlot
import org.lwjgl.glfw.GLFW

/**
 * 量子套 / 纳米套专用按键（夜视、飞行）
 *
 * - Alt+N：纳米/量子头盔夜视
 * - Alt+F：量子胸甲飞行
 *
 * 注：M 键用于铱钻头、夜视仪、采矿镭射等手持设备，量子套不共用。
 */
@Environment(EnvType.CLIENT)
object ArmorKeybinds {
    private val toggleVisionKey = KeyBindingHelper.registerKeyBinding(
        KeyBinding(
            "key.ic2_120.toggle_vision",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            "category.ic2_120.ic2"
        )
    )

    private val toggleFlightKey = KeyBindingHelper.registerKeyBinding(
        KeyBinding(
            "key.ic2_120.toggle_flight",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F,
            "category.ic2_120.ic2"
        )
    )

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val player = client.player ?: return@register

            while (toggleVisionKey.wasPressed()) {
                if (!isAltDown(client)) continue

                // 优先级 1：头盔 → 夜视眼镜
                val helmet = player.getEquippedStack(EquipmentSlot.HEAD)
                if (helmet.item is NightVisionGoggles) {
                    ClientPlayNetworking.send(ToggleNightVisionGogglesPayload)
                    return@register
                }

                // 优先级 2：头盔 → 纳米/量子头盔夜视
                if (helmet.item is NanoHelmet || helmet.item is QuantumHelmet) {
                    ClientPlayNetworking.send(ToggleNanoVisionPayload)
                }
            }

            while (toggleFlightKey.wasPressed()) {
                if (!isAltDown(client)) continue

                val chest = player.getEquippedStack(EquipmentSlot.CHEST)
                if (chest.item is QuantumChestplate) {
                    ClientPlayNetworking.send(ToggleQuantumFlightPayload)
                }
            }
        }
    }

    private fun isAltDown(client: net.minecraft.client.MinecraftClient): Boolean {
        val window = client.window.handle
        return InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_ALT) ||
               InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_ALT)
    }

    fun getVisionKey(): KeyBinding = toggleVisionKey
    fun getFlightKey(): KeyBinding = toggleFlightKey
}
