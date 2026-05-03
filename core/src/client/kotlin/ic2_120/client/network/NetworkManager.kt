package ic2_120.client.network

import ic2_120.Ic2_120
import ic2_120.content.network.BandwidthHudPacket
import ic2_120.content.network.ReactorHeatInfoPacket
import ic2_120.content.network.ScannerResultPacket
import ic2_120.content.network.TeleporterVisualStatePacket
import ic2_120.content.network.ConfigSyncPacket
import ic2_120.content.network.ConfigSyncReceiver
import ic2_120.content.network.WindRotorStatePacket
import ic2_120.content.network.WaterRotorStatePacket

import ic2_120.client.screen.ScannerScreen
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.MinecraftClient
import net.minecraft.util.Identifier

object NetworkManager {
    private val REACTOR_HEAT_INFO_PACKET = Identifier(Ic2_120.MOD_ID, "reactor_heat_info")
    private val BANDWIDTH_HUD_PACKET = BandwidthHudPacket.ID
    private val WIND_ROTOR_STATE_PACKET = WindRotorStatePacket.ID
    private val WATER_ROTOR_STATE_PACKET = WaterRotorStatePacket.ID
    
    private val SCANNER_RESULT_PACKET = ScannerResultPacket.ID
    private val TELEPORTER_VISUAL_STATE_PACKET = TeleporterVisualStatePacket.ID
    private val CONFIG_SYNC_PACKET = ConfigSyncPacket.ID

    fun register() {
        // 注册客户端接收处理器
        ClientPlayNetworking.registerGlobalReceiver(REACTOR_HEAT_INFO_PACKET) { client, handler, buf, responseSender ->
            val packet = ReactorHeatInfoPacket.read(buf)
            client.execute {
                val blockEntity = client.world?.getBlockEntity(packet.pos)
                if (blockEntity is ic2_120.content.block.nuclear.NuclearReactorBlockEntity) {
                    blockEntity.slotHeatInfo.clear()
                    blockEntity.slotHeatInfo.putAll(packet.slotHeatInfo)
                }
            }
        }

        ClientPlayNetworking.registerGlobalReceiver(BANDWIDTH_HUD_PACKET) { client, _, buf, _ ->
            val packet = BandwidthHudPacket.read(buf)
            client.execute {
                BandwidthHudState.update(packet.serverBytesPerSecond, packet.players)
            }
        }

        // 注册风力发电机转子状态接收处理器
        ClientPlayNetworking.registerGlobalReceiver(WIND_ROTOR_STATE_PACKET) { client, handler, buf, responseSender ->
            val packet = WindRotorStatePacket.read(buf)
            client.execute {
                val blockEntity = client.world?.getBlockEntity(packet.pos)
                if (blockEntity is ic2_120.content.block.machines.WindKineticGeneratorBlockEntity) {
                    blockEntity.receiveRotorState(packet.isStuck, packet.stuckAngle)
                }
            }
        }

        // 注册水力发电机转子状态接收处理器
        ClientPlayNetworking.registerGlobalReceiver(WATER_ROTOR_STATE_PACKET) { client, handler, buf, responseSender ->
            val packet = WaterRotorStatePacket.read(buf)
            client.execute {
                val blockEntity = client.world?.getBlockEntity(packet.pos)
                if (blockEntity is ic2_120.content.block.machines.WaterKineticGeneratorBlockEntity) {
                    blockEntity.receiveRotorState(packet.isStuck, packet.stuckAngle)
                }
            }
        }

        

        // 注册扫描结果 S2C 包
        ClientPlayNetworking.registerGlobalReceiver(SCANNER_RESULT_PACKET) { client, handler, buf, responseSender ->
            val packet = ScannerResultPacket.read(buf)
            client.execute {
                ScannerScreen.receiveResults(packet)
            }
        }

        ClientPlayNetworking.registerGlobalReceiver(TELEPORTER_VISUAL_STATE_PACKET) { client, _, buf, _ ->
            val packet = TeleporterVisualStatePacket.read(buf)
            client.execute {
                val be = client.world?.getBlockEntity(packet.pos)
                if (be is ic2_120.content.block.machines.TeleporterBlockEntity) {
                    be.applyClientVisualState(
                        charging = packet.charging,
                        progress = packet.chargeProgress,
                        max = packet.chargeMax,
                        range = packet.teleportRange,
                        entityId = packet.chargingEntityId
                    )
                }
            }
        }

        ClientPlayNetworking.registerGlobalReceiver(CONFIG_SYNC_PACKET) { client, _, buf, _ ->
            val packet = ConfigSyncPacket.read(buf)
            client.execute {
                ConfigSyncReceiver.accept(packet)
            }
        }
    }
}
