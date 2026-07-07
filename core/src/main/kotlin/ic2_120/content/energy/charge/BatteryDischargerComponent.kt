package ic2_120.content.energy.charge

import ic2_120.content.item.energy.IBatteryItem
import net.minecraft.item.Items
import net.minecraft.inventory.Inventory

/**
 * 通用电池放电组件。
 *
 * 通过组合方式复用"电池给机器供能"逻辑，避免各类机器重复实现。
 * 同时支持红石一次性转化：放电槽中放入红石粉时，每 tick 消耗 1 个转化为 800 EU。
 * 红石转化不受 canDischargeNow / maxDemand 限制，允许溢出机器容量上限。
 */
class BatteryDischargerComponent(
    private val inventory: Inventory,
    private val batterySlot: Int,
    private val machineTierProvider: () -> Int,
    private val canDischargeNow: () -> Boolean = { true }
) {
    companion object {
        const val REDSTONE_EU_PER_ITEM = 800L
    }

    /**
     * 执行一次放电流程，返回本次实际放电量（EU）。
     *
     * 红石转化优先：放电槽中是红石粉时消耗 1 个返回 800 EU，不受 canDischargeNow / maxDemand 限制。
     * 调用方应使用 forceInsertEnergy 接收返回值以允许溢出容量。
     */
    fun tick(maxDemand: Long): Long {
        // 红石转化：无视 canDischargeNow / maxDemand
        val stack = inventory.getStack(batterySlot)
        if (!stack.isEmpty && stack.item === Items.REDSTONE) {
            stack.decrement(1)
            inventory.setStack(batterySlot, stack)
            return REDSTONE_EU_PER_ITEM
        }

        if (!canDischargeNow()) return 0L

        val demand = maxDemand.coerceAtLeast(0L)
        if (demand <= 0L) return 0L

        val battery = stack.item as? IBatteryItem ?: return 0L

        // 低级电池不能给高级机器供电。
        if (battery.tier < machineTierProvider()) return 0L
        if (battery.isEmpty(stack)) return 0L

        val transferLimit = battery.nominalEuPerTick()
        val available = battery.getCurrentCharge(stack).coerceAtLeast(0L)
        val requested = minOf(demand, transferLimit, available)
        if (requested <= 0L) return 0L

        val discharged = battery.discharge(stack, requested).coerceIn(0L, requested)
        if (discharged <= 0L) return 0L

        inventory.setStack(batterySlot, stack)
        return discharged
    }
}
