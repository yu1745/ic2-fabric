package ic2_120.content.item.energy

import ic2_120.content.energy.EnergyTier

/**
 * 电池物品接口
 *
 * 所有电池物品都应实现此接口，提供统一的能量访问方式。
 */
interface IBatteryItem : ITiered {
    /**
     * 最大容量（EU）
     */
    val maxCapacity: Long

    /**
     * 充放电速度（EU/t），默认由 [tier] 经 [EnergyTier.euPerTickFromTier] 得出。
     */
    val transferSpeed: Int
        get() = EnergyTier.euPerTickFromTier(tier).toInt().coerceAtLeast(1)

    /**
     * 是否可以无线充电
     * 如果为 true，电池可以给玩家物品栏中的装备充电
     */
    val canChargeWireless: Boolean

    /**
     * 是否可以充电
     * 一次性电池应重写为 false
     */
    val canCharge: Boolean get() = true

    /**
     * 获取当前剩余容量（EU）
     */
    fun getCurrentCharge(stack: net.minecraft.item.ItemStack): Long

    /**
     * 设置当前剩余容量（EU）
     * @param charge 要设置的电量，会自动限制在 [0, maxCapacity] 范围内
     */
    fun setCurrentCharge(stack: net.minecraft.item.ItemStack, charge: Long)

    /**
     * 充电
     * @param amount 要充入的电量
     * @return 实际充入的电量（可能小于 amount，如果快满了）
     */
    fun charge(stack: net.minecraft.item.ItemStack, amount: Long): Long {
        val current = getCurrentCharge(stack)
        val canAccept = maxCapacity - current
        val toCharge = minOf(amount, canAccept.toLong())
        setCurrentCharge(stack, current + toCharge)
        return toCharge
    }

    /**
     * 放电
     * @param amount 要放电的电量
     * @return 实际放电的电量（可能小于 amount，如果电量不足）
     */
    fun discharge(stack: net.minecraft.item.ItemStack, amount: Long): Long {
        val current = getCurrentCharge(stack)
        val toDischarge = minOf(amount, current)
        setCurrentCharge(stack, current - toDischarge)
        return toDischarge
    }

    /**
     * 是否已充满
     */
    fun isFullyCharged(stack: net.minecraft.item.ItemStack): Boolean {
        return getCurrentCharge(stack) >= maxCapacity
    }

    /**
     * 是否已耗尽
     */
    fun isEmpty(stack: net.minecraft.item.ItemStack): Boolean {
        return getCurrentCharge(stack) <= 0
    }

    /**
     * 获取剩余电量百分比（0.0 - 1.0）
     */
    fun getChargeRatio(stack: net.minecraft.item.ItemStack): Double {
        return if (maxCapacity > 0) {
            getCurrentCharge(stack).toDouble() / maxCapacity.toDouble()
        } else {
            0.0
        }
    }
}

/**
 * 检查物品栈是否可以被充电（可充电电池或电动工具）
 */
fun net.minecraft.item.ItemStack.canBeCharged(): Boolean {
    return (item as? IBatteryItem)?.canCharge == true || item is IElectricTool
}
