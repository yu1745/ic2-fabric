package ic2_120.content.energy.charge

import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.item.energy.IElectricTool
import net.minecraft.inventory.Inventory

/**
 * 通用充电组件。
 *
 * 支持电池（[IBatteryItem]）与电动工具（[IElectricTool]）充电。
 * 通过组合方式复用“机器给可充电物品充电”逻辑，避免各类机器重复实现。
 * 每 tick 上限为 [ic2_120.content.item.energy.ITiered.nominalEuPerTick]（与标称线速一致）。
 */
class BatteryChargerComponent(
    private val inventory: Inventory,
    private val batterySlot: Int,
    private val machineTierProvider: () -> Int,
    private val machineEnergyProvider: () -> Long,
    //从机器提取能量的函数，返回实际提取量（EU）
    //机器应传入自己的 extractEnergy 方法的 lambda，以正确追踪输出速率
    private val extractEnergy: (Long) -> Long,
    private val canChargeNow: () -> Boolean = { true }
) {
    /**
     * 执行一次充电流程，返回本次实际充电量（EU）。
     */
    fun tick(): Long {
        if (!canChargeNow()) return 0L

        val stack = inventory.getStack(batterySlot)
        val item = stack.item

        return when (item) {
            is IBatteryItem -> chargeBattery(stack, item)
            is IElectricTool -> chargeElectricTool(stack, item)
            else -> 0L
        }
    }

    private fun chargeBattery(stack: net.minecraft.item.ItemStack, battery: IBatteryItem): Long {
        if (!battery.canCharge) return 0L
        if (battery.tier > machineTierProvider()) return 0L
        if (battery.isFullyCharged(stack)) return 0L

        val machineEnergy = machineEnergyProvider().coerceAtLeast(0L)
        val remaining = (battery.maxCapacity - battery.getCurrentCharge(stack)).coerceAtLeast(0L)
        val transferLimit = battery.nominalEuPerTick()
        val requested = minOf(transferLimit, machineEnergy, remaining)
        if (requested <= 0L) return 0L

        val extracted = extractEnergy(requested).coerceIn(0L, requested)
        if (extracted <= 0L) return 0L

        val charged = battery.charge(stack, extracted).coerceIn(0L, extracted)
        if (charged < extracted) {
            battery.discharge(stack, extracted - charged)
        }

        inventory.setStack(batterySlot, stack)
        return charged
    }

    private fun chargeElectricTool(stack: net.minecraft.item.ItemStack, tool: IElectricTool): Long {
        if (tool.tier > machineTierProvider()) return 0L
        if (tool.isFullyCharged(stack)) return 0L

        val machineEnergy = machineEnergyProvider().coerceAtLeast(0L)
        val remaining = (tool.maxCapacity - tool.getEnergy(stack)).coerceAtLeast(0L)
        val transferLimit = tool.nominalEuPerTick()
        val requested = minOf(transferLimit, machineEnergy, remaining)
        if (requested <= 0L) return 0L
        val extracted = extractEnergy(requested).coerceIn(0L, requested)
        if (extracted <= 0L) return 0L

        tool.setEnergy(stack, tool.getEnergy(stack) + extracted)
        inventory.setStack(batterySlot, stack)
        return extracted
    }
}
