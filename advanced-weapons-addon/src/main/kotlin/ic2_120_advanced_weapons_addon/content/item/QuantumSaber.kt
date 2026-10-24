package ic2_120_advanced_weapons_addon.content.item

<<<<<<< HEAD
import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import ic2_120.content.item.energy.IElectricTool
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.SwordItem
import net.minecraft.item.ToolMaterial
import net.minecraft.recipe.Ingredient
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World
import java.util.UUID
=======
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
>>>>>>> backup/1.21.1

private object QuantumSaberMaterial : ToolMaterial {
    override fun getDurability() = 1
    override fun getMiningSpeedMultiplier() = 1.0f
    override fun getAttackDamage() = 0f
<<<<<<< HEAD
    override fun getMiningLevel() = 0
    override fun getEnchantability() = 0
=======
    override fun getEnchantability() = 0
    override fun getInverseTag() = BlockTags.INCORRECT_FOR_WOODEN_TOOL
>>>>>>> backup/1.21.1
    override fun getRepairIngredient(): Ingredient = Ingredient.EMPTY
}

@ModItem(name = "quantum_saber", tab = CreativeTab.IC2_ADVANCED_WEAPONS, group = "electric_tools")
class QuantumSaber : SwordItem(
    QuantumSaberMaterial,
<<<<<<< HEAD
    0,
    -2.4f,
    FabricItemSettings().maxCount(1)
=======
    Item.Settings().maxCount(1)
>>>>>>> backup/1.21.1
), IElectricTool {

    companion object {
        private const val NBT_ACTIVE = "QuantumSaberActive"
        private const val ENERGY_PER_HIT = 1000L

<<<<<<< HEAD
        private val ATTACK_DAMAGE_MODIFIER_ID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF")
        private val ATTACK_SPEED_MODIFIER_ID = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3")

        private const val DAMAGE_INACTIVE_TOTAL = 5.0
        private const val DAMAGE_ACTIVE_TOTAL = 31.0

        fun isActive(stack: ItemStack): Boolean = stack.orCreateNbt.getBoolean(NBT_ACTIVE)

        fun toggleActive(stack: ItemStack): Boolean {
            val nbt = stack.orCreateNbt
            val v = !nbt.getBoolean(NBT_ACTIVE)
            nbt.putBoolean(NBT_ACTIVE, v)
=======
        private val ATTACK_DAMAGE_MODIFIER_ID = Identifier.ofVanilla("quantum_saber_attack_damage")
        private val ATTACK_SPEED_MODIFIER_ID = Identifier.ofVanilla("quantum_saber_attack_speed")

        private const val DAMAGE_ACTIVE_TOTAL = 31.0
        private const val DAMAGE_INACTIVE_TOTAL = 5.0

        fun isActive(stack: ItemStack): Boolean = stack.getCustomData()?.getBoolean(NBT_ACTIVE) ?: false

        fun toggleActive(stack: ItemStack): Boolean {
            val v = !isActive(stack)
            stack.editCustomData { it.putBoolean(NBT_ACTIVE, v) }
>>>>>>> backup/1.21.1
            return v
        }
    }

<<<<<<< HEAD
=======
    override fun postDamageEntity(stack: ItemStack, target: LivingEntity, attacker: LivingEntity) {
        // 电动工具不使用原版耐久体系，阻止 SwordItem 的 stack.damage(1) 调用
    }

>>>>>>> backup/1.21.1
    override val tier = 3
    override val maxCapacity = 160_000L

    override fun getEnergy(stack: ItemStack) = IElectricTool.getEnergy(stack)
    override fun setEnergy(stack: ItemStack, energy: Long) = IElectricTool.setEnergy(stack, energy, maxCapacity)

<<<<<<< HEAD
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
    ): Multimap<EntityAttribute, EntityAttributeModifier> {
        if (slot != EquipmentSlot.MAINHAND) {
            return super.getAttributeModifiers(stack, slot)
        }
        val builder = ImmutableMultimap.builder<EntityAttribute, EntityAttributeModifier>()
        builder.put(
            EntityAttributes.GENERIC_ATTACK_DAMAGE,
            EntityAttributeModifier(
                ATTACK_DAMAGE_MODIFIER_ID,
                "Weapon modifier",
                weaponAttackModifier(stack),
                EntityAttributeModifier.Operation.ADDITION
            )
        )
        builder.put(
            EntityAttributes.GENERIC_ATTACK_SPEED,
            EntityAttributeModifier(
                ATTACK_SPEED_MODIFIER_ID,
                "Weapon modifier",
                -2.4,
                EntityAttributeModifier.Operation.ADDITION
            )
        )
        return builder.build()
=======
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
>>>>>>> backup/1.21.1
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
<<<<<<< HEAD
        return TypedActionResult.success(stack, world.isClient)
    }

    override fun postHit(stack: ItemStack, target: LivingEntity, attacker: LivingEntity): Boolean {
        if (!attacker.world.isClient && isActive(stack) && getEnergy(stack) >= ENERGY_PER_HIT) {
            setEnergy(stack, getEnergy(stack) - ENERGY_PER_HIT)
=======
        return TypedActionResult.success(stack)
    }

    override fun postHit(stack: ItemStack, target: LivingEntity, attacker: LivingEntity): Boolean {
        if (!attacker.world.isClient) {
            val active = isActive(stack)
            if (active && getEnergy(stack) >= ENERGY_PER_HIT) {
                setEnergy(stack, getEnergy(stack) - ENERGY_PER_HIT)
            } else {
                target.heal((DAMAGE_ACTIVE_TOTAL - DAMAGE_INACTIVE_TOTAL).toFloat())
            }
>>>>>>> backup/1.21.1
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
<<<<<<< HEAD
        world: net.minecraft.world.World?,
        tooltip: MutableList<Text>,
        context: net.minecraft.client.item.TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)
=======
        context: Item.TooltipContext,
        tooltip: MutableList<Text>,
        type: TooltipType
    ) {
        super.appendTooltip(stack, context, tooltip, type)
>>>>>>> backup/1.21.1
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
