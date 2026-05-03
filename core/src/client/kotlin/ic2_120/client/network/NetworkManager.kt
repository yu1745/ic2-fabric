package ic2_120.client.network

import ic2_120.content.network.BandwidthHudPacket
import ic2_120.content.network.ReactorHeatInfoPacket
import ic2_120.content.network.ScannerResultPacket
import ic2_120.content.network.TeleporterVisualStatePacket
import ic2_120.content.network.ConfigSyncPacket
import ic2_120.content.network.ConfigSyncReceiver
import ic2_120.content.network.WindRotorStatePacket
import ic2_120.content.network.WaterRotorStatePacket

import ic2_120.client.screen.ScannerScreen
import ic2_120.content.block.machines.TeleporterBlockEntity
import ic2_120.content.block.machines.WaterKineticGeneratorBlockEntity
import ic2_120.content.block.machines.WindKineticGeneratorBlockEntity
import ic2_120.content.block.nuclear.NuclearReactorBlockEntity
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.MinecraftClient

object NetworkManager {
    private val REACTOR_HEAT_INFO_PACKET = Identifier(Ic2_120.MOD_ID, "reactor_heat_info")
    private val BANDWIDTH_HUD_PACKET = BandwidthHudPacket.ID
    private val WIND_ROTOR_STATE_PACKET = WindRotorStatePacket.ID
    private val WATER_ROTOR_STATE_PACKET = WaterRotorStatePacket.ID
    
    private val SCANNER_RESULT_PACKET = ScannerResultPacket.ID
    private val TELEPORTER_VISUAL_STATE_PACKET = TeleporterVisualStatePacket.ID
    private val CONFIG_SYNC_PACKET = ConfigSyncPacket.ID

>>>>>>> 59e897e (feat: JEI 配方显示(分子重组仪/复制机) + 配置全量分包同步)
    fun register() {
        ClientPlayNetworking.registerGlobalReceiver(ReactorHeatInfoPacket.ID) { payload, _ ->
            val blockEntity = MinecraftClient.getInstance().world?.getBlockEntity(payload.pos)
            if (blockEntity is NuclearReactorBlockEntity) {
                blockEntity.slotHeatInfo.clear()
                blockEntity.slotHeatInfo.putAll(payload.slotHeatInfo)
            }
        }

        ClientPlayNetworking.registerGlobalReceiver(BandwidthHudPacket.ID) { payload, _ ->
            BandwidthHudState.update(payload.serverBytesPerSecond, payload.players)
        }

        ClientPlayNetworking.registerGlobalReceiver(WindRotorStatePacket.ID) { payload, _ ->
            val blockEntity = MinecraftClient.getInstance().world?.getBlockEntity(payload.pos)
            if (blockEntity is WindKineticGeneratorBlockEntity) {
                blockEntity.receiveRotorState(payload.isStuck, payload.stuckAngle)
            }
        }

        ClientPlayNetworking.registerGlobalReceiver(WaterRotorStatePacket.ID) { payload, _ ->
            val blockEntity = MinecraftClient.getInstance().world?.getBlockEntity(payload.pos)
            if (blockEntity is WaterKineticGeneratorBlockEntity) {
                blockEntity.receiveRotorState(payload.isStuck, payload.stuckAngle)
            }
        }

        ClientPlayNetworking.registerGlobalReceiver(ScannerResultPacket.ID) { payload, _ ->
            ScannerScreen.receiveResults(payload)
        }

        ClientPlayNetworking.registerGlobalReceiver(TeleporterVisualStatePacket.ID) { payload, _ ->
            val be = MinecraftClient.getInstance().world?.getBlockEntity(payload.pos)
            if (be is TeleporterBlockEntity) {
                be.applyClientVisualState(
                    charging = payload.charging,
                    progress = payload.chargeProgress,
                    max = payload.chargeMax,
                    range = payload.teleportRange,
                    entityId = payload.chargingEntityId
                )
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
