package ic2_120.client

import ic2_120.content.item.FoamSprayerItem
import ic2_120.content.network.NetworkManager
import io.netty.buffer.Unpooled
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.network.PacketByteBuf
import org.lwjgl.glfw.GLFW

/**
 * 通用功能切换键（默认 M，可在控制中改键）。
 *
 * - 泡沫喷枪：按住 **Alt** 并**按下**此键（边沿触发）切换（见 [register] 内 ClientTickEvents）。
 * - 采矿镭射枪：**按住**此键并**右键**切换模式（见 [MiningLaserModeHandler]）。
 *
 * 飞行（量子胸甲/喷气背包）使用双击空格触发，见 [FlightDoubleTapHandler]。
 * 量子套夜视(Alt+N) 使用 ArmorKeybinds。
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

    /** 泡沫枪专用模式切换键，独立于通用 M 键，避免与其他装备冲突 */
    private val toggleFoamModeKey = KeyBindingHelper.registerKeyBinding(
        KeyBinding(
            "key.ic2_120.toggle_foam_sprayer_mode",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "category.ic2_120.ic2"
        )
    )

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val player = client.player ?: return@register

            // 泡沫枪专用键
            while (toggleFoamModeKey.wasPressed()) {
                if (!isAltDown(client)) continue
                val offHand = player.offHandStack
                val mainHand = player.mainHandStack
                if (mainHand.item is FoamSprayerItem || offHand.item is FoamSprayerItem) {
                    ClientPlayNetworking.send(
                        NetworkManager.TOGGLE_FOAM_SPRAYER_MODE_PACKET,
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

    /** 通用模式切换键（喷气背包、钻头、镭射枪等） */
    fun getModeKey(): KeyBinding = toggleModeKey

    /** 泡沫枪专用模式切换键 */
    fun getFoamModeKey(): KeyBinding = toggleFoamModeKey

    fun isModeKeyDown(): Boolean = toggleModeKey.isPressed
}
