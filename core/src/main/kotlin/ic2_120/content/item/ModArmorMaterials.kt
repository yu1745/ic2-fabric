package ic2_120.content.item

import ic2_120.Ic2_120
import net.minecraft.item.ArmorItem
import net.minecraft.item.ArmorMaterial
import net.minecraft.item.Item
import net.minecraft.recipe.Ingredient
import net.minecraft.registry.Registries
import net.minecraft.sound.SoundEvent
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Identifier

/**
 * 护甲材料定义
 *
 * 包含所有 IC2 模组的护甲材料，供护甲类使用。
 */
object ModArmorMaterials {

    // ========== 修复原料 ==========
    private val bronzeIngot = Ingredient.ofItems(Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "bronze_ingot")))
    private val rubber = Ingredient.ofItems(Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "rubber")))
    private val carbonFibre = Ingredient.ofItems(Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "carbon_fibre")))
    private val advancedAlloy = Ingredient.ofItems(Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "alloy")))
    private val iridium = Ingredient.ofItems(Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "iridium")))

    /**
     * 创建自定义护甲材料
     */
    private fun createArmorMaterial(
        name: String,
        durabilityMultiplier: Int,
        protection: Map<ArmorItem.Type, Int>,
        enchantability: Int,
        equipSound: SoundEvent,
        toughness: Float,
        knockbackResistance: Float,
        repairIngredient: Ingredient
    ): ArmorMaterial = object : ArmorMaterial {
        private val durabilityMap = mapOf(
            ArmorItem.Type.HELMET to 11 * durabilityMultiplier,
            ArmorItem.Type.CHESTPLATE to 16 * durabilityMultiplier,
            ArmorItem.Type.LEGGINGS to 15 * durabilityMultiplier,
            ArmorItem.Type.BOOTS to 13 * durabilityMultiplier
        )

        override fun getDurability(type: ArmorItem.Type) = durabilityMap[type] ?: 0
        override fun getProtection(type: ArmorItem.Type) = protection[type] ?: 0
        override fun getEnchantability() = enchantability
        override fun getEquipSound() = equipSound
        override fun getRepairIngredient() = repairIngredient
        override fun getName() = name
        override fun getToughness() = toughness
        override fun getKnockbackResistance() = knockbackResistance
    }

    // ========== 纳米护甲材料 ==========
    /**
     * IC2 高级护甲套装，能量驱动的自修复护甲。
     * 参考 MC百科 https://www.mcmod.cn/item/208.html
     */
    val NANO_ARMOR = createArmorMaterial(
        name = "ic2_nano",
        durabilityMultiplier = 15,
        protection = mapOf(
            ArmorItem.Type.HELMET to 3,
            ArmorItem.Type.CHESTPLATE to 8,
            ArmorItem.Type.LEGGINGS to 6,
            ArmorItem.Type.BOOTS to 3
        ),
        enchantability = 10,
        equipSound = SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND,
        toughness = 2f,
        knockbackResistance = 0f,
        repairIngredient = carbonFibre
    )

    // ========== 量子护甲材料 ==========
    /**
     * IC2 顶级护甲套装，具备全方位强化。
     * 参考 MC百科 https://www.mcmod.cn/item/212.html
     */
    val QUANTUM_ARMOR = createArmorMaterial(
        name = "ic2_quantum",
        durabilityMultiplier = 25,
        protection = mapOf(
            ArmorItem.Type.HELMET to 4,
            ArmorItem.Type.CHESTPLATE to 9,
            ArmorItem.Type.LEGGINGS to 6,
            ArmorItem.Type.BOOTS to 4
        ),
        enchantability = 15,
        equipSound = SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE,
        toughness = 3f,
        knockbackResistance = 0.4f,
        repairIngredient = iridium
    )

    // ========== 喷气背包护甲材料 ==========
    /**
     * 喷气背包护甲材料（钻石级护甲值）
     */
    val JETPACK_ARMOR = createArmorMaterial(
        name = "ic2_jet_pack",
        durabilityMultiplier = 15,
        protection = mapOf(
            ArmorItem.Type.HELMET to 0,
            ArmorItem.Type.CHESTPLATE to 8,  // 钻石胸甲级别
            ArmorItem.Type.LEGGINGS to 0,
            ArmorItem.Type.BOOTS to 0
        ),
        enchantability = 10,
        equipSound = SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
        toughness = 2f,  // 钻石级韧性
        knockbackResistance = 0f,
        repairIngredient = bronzeIngot
    )

    // ========== 电力喷气背包护甲材料 ==========
    /**
     * 电力喷气背包护甲材料（钻石级护甲值）
     */
    val ELECTRIC_JETPACK_ARMOR = createArmorMaterial(
        name = "ic2_electric_jet_pack",
        durabilityMultiplier = 15,
        protection = mapOf(
            ArmorItem.Type.HELMET to 0,
            ArmorItem.Type.CHESTPLATE to 8,  // 钻石胸甲级别
            ArmorItem.Type.LEGGINGS to 0,
            ArmorItem.Type.BOOTS to 0
        ),
        enchantability = 10,
        equipSound = SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND,
        toughness = 2f,  // 钻石级韧性
        knockbackResistance = 0f,
        repairIngredient = carbonFibre
    )
}
