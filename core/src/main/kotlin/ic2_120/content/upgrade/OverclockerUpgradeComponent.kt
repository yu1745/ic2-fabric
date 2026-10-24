package ic2_120.content.upgrade

import ic2_120.content.item.OverclockerUpgrade
import net.minecraft.world.Container

/**
 * 加速升级处理组件。
 *
 * 识别升级槽中 [OverclockerUpgrade] 的数量，按指数形式累乘计算速度与耗能倍率，
 * 并写入实现 [IOverclockerUpgradeSupport] 的机器。
 *
 * 公式（可叠加，指数增长）：
 * - 每个超频：工作时间缩短到 70%（速度倍率 = 1/0.7）
 * - 每个超频：额外消耗 60% 电力（耗能倍率 = 1.6）
 * - n 个：速度 = (1/0.7)^n，耗能 = 1.6^n
 *
 * 无数量上限（2026-08 起移除 16 个 clamp）。大 count 下 Float 只正溢出 +Infinity
 * （速度 n≈249、耗能 n≈189，永不为负），各机器 toInt/toLong 饱和、consumeEnergy
 * 全有全无，超量超频只会让机器因耗能超容量而停摆，不会出现负值/白嫖。
 */
object OverclockerUpgradeComponent {

    /** 每个超频的速度倍率（工作时间 70% = 速度 1/0.7） */
    private const val SPEED_PER_UPGRADE = 1f / 0.7f

    /** 每个超频的耗能倍率（额外 60% = 1.6） */
    private const val ENERGY_PER_UPGRADE = 1.6f

    /**
     * 从升级槽统计加速升级数量，并应用到机器。
     * 若 [machine] 未实现 [IOverclockerUpgradeSupport]，则不执行。
     */
    fun apply(inventory: Container, upgradeSlotIndices: IntArray, machine: Any) {
        if (machine !is IOverclockerUpgradeSupport) return

        val count = countOverclockers(inventory, upgradeSlotIndices)

        machine.speedMultiplier = speedMultiplier(count)
        machine.energyMultiplier = energyMultiplier(count)
    }

    /** 统计升级槽中加速升级的数量 */
    fun countOverclockers(inventory: Container, upgradeSlotIndices: IntArray): Int {
        var count = 0
        for (idx in upgradeSlotIndices) {
            val stack = inventory.getItem(idx)
            if (!stack.isEmpty && stack.item is OverclockerUpgrade) {
                count += stack.count
            }
        }
        return count
    }

    /** 速度倍率：(1/0.7)^n，无上限 */
    fun speedMultiplier(count: Int): Float {
        if (count <= 0) return 1f
        var m = 1f
        repeat(count) { m *= SPEED_PER_UPGRADE }
        return m
    }

    /** 耗能倍率：1.6^n，无上限 */
    fun energyMultiplier(count: Int): Float {
        if (count <= 0) return 1f
        var m = 1f
        repeat(count) { m *= ENERGY_PER_UPGRADE }
        return m
    }
}
