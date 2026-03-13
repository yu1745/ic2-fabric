package ic2_120.content.item.armor

import ic2_120.content.item.energy.IElectricTool
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.item.ArmorItem
import net.minecraft.item.ArmorMaterial
import net.minecraft.item.ItemStack
import net.minecraft.text.Text

/**
 * 能量护甲基类
 *
 * 实现 [IElectricTool] 接口的通用逻辑（能量存储、充放电）。
 * 提供统一的工具提示（电量显示、耐久条、快捷键提示）。
 * 提供抽象方法 [getDamageReduction] 让子类定义减伤比例。
 *
 * ## 子类实现
 *
 * 子类需要实现：
 * - [maxCapacity]: 最大电量
 * - [tier]: 能量等级
 * - [getDamageReduction]: 减伤比例（0.0-1.0）
 *
 * ## 减伤机制
 *
 * 减伤逻辑统一在 [ic2_120.mixin.PlayerEntityMixin] 中处理。
 * 每个装备独立计算减伤，只穿戴该部位就只享受该部位的减伤效果。
 * 能量均匀扣除到所有纳米/量子装备，避免单件快速耗尽。
 *
 * ## 能量消耗
 *
 * - 每减免 1 点伤害消耗 5000 EU
 * - 能量从所有纳米/量子装备均匀扣除
 * - 能量不足时回退到普通护甲效果
 */
abstract class ElectricArmorItem(
    material: ArmorMaterial,
    type: Type,
    settings: FabricItemSettings
) : ArmorItem(material, type, settings), IElectricTool {

    // ========== IElectricTool 实现 ==========

    override fun getEnergy(stack: ItemStack): Long =
        stack.orCreateNbt.getLong(IElectricTool.ENERGY_KEY)

    override fun setEnergy(stack: ItemStack, energy: Long) {
        stack.orCreateNbt.putLong(IElectricTool.ENERGY_KEY, energy.coerceIn(0L, maxCapacity))
    }

    // ========== 抽象方法 ==========

    /**
     * 获取该护甲的减伤比例（0.0-1.0）
     *
     * 例如：
     * - 0.15f 表示减免 15% 伤害
     * - 0.44f 表示减免 44% 伤害
     */
    abstract fun getDamageReduction(): Float

    // ========== UI 渲染 ==========

    override fun isItemBarVisible(stack: ItemStack): Boolean = true

    override fun getItemBarStep(stack: ItemStack): Int =
        ((getEnergy(stack).toDouble() / maxCapacity) * 13).toInt().coerceIn(0, 13)

    override fun getItemBarColor(stack: ItemStack): Int =
        getEnergyBarColor(stack)

    override fun appendTooltip(stack: ItemStack, world: net.minecraft.world.World?, tooltip: MutableList<Text>, context: net.minecraft.client.item.TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        appendEnergyTooltip(stack, tooltip)
    }
}
