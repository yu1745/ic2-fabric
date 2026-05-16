package ic2_120.content.network

import ic2_120.Ic2_120
import ic2_120.content.block.machines.PatternStorageBlockEntity
import ic2_120.content.block.machines.ReplicatorBlockEntity
import ic2_120.content.item.FoamSprayerItem
import ic2_120.content.item.IridiumDrill
import ic2_120.content.item.MiningLaserItem
import ic2_120.content.item.MiningLaserServerSuppress
import ic2_120.content.item.NightVisionGoggles
import ic2_120.content.item.ElectricJetpack
import ic2_120.content.item.armor.JetpackItem
import ic2_120.content.item.armor.NanoHelmet
import ic2_120.content.item.armor.QuantumChestplate
import ic2_120.content.item.armor.QuantumHelmet
import ic2_120.content.item.armor.QuantumLeggings
import ic2_120.content.item.armor.QuantumBoots
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.entity.EquipmentSlot
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.server.world.ServerWorld
import net.minecraft.network.PacketByteBuf
import io.netty.buffer.Unpooled

object NetworkManager {
    private val REACTOR_HEAT_INFO_PACKET = Identifier.of(Ic2_120.MOD_ID, "reactor_heat_info")
    private val WIND_ROTOR_STATE_PACKET = Identifier.of(Ic2_120.MOD_ID, "wind_rotor_state")
    private val WATER_ROTOR_STATE_PACKET = Identifier.of(Ic2_120.MOD_ID, "water_rotor_state")
    private val REACTOR_LAYOUT_LOCK_PACKET = Identifier.of(Ic2_120.MOD_ID, "reactor_layout_lock")

    private val TELEPORTER_VISUAL_STATE_PACKET = TeleporterVisualStatePacket.ID
    val TOGGLE_NIGHT_VISION_GOGGLES_PACKET = Identifier.of(Ic2_120.MOD_ID, "toggle_night_vision_goggles")
    val TOGGLE_NANO_VISION_PACKET = Identifier.of(Ic2_120.MOD_ID, "toggle_nano_vision")
    val TOGGLE_QUANTUM_FLIGHT_PACKET = Identifier.of(Ic2_120.MOD_ID, "toggle_quantum_flight")
    val TOGGLE_IRIDIUM_SILK_TOUCH_PACKET = Identifier.of(Ic2_120.MOD_ID, "toggle_iridium_silk_touch")
    val TOGGLE_JETPACK_FLIGHT_PACKET = Identifier.of(Ic2_120.MOD_ID, "toggle_jetpack_flight")
    val TOGGLE_FOAM_SPRAYER_MODE_PACKET = Identifier.of(Ic2_120.MOD_ID, "toggle_foam_sprayer_mode")
    val TOGGLE_MINING_LASER_MODE_PACKET = Identifier.of(Ic2_120.MOD_ID, "toggle_mining_laser_mode")
    val TOGGLE_QUANTUM_LEGGINGS_SPEED_PACKET = Identifier.of(Ic2_120.MOD_ID, "toggle_quantum_leggings_speed")
    val TOGGLE_QUANTUM_BOOTS_JUMP_PACKET = Identifier.of(Ic2_120.MOD_ID, "toggle_quantum_boots_jump")

