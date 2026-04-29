package ic2_120.content.upgrade

import ic2_120.content.item.RedstoneInverterUpgrade
import net.minecraft.inventory.Inventory

/**
 * 红石反转升级处理组件。
 *
 * 只要升级槽中存在任意 [RedstoneInverterUpgrade]，就启用反转模式。
 */
object RedstoneInverterUpgradeComponent {

    fun apply(inventory: Inventory, upgradeSlotIndices: IntArray, machine: Any) {
        if (machine !is IRedstoneControlSupport) return
        machine.redstoneInverted = hasInverter(inventory, upgradeSlotIndices)
    }

    fun hasInverter(inventory: Inventory, upgradeSlotIndices: IntArray): Boolean {
        for (idx in upgradeSlotIndices) {
            val stack = inventory.getStack(idx)
            if (!stack.isEmpty && stack.item is RedstoneInverterUpgrade) return true
        }
        return false
    }
}

