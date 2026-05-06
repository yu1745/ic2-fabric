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
 * - 头盔：12%   （量子 15% × 0.8）
 * - 胸甲：36%   （量子 45% × 0.8）
 * - 护腿：24%   （量子 30% × 0.8）
 * - 靴子：8%    （量子 10% × 0.8）
 * - **总计：80% = 量子 × 0.8**（全套穿戴时）
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
        is NanoHelmet -> 0.12f        // 12%   = 量子 15% × 0.8
        is NanoChestplate -> 0.36f    // 36%   = 量子 45% × 0.8
        is NanoLeggings -> 0.24f      // 24%   = 量子 30% × 0.8
        is NanoBoots -> 0.08f         // 8%    = 量子 10% × 0.8
        else -> 0f
    }
}
