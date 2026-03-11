package ic2_120.content.network

import ic2_120.Ic2_120
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier

object NetworkManager {
    private val REACTOR_HEAT_INFO_PACKET = Identifier(Ic2_120.MOD_ID, "reactor_heat_info")

    fun register() {
        // 注册服务端接收处理器（如果需要）
        ServerPlayNetworking.registerGlobalReceiver(REACTOR_HEAT_INFO_PACKET) { server, player, handler, buf, responseSender ->
            val packet = ReactorHeatInfoPacket.read(buf)
            server.execute { 
                // 处理服务端接收到的数据包（如果需要）
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