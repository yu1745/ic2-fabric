package ic2_120.content.upgrade

import ic2_120.content.energy.EnergyTier
import ic2_120.content.item.TransformerUpgrade
import ic2_120.registry.instance
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag

/**
 * 高压（变压器）升级处理组件。
 *
 * 每个高压升级提高电压等级 1，从而增加 maxInsertPerTick。
 * 等级 1 = 32 EU/t，等级 2 = 128 EU/t，等级 3 = 512 EU/t。
 */
object TransformerUpgradeComponent {

    /** 高压升级物品的 registry id（延迟解析，注册完成后可用）。 */
    private val UPGRADE_ITEM_ID: String by lazy {
        net.minecraft.core.registries.BuiltInRegistries.ITEM
            .getKey(TransformerUpgrade::class.instance())
            .toString()
    }

    /** 每个电压等级对应的 maxInsertPerTick，委托 [EnergyTier.euPerTickFromTier] */
    fun maxInsertForTier(tier: Int): Long = EnergyTier.euPerTickFromTier(tier)

    /**
     * 从升级槽统计高压升级数量，并应用到机器。
     */
    fun apply(inventory: Container, upgradeSlotIndices: IntArray, machine: Any) {
        if (machine !is ITransformerUpgradeSupport) return

        val count = countUpgrades(inventory, upgradeSlotIndices)
        machine.voltageTierBonus = count
    }

    fun countUpgrades(inventory: Container, upgradeSlotIndices: IntArray): Int {
        var count = 0
        for (idx in upgradeSlotIndices) {
            val stack = inventory.getItem(idx)
            if (!stack.isEmpty && stack.item is TransformerUpgrade) {
                count += stack.count
            }
        }
        return count
    }

    /**
     * 从存档 NBT 的 "Items" 列表统计高压升级数量，语义与 [countUpgrades] 一致。
     *
     * 用于机器 BE 加载后、首次 tick 之前立即恢复 [ITransformerUpgradeSupport.voltageTierBonus]：
     * [ic2_120.content.block.machines.MachineBlockEntity.readNbt] 在子类 inventory 载入之前执行，
     * 无法扫描内存 inventory，而所有机器的物品都经 [net.minecraft.world.ContainerHelper] 以默认
     * "Items" 键落盘，直接解析 NBT 即可得到相同结果。升级物品只能进入升级槽（isValid 强制），
     * 因此全槽扫描与按升级槽统计等价。
     */
    fun countUpgradesInNbt(nbt: CompoundTag): Int {
        // TODO(1.21.1): ItemStack.fromNbt 已移除（parse 需要 HolderLookup.Provider），
        // 这里直接按原版 ContainerHelper 的存档格式读 id/Count，避免引入 lookup 参数改动调用方。
        val items = nbt.getListOrEmpty("Items")
        var count = 0
        for (i in 0 until items.size) {
            val entry = items.getCompoundOrEmpty(i)
            if (entry.getStringOr("id", "") == UPGRADE_ITEM_ID) {
                count += entry.getIntOr("Count", 0).coerceAtLeast(0)
            }
        }
        return count
    }
}
