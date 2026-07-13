package ic2_120.client

import ic2_120.content.item.armor.QuantumLeggings
import ic2_120.content.network.NetworkManager
import io.netty.buffer.Unpooled
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.entity.EquipmentSlot
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.math.Vec3d

/**
 * 在客户端复刻原版 IC2 量子护腿的定向推进。
 *
 * 玩家移动由客户端预测，因此推进在客户端执行；每次实际推进后通知服务端验证并扣电。
 */
@Environment(EnvType.CLIENT)
object QuantumLeggingsSpeedController {
    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val player = client.player ?: return@register
            if (player.hasVehicle() || player.abilities.flying || player.isSpectator) return@register

            val stack = player.getEquippedStack(EquipmentSlot.LEGS)
            val leggings = stack.item as? QuantumLeggings ?: return@register
            val tier = QuantumLeggings.getSpeedTier(stack)
            if (tier <= 0 || leggings.getEnergy(stack) <= 0L) return@register
            if (!player.input.pressingForward) return@register

            val inWater = player.isTouchingWater
            if (!inWater && !player.isOnGround) return@register

            val boost = if (inWater) {
                QuantumLeggings.getWaterBoost(tier)
            } else {
                QuantumLeggings.getGroundBoost(tier)
            }
            if (boost <= 0.0) return@register

            player.updateVelocity(boost.toFloat(), Vec3d(0.0, 0.0, 1.0))
            if (inWater && player.input.jumping) {
                player.velocity = player.velocity.add(0.0, boost, 0.0)
            }
            ClientPlayNetworking.send(
                NetworkManager.QUANTUM_LEGGINGS_SPEED_TICK_PACKET,
                PacketByteBuf(Unpooled.buffer())
            )
        }
    }
}
