package ic2_120.client.network

import ic2_120.Ic2_120
import ic2_120.content.network.ReactorHeatInfoPacket
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.MinecraftClient
import net.minecraft.util.Identifier

object NetworkManager {
    private val REACTOR_HEAT_INFO_PACKET = Identifier(Ic2_120.MOD_ID, "reactor_heat_info")

    fun register() {
        // 注册客户端接收处理器
        ClientPlayNetworking.registerGlobalReceiver(REACTOR_HEAT_INFO_PACKET) { client, handler, buf, responseSender ->
            val packet = ReactorHeatInfoPacket.read(buf)
            client.execute { 
                // 处理客户端接收到的数据包
                val blockEntity = client.world?.getBlockEntity(packet.pos)
                if (blockEntity is ic2_120.content.block.machines.NuclearReactorBlockEntity) {
                    blockEntity.slotHeatInfo.clear()
                    blockEntity.slotHeatInfo.putAll(packet.slotHeatInfo)
                }
            }
        }
    }
}