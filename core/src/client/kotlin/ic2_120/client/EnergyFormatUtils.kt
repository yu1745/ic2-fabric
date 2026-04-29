package ic2_120.client

/**
 * 能量格式化工具类
 * 提供能量值（EU）的格式化显示功能
 */
object EnergyFormatUtils {

    /**
     * 格式化能量值为可读字符串
     * - >= 1,000,000: 显示为 M（百万），如 "1.5M"
     * - >= 1,000: 显示为 K（千），如 "10.5K"
     * - < 1,000: 直接显示数值，如 "999"
     *
     * @param value 能量值（EU）
     * @return 格式化后的字符串
     */
    fun formatEu(value: Long): String = when {
        value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
        value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
        else -> value.toString()
    }

    /**
     * 格式化能量值为可读字符串（Int 版本）
     *
     * @param value 能量值（EU）
     * @return 格式化后的字符串
     */
    fun formatEu(value: Int): String = formatEu(value.toLong())
}
