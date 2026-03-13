package ic2_120.content.network

import ic2_120.Ic2_120
import ic2_120.content.item.NightVisionGoggles
import ic2_120.content.item.armor.NanoHelmet
import ic2_120.content.item.armor.QuantumChestplate
import ic2_120.content.item.armor.QuantumHelmet
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.PacketByteBuf
import net.minecraft.entity.EquipmentSlot
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier

object NetworkManager {
    private val REACTOR_HEAT_INFO_PACKET = Identifier(Ic2_120.MOD_ID, "reactor_heat_info")
    val TOGGLE_NIGHT_VISION_GOGGLES_PACKET = Identifier(Ic2_120.MOD_ID, "toggle_night_vision_goggles")
    val TOGGLE_NANO_VISION_PACKET = Identifier(Ic2_120.MOD_ID, "toggle_nano_vision")
    val TOGGLE_QUANTUM_FLIGHT_PACKET = Identifier(Ic2_120.MOD_ID, "toggle_quantum_flight")

    fun register() {
        // 注册服务端接收处理器（如果需要）
        ServerPlayNetworking.registerGlobalReceiver(REACTOR_HEAT_INFO_PACKET) { server, player, handler, buf, responseSender ->
            val packet = ReactorHeatInfoPacket.read(buf)
            server.execute {
                // 处理服务端接收到的数据包（如果需要）
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_NIGHT_VISION_GOGGLES_PACKET) { server, player, _, _, _ ->
            server.execute {
                val stack = player.getEquippedStack(EquipmentSlot.HEAD)
                val goggles = stack.item as? NightVisionGoggles ?: return@execute
                val enabled = NightVisionGoggles.toggleEnabled(stack)
                val key = if (enabled) "message.ic2_120.night_vision_goggles.enabled" else "message.ic2_120.night_vision_goggles.disabled"
                player.sendMessage(Text.translatable(key), true)
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_NANO_VISION_PACKET) { server, player, _, _, _ ->
            server.execute {
                val stack = player.getEquippedStack(EquipmentSlot.HEAD)
                when (val item = stack.item) {
                    is NanoHelmet -> {
                        val enabled = NanoHelmet.toggleNightVision(stack)
                        player.sendMessage(Text.translatable(
                            if (enabled) "message.ic2_120.nano_helmet.nv_on" else "message.ic2_120.nano_helmet.nv_off"
                        ), true)
                    }
                    is QuantumHelmet -> {
                        val enabled = QuantumHelmet.toggleNightVision(stack)
                        player.sendMessage(Text.translatable(
                            if (enabled) "message.ic2_120.quantum_helmet.nv_on" else "message.ic2_120.quantum_helmet.nv_off"
                        ), true)
                    }
                }
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_QUANTUM_FLIGHT_PACKET) { server, player, _, _, _ ->
            server.execute {
                val stack = player.getEquippedStack(EquipmentSlot.CHEST)
                if (stack.item is QuantumChestplate) {
                    val enabled = QuantumChestplate.toggleFlight(stack)
                    player.sendMessage(Text.translatable(
                        if (enabled) "message.ic2_120.quantum_chestplate.flight_on" else "message.ic2_120.quantum_chestplate.flight_off"
                    ), true)
                }
            }
        }
    }

    // 发送数据包到客户端
    fun sendToClient(player: ServerPlayerEntity, packet: ReactorHeatInfoPacket) {
        val buf = PacketByteBuf(Unpooled.buffer())
        ReactorHeatInfoPacket.write(packet, buf)
        ServerPlayNetworking.send(player, REACTOR_HEAT_INFO_PACKET, buf)
    }
}
