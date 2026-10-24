package ic2_120_advanced_weapons_addon.content.item

import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import ic2_120.content.item.energy.IElectricTool
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.SwordItem
import net.minecraft.world.item.Tier
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.level.Level
import java.util.UUID

private object QuantumSaberMaterial : Tier {
    override fun getDurability() = 1
    override fun getMiningSpeedMultiplier() = 1.0f
    override fun getAttackDamage() = 0f
    override fun getMiningLevel() = 0
    override fun getEnchantability() = 0
    override fun getRepairIngredient(): Ingredient = Ingredient.EMPTY
}

@ModItem(name = "quantum_saber", tab = CreativeTab.IC2_ADVANCED_WEAPONS, group = "electric_tools")
class QuantumSaber : SwordItem(
    QuantumSaberMaterial,
    0,
    -2.4f,
    Item.Settings().maxCount(1)
), IElectricTool {

    companion object {
        private const val NBT_ACTIVE = "QuantumSaberActive"
        private const val ENERGY_PER_HIT = 1000L

        private val ATTACK_DAMAGE_MODIFIER_ID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF")
        private val ATTACK_SPEED_MODIFIER_ID = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3")

        private const val DAMAGE_INACTIVE_TOTAL = 5.0
        private const val DAMAGE_ACTIVE_TOTAL = 31.0

        fun isActive(stack: ItemStack): Boolean = stack.orCreateNbt.getBoolean(NBT_ACTIVE)

        fun toggleActive(stack: ItemStack): Boolean {
            val nbt = stack.orCreateNbt
            val v = !nbt.getBoolean(NBT_ACTIVE)
            nbt.putBoolean(NBT_ACTIVE, v)
            return v
        }
    }

    override val tier = 3
    override val maxCapacity = 160_000L

    override fun getEnergy(stack: ItemStack) = IElectricTool.getEnergy(stack)
    override fun setEnergy(stack: ItemStack, energy: Long) = IElectricTool.setEnergy(stack, energy, maxCapacity)

    override fun isDamageable() = false

    override fun isEnchantable(stack: ItemStack) = false

    private fun weaponAttackModifier(stack: ItemStack): Double {
        val active = isActive(stack)
        val fullPower = active && getEnergy(stack) > 0
        val total = if (fullPower) DAMAGE_ACTIVE_TOTAL else DAMAGE_INACTIVE_TOTAL
        return total - 1.0
    }

    override fun getAttributeModifiers(
        stack: ItemStack,
        slot: EquipmentSlot
    ): Multimap<Attribute, AttributeModifier> {
        if (slot != EquipmentSlot.MAINHAND) {
            return super.getAttributeModifiers(stack, slot)
        }
        val builder = ImmutableMultimap.builder<Attribute, AttributeModifier>()
        builder.put(
            Attributes.GENERIC_ATTACK_DAMAGE,
            AttributeModifier(
                ATTACK_DAMAGE_MODIFIER_ID,
                "Weapon modifier",
                weaponAttackModifier(stack),
                AttributeModifier.Operation.ADDITION
            )
        )
        builder.put(
            Attributes.GENERIC_ATTACK_SPEED,
            AttributeModifier(
                ATTACK_SPEED_MODIFIER_ID,
                "Weapon modifier",
                -2.4,
                AttributeModifier.Operation.ADDITION
            )
        )
        return builder.build()
    }

    override fun use(world: Level, user: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = user.getStackInHand(hand)
        if (!world.isClient) {
            val on = toggleActive(stack)
            user.sendMessage(
                Component.translatable(
                    if (on) "message.ic2_120_advanced_weapons_addon.quantum_saber.active_on"
                    else "message.ic2_120_advanced_weapons_addon.quantum_saber.active_off"
                ),
                true
            )
        }
        return InteractionResultHolder.success(stack, world.isClient)
    }

    override fun postHit(stack: ItemStack, target: LivingEntity, attacker: LivingEntity): Boolean {
        if (!attacker.world.isClient && isActive(stack) && getEnergy(stack) >= ENERGY_PER_HIT) {
            setEnergy(stack, getEnergy(stack) - ENERGY_PER_HIT)
        }
        return true
    }

    override fun postMine(
        stack: ItemStack,
        world: Level,
        state: net.minecraft.world.level.block.state.BlockState,
        pos: net.minecraft.core.BlockPos,
        miner: LivingEntity
    ): Boolean {
        return true
    }

    override fun appendTooltip(
        stack: ItemStack,
        world: net.minecraft.world.level.Level?,
        tooltip: MutableList<Component>,
        context: net.minecraft.client.item.TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)
        appendEnergyTooltip(stack, tooltip)
        val key = if (isActive(stack))
            "tooltip.ic2_120_advanced_weapons_addon.quantum_saber.active"
        else
            "tooltip.ic2_120_advanced_weapons_addon.quantum_saber.inactive"
        tooltip.add(Component.translatable(key).formatted(ChatFormatting.GRAY))
    }

    override fun isItemBarVisible(stack: ItemStack) = true
    override fun getItemBarStep(stack: ItemStack) = getEnergyBarStep(stack)
    override fun getItemBarColor(stack: ItemStack) = getEnergyBarColor(stack)
}
