package ic2_120.content.player

import ic2_120.content.item.ElectricJetpack
import ic2_120.content.item.armor.JetpackItem
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
            // 从量子胸甲切换到喷气背包时，清理残留的量子飞行状态
            if (!player.isCreative && !player.isSpectator
                && player.abilities.allowFlying && !jetpackGrantedPlayers.contains(player.uuid)) {
                disableQuantumFlight(player)
            }
            handleJetpackFlight(player, chestStack)
            return
        }
        if (chestStack.item is ElectricJetpack) {
            if (!player.isCreative && !player.isSpectator
                && player.abilities.allowFlying && !jetpackGrantedPlayers.contains(player.uuid)) {
                disableQuantumFlight(player)
            }
            handleElectricJetpackFlight(player, chestStack)
            return
        }
        disableJetpackFlight(player, null)
        handleQuantumFlight(player, chestStack)
        // 不穿量子胸甲时清理残留的量子飞行状态（非创造/旁观模式）
        if (chestStack.item !is QuantumChestplate
            && !player.isCreative && !player.isSpectator) {
            disableQuantumFlight(player)
        }
    }

    private fun handleJetpackFlight(player: PlayerEntity, jetpackStack: ItemStack) {
        if (player.isCreative || player.isSpectator || !JetpackItem.isFlightEnabled(jetpackStack)) {
            disableJetpackFlight(player, jetpackStack)
            return
        }
        if (player.isOnGround || player.isTouchingWater || player.isClimbing
            || (jetpackGrantedPlayers.contains(player.uuid) && !player.abilities.flying)) {
            JetpackItem.setFlightEnabled(jetpackStack, false)
            disableJetpackFlight(player, jetpackStack)
            return
        }

        if (!JetpackItem.consumeFuelPerTick(jetpackStack)) {
            JetpackItem.setFlightEnabled(jetpackStack, false)
            disableJetpackFlight(player, jetpackStack)
            return
        }
        enableJetpackFlight(player, jetpackStack)
    }

    private fun handleElectricJetpackFlight(player: PlayerEntity, jetpackStack: ItemStack) {
        val jetpack = jetpackStack.item as ElectricJetpack

        if (player.isCreative || player.isSpectator || !jetpack.isFlightEnabled(jetpackStack)) {
            disableJetpackFlight(player, jetpackStack)
            return
        }
        if (player.isOnGround || player.isTouchingWater || player.isClimbing
            || (jetpackGrantedPlayers.contains(player.uuid) && !player.abilities.flying)) {
            jetpack.setFlightEnabled(jetpackStack, false)
            disableJetpackFlight(player, jetpackStack)
            return
        }
        if (!jetpack.consumeFlightEnergyPerTick(jetpackStack)) {
            jetpack.setFlightEnabled(jetpackStack, false)
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
        if (currentEnergy <= 0) {
            QuantumChestplate.setFlightEnabled(chestStack, false)
            disableQuantumFlight(player)
            return
        }

        // 落地/攀爬时自动关闭飞行。入水不关闭，允许从水里起飞。
        // 同时检测 flying 状态被游戏自动关闭的情况（飞行模式下落地时 isOnGround 可能滞后）。
        if (player.isOnGround || player.isClimbing
            || (isActive && !player.abilities.flying)) {
            QuantumChestplate.setFlightEnabled(chestStack, false)
            if (isActive) {
                disableQuantumFlight(player)
            }
            return
        }

        if (!QuantumChestplate.consumeFlightEnergyPerTick(chestStack)) {
            QuantumChestplate.setFlightEnabled(chestStack, false)
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