package ic2_120.client

import ic2_120.content.item.ElectricJetpack
import ic2_120.content.item.armor.JetpackItem
import ic2_120.content.item.armor.QuantumChestplate
import ic2_120.content.network.ToggleJetpackFlightPayload
import ic2_120.content.network.ToggleQuantumFlightPayload
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.player.PlayerEntity

/**
 * 双击空格启动飞行（喷气背包 / 量子胸甲）
 *
 * 类似创造模式：单击空格跳跃，双击空格切换飞行。
 * 支持 [JetpackItem]、[ElectricJetpack] 和 [QuantumChestplate]。
 */
@Environment(EnvType.CLIENT)
object FlightDoubleTapHandler {
    private var wasJumpPressed = false
    private var lastJumpPressTime = 0L
    private const val DOUBLE_TAP_MS = 250L

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val player = client.player ?: return@register

            val isJumpPressed = client.options.jumpKey.isPressed
            val now = System.currentTimeMillis()

            if (isJumpPressed && !wasJumpPressed) {
                // 上升沿：空格键刚被按下
                if (now - lastJumpPressTime < DOUBLE_TAP_MS) {
                    // 双击！切换飞行
                    toggleFlight(player)
                }
                lastJumpPressTime = now
            }

            wasJumpPressed = isJumpPressed
        }
    }

    private fun toggleFlight(player: PlayerEntity) {
        val chest = player.getEquippedStack(EquipmentSlot.CHEST)
        val item = chest.item

        when {
            item is QuantumChestplate -> {
                ClientPlayNetworking.send(ToggleQuantumFlightPayload)
            }
            item is JetpackItem || item is ElectricJetpack -> {
                ClientPlayNetworking.send(ToggleJetpackFlightPayload)
            }
        }
    }
}
