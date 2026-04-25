package ic2_120.content.network

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
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.entity.EquipmentSlot
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Hand

object NetworkManager {
    fun register() {
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
    }

    fun sendToClient(player: ServerPlayerEntity, packet: ReactorHeatInfoPacket) {
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
}
