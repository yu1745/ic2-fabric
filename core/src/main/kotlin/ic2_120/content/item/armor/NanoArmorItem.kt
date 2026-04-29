package ic2_120.content.item.armor

import ic2_120.content.item.ModArmorMaterials
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.item.ArmorItem
import net.minecraft.item.ArmorMaterial

/**
 * 纳米护甲基类
 *
 * ## 核心参数
 *
 * - 载电量：1,000,000 EU（1 MEU）
 * - 能量等级：3
 * - 转移速度：512 EU/t
 *
 * ## 减伤比例
 *
 * - 头盔：11.54%
 * - 胸甲：33.85%
 * - 护腿：23.07%
 * - 靴子：11.54%
 * - **总计：80%**（全套穿戴时）
 *
 * ## 能量消耗
 *
 * - 每减免 1 点伤害消耗 5000 EU
 * - 能量从所有纳米装备均匀扣除
 *
 * ## 夜视功能（仅头盔）
 *
 * - 耗电：14 EU/tick（1M EU / 1小时）
 * - 快捷键：Alt + N
 * - 光线 >= 8 时移除夜视并添加致盲
 */
abstract class NanoArmorItem(
    material: ArmorMaterial,
    type: Type,
    settings: FabricItemSettings
) : ElectricArmorItem(material, type, settings) {

    override val tier: Int = 3
    override val maxCapacity: Long = 1_000_000L

    /**
     * 获取该护甲的减伤比例
     *
     * 减伤逻辑统一在 Mixin 中处理，这里只提供减伤比例。
     * 四件装备总减伤为 80%。
     */
    override fun getDamageReduction(): Float = when (this) {
        is NanoHelmet -> 0.1154f      // 11.54%
        is NanoChestplate -> 0.3385f  // 33.85%
        is NanoLeggings -> 0.2307f    // 23.07%
        is NanoBoots -> 0.1154f       // 11.54%
        else -> 0f
    }
}
