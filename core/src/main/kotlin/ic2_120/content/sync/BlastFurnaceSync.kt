package ic2_120.content.sync

import ic2_120.content.syncs.SyncSchema

/**
 * 高炉同步数据。
 *
 * HU 无储值，直接消耗每 tick 传入的 HU。
 *
 * 升温消耗（已减半）：
 * - 0–1400：50 HU/tick
 * - 1401–1500：40 HU/tick
 * - 1501–1600：30 HU/tick
 * - 1601–1700：20 HU/tick
 *
 * 工作条件：温度 > 1400。
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
 *
 * 工作时温度冻结，需持续消耗对应温度段的 HU 维持。
 * HU 输入不达标则温度衰减。
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
    /** 升温条件是否满足（消费前缓存 >= 当前温度段 HU/tick 需求），1=满足 0=不满足 */
    var warmActive by schema.int("WarmActive", default = 0)
}
