package ic2_120.client

import ic2_120.Ic2_120
import ic2_120.content.item.NightVisionGoggles
import ic2_120.content.item.armor.NanoHelmet
import ic2_120.content.item.armor.QuantumChestplate
import ic2_120.content.item.armor.QuantumHelmet
import ic2_120.content.network.NetworkManager
import io.netty.buffer.Unpooled
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.entity.EquipmentSlot
import net.minecraft.network.PacketByteBuf
import org.lwjgl.glfw.GLFW

/**
 * 护甲按键绑定管理器
 *
 * 管理所有护甲相关的快捷键绑定。
 *
 * ## 支持的快捷键
 *
 * - **夜视切换**（默认 Alt+N）：切换纳米/量子头盔的夜视功能
 * - **飞行切换**（默认 Alt+F）：切换量子胸甲的飞行功能
 *
 * ## 自定义按键
 *
 * 所有按键都会自动出现在 Minecraft 的「选项 → 控制 → 按键绑定」界面中。
 * 玩家可以在游戏中随时重新绑定快捷键。
 *
 * ## 设计原则
 *
 * - 使用 Fabric `KeyBindingHelper.registerKeyBinding()` API 注册
 * - 按键会自动出现在 Minecraft 的控制界面中
 * - 玩家可以在游戏中随时重新绑定快捷键
 * - 使用翻译键支持多语言显示
 */
@Environment(EnvType.CLIENT)
object ArmorKeybinds {
    // 夜视切换键（默认 Alt+N）
    private val toggleVisionKey = KeyBindingHelper.registerKeyBinding(
        KeyBinding(
            "key.ic2_120.toggle_vision",  // 翻译键，在 lang 文件中定义显示文本
            InputUtil.Type.KEYSYM,         // 输入类型：键盘按键
            GLFW.GLFW_KEY_N,               // 默认按键：N 键
            "category.ic2_120.ic2"         // 分类：工业时代2（在 lang 文件中翻译）
        )
    )

    // 飞行切换键（默认 Alt+F）
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

            // Alt + N: 切换夜视
            while (toggleVisionKey.wasPressed()) {
                if (!isAltDown(client)) continue

                val helmet = player.getEquippedStack(EquipmentSlot.HEAD)
                val packet = when (helmet.item) {
                    is NightVisionGoggles -> NetworkManager.TOGGLE_NIGHT_VISION_GOGGLES_PACKET
                    is NanoHelmet, is QuantumHelmet -> NetworkManager.TOGGLE_NANO_VISION_PACKET
                    else -> null
                }

                if (packet != null) {
                    ClientPlayNetworking.send(packet, PacketByteBuf(Unpooled.buffer()))
                }
            }

            // Alt + F: 切换飞行（仅量子胸甲）
            while (toggleFlightKey.wasPressed()) {
                if (!isAltDown(client)) continue

                val chest = player.getEquippedStack(EquipmentSlot.CHEST)
                if (chest.item is QuantumChestplate) {
                    ClientPlayNetworking.send(NetworkManager.TOGGLE_QUANTUM_FLIGHT_PACKET, PacketByteBuf(Unpooled.buffer()))
                }
            }
        }
    }

    private fun isAltDown(client: net.minecraft.client.MinecraftClient): Boolean {
        val window = client.window.handle
        return InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_ALT) ||
               InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_ALT)
    }

    // 暴露给tooltip使用的按键获取方法
    fun getVisionKey(): KeyBinding = toggleVisionKey
    fun getFlightKey(): KeyBinding = toggleFlightKey
}
