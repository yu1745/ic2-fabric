package ic2_120_advanced_weapons_addon.content.item

import ic2_120.content.item.energy.IElectricTool
import ic2_120.editCustomData
import ic2_120.getCustomData
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import net.minecraft.component.type.AttributeModifierSlot
import net.minecraft.component.type.AttributeModifiersComponent
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.SwordItem
import net.minecraft.item.ToolMaterial
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.recipe.Ingredient
import net.minecraft.registry.tag.BlockTags
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World

private object QuantumSaberMaterial : ToolMaterial {
    override fun getDurability() = 1
    override fun getMiningSpeedMultiplier() = 1.0f
    override fun getAttackDamage() = 0f
    override fun getEnchantability() = 0
    override fun getInverseTag() = BlockTags.INCORRECT_FOR_WOODEN_TOOL
    override fun getRepairIngredient(): Ingredient = Ingredient.EMPTY
}

@ModItem(name = "quantum_saber", tab = CreativeTab.IC2_ADVANCED_WEAPONS, group = "electric_tools")
class QuantumSaber : SwordItem(
    QuantumSaberMaterial,
    Item.Settings()
), IElectricTool {

    companion object {
        private const val NBT_ACTIVE = "QuantumSaberActive"
        private const val ENERGY_PER_HIT = 1000L

        private val ATTACK_DAMAGE_MODIFIER_ID = Identifier.ofVanilla("cb3f55d3-645c-4f38-a497-9c13a33db5cf")
        private val ATTACK_SPEED_MODIFIER_ID = Identifier.ofVanilla("fa233e1c-4180-4865-b01b-bcce9785aca3")

        private const val DAMAGE_ACTIVE_TOTAL = 31.0

        fun isActive(stack: ItemStack): Boolean = stack.getCustomData()?.getBoolean(NBT_ACTIVE) ?: false

        fun toggleActive(stack: ItemStack): Boolean {
            val v = !(stack.getCustomData()?.getBoolean(NBT_ACTIVE) ?: false)
            stack.editCustomData { it.putBoolean(NBT_ACTIVE, v) }
            return v
        }
    }

    override val tier = 3
    override val maxCapacity = 160_000L

    override fun getEnergy(stack: ItemStack) = IElectricTool.getEnergy(stack)
    override fun setEnergy(stack: ItemStack, energy: Long) = IElectricTool.setEnergy(stack, energy, maxCapacity)

    override fun isEnchantable(stack: ItemStack) = false

    override fun getAttributeModifiers(): AttributeModifiersComponent {
        return AttributeModifiersComponent.builder()
            .add(
                EntityAttributes.GENERIC_ATTACK_DAMAGE,
                EntityAttributeModifier(
                    ATTACK_DAMAGE_MODIFIER_ID,
                    DAMAGE_ACTIVE_TOTAL - 1.0,
                    EntityAttributeModifier.Operation.ADD_VALUE
                ),
                AttributeModifierSlot.MAINHAND
            )
            .add(
                EntityAttributes.GENERIC_ATTACK_SPEED,
                EntityAttributeModifier(
                    ATTACK_SPEED_MODIFIER_ID,
                    -2.4,
                    EntityAttributeModifier.Operation.ADD_VALUE
                ),
                AttributeModifierSlot.MAINHAND
            )
            .build()
    }

    override fun use(world: World, user: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val stack = user.getStackInHand(hand)
        if (!world.isClient) {
            val on = toggleActive(stack)
            user.sendMessage(
                Text.translatable(
                    if (on) "message.ic2_120_advanced_weapons_addon.quantum_saber.active_on"
                    else "message.ic2_120_advanced_weapons_addon.quantum_saber.active_off"
                ),
                true
            )
        }
        return TypedActionResult.success(stack)
    }

    override fun postHit(stack: ItemStack, target: LivingEntity, attacker: LivingEntity): Boolean {
        if (!attacker.world.isClient && isActive(stack) && getEnergy(stack) >= ENERGY_PER_HIT) {
            setEnergy(stack, getEnergy(stack) - ENERGY_PER_HIT)
        }
        return true
    }

    override fun postMine(
        stack: ItemStack,
        world: World,
        state: net.minecraft.block.BlockState,
        pos: net.minecraft.util.math.BlockPos,
        miner: LivingEntity
    ): Boolean {
        return true
    }

    override fun appendTooltip(
        stack: ItemStack,
        context: Item.TooltipContext,
        tooltip: MutableList<Text>,
        type: TooltipType
    ) {
        super.appendTooltip(stack, context, tooltip, type)
        appendEnergyTooltip(stack, tooltip)
        val key = if (isActive(stack))
            "tooltip.ic2_120_advanced_weapons_addon.quantum_saber.active"
        else
            "tooltip.ic2_120_advanced_weapons_addon.quantum_saber.inactive"
        tooltip.add(Text.translatable(key).formatted(Formatting.GRAY))
    }

    override fun isItemBarVisible(stack: ItemStack) = true
    override fun getItemBarStep(stack: ItemStack) = getEnergyBarStep(stack)
    override fun getItemBarColor(stack: ItemStack) = getEnergyBarColor(stack)
}
