package ic2_120.client.ui

import kotlin.reflect.KProperty

/**
 * 用于 GUI 中能量输入/输出速率显示的指数移动平均滤波委派。
 * 每 tick 用 `=` 赋值一次当前容量，读取时返回滤波后的速率，减少因同步抖动造成的数值闪烁。
 *
 * @param alpha 滤波系数 (0, 1]，越小越平滑、响应越慢，推荐 0.2~0.4
 * @param rateFromDelta 从容量差计算瞬时速率：(lastCapacity, currentCapacity) -> rate
 */
fun FilteredRate(
    alpha: Float = 0.05f,
    rateFromDelta: (last: Long, current: Long) -> Long
): FilteredRateDelegate =
    object : FilteredRateDelegate {
        private var lastCapacity: Long = -1
        private var filtered: Float = 0f

        override fun getValue(thisRef: Any?, property: KProperty<*>): Long =
            filtered.toLong().coerceAtLeast(0)

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Long) {
            if (value == lastCapacity) return  // 跳过重复帧：render 每帧调用，sync 每 tick 更新，避免用 0 稀释滤波值
            val raw = if (lastCapacity >= 0) rateFromDelta(lastCapacity, value) else 0L
            lastCapacity = value
            filtered = alpha * raw + (1 - alpha) * filtered
        }
    }

/** 委派接口：赋值接收 Long（当前容量），读取返回 Long（滤波后速率） */
interface FilteredRateDelegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Long
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Long)
}

/** 输出速率：容量减少时为正 */
fun FilteredOutputRate(alpha: Float = 0.25f): FilteredRateDelegate =
    FilteredRate(alpha) { last, current ->
        if (current < last) last - current else 0L
    }

/** 输入速率：容量增加时为正 */
fun FilteredInputRate(alpha: Float = 0.25f): FilteredRateDelegate =
    FilteredRate(alpha) { last, current ->
        if (current > last) current - last else 0L
    }
