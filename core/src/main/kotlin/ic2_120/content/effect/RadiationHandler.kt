package ic2_120.content.effect

import ic2_120.content.item.*
import ic2_120.content.item.armor.QuantumArmorItem
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.ItemStack
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.MinecraftServer

object RadiationHandler {

    private var tickCounter = 0

    fun register() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            tick(server)
        }
    }

    private fun tick(server: MinecraftServer) {
        tickCounter++
        if (tickCounter % 20 != 0) return

        for (world in server.worlds) {
            for (player in world.players) {
                if (player.isDead) continue
                if (isImmune(player)) continue
                if (hasRadioactiveItem(player)) {
                    player.addStatusEffect(
                        MobEffectInstance(ModStatusEffects.RADIATION, 400, 0, true, true, true)
                    )
                }
            }
        }
    }

    private fun isImmune(entity: LivingEntity): Boolean {
        if (entity is Player && QuantumArmorItem.hasFullQuantumArmor(entity)) return true
        return hasFullHazmat(entity)
    }

    private fun hasFullHazmat(entity: LivingEntity): Boolean {
        val helmet = entity.getItemBySlot(EquipmentSlot.HEAD)
        val chest = entity.getItemBySlot(EquipmentSlot.CHEST)
        val legs = entity.getItemBySlot(EquipmentSlot.LEGS)
        val feet = entity.getItemBySlot(EquipmentSlot.FEET)

        fun isHazmat(stack: ItemStack): Boolean {
            if (stack.isEmpty || stack.item !is ArmorItem) return false
            val id = BuiltInRegistries.ITEM.getId(stack.item)
            return "hazmat" in id.path || id.path == "rubber_boots"
        }

        return isHazmat(helmet) && isHazmat(chest) && isHazmat(legs) && isHazmat(feet)
    }

    private fun hasRadioactiveItem(entity: LivingEntity): Boolean {
        val slots = listOf(
            EquipmentSlot.MAINHAND,
            EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
        )
        for (slot in slots) {
            if (isRadioactiveItem(entity.getItemBySlot(slot))) return true
        }

        if (entity is Player) {
            for (stack in entity.inventory.main) {
                if (isRadioactiveItem(stack)) return true
            }
            for (stack in entity.inventory.offHand) {
                if (isRadioactiveItem(stack)) return true
            }
        }

        return false
    }

    private fun isRadioactiveItem(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        val item = stack.item

        return when (item) {
            is AbstractUraniumFuelRodItem,
            is AbstractMoxFuelRodItem,
            is LithiumFuelRodItem,
            is DepletedIsotopeFuelRodItem,
            is DepletedUraniumFuelRodItem,
            is DepletedDualUraniumFuelRodItem,
            is DepletedQuadUraniumFuelRodItem,
            is DepletedMoxFuelRodItem,
            is DepletedDualMoxFuelRodItem,
            is DepletedQuadMoxFuelRodItem,
            is Uranium,
            is Mox,
            is Plutonium,
            is Uranium235,
            is Uranium238,
            is SmallUranium235,
            is SmallUranium238,
            is SmallPlutonium,
            is RtgPellet -> true
            else -> false
        }
    }
}
