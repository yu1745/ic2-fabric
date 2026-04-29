package ic2_120.content.block.misc

import kotlin.reflect.KProperty

/**
 * 用于 GUI 中显示后端同步的数值的滑动窗口平均滤波委派。
 * 直接对传入的值进行滤波，不需要通过容量变化计算速率。
 * 适用于后端已经追踪好实际值的情况（如变压器的输入/输出电流）。
 *
 * @param windowSize 窗口大小（tick 数），默认 20（1 秒）
 */
fun FilteredValue(windowSize: Int = 20): FilteredRateDelegate =
    object : FilteredRateDelegate {
        private val window = ArrayDeque<Long>()
        private var lastValue: Long = -1

        override fun getValue(thisRef: Any?, property: KProperty<*>): Long {
            if (window.isEmpty()) return 0
            return window.sum() / window.size
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Long) {
            // if (value == lastValue) return  // 跳过重复帧
            lastValue = value

            window.addLast(value)
            if (window.size > windowSize) {
                window.removeFirst()
            }
        }
    }

/** 委派接口：赋值接收 Long（当前容量），读取返回 Long（滤波后速率） */
interface FilteredRateDelegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Long
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Long)
}