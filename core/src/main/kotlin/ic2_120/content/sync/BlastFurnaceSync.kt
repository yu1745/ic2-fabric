package ic2_120.content.sync

import ic2_120.content.syncs.SyncSchema

/**
 * 高炉同步数据。
 *
 * HU 无储值，直接消耗每 tick 传入的 HU。
 *
 * 升温消耗：
 * - 0–1400：100 HU/tick
 * - 1401–1500：80 HU/tick
 * - 1501–1600：60 HU/tick
 * - 1601–1700：40 HU/tick
 *
 * 工作条件：温度 > 1400，每钢锭消耗 6,000 mB 压缩空气。
 *
 * 工作时间（随温度提速）：
 * - 1401–1500：8,400 ticks（420 秒）
 * - 1501–1600：6,000 ticks（300 秒）
 * - 1601–1700：4,000 ticks（200 秒）
 *
 * 工作时温度冻结，需持续消耗对应温度段的 HU 维持。
 * HU 输入不达标则温度衰减。
 */
class BlastFurnaceSync(schema: SyncSchema) {

    companion object {
        const val TEMP_MAX = 1_700
        const val TEMP_WORK_MIN = 1_401

        const val AIR_CAPACITY_MB = 8_000
        const val AIR_PER_STEEL_MB = 6_000

        const val PROGRESS_MAX_SLOW = 8_400
        const val PROGRESS_MAX_MEDIUM = 6_000
        const val PROGRESS_MAX_FAST = 4_000
    }

    var huInput by schema.int("HuInput", default = 0)
    var temperature by schema.int("Temperature", default = 0)
    var progress by schema.int("Progress", default = 0)
    var heatInput by schema.int("HeatInput", default = 0)
    var airAmountMb by schema.int("AirAmount", default = 0)
}
