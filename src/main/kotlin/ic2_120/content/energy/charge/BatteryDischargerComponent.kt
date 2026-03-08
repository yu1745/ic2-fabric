package ic2_120.content.energy.charge

import ic2_120.content.item.energy.IBatteryItem
import net.minecraft.inventory.Inventory

/**
 * 通用电池放电组件。
 *
 * 通过组合方式复用“电池给机器供能”逻辑，避免各类机器重复实现。
 */
class BatteryDischargerComponent(
    private val inventory: Inventory,
    private val batterySlot: Int,
    private val machineTierProvider: () -> Int,
    private val canDischargeNow: () -> Boolean = { true }
) {
    /**
     * 执行一次放电流程，返回本次实际放电量（EU）。
     */
    fun tick(maxDemand: Long): Long {
        if (!canDischargeNow()) return 0L

        val demand = maxDemand.coerceAtLeast(0L)
        if (demand <= 0L) return 0L

        val batteryStack = inventory.getStack(batterySlot)
        val battery = batteryStack.item as? IBatteryItem ?: return 0L

        // 低级电池不能给高级机器供电。
        if (battery.tier < machineTierProvider()) return 0L
        if (battery.isEmpty(batteryStack)) return 0L

        val transferLimit = battery.transferSpeed.toLong().coerceAtLeast(0L)
        val available = battery.getCurrentCharge(batteryStack).coerceAtLeast(0L)
        val requested = minOf(demand, transferLimit, available)
        if (requested <= 0L) return 0L

        val discharged = battery.discharge(batteryStack, requested).coerceIn(0L, requested)
        if (discharged <= 0L) return 0L

        inventory.setStack(batterySlot, batteryStack)
        return discharged
    }
}
