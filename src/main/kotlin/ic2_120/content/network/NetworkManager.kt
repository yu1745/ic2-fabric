package ic2_120.content.network

import ic2_120.Ic2_120
import ic2_120.content.item.IridiumDrill
import ic2_120.content.item.NightVisionGoggles
import ic2_120.content.item.ElectricJetpack
import ic2_120.content.item.armor.JetpackItem
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
    private val WIND_ROTOR_STATE_PACKET = Identifier(Ic2_120.MOD_ID, "wind_rotor_state")
    val TOGGLE_NIGHT_VISION_GOGGLES_PACKET = Identifier(Ic2_120.MOD_ID, "toggle_night_vision_goggles")
    val TOGGLE_NANO_VISION_PACKET = Identifier(Ic2_120.MOD_ID, "toggle_nano_vision")
    val TOGGLE_QUANTUM_FLIGHT_PACKET = Identifier(Ic2_120.MOD_ID, "toggle_quantum_flight")
    val TOGGLE_IRIDIUM_SILK_TOUCH_PACKET = Identifier(Ic2_120.MOD_ID, "toggle_iridium_silk_touch")
    val TOGGLE_JETPACK_FLIGHT_PACKET = Identifier(Ic2_120.MOD_ID, "toggle_jetpack_flight")

    fun register() {
        // 注册服务端接收处理器（如果需要）
        ServerPlayNetworking.registerGlobalReceiver(REACTOR_HEAT_INFO_PACKET) { server, player, handler, buf, responseSender ->
            val packet = ReactorHeatInfoPacket.read(buf)
            server.execute {
                // 处理服务端接收到的数据包（如果需要）
            }
        }

        // 风力发电机转子状态包（仅 S2C，服务端不需要接收处理器）
        ServerPlayNetworking.registerGlobalReceiver(WIND_ROTOR_STATE_PACKET) { server, player, handler, buf, responseSender ->
            // 空实现，这个包只用于服务端发送到客户端
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

        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_IRIDIUM_SILK_TOUCH_PACKET) { server, player, _, _, _ ->
            server.execute {
                val stack = player.mainHandStack
                if (stack.item is IridiumDrill) {
                    val enabled = IridiumDrill.toggleSilkTouch(stack)
                    player.sendMessage(Text.translatable(
                        if (enabled) "message.ic2_120.iridium_drill.silk_touch_on" else "message.ic2_120.iridium_drill.silk_touch_off"
                    ), true)
                }
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_JETPACK_FLIGHT_PACKET) { server, player, _, _, _ ->
            server.execute {
                val stack = player.getEquippedStack(EquipmentSlot.CHEST)
                if (stack.item is JetpackItem || stack.item is ElectricJetpack) {
                    val enabled = when (val item = stack.item) {
                        is JetpackItem -> JetpackItem.toggleFlightEnabled(stack)
                        is ElectricJetpack -> item.toggleFlightEnabled(stack)
                        else -> false
                    }
                    val messageKey = if (enabled) {
                        "message.ic2_120.jetpack.flight_on"
                    } else {
                        "message.ic2_120.jetpack.flight_off"
                    }
                    player.sendMessage(Text.translatable(messageKey), true)
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

    // 发送风力发电机转子状态到附近玩家
    fun sendWindRotorStateToNearby(world: net.minecraft.world.World, pos: net.minecraft.util.math.BlockPos, isStuck: Boolean, stuckAngle: Float) {
        if (world.isClient) return

        val serverWorld = world as net.minecraft.server.world.ServerWorld
        for (player in serverWorld.players) {
            // 检查玩家是否在能看到这个方块的范围内（64格内）
            if (player.squaredDistanceTo(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()) < 64 * 64) {
                val buf = PacketByteBuf(Unpooled.buffer())
                WindRotorStatePacket.write(WindRotorStatePacket(pos, isStuck, stuckAngle), buf)
                ServerPlayNetworking.send(player, WindRotorStatePacket.ID, buf)
            }
        }
    }
}
