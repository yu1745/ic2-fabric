package ic2_120.client

import ic2_120.content.item.FoamSprayerItem
import ic2_120.content.item.ElectricJetpack
import ic2_120.content.item.armor.JetpackItem
import ic2_120.content.network.ToggleJetpackFlightPayload
import ic2_120.content.network.ToggleFoamSprayerModePayload
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
 * 通用功能切换键（默认 M，可在控制中改键）。
 *
 * - 喷气背包 / 泡沫喷枪：按住 **Alt** 并**按下**此键（边沿触发）切换（见 [register] 内 ClientTickEvents）。
 * - 采矿镭射枪：**按住**此键并**右键**切换模式（见 [MiningLaserModeHandler]）。
 *
 * 注：量子套夜视(Alt+N)、飞行(Alt+F) 使用 ArmorKeybinds，不共用本键。
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
                    ClientPlayNetworking.send(ToggleJetpackFlightPayload)
                    return@register
                }

                val offHand = player.offHandStack
                val mainHand = player.mainHandStack
                if (mainHand.item is FoamSprayerItem || offHand.item is FoamSprayerItem) {
                    ClientPlayNetworking.send(ToggleFoamSprayerModePayload)
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

    fun isModeKeyDown(): Boolean = toggleModeKey.isPressed
}
