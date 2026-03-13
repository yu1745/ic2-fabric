package ic2_120.content.energy.charge

import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.item.energy.IElectricTool
import net.minecraft.inventory.Inventory

/** 电动工具默认充电速度（EU/t），按等级缩放 */
private const val ELECTRIC_TOOL_CHARGE_SPEED_BASE = 32

/**
 * 通用充电组件。
 *
 * 支持电池（[IBatteryItem]）与电动工具（[IElectricTool]）充电。
 * 通过组合方式复用“机器给可充电物品充电”逻辑，避免各类机器重复实现。
 */
class BatteryChargerComponent(
    private val inventory: Inventory,
    private val batterySlot: Int,
    private val machineTierProvider: () -> Int,
    private val machineEnergyProvider: () -> Long,
    //从机器提取能量的函数，返回实际提取量（EU）
    //机器通常需要传入自己的consumeEnergy函数的lambda
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
        if (battery.tier > machineTierProvider()) return 0L
        if (battery.isFullyCharged(stack)) return 0L

        val machineEnergy = machineEnergyProvider().coerceAtLeast(0L)
        val remaining = (battery.maxCapacity - battery.getCurrentCharge(stack)).coerceAtLeast(0L)
        val transferLimit = battery.transferSpeed.toLong().coerceAtLeast(0L) * 4 //充电速度设置为4倍
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
        val transferLimit = (ELECTRIC_TOOL_CHARGE_SPEED_BASE * tool.tier * 4).toLong().coerceAtLeast(1L) * 4 //充电速度设置为放电4倍
        val requested = minOf(transferLimit, machineEnergy, remaining)
        if (requested <= 0L) return 0L
        val extracted = extractEnergy(requested).coerceIn(0L, requested)
        if (extracted <= 0L) return 0L

        tool.setEnergy(stack, tool.getEnergy(stack) + extracted)
        inventory.setStack(batterySlot, stack)
        return extracted
    }
}
