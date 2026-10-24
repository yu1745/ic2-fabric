package ic2_120.client

import ic2_120.content.item.NightVisionGoggles
import ic2_120.content.item.armor.NanoHelmet
import ic2_120.content.item.armor.QuantumBoots
import ic2_120.content.item.armor.QuantumHelmet
<<<<<<< HEAD
import ic2_120.content.item.armor.QuantumLeggings
import ic2_120.content.network.NetworkManager
import io.netty.buffer.Unpooled
=======
import ic2_120.content.network.ToggleNightVisionGogglesPayload
import ic2_120.content.network.ToggleNanoVisionPayload
import ic2_120.content.network.ToggleQuantumLeggingsSpeedPayload
import ic2_120.content.network.ToggleQuantumBootsJumpPayload
import ic2_120.content.item.armor.QuantumLeggings
>>>>>>> backup/1.21.1
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
 * 量子套 / 纳米套专用按键（夜视、神行、大跳）
 *
 * - Alt+N：纳米/量子头盔夜视
 * - Alt+L：量子护腿神行
 * - Alt+J：量子靴子大跳
 *
 * 飞行（量子胸甲/喷气背包）复用原版创造飞行的双击空格触发。
<<<<<<< HEAD
 * 手持工具的模式切换使用 Alt+右键；盔甲功能不共用该交互。
=======
 * M 键用于铱钻头、夜视仪、采矿镭射等手持设备，量子套不共用。
>>>>>>> backup/1.21.1
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

    private val toggleSpeedKey = KeyBindingHelper.registerKeyBinding(
        KeyBinding(
            "key.ic2_120.toggle_speed",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_L,
            "category.ic2_120.ic2"
        )
    )

    private val toggleJumpKey = KeyBindingHelper.registerKeyBinding(
        KeyBinding(
            "key.ic2_120.toggle_jump",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
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

            while (toggleSpeedKey.wasPressed()) {
                if (!isAltDown(client)) continue

                val legs = player.getEquippedStack(EquipmentSlot.LEGS)
                if (legs.item is QuantumLeggings) {
<<<<<<< HEAD
                    ClientPlayNetworking.send(
                        NetworkManager.TOGGLE_QUANTUM_LEGGINGS_SPEED_PACKET,
                        PacketByteBuf(Unpooled.buffer())
                    )
=======
                    ClientPlayNetworking.send(ToggleQuantumLeggingsSpeedPayload)
>>>>>>> backup/1.21.1
                }
            }

            while (toggleJumpKey.wasPressed()) {
                if (!isAltDown(client)) continue

                val feet = player.getEquippedStack(EquipmentSlot.FEET)
                if (feet.item is QuantumBoots) {
<<<<<<< HEAD
                    ClientPlayNetworking.send(
                        NetworkManager.TOGGLE_QUANTUM_BOOTS_JUMP_PACKET,
                        PacketByteBuf(Unpooled.buffer())
                    )
=======
                    ClientPlayNetworking.send(ToggleQuantumBootsJumpPayload)
>>>>>>> backup/1.21.1
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
    fun getSpeedKey(): KeyBinding = toggleSpeedKey
    fun getJumpKey(): KeyBinding = toggleJumpKey
}
