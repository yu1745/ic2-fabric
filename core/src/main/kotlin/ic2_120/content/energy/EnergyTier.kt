package ic2_120.content.energy

/**
 * 电压/能量等级对应的标称 EU/t（32 × 4^(tier−1)，tier≤1 为 32）。
 *
 * 线缆、机器 I/O、电池与电动工具的传输速率均基于此表，避免分散填数。
 */
object EnergyTier {
    fun euPerTickFromTier(tier: Int): Long {
        if (tier <= 1) return 32L
        var m = 32L
        repeat((tier - 1).coerceAtMost(8)) { m *= 4 }
        return m
    }
}
