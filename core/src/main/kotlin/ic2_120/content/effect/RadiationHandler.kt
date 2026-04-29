package ic2_120.content.effect

import ic2_120.content.item.*
import ic2_120.content.item.armor.QuantumArmorItem
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ArmorItem
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.util.math.Box

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
            val entities = world.getEntitiesByClass(
                LivingEntity::class.java,
                Box(-3e7, -3e7, -3e7, 3e7, 3e7, 3e7)
            ) { true }

            for (entity in entities) {
                if (entity.isDead) continue
                if (isImmune(entity)) continue
                if (hasRadioactiveItem(entity)) {
                    entity.addStatusEffect(
                        StatusEffectInstance(
                            ModStatusEffects.RADIATION,
                            400, // 20 秒
                            0,
                            true,
                            true,
                            true
                        )
                    )
                }
            }
        }
    }

    private fun isImmune(entity: LivingEntity): Boolean {
        if (entity is PlayerEntity && QuantumArmorItem.hasFullQuantumArmor(entity)) return true
        return hasFullHazmat(entity)
    }

    private fun hasFullHazmat(entity: LivingEntity): Boolean {
        val helmet = entity.getEquippedStack(EquipmentSlot.HEAD)
        val chest = entity.getEquippedStack(EquipmentSlot.CHEST)
        val legs = entity.getEquippedStack(EquipmentSlot.LEGS)
        val feet = entity.getEquippedStack(EquipmentSlot.FEET)

        fun isHazmat(stack: ItemStack): Boolean {
            if (stack.isEmpty || stack.item !is ArmorItem) return false
            val id = Registries.ITEM.getId(stack.item)
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
            if (isRadioactiveItem(entity.getEquippedStack(slot))) return true
        }

        if (entity is PlayerEntity) {
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
