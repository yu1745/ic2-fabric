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

        const val TANK_CAPACITY_MB = 10_000
        const val PRODUCE_INTERVAL_TICKS = 80
        //todo 暂时放大，因为我要测试管道
        const val PRODUCE_MB_PER_CYCLE = 1000
    }

    var waterInputMb by schema.int(NBT_WATER_INPUT)
    var distilledOutputMb by schema.int(NBT_DISTILLED_OUTPUT)
    var progress by schema.int(NBT_PROGRESS)
    var isWorking by schema.int(NBT_IS_WORKING)
}

