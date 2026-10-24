package ic2_120.content.energy.charge

import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.item.energy.IElectricTool
import ic2_120.content.item.energy.ITiered
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack

/**
 * 通用充电组件。
 *
 * 支持电池（[IBatteryItem]）与电动工具（[IElectricTool]）充电。
 * 通过组合方式复用“机器给可充电物品充电”逻辑，避免各类机器重复实现。
 * 每 tick 上限默认取物品自身 [ic2_120.content.item.energy.ITiered.nominalEuPerTick]（与标称线速一致），
 * 也可通过 [transferRateProvider] 覆盖，供机器实现“升级加成充电速度”
 * （如采矿机高压升级提升扫描仪充电电压等级）。
 */
class BatteryChargerComponent(
    private val inventory: Container,
    private val batterySlot: Int,
    private val machineTierProvider: () -> Int,
    private val machineEnergyProvider: () -> Long,
    //从机器提取能量的函数，返回实际提取量（EU）
    //机器应传入自己的 extractEnergy 方法的 lambda，以正确追踪输出速率
    private val extractEnergy: (Long) -> Long,
    //向机器插入能量的函数，返回实际插入量（EU），用于放电模式
    private val insertEnergy: (Long) -> Long = { 0L },
    //单 tick 充电速率上限（EU）。默认取物品自身 nominalEuPerTick()；
    //机器可覆盖此回调实现充电速度加成（如每个高压升级让扫描仪充电快 4 倍）。
    private val transferRateProvider: (ItemStack) -> Long = { stack ->
        (stack.item as? ITiered)?.nominalEuPerTick() ?: 0L
    },
    private val canChargeNow: () -> Boolean = { true }
) {
    /**
     * 执行一次充电流程，返回本次实际充电量（EU）。
     */
    fun tick(): Long {
        if (!canChargeNow()) return 0L

        val stack = inventory.getItem(batterySlot)
        val item = stack.item

        return when (item) {
            is IBatteryItem -> chargeBattery(stack, item)
            is IElectricTool -> chargeElectricTool(stack, item)
            else -> 0L
        }
    }

    /**
     * 执行一次放电流程（电池→机器），返回本次实际放电量（EU）。
     * 仅支持 [IBatteryItem]，不支持电动工具。
     */
    fun discharge(): Long {
        if (!canChargeNow()) return 0L

       val stack = inventory.getItem(batterySlot)
       val item = stack.item as? IBatteryItem ?: return 0L
       if (item.tier > machineTierProvider()) return 0L
        if (stack.count > 1) return 0L

        val currentCharge = item.getCurrentCharge(stack)
        if (currentCharge <= 0L) return 0L

        // machineEnergyProvider 在放电模式下应返回机器剩余容量
        val remainingCapacity = machineEnergyProvider().coerceAtLeast(0L)
        if (remainingCapacity <= 0L) return 0L

        val transferLimit = item.nominalEuPerTick()
        val requested = minOf(transferLimit, currentCharge, remainingCapacity)
        if (requested <= 0L) return 0L

        val discharged = item.discharge(stack, requested).coerceIn(0L, requested)
        if (discharged <= 0L) return 0L

        val inserted = insertEnergy(discharged).coerceIn(0L, discharged)
        if (inserted < discharged) {
            // 机器没接受的部分回充到电池
            item.charge(stack, discharged - inserted)
        }

        inventory.setItem(batterySlot, stack)
        return inserted
    }

    private fun chargeBattery(stack: net.minecraft.world.item.ItemStack, battery: IBatteryItem): Long {
        if (!battery.canCharge) return 0L
        if (battery.tier > machineTierProvider()) return 0L
        if (battery.isFullyCharged(stack)) return 0L
        // 堆叠的电池不能充电，避免 NBT 数据混乱
        if (stack.count > 1) return 0L

        val machineEnergy = machineEnergyProvider().coerceAtLeast(0L)
        val remaining = (battery.maxCapacity - battery.getCurrentCharge(stack)).coerceAtLeast(0L)
        val transferLimit = transferRateProvider(stack)
        val requested = minOf(transferLimit, machineEnergy, remaining)
        if (requested <= 0L) return 0L

        val extracted = extractEnergy(requested).coerceIn(0L, requested)
        if (extracted <= 0L) return 0L

        val charged = battery.charge(stack, extracted).coerceIn(0L, extracted)
        if (charged < extracted) {
            battery.discharge(stack, extracted - charged)
        }

        inventory.setItem(batterySlot, stack)
        return charged
    }

    private fun chargeElectricTool(stack: net.minecraft.world.item.ItemStack, tool: IElectricTool): Long {
        if (tool.tier > machineTierProvider()) return 0L
        if (tool.isFullyCharged(stack)) return 0L

        val machineEnergy = machineEnergyProvider().coerceAtLeast(0L)
        val remaining = (tool.maxCapacity - tool.getEnergy(stack)).coerceAtLeast(0L)
        val transferLimit = transferRateProvider(stack)
        val requested = minOf(transferLimit, machineEnergy, remaining)
        if (requested <= 0L) return 0L
        val extracted = extractEnergy(requested).coerceIn(0L, requested)
        if (extracted <= 0L) return 0L

        tool.setEnergy(stack, tool.getEnergy(stack) + extracted)
        inventory.setItem(batterySlot, stack)
        return extracted
    }
}
