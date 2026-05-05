package ic2_120.content.player

import ic2_120.content.item.ElectricJetpack
import ic2_120.content.item.armor.JetpackItem
import ic2_120.content.item.armor.QuantumChestplate
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.MinecraftServer

object FlightManager {
    private const val JETPACK_HOVER_KEY = "IsHover"
    private val jetpackGrantedPlayers = mutableSetOf<java.util.UUID>()

    fun tick(server: MinecraftServer) {
        for (world in server.worlds) {
            for (player in world.players) {
                tickPlayer(player)
            }
        }
    }

    private fun tickPlayer(player: PlayerEntity) {
        val chestStack = player.getEquippedStack(EquipmentSlot.CHEST)

        if (chestStack.item is JetpackItem) {
            handleJetpackFlight(player, chestStack)
            return
        }
        if (chestStack.item is ElectricJetpack) {
            handleElectricJetpackFlight(player, chestStack)
            return
        }
        disableJetpackFlight(player, null)
        handleQuantumFlight(player, chestStack)
    }

    private fun handleJetpackFlight(player: PlayerEntity, jetpackStack: ItemStack) {
        val nbt = jetpackStack.orCreateNbt

        if (player.isCreative || player.isSpectator || !JetpackItem.isFlightEnabled(jetpackStack)) {
            disableJetpackFlight(player, nbt)
            return
        }
        if (player.isOnGround || player.isTouchingWater || player.isClimbing) {
            disableJetpackFlight(player, nbt)
            return
        }

        if (!JetpackItem.consumeFuelPerTick(jetpackStack)) {
            disableJetpackFlight(player, nbt)
            return
        }
        enableJetpackFlight(player, nbt)
    }

    private fun handleElectricJetpackFlight(player: PlayerEntity, jetpackStack: ItemStack) {
        val jetpack = jetpackStack.item as ElectricJetpack
        val nbt = jetpackStack.orCreateNbt

        if (player.isCreative || player.isSpectator || !jetpack.isFlightEnabled(jetpackStack)) {
            disableJetpackFlight(player, nbt)
            return
        }
        if (player.isOnGround || player.isTouchingWater || player.isClimbing) {
            disableJetpackFlight(player, nbt)
            return
        }
        if (!jetpack.consumeFlightEnergyPerTick(jetpackStack)) {
            disableJetpackFlight(player, nbt)
            return
        }

        enableJetpackFlight(player, nbt)
    }

    private fun enableJetpackFlight(player: PlayerEntity, nbt: NbtCompound) {
        var changed = false
        if (!player.abilities.allowFlying) {
            player.abilities.allowFlying = true
            changed = true
        }
        if (!player.abilities.flying) {
            player.abilities.flying = true
            changed = true
        }
        if (changed) {
            player.sendAbilitiesUpdate()
        }
        nbt.putBoolean(JETPACK_HOVER_KEY, true)
        jetpackGrantedPlayers.add(player.uuid)
    }

    private fun disableJetpackFlight(player: PlayerEntity, nbt: NbtCompound?) {
        if (!jetpackGrantedPlayers.contains(player.uuid) && (nbt == null || !nbt.getBoolean(JETPACK_HOVER_KEY))) {
            return
        }

        if (!player.isCreative && !player.isSpectator) {
            var changed = false
            if (player.abilities.flying) {
                player.abilities.flying = false
                changed = true
            }
            if (player.abilities.allowFlying) {
                player.abilities.allowFlying = false
                changed = true
            }
            if (changed) {
                player.sendAbilitiesUpdate()
            }
        }

        nbt?.putBoolean(JETPACK_HOVER_KEY, false)
        jetpackGrantedPlayers.remove(player.uuid)
    }

    private fun handleQuantumFlight(player: PlayerEntity, chestStack: ItemStack) {
        val chestplate = chestStack.item as? QuantumChestplate ?: return
        val nbt = chestStack.orCreateNbt
        val flightEnabled = QuantumChestplate.isFlightEnabled(chestStack)
        val isActive = player.abilities.allowFlying

        if (player.isCreative || player.isSpectator || (player.abilities.flying && !isActive)) {
            return
        }

        if (!flightEnabled) {
            if (isActive) {
                disableQuantumFlight(player)
            }
            return
        }

        val currentEnergy = chestplate.getEnergy(chestStack)
        if (currentEnergy <= 0) {
            nbt.putBoolean("QuantumFlightEnabled", false)
            disableQuantumFlight(player)
            return
        }

        if (player.isOnGround || player.isTouchingWater || player.isClimbing) {
            if (isActive) {
                disableQuantumFlight(player)
            }
            return
        }

        if (!QuantumChestplate.consumeFlightEnergyPerTick(chestStack)) {
            nbt.putBoolean("QuantumFlightEnabled", false)
            disableQuantumFlight(player)
            return
        }
        if (!player.abilities.allowFlying || !player.abilities.flying) {
            player.abilities.allowFlying = true
            player.abilities.flying = true
            player.sendAbilitiesUpdate()
        }
    }

    private fun disableQuantumFlight(player: PlayerEntity) {
        if (!player.isCreative && !player.isSpectator) {
            var changed = false
            if (player.abilities.flying) {
                player.abilities.flying = false
                changed = true
            }
            if (player.abilities.allowFlying) {
                player.abilities.allowFlying = false
                changed = true
            }
            if (changed) {
                player.sendAbilitiesUpdate()
            }
        }
    }
}