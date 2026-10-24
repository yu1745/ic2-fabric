package ic2_120.content.energy

/**
 * 电压/能量等级对应的标称 EU/t（32 × 4^(tier−1)，tier≤1 为 32）。
 *
 * 线缆、机器 I/O、电池与电动工具的传输速率均基于此表，避免分散填数。
 *
 * 上限 clamp 到 tier 16（coerceAtMost(15)）：覆盖工业升级附属最高 tier 11 的中子太阳能发电机，
 * 以及高压玻璃导线链顶端的 tier 16 无限导线，同时防止异常 tier 值导致 4^n 数值爆炸。
 * tier≥16 按 tier 16 处理（输出 = 32 × 4^15 = 34,359,738,368，远小于 Long.MAX）。
 */
object EnergyTier {
    fun euPerTickFromTier(tier: Int): Long {
        if (tier <= 1) return 32L
        var m = 32L
        repeat((tier - 1).coerceAtMost(15)) { m *= 4 }
        return m
    }
}
