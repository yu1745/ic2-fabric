package ic2_120.client.network


import ic2_120.Ic2_120
import ic2_120.content.network.ReactorHeatInfoPacket
import ic2_120.content.network.ReactorLayoutLockPacket
import ic2_120.content.network.ScannerResultPacket
import ic2_120.content.network.TeleporterVisualStatePacket
import ic2_120.content.network.ConfigSyncPacket
import ic2_120.content.network.ConfigSyncReceiver
import ic2_120.content.network.ReplicationCostsSyncReceiver
import ic2_120.content.network.WindRotorStatePacket
import ic2_120.content.network.WaterRotorStatePacket
import ic2_120.content.network.SemifluidGeneratorFuelStatePacket

import ic2_120.client.screen.ScannerScreen
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
private val minecraft = net.minecraft.client.Minecraft.getInstance()

object NetworkManager {
    private val REACTOR_HEAT_INFO_PACKET = ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "reactor_heat_info")
    private val REACTOR_LAYOUT_LOCK_PACKET = ResourceLocation.fromNamespaceAndPath(Ic2_120.MOD_ID, "reactor_layout_lock")
    private val WIND_ROTOR_STATE_PACKET = WindRotorStatePacket.ID
    private val WATER_ROTOR_STATE_PACKET = WaterRotorStatePacket.ID
    
    private val SCANNER_RESULT_PACKET = ScannerResultPacket.ID
    private val TELEPORTER_VISUAL_STATE_PACKET = TeleporterVisualStatePacket.ID
    private val CONFIG_SYNC_PACKET = ConfigSyncPacket.ID
    private val REPLICATION_COSTS_SYNC_PACKET = ConfigSyncPacket.REPLICATION_COSTS_ID
    private val SEMIFLUID_GENERATOR_FUEL_STATE_PACKET = SemifluidGeneratorFuelStatePacket.ID

    fun register() {
        // 注册客户端接收处理器
        ClientPlayNetworking.registerGlobalReceiver(REACTOR_HEAT_INFO_PACKET) { payload, context ->

        val packet = payload
            minecraft.execute {
                val blockEntity = minecraft.level?.getBlockEntity(packet.pos)
                if (blockEntity is ic2_120.content.block.nuclear.NuclearReactorBlockEntity) {
                    blockEntity.slotHeatInfo.clear()
                    blockEntity.slotHeatInfo.putAll(packet.slotHeatInfo)
                }
            }
        }

        ClientPlayNetworking.registerGlobalReceiver(REACTOR_LAYOUT_LOCK_PACKET) { payload, context ->

        val packet = payload
            minecraft.execute {
                val be = minecraft.level?.getBlockEntity(packet.pos)
                if (be is ic2_120.content.block.nuclear.NuclearReactorBlockEntity) {
                    be.lockedSlots.clear()
                    be.lockedSlots.putAll(packet.lockedSlots)
                }
            }
        }

        // 注册风力发电机转子状态接收处理器
        ClientPlayNetworking.registerGlobalReceiver(WIND_ROTOR_STATE_PACKET) { payload, context ->

        val packet = payload
            minecraft.execute {
                val blockEntity = minecraft.level?.getBlockEntity(packet.pos)
                if (blockEntity is ic2_120.content.block.machines.WindKineticGeneratorBlockEntity) {
                    blockEntity.receiveRotorState(packet.isStuck, packet.stuckAngle)
                }
            }
        }

        // 注册水力发电机转子状态接收处理器
        ClientPlayNetworking.registerGlobalReceiver(WATER_ROTOR_STATE_PACKET) { payload, context ->

        val packet = payload
            minecraft.execute {
                val blockEntity = minecraft.level?.getBlockEntity(packet.pos)
                if (blockEntity is ic2_120.content.block.machines.WaterKineticGeneratorBlockEntity) {
                    blockEntity.receiveRotorState(packet.isStuck, packet.stuckAngle)
                }
            }
        }

        

        // 注册扫描结果 S2C 包
        ClientPlayNetworking.registerGlobalReceiver(SCANNER_RESULT_PACKET) { payload, context ->

        val packet = payload
            minecraft.execute {
                ScannerScreen.receiveResults(packet)
            }
        }

        ClientPlayNetworking.registerGlobalReceiver(TELEPORTER_VISUAL_STATE_PACKET) { payload, context ->

        val packet = payload
            minecraft.execute {
                val be = minecraft.level?.getBlockEntity(packet.pos)
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

        ClientPlayNetworking.registerGlobalReceiver(CONFIG_SYNC_PACKET) { payload, context ->

        val packet = payload
            minecraft.execute {
                ConfigSyncReceiver.accept(packet)
            }
        }

        ClientPlayNetworking.registerGlobalReceiver(REPLICATION_COSTS_SYNC_PACKET) { payload, context ->

        val packet = payload
            minecraft.execute {
                ReplicationCostsSyncReceiver.accept(packet)
            }
        }

        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            ic2_120.config.Ic2Config.clearServerReplicationCosts()
        }

        ClientPlayNetworking.registerGlobalReceiver(SEMIFLUID_GENERATOR_FUEL_STATE_PACKET) { payload, context ->

        val packet = payload
            minecraft.execute {
                val be = minecraft.level?.getBlockEntity(packet.pos)
                if (be is ic2_120.content.block.machines.SemifluidGeneratorBlockEntity) {
                    be.clientFuelColorArgb = packet.fuelColorArgb
                }
            }
        }
    }
}
