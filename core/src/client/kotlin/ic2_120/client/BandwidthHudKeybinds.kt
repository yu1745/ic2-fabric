package ic2_120.client

import ic2_120.compat.displayClientMessage

import ic2_120.client.network.BandwidthHudState
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import net.minecraft.client.Minecraft

@Environment(EnvType.CLIENT)
object BandwidthHudKeybinds {
    private val toggleBandwidthHudKey = KeyMappingHelper.registerKeyMapping(
        KeyMapping(
            "key.ic2_120.toggle_bandwidth_hud",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.value,
            Ic2KeyCategory.category
        )
    )

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (toggleBandwidthHudKey.consumeClick()) {
                BandwidthHudState.enabled = !BandwidthHudState.enabled
                client.player?.displayClientMessage(
                    Component.literal("Bandwidth HUD: " + if (BandwidthHudState.enabled) "ON" else "OFF"),
                    true
                )
            }
        }
    }
}

