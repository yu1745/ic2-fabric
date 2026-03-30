package ic2_120.content.block

import ic2_120.content.energy.EnergyTier
import ic2_120.content.item.energy.ITiered
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import net.minecraft.util.math.Direction

/**
 * 能量机器接口
 *
 * 继承 [ITiered]，表示具有能量等级的机器方块实体。
 * 用于区分不同层级的能量设备，确保充电规则正确应用。
 *
 * 分面电压等级（可选）：
 * 某些机器（如变压器）的不同面可能有不同的电压等级。
 * 如果实现了分面电压，机器应重写 [getVoltageTierForSide] 方法。
 */
interface ITieredMachine : ITiered {

    companion object {
        /** 每个电压等级对应的 EU/t，与 [EnergyTier.euPerTickFromTier] 一致。 */
        fun euPerTickFromTier(tier: Int): Long = EnergyTier.euPerTickFromTier(tier)
    }

    /**
     * 机器的能量等级（1-5）
     *
     * 根据 ITiered 接口的规则，机器只能给等级 <= 自己的设备充电。
     * 例如：
     * - 等级 1 发电机：只能给等级 1 电池充电 32
     * - 等级 2 发电机：可以给等级 1 和等级 2 电池充电 128
     * - 等级 3 发电机：可以给等级 1、2、3 电池充电 512
     * - 等级 4 发电机：可以给等级 1、2、3、4 电池充电 2048
     * - 等级 5 发电机：可以给等级 1、2、3、4、5 电池充电 8192
     * 机器的输入速度同样遵循上面规则，例如
     * - 等级 1 机器只能输入 32 EU/t，
     * - 等级 2 机器可以输入 128 EU/t，
     * - 等级 3 机器可以输入 512 EU/t，
     * - 等级 4 机器可以输入 2048 EU/t，
     * - 等级 5 机器可以输入 8192 EU/t。
     * 如果机器想提高耐压等级，那就放置高压升级，每放一个电压等级提升1
     */
    // tier 由 ITiered 继承

    /**
     * 获取机器在特定方向的电压等级。
     *
     * 默认实现返回全局电压等级 [tier]，表示所有面使用相同的电压等级。
     *
     * 对于变压器等设备，不同面可能有不同的电压等级：
     * - 升压模式：正面接收低级能量（tier），其他面输出高级能量（tier+1）
     * - 降压模式：其他面接收高级能量（tier+1），正面输出低级能量（tier）
     * 这些机器应重写此方法以实现分面电压。
     *
     * @param side 方向，null 表示查询整体等级
     * @return 该方向的电压等级
     */
    fun getVoltageTierForSide(side: Direction?): Int = tier

    /**
     * 机器的有效耐压等级（用于电网过压检测与电网输出等级计算）。
     * 若机器实现了 [ITransformerUpgradeSupport]，则为基础等级 + 高压升级带来的等级加成；
     * 否则为 [tier]。
     */
    fun effectiveVoltageTier(): Int =
        tier + (if (this is ITransformerUpgradeSupport) voltageTierBonus else 0)

    /**
     * 获取机器在特定方向的有效电压等级。
     *
     * 返回该面的电压等级加上高压升级带来的加成。
     * 对于实现了分面电压的机器（如变压器），使用该面的特定等级；
     * 对于其他机器，所有面使用相同的全局有效等级。
     *
     * @param side 方向，null 表示整体等级
     * @return 该方向的有效电压等级（基础等级 + 升级加成）
     */
    fun effectiveVoltageTierForSide(side: Direction?): Int {
        // 获取该面的基础电压等级，然后加上高压升级加成
        val sidedTier = getVoltageTierForSide(side)
        return sidedTier + (if (this is ITransformerUpgradeSupport) voltageTierBonus else 0)
    }
}
