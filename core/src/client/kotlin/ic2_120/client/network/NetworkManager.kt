package ic2_120.client.network

import ic2_120.content.network.BandwidthHudPacket
import ic2_120.content.network.ConfigSyncPacket
import ic2_120.content.network.ConfigSyncReceiver
import ic2_120.content.network.ReactorHeatInfoPacket
import ic2_120.content.network.ScannerResultPacket
import ic2_120.content.network.TeleporterVisualStatePacket
import ic2_120.content.network.WaterRotorStatePacket
import ic2_120.content.network.WindRotorStatePacket

import ic2_120.client.screen.ScannerScreen
import ic2_120.content.block.machines.TeleporterBlockEntity
import ic2_120.content.block.machines.WaterKineticGeneratorBlockEntity
import ic2_120.content.block.machines.WindKineticGeneratorBlockEntity
import ic2_120.content.block.nuclear.NuclearReactorBlockEntity
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.MinecraftClient

object NetworkManager {
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

        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPacket.ID) { payload, _ ->
            ConfigSyncReceiver.accept(payload)
        }
    }
}
