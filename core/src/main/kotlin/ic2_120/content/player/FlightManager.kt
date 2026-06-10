package ic2_120.content.player

import ic2_120.content.item.ElectricJetpack
import ic2_120.content.item.armor.JetpackItem
import ic2_120.content.item.armor.QuantumChestplate
import ic2_120.content.item.energy.IElectricTool
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.server.MinecraftServer
import java.util.UUID

/**
 * 喷气背包/电力喷气背包/量子胸甲的飞行管理（服务端 tick）。
 *
 * 服务端只根据胸甲是否可飞行来授予或剥夺 vanilla 飞行权限；双击空格、
 * 起飞/停止等行为交给原版 PlayerAbilities 流程处理。
 */
object FlightManager {
    private val abilitySnapshots = mutableMapOf<UUID, FlightAbilitySnapshot>()

    fun tick(server: MinecraftServer) {
        for (world in server.worlds) {
            for (player in world.players) {
                tickPlayer(player)
            }
        }
    }

    private fun tickPlayer(player: PlayerEntity) {
        if (player.isCreative || player.isSpectator) {
            abilitySnapshots.remove(player.uuid)
            return
        }

        val chest = player.getEquippedStack(EquipmentSlot.CHEST)
        val source = flightSource(chest)
        if (source == null || !source.hasEnergy(chest)) {
            restorePreviousFlightState(player)
            return
        }

        grantFlightPermission(player)

        if (player.abilities.flying) {
            source.consume(chest)
        }
    }

    private fun grantFlightPermission(player: PlayerEntity) {
        abilitySnapshots.getOrPut(player.uuid) {
            FlightAbilitySnapshot(
                allowFlying = player.abilities.allowFlying,
                flying = player.abilities.flying
            )
        }

        if (player.abilities.allowFlying) return

        player.abilities.allowFlying = true
        player.sendAbilitiesUpdate()
    }

    private fun restorePreviousFlightState(player: PlayerEntity) {
        val snapshot = abilitySnapshots.remove(player.uuid) ?: return

        var changed = false
        if (player.abilities.flying != snapshot.flying) {
            player.abilities.flying = snapshot.flying
            changed = true
        }
        if (player.abilities.allowFlying != snapshot.allowFlying) {
            player.abilities.allowFlying = snapshot.allowFlying
            changed = true
        }
        if (changed) {
            player.sendAbilitiesUpdate()
        }
    }

    private data class FlightAbilitySnapshot(
        val allowFlying: Boolean,
        val flying: Boolean
    )

    private fun flightSource(stack: ItemStack): FlightSource? = when (stack.item) {
        is JetpackItem -> JetpackSource
        is ElectricJetpack -> ElectricJetpackSource
        is QuantumChestplate -> QuantumSource
        else -> null
    }

    private interface FlightSource {
        fun hasEnergy(stack: ItemStack): Boolean
        fun consume(stack: ItemStack)
    }

    private object JetpackSource : FlightSource {
        override fun hasEnergy(stack: ItemStack): Boolean = JetpackItem.getFuel(stack) > 0
        override fun consume(stack: ItemStack) {
            JetpackItem.consumeFuelPerTick(stack)
        }
    }

    private object ElectricJetpackSource : FlightSource {
        override fun hasEnergy(stack: ItemStack): Boolean =
            (stack.item as ElectricJetpack).getEnergy(stack) > 0

        override fun consume(stack: ItemStack) {
            (stack.item as ElectricJetpack).consumeFlightEnergyPerTick(stack)
        }
    }

    private object QuantumSource : FlightSource {
        override fun hasEnergy(stack: ItemStack): Boolean = IElectricTool.getEnergy(stack) > 0
        override fun consume(stack: ItemStack) {
            QuantumChestplate.consumeFlightEnergyPerTick(stack)
        }
    }
}
