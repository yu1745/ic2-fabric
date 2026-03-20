package ic2_120.client.network

import ic2_120.Ic2_120
import ic2_120.content.network.ReactorHeatInfoPacket
import ic2_120.content.network.WindRotorStatePacket
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.MinecraftClient
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory

object NetworkManager {
    private val LOGGER = LoggerFactory.getLogger("ic2_120/WindKineticGenerator-Client")
    private val REACTOR_HEAT_INFO_PACKET = Identifier(Ic2_120.MOD_ID, "reactor_heat_info")
    private val WIND_ROTOR_STATE_PACKET = WindRotorStatePacket.ID

    fun register() {
        // 注册客户端接收处理器
        ClientPlayNetworking.registerGlobalReceiver(REACTOR_HEAT_INFO_PACKET) { client, handler, buf, responseSender ->
            val packet = ReactorHeatInfoPacket.read(buf)
            client.execute {
                // 处理客户端接收到的数据包
                val blockEntity = client.world?.getBlockEntity(packet.pos)
                if (blockEntity is ic2_120.content.block.nuclear.NuclearReactorBlockEntity) {
                    blockEntity.slotHeatInfo.clear()
                    blockEntity.slotHeatInfo.putAll(packet.slotHeatInfo)
                }
            }
        }

        // 注册风力发电机转子状态接收处理器
        ClientPlayNetworking.registerGlobalReceiver(WIND_ROTOR_STATE_PACKET) { client, handler, buf, responseSender ->
            val packet = WindRotorStatePacket.read(buf)
            LOGGER.info("[WindKineticGenerator Client] Network packet received: pos={} isStuck={} angle={}",
                packet.pos, packet.isStuck, packet.stuckAngle.toInt())
            client.execute {
                val blockEntity = client.world?.getBlockEntity(packet.pos)
                if (blockEntity is ic2_120.content.block.machines.WindKineticGeneratorBlockEntity) {
                    blockEntity.receiveRotorState(packet.isStuck, packet.stuckAngle)
                } else {
                    LOGGER.warn("[WindKineticGenerator Client] BlockEntity not found at {}", packet.pos)
                }
            }
        }
    }
}