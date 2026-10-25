package ic2_120.compat

import ic2_120.compat.displayClientMessage

import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player

fun Player.displayClientMessage(message: Component, overlay: Boolean) {
    if (overlay) sendOverlayMessage(message) else sendSystemMessage(message)
}
