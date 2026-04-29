package ic2_120.content.player

import ic2_120.content.item.ElectricJetpack
import ic2_120.content.item.armor.JetpackItem
import ic2_120.content.item.armor.QuantumArmorItem
import ic2_120.content.item.armor.QuantumChestplate
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.server.MinecraftServer
import ic2_120.editCustomData
import ic2_120.getCustomData

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
        val fuel = JetpackItem.getFuel(jetpackStack)

        if (fuel <= 0L || player.isCreative || player.isSpectator || !JetpackItem.isFlightEnabled(jetpackStack)) {
            disableJetpackFlight(player, jetpackStack)
            return
        }
        if (player.isOnGround || player.isTouchingWater || player.isClimbing) {
            disableJetpackFlight(player, jetpackStack)
            return
        }

        JetpackItem.setFuel(jetpackStack, fuel - JetpackItem.fuelPerTick)
        enableJetpackFlight(player, jetpackStack)
    }

    private fun handleElectricJetpackFlight(player: PlayerEntity, jetpackStack: ItemStack) {
        val jetpack = jetpackStack.item as ElectricJetpack

        if (player.isCreative || player.isSpectator || !jetpack.isFlightEnabled(jetpackStack)) {
            disableJetpackFlight(player, jetpackStack)
            return
        }
        if (player.isOnGround || player.isTouchingWater || player.isClimbing) {
            disableJetpackFlight(player, jetpackStack)
            return
        }
        if (!jetpack.consumeFlightEnergyPerTick(jetpackStack)) {
            disableJetpackFlight(player, jetpackStack)
            return
        }

        enableJetpackFlight(player, jetpackStack)
    }

    private fun enableJetpackFlight(player: PlayerEntity, stack: ItemStack) {
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
        stack.editCustomData { it.putBoolean(JETPACK_HOVER_KEY, true) }
        jetpackGrantedPlayers.add(player.uuid)
    }

    private fun disableJetpackFlight(player: PlayerEntity, stack: ItemStack?) {
        val hovering = stack?.getCustomData()?.getBoolean(JETPACK_HOVER_KEY) == true
        if (!jetpackGrantedPlayers.contains(player.uuid) && !hovering) {
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

        stack?.editCustomData { it.putBoolean(JETPACK_HOVER_KEY, false) }
        jetpackGrantedPlayers.remove(player.uuid)
    }

    private fun handleQuantumFlight(player: PlayerEntity, chestStack: ItemStack) {
        if (!hasFullQuantumArmor(player)) {
            if (isQuantumFlightActive(player)) {
                disableQuantumFlight(player)
            }
            return
        }

        val chestplate = chestStack.item as? QuantumChestplate ?: return
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
        if (currentEnergy < QuantumChestplate.flightCostPerTick) {
            chestStack.editCustomData { it.putBoolean("QuantumFlightEnabled", false) }
            disableQuantumFlight(player)
            return
        }

        if (player.isOnGround || player.isTouchingWater || player.isClimbing) {
            if (isActive) {
                disableQuantumFlight(player)
            }
            return
        }

        chestplate.setEnergy(chestStack, currentEnergy - QuantumChestplate.flightCostPerTick)
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

    private fun hasFullQuantumArmor(player: PlayerEntity): Boolean {
        val armorSlots = arrayOf(
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
        )
        for (slot in armorSlots) {
            if (player.getEquippedStack(slot).item !is QuantumArmorItem) {
                return false
            }
        }
        return true
    }

    private fun isQuantumFlightActive(player: PlayerEntity): Boolean {
        return player.abilities.allowFlying && player.getEquippedStack(EquipmentSlot.CHEST).item is QuantumChestplate
    }
}
