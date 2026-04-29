package ic2_120.content.upgrade

/**
 * 机器支持高压（变压器）升级的接口。
 *
 * 每个高压升级提高电压等级 1，从而增加从导线获取能量的速度。
 * 各等级 EU/t 见 [ic2_120.content.energy.EnergyTier]（32 × 4^(tier−1)）。
 * 超频升级放多后耗能增加，等级 1 的输入速度跟不上，需配合高压升级。
 */
interface ITransformerUpgradeSupport {

    /** 高压升级带来的电压等级加成，由 [TransformerUpgradeComponent] 每 tick 写入 */
    var voltageTierBonus: Int
}