    fun register() {
        registerPayloadTypes()

        ServerPlayNetworking.registerGlobalReceiver(ToggleNightVisionGogglesPayload.ID) { _, context ->
            val player = context.player()
            val stack = player.getEquippedStack(EquipmentSlot.HEAD)
            val goggles = stack.item as? NightVisionGoggles ?: return@registerGlobalReceiver
            val enabled = NightVisionGoggles.toggleEnabled(stack)
            val key = if (enabled) "message.ic2_120.night_vision_goggles.enabled" else "message.ic2_120.night_vision_goggles.disabled"
            player.sendMessage(Text.translatable(key), true)
        }

        ServerPlayNetworking.registerGlobalReceiver(ToggleNanoVisionPayload.ID) { _, context ->
            val player = context.player()
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

        ServerPlayNetworking.registerGlobalReceiver(ToggleQuantumFlightPayload.ID) { _, context ->
            val player = context.player()
            val stack = player.getEquippedStack(EquipmentSlot.CHEST)
            if (stack.item is QuantumChestplate) {
                val enabled = QuantumChestplate.toggleFlight(stack)
                player.sendMessage(Text.translatable(
                    if (enabled) "message.ic2_120.quantum_chestplate.flight_on" else "message.ic2_120.quantum_chestplate.flight_off"
                ), true)
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(ToggleQuantumLeggingsSpeedPayload.ID) { _, context ->
            val player = context.player()
            val stack = player.getEquippedStack(EquipmentSlot.LEGS)
            if (stack.item is QuantumLeggings) {
                val tier = QuantumLeggings.cycleSpeedTier(stack)
                player.sendMessage(Text.translatable(
                    when (tier) {
                        1 -> "message.ic2_120.quantum_leggings.speed_tier1"
                        2 -> "message.ic2_120.quantum_leggings.speed_tier2"
                        else -> "message.ic2_120.quantum_leggings.speed_off"
                    }
                ), true)
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(ToggleQuantumBootsJumpPayload.ID) { _, context ->
            val player = context.player()
            val stack = player.getEquippedStack(EquipmentSlot.FEET)
            if (stack.item is QuantumBoots) {
                val enabled = QuantumBoots.toggleSuperJump(stack)
                player.sendMessage(Text.translatable(
                    if (enabled) "message.ic2_120.quantum_boots.jump_on" else "message.ic2_120.quantum_boots.jump_off"
                ), true)
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(ToggleIridiumSilkTouchPayload.ID) { _, context ->
            val player = context.player()
            val stack = player.mainHandStack
            if (stack.item is IridiumDrill) {
                val enabled = IridiumDrill.toggleSilkTouch(stack)
                player.sendMessage(Text.translatable(
                    if (enabled) "message.ic2_120.iridium_drill.silk_touch_on" else "message.ic2_120.iridium_drill.silk_touch_off"
                ), true)
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(ToggleJetpackFlightPayload.ID) { _, context ->
            val player = context.player()
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

        ServerPlayNetworking.registerGlobalReceiver(ToggleFoamSprayerModePayload.ID) { _, context ->
            val player = context.player()
            for (hand in arrayOf(Hand.MAIN_HAND, Hand.OFF_HAND)) {
                val stack = player.getStackInHand(hand)
                if (stack.item is FoamSprayerItem) {
                    val multi = FoamSprayerItem.toggleMultiMode(stack)
                    player.setStackInHand(hand, stack)
                    player.sendMessage(
                        Text.translatable(
                            if (multi) "message.ic2_120.foam_sprayer.mode_multi" else "message.ic2_120.foam_sprayer.mode_single"
                        ),
                        true
                    )
                    return@registerGlobalReceiver
                }
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(ToggleMiningLaserModePayload.ID) { _, context ->
            val player = context.player()
            for (hand in arrayOf(Hand.MAIN_HAND, Hand.OFF_HAND)) {
                val stack = player.getStackInHand(hand)
                if (stack.item is MiningLaserItem) {
                    MiningLaserServerSuppress.onMiningLaserModeToggled(player)
                    val newMode = MiningLaserItem.cycleMode(stack)
                    player.setStackInHand(hand, stack)
                    player.sendMessage(
                        Text.translatable(newMode.translationKey),
                        true
                    )
                    return@registerGlobalReceiver
                }
            }
        }

        // 模板选择 C2S 包：绕过 ButtonClickC2SPacket 的 buttonId byte 限制
        ServerPlayNetworking.registerGlobalReceiver(SelectTemplatePayload.ID) { payload, context ->
            val player = context.player()
            val be = player.world.getBlockEntity(payload.pos)
            when (be) {
                is ReplicatorBlockEntity -> be.selectTemplate(payload.index)
                is PatternStorageBlockEntity -> be.selectTemplate(payload.index)
            }
        }
    }

    fun sendToClient(player: ServerPlayerEntity, packet: ReactorHeatInfoPacket) {
        ServerPlayNetworking.send(player, packet)
    }

    fun sendToClient(player: ServerPlayerEntity, packet: ReactorLayoutLockPacket) {
        ServerPlayNetworking.send(player, packet)
    }

    fun sendWindRotorStateToNearby(world: net.minecraft.world.World, pos: net.minecraft.util.math.BlockPos, isStuck: Boolean, stuckAngle: Float) {
        if (world.isClient) return
        val serverWorld = world as net.minecraft.server.world.ServerWorld
        val packet = WindRotorStatePacket(pos, isStuck, stuckAngle)
        for (player in serverWorld.players) {
            if (player.squaredDistanceTo(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()) < 64 * 64) {
                ServerPlayNetworking.send(player, packet)
            }
        }
    }

    fun sendTeleporterVisualStateToNearby(
        world: net.minecraft.world.World,
        pos: net.minecraft.util.math.BlockPos,
        be: ic2_120.content.block.machines.TeleporterBlockEntity
    ) {
        if (world.isClient) return
        val serverWorld = world as net.minecraft.server.world.ServerWorld
        val packet = TeleporterVisualStatePacket(
            pos = pos,
            charging = be.sync.charging != 0,
            chargeProgress = be.sync.chargeProgress.coerceAtLeast(0),
            chargeMax = be.sync.chargeMax.coerceAtLeast(0),
            teleportRange = be.getTeleportRange(),
            chargingEntityId = be.getChargingEntityId()
        )
        for (player in serverWorld.players) {
            if (player.squaredDistanceTo(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()) < 64 * 64) {
                ServerPlayNetworking.send(player, packet)
            }
        }
    }

    fun sendWaterRotorStateToNearby(world: net.minecraft.world.World, pos: net.minecraft.util.math.BlockPos, isStuck: Boolean, stuckAngle: Float) {
        if (world.isClient) return
        val serverWorld = world as net.minecraft.server.world.ServerWorld
        val packet = WaterRotorStatePacket(pos, isStuck, stuckAngle)
        for (player in serverWorld.players) {
            if (player.squaredDistanceTo(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()) < 64 * 64) {
                ServerPlayNetworking.send(player, packet)
            }
        }
    }

    private fun registerPayloadTypes() {
        // C2S toggle payloads
        PayloadTypeRegistry.playC2S().register(ToggleNightVisionGogglesPayload.ID, ToggleNightVisionGogglesPayload.CODEC)
        PayloadTypeRegistry.playC2S().register(ToggleNanoVisionPayload.ID, ToggleNanoVisionPayload.CODEC)
        PayloadTypeRegistry.playC2S().register(ToggleQuantumFlightPayload.ID, ToggleQuantumFlightPayload.CODEC)
        PayloadTypeRegistry.playC2S().register(ToggleQuantumLeggingsSpeedPayload.ID, ToggleQuantumLeggingsSpeedPayload.CODEC)
        PayloadTypeRegistry.playC2S().register(ToggleQuantumBootsJumpPayload.ID, ToggleQuantumBootsJumpPayload.CODEC)
        PayloadTypeRegistry.playC2S().register(ToggleIridiumSilkTouchPayload.ID, ToggleIridiumSilkTouchPayload.CODEC)
        PayloadTypeRegistry.playC2S().register(ToggleJetpackFlightPayload.ID, ToggleJetpackFlightPayload.CODEC)
        PayloadTypeRegistry.playC2S().register(ToggleFoamSprayerModePayload.ID, ToggleFoamSprayerModePayload.CODEC)
        PayloadTypeRegistry.playC2S().register(ToggleMiningLaserModePayload.ID, ToggleMiningLaserModePayload.CODEC)
        PayloadTypeRegistry.playC2S().register(SelectTemplatePayload.ID, SelectTemplatePayload.CODEC)

        // S2C sync payloads
        PayloadTypeRegistry.playS2C().register(ReactorHeatInfoPacket.ID, ReactorHeatInfoPacket.CODEC)
        PayloadTypeRegistry.playS2C().register(BandwidthHudPacket.ID, BandwidthHudPacket.CODEC)
        PayloadTypeRegistry.playS2C().register(WindRotorStatePacket.ID, WindRotorStatePacket.CODEC)
        PayloadTypeRegistry.playS2C().register(WaterRotorStatePacket.ID, WaterRotorStatePacket.CODEC)
        PayloadTypeRegistry.playS2C().register(ScannerResultPacket.ID, ScannerResultPacket.CODEC)
        PayloadTypeRegistry.playS2C().register(TeleporterVisualStatePacket.ID, TeleporterVisualStatePacket.CODEC)
        PayloadTypeRegistry.playS2C().register(ConfigSyncPacket.ID, ConfigSyncPacket.CODEC)
        PayloadTypeRegistry.playS2C().register(SemifluidGeneratorFuelStatePacket.ID, SemifluidGeneratorFuelStatePacket.CODEC)
        PayloadTypeRegistry.playS2C().register(ReactorLayoutLockPacket.ID, ReactorLayoutLockPacket.CODEC)
    }

    fun sendSemifluidGeneratorFuelState(world: ServerWorld, pos: BlockPos, fuelColorArgb: Int) {
        val packet = SemifluidGeneratorFuelStatePacket(pos, fuelColorArgb)
        for (player in world.players) {
            if (player.squaredDistanceTo(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()) < 64 * 64) {
                ServerPlayNetworking.send(player, packet)
            }
        }
    }
}
