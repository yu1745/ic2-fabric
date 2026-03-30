package ic2_120.content.upgrade

import ic2_120.content.energy.EnergyTier
import ic2_120.content.item.TransformerUpgrade
import net.minecraft.inventory.Inventory

/**
 * 高压（变压器）升级处理组件。
 *
 * 每个高压升级提高电压等级 1，从而增加 maxInsertPerTick。
 * 等级 1 = 32 EU/t，等级 2 = 128 EU/t，等级 3 = 512 EU/t。
 */
object TransformerUpgradeComponent {

    /** 每个电压等级对应的 maxInsertPerTick，委托 [EnergyTier.euPerTickFromTier] */
    fun maxInsertForTier(tier: Int): Long = EnergyTier.euPerTickFromTier(tier)

    /**
     * 从升级槽统计高压升级数量，并应用到机器。
     */
    fun apply(inventory: Inventory, upgradeSlotIndices: IntArray, machine: Any) {
        if (machine !is ITransformerUpgradeSupport) return

        val count = countUpgrades(inventory, upgradeSlotIndices)
        machine.voltageTierBonus = count
    }

    fun countUpgrades(inventory: Inventory, upgradeSlotIndices: IntArray): Int {
        var count = 0
        for (idx in upgradeSlotIndices) {
            val stack = inventory.getStack(idx)
            if (!stack.isEmpty && stack.item is TransformerUpgrade) {
                count += stack.count
            }
        }
        return count
    }
}
