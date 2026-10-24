package ic2_120.content.item.energy

import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import ic2_120.editCustomData
import ic2_120.getCustomData

/**
 * 电动工具接口
 *
 * 实现 [ITiered] 能量等级，提供 EU 电量存储。
 * 电动工具使用 NBT 存储电量，电量耗尽后无法使用。
 *
 * ## 与电池的区别
 *
 * - **电动工具**：仅可被充电，不可放电给机器供电。
 * - **电池**（[IBatteryItem]）：既可充电，也可放电。
 *
 * ## 能量等级规则
 *
 * 遵循 [ITiered] 的充电规则：机器只能给等级 <= 自己的设备充电。
 *
 * ## 实现说明
 *
 * 使用接口而非基类，便于子类继承其他类（如 [net.minecraft.item.AxeItem]）。
 * 实现类需提供 [tier]、[maxCapacity]，并重写 [net.minecraft.item.Item.appendTooltip]、
 * [net.minecraft.item.Item.isItemBarVisible]、[net.minecraft.item.Item.getItemBarStep]、
 * [net.minecraft.item.Item.getItemBarColor] 以显示电量。
 */
interface IElectricTool : ITiered {

    /** 最大电量（EU） */
    val maxCapacity: Long

    /** 获取当前电量（EU） */
    fun getEnergy(stack: ItemStack): Long

    /** 获取最大电量（EU） */
    fun getMaxEnergy(): Long = maxCapacity

    /** 设置电量 */
    fun setEnergy(stack: ItemStack, energy: Long)

    /** 获取电量比例（0.0 - 1.0） */
    fun getEnergyRatio(stack: ItemStack): Double =
        if (maxCapacity > 0) getEnergy(stack).toDouble() / maxCapacity else 0.0

    /** 是否已耗尽 */
    fun isEmpty(stack: ItemStack): Boolean = getEnergy(stack) <= 0

    /** 是否已充满 */
    fun isFullyCharged(stack: ItemStack): Boolean = getEnergy(stack) >= maxCapacity

    /**
     * 添加电量相关工具提示，实现类在 [net.minecraft.item.Item.appendTooltip] 中调用。
     */
    fun appendEnergyTooltip(stack: ItemStack, tooltip: MutableList<Text>) {
        val energy = getEnergy(stack)
        val ratio = getEnergyRatio(stack)
        tooltip.add(
            Text.literal("⚡ ${formatEnergy(energy, maxCapacity)} (${formatPercentage(ratio)})")
        )
        tooltip.add(
            Text.literal("等级: $tier")
                .formatted(net.minecraft.util.Formatting.GRAY)
        )
    }

    /** 获取物品栏电量条步数（0-13），实现类在 [net.minecraft.item.Item.getItemBarStep] 中调用 */
    fun getEnergyBarStep(stack: ItemStack): Int {
        val ratio = getEnergyRatio(stack)
        return (ratio * 13).toInt().coerceIn(0, 13)
    }

    /** 获取物品栏电量条颜色，实现类在 [net.minecraft.item.Item.getItemBarColor] 中调用 */
    fun getEnergyBarColor(stack: ItemStack): Int {
        val ratio = getEnergyRatio(stack)
        return when {
            ratio > 0.5 -> 0x4AFF4A // 绿色
            ratio > 0.2 -> 0xFFFF4A // 黄色
            else -> 0xFF4A4A // 红色
        }
    }

    companion object {
        /** NBT 键：存储电量（与 BatteryItemBase 一致，便于充电兼容） */
        const val ENERGY_KEY = "Energy"

        /** 从物品栈获取电量 */
        fun getEnergy(stack: ItemStack): Long = stack.getCustomData()?.getLong(ENERGY_KEY) ?: 0L

        /** 设置物品栈的电量 */
        fun setEnergy(stack: ItemStack, energy: Long, maxCapacity: Long) {
            stack.editCustomData { it.putLong(ENERGY_KEY, energy.coerceIn(0, maxCapacity)) }
        }

        fun formatEnergy(energy: Long, maxCapacity: Long): String =
            "%,d / %,d EU".format(energy, maxCapacity)

        fun formatPercentage(ratio: Double): String = "%.1f%%".format(ratio * 100)
    }
}
