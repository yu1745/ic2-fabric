package ic2_120.content.upgrade

import ic2_120.content.item.EnergyStorageUpgrade
import net.minecraft.inventory.Inventory

/**
 * 储能升级处理组件。
 *
 * 每个储能升级增加 1 万 EU 电量缓冲。
 */
object EnergyStorageUpgradeComponent {

    /** 每个储能升级增加的容量（EU） */
    const val CAPACITY_PER_UPGRADE = 10_000L

    /**
     * 从升级槽统计储能升级数量，并应用到机器。
     */
    fun apply(inventory: Inventory, upgradeSlotIndices: IntArray, machine: Any) {
        if (machine !is IEnergyStorageUpgradeSupport) return

        val count = countUpgrades(inventory, upgradeSlotIndices)
        machine.capacityBonus = count * CAPACITY_PER_UPGRADE
    }

    fun countUpgrades(inventory: Inventory, upgradeSlotIndices: IntArray): Int {
        var count = 0
        for (idx in upgradeSlotIndices) {
            val stack = inventory.getStack(idx)
            if (!stack.isEmpty && stack.item is EnergyStorageUpgrade) {
                count += stack.count
            }
        }
        return count
    }
}
