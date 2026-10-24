package ic2_120_advanced_weapons_addon.content.item

import ic2_120.content.item.energy.IElectricTool
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.BlockTags
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.SwordItem
import net.minecraft.world.item.Tier
import net.minecraft.world.item.component.ItemAttributeModifiers
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import ic2_120.compat.*

private object QuantumSaberMaterial : Tier {
    override fun getUses() = 1
    override fun getSpeed() = 1.0f
    override fun getAttackDamageBonus() = 0f
    override fun getIncorrectBlocksForDrops(): net.minecraft.tags.TagKey<Block> = BlockTags.INCORRECT_FOR_WOODEN_TOOL
    override fun getEnchantmentValue() = 0
    override fun getRepairIngredient(): Ingredient = Ingredient.EMPTY
}

@ModItem(name = "quantum_saber", tab = CreativeTab.IC2_ADVANCED_WEAPONS, group = "electric_tools")
class QuantumSaber : SwordItem(
    QuantumSaberMaterial,
    Item.Properties().stacksTo(1).attributes(SwordItem.createAttributes(QuantumSaberMaterial, 0, -2.4f))
), IElectricTool {

    companion object {
        private const val NBT_ACTIVE = "QuantumSaberActive"
        private const val ENERGY_PER_HIT = 1000L

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

    override fun isEnchantable(stack: ItemStack) = false

    private fun weaponAttackModifier(stack: ItemStack): Double {
        val active = isActive(stack)
        val fullPower = active && getEnergy(stack) > 0
        val total = if (fullPower) DAMAGE_ACTIVE_TOTAL else DAMAGE_INACTIVE_TOTAL
        return total - 1.0
    }

    /** 1.20.5+ 属性走 DataComponents：每次状态变化时重写组件 */
    private fun syncAttributeModifiers(stack: ItemStack) {
        val mods = ItemAttributeModifiers.builder()
            .add(
                Attributes.ATTACK_DAMAGE,
                AttributeModifier(
                    ResourceLocation.withDefaultNamespace("quantum_saber_attack_damage"),
                    weaponAttackModifier(stack),
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            )
            .add(
                Attributes.ATTACK_SPEED,
                AttributeModifier(
                    ResourceLocation.withDefaultNamespace("quantum_saber_attack_speed"),
                    -2.4,
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            )
            .build()
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, mods)
    }

    override fun use(world: Level, user: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = user.getItemInHand(hand)
        if (!world.isClient) {
            val on = toggleActive(stack)
            syncAttributeModifiers(stack)
            user.displayClientMessage(
                Component.translatable(
                    if (on) "message.ic2_120_advanced_weapons_addon.quantum_saber.active_on"
                    else "message.ic2_120_advanced_weapons_addon.quantum_saber.active_off"
                ),
                true
            )
        }
        return InteractionResultHolder.success(stack)
    }

    override fun inventoryTick(stack: ItemStack, world: Level, entity: net.minecraft.world.entity.Entity, slot: Int, selected: Boolean) {
        syncAttributeModifiers(stack)
    }

    override fun postHurtEnemy(stack: ItemStack, target: LivingEntity, attacker: LivingEntity) {
        if (!attacker.level().isClientSide && isActive(stack) && getEnergy(stack) >= ENERGY_PER_HIT) {
            setEnergy(stack, getEnergy(stack) - ENERGY_PER_HIT)
        }
    }

    override fun mineBlock(
        stack: ItemStack,
        world: Level,
        state: net.minecraft.world.level.block.state.BlockState,
        pos: net.minecraft.core.BlockPos,
        miner: LivingEntity
    ): Boolean {
        return true
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: Item.TooltipContext,
        tooltip: MutableList<Component>,
        type: net.minecraft.world.item.TooltipFlag
    ) {
        appendEnergyTooltip(stack, tooltip)
        val key = if (isActive(stack))
            "tooltip.ic2_120_advanced_weapons_addon.quantum_saber.active"
        else
            "tooltip.ic2_120_advanced_weapons_addon.quantum_saber.inactive"
        tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY))
    }

    override fun isBarVisible(stack: ItemStack) = true
    override fun getBarWidth(stack: ItemStack) = getEnergyBarStep(stack)
    override fun getBarColor(stack: ItemStack) = getEnergyBarColor(stack)
}
