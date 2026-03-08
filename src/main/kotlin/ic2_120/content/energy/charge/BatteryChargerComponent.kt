package ic2_120.content.energy.charge

import ic2_120.content.item.energy.IBatteryItem
import net.minecraft.inventory.Inventory

/**
 * 通用电池充电组件。
 *
 * 通过组合方式复用“机器给电池充电”逻辑，避免各类机器重复实现。
 */
class BatteryChargerComponent(
    private val inventory: Inventory,
    private val batterySlot: Int,
    private val machineTierProvider: () -> Int,
    private val machineEnergyProvider: () -> Long,
    private val extractEnergy: (Long) -> Long,
    private val canChargeNow: () -> Boolean = { true }
) {
    /**
     * 执行一次充电流程，返回本次实际充电量（EU）。
     */
    fun tick(): Long {
        if (!canChargeNow()) return 0L

        val batteryStack = inventory.getStack(batterySlot)
        val battery = batteryStack.item as? IBatteryItem ?: return 0L

        if (battery.tier > machineTierProvider()) return 0L
        if (battery.isFullyCharged(batteryStack)) return 0L

        val machineEnergy = machineEnergyProvider().coerceAtLeast(0L)
        val batteryRemaining = (battery.maxCapacity - battery.getCurrentCharge(batteryStack)).coerceAtLeast(0L)
        val transferLimit = battery.transferSpeed.toLong().coerceAtLeast(0L)
        val requested = minOf(transferLimit, machineEnergy, batteryRemaining)
        if (requested <= 0L) return 0L

        val extracted = extractEnergy(requested).coerceIn(0L, requested)
        if (extracted <= 0L) return 0L

        val charged = battery.charge(batteryStack, extracted).coerceIn(0L, extracted)
        if (charged < extracted) {
            battery.discharge(batteryStack, extracted - charged)
        }

        inventory.setStack(batterySlot, batteryStack)
        return charged
    }
}
