package ic2_120.content.sync

import ic2_120.content.syncs.SyncSchema

/**
 * 高炉同步数据。
 *
 * 纯 HU 累积温度模型：散热随温度线性增长，净HU 驱动温度变化。
 * - T = 0..1401：散热 0 → 50 HU/t
 * - T = 1402..1700：散热 50 → 100 HU/t
 * - 稳态温度完全由 HU 输入决定：50 HU/t → 1401，100 HU/t → 1700，中间线性
 * - 升温速度 ∝ 净HU（输入 − 散热），HU 输入越高升温越快
 *
 * 工作条件：温度 ≥ 1401。温度逻辑每 tick 独立运行，工作时不再冻结温度。
 * 炼钢仅消耗压缩空气，不额外消耗 HU。
 *
 * 每钢锭空气消耗（随温度线性递减，单位 droplets）：
 * - 1401→1500：486,000→421,200 droplets
 * - 1501→1600：421,119→380,700 droplets
 * - 1601→1700：380,619→340,200 droplets
 *
 * 工作时间（随温度线性递减）：
 * - 1401→1500：10000→8400 ticks
 * - 1501→1600：8399→6000 ticks
 * - 1601→1700：5999→4000 ticks
 */
class BlastFurnaceSync(schema: SyncSchema) {

    companion object {
        const val TEMP_MAX = 1_700
        const val TEMP_WORK_MIN = 1_401

        /** 空气储罐容量：8 BUCKET = 648,000 droplets */
        const val AIR_TANK_BUCKETS = 8

        /** 根据温度线性计算每钢锭空气消耗量（droplets） */
        fun getAirPerSteelDroplets(temp: Int): Int = when {
            temp < TEMP_WORK_MIN -> 0
            temp in TEMP_WORK_MIN..1500 -> 486_000 - 64_800 * (temp - 1401) / 99
            temp in 1501..1600 -> 421_119 - 40_419 * (temp - 1501) / 99
            else -> 380_619 - 40_419 * (temp - 1601) / 99  // 1601..TEMP_MAX
        }

        /** 根据温度线性计算加工总 tick 数 */
        fun getProgressMax(temp: Int): Int = when {
            temp < TEMP_WORK_MIN -> 0
            temp in TEMP_WORK_MIN..1500 -> 10000 - 1600 * (temp - 1401) / 99
            temp in 1501..1600 -> 8399 - 2399 * (temp - 1501) / 99
            else -> 5999 - 1999 * (temp - 1601) / 99  // 1601..TEMP_MAX
        }
    }

    var huInput by schema.int("HuInput", default = 0)
    var temperature by schema.int("Temperature", default = 0)
    var progress by schema.int("Progress", default = 0)
    var heatInput by schema.int("HeatInput", default = 0)
    /** 压缩空气储量（droplets），0..648,000 */
    var airAmount by schema.int("AirAmount", default = 0)
    /** 升温条件是否满足（净HU ≥ 0，即缓存足以覆盖当前散热），1=满足 0=不满足 */
    var warmActive by schema.int("WarmActive", default = 0)
}
