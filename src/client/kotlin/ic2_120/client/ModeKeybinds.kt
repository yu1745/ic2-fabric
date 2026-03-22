package ic2_120.client

import ic2_120.content.item.IridiumDrill
import ic2_120.content.item.ElectricJetpack
import ic2_120.content.item.armor.JetpackItem
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
 * 通用功能切换按键（默认 Alt+M）
 *
 * 用于手持类设备的模式切换：
 * - 夜视仪：夜视开关
 * - 铱钻头：精准采集开关
 * - 采矿镭射等：模式切换（预留）
 *
 * 注：量子套夜视(Alt+N)、飞行(Alt+F) 使用 ArmorKeybinds，不共用 M。
 */
@Environment(EnvType.CLIENT)
object ModeKeybinds {
    private val toggleModeKey = KeyBindingHelper.registerKeyBinding(
        KeyBinding(
            "key.ic2_120.toggle_mode",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "category.ic2_120.ic2"
        )
    )

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val player = client.player ?: return@register

            while (toggleModeKey.wasPressed()) {
                if (!isAltDown(client)) continue

                // 优先级 1：胸甲 → 喷气背包飞行开关
                val chest = player.getEquippedStack(EquipmentSlot.CHEST)
                if (chest.item is JetpackItem || chest.item is ElectricJetpack) {
                    ClientPlayNetworking.send(
                        NetworkManager.TOGGLE_JETPACK_FLIGHT_PACKET,
                        PacketByteBuf(Unpooled.buffer())
                    )
                    return@register
                }

                // 优先级 2：主手铱钻头 → 精准采集
                val mainHand = player.mainHandStack
                if (mainHand.item is IridiumDrill) {
                    ClientPlayNetworking.send(
                        NetworkManager.TOGGLE_IRIDIUM_SILK_TOUCH_PACKET,
                        PacketByteBuf(Unpooled.buffer())
                    )
                    return@register
                }
            }
        }
    }

    private fun isAltDown(client: net.minecraft.client.MinecraftClient): Boolean {
        val window = client.window.handle
        return InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_ALT) ||
               InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_ALT)
    }

    /** 供 tooltip 动态显示快捷键 */
    fun getModeKey(): KeyBinding = toggleModeKey
}
