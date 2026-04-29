package ic2_120.client

import ic2_120.client.network.BandwidthHudState
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW

@Environment(EnvType.CLIENT)
object BandwidthHudKeybinds {
    private val toggleBandwidthHudKey = KeyBindingHelper.registerKeyBinding(
        KeyBinding(
            "key.ic2_120.toggle_bandwidth_hud",
            InputUtil.Type.KEYSYM,
            InputUtil.UNKNOWN_KEY.getCode(),
            "category.ic2_120.ic2"
        )
    )

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (toggleBandwidthHudKey.wasPressed()) {
                BandwidthHudState.enabled = !BandwidthHudState.enabled
                client.player?.sendMessage(
                    Text.literal("Bandwidth HUD: " + if (BandwidthHudState.enabled) "ON" else "OFF"),
                    true
                )
            }
        }
    }
}

