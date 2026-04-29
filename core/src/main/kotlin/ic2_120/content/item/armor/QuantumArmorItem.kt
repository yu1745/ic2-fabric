package ic2_120.content.item.armor

import ic2_120.content.item.ModArmorMaterials
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ArmorItem
import net.minecraft.item.ArmorMaterial

/**
 * 量子护甲基类
 *
 * ## 核心参数
 *
 * - 载电量：10,000,000 EU（10 MEU）
 * - 能量等级：4
 * - 转移速度：2048 EU/t
 *
 * ## 减伤比例
 *
 * - 头盔：15%
 * - 胸甲：44%
 * - 护腿：30%
 * - 靴子：11%
 * - **总计：100%**（全套穿戴时完全免疫伤害）
 *
 * ## 能量消耗
 *
 * - 每减免 1 点伤害消耗 5000 EU
 * - 能量从所有量子装备均匀扣除
 *
 * ## 特殊功能
 *
 * - **头盔**：夜视、水下呼吸、补充饱食度；全套时：消除 debuff
 * - **胸甲**：飞行（需全套量子护甲）
 * - **护腿**：（待实现）3 倍奔跑速度
 * - **靴子**：（待实现）超级跳
 */
abstract class QuantumArmorItem(
    material: ArmorMaterial,
    type: Type,
    settings: FabricItemSettings
) : ElectricArmorItem(material, type, settings) {

    override val tier: Int = 4

    companion object {
        /** 检查玩家是否穿戴全套量子护甲 */
        fun hasFullQuantumArmor(player: PlayerEntity): Boolean {
            val slots = arrayOf(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)
            return slots.all { player.getEquippedStack(it).item is QuantumArmorItem }
        }
    }
    override val maxCapacity: Long = 10_000_000L

    /**
     * 获取该护甲的减伤比例
     *
     * 减伤逻辑统一在 Mixin 中处理，这里只提供减伤比例。
     * 四件装备总减伤为 100%。
     */
    override fun getDamageReduction(): Float = when (this) {
        is QuantumHelmet -> 0.15f      // 15%
        is QuantumChestplate -> 0.44f  // 44%
        is QuantumLeggings -> 0.30f    // 30%
        is QuantumBoots -> 0.11f       // 11%
        else -> 0f
    }
}
