package ic2_120.content.sync

import ic2_120.content.syncs.SyncSchema

/**
 * 太阳能蒸馏机同步属性。
 */
class SolarDistillerSync(
    schema: SyncSchema
) : SyncSchema by schema {

    companion object {
        const val NBT_WATER_INPUT = "WaterInput"
        const val NBT_DISTILLED_OUTPUT = "DistilledOutput"
        const val NBT_PROGRESS = "Progress"
        const val NBT_IS_WORKING = "IsWorking"

        /** 容量：8 BUCKET = 648,000 droplets */
        const val TANK_CAPACITY_BUCKETS = 8
        const val PRODUCE_INTERVAL_TICKS = 80
        /** 每周期（80 tick）产出/消耗：1 mB = 81 droplets */
        const val PRODUCE_DROPLETS_PER_CYCLE = 81
    }

    /** 水储量（droplets） */
    var waterInput by schema.int(NBT_WATER_INPUT)
    /** 蒸馏水储量（droplets） */
    var distilledOutput by schema.int(NBT_DISTILLED_OUTPUT)
    var progress by schema.int(NBT_PROGRESS)
    var isWorking by schema.int(NBT_IS_WORKING)
}

