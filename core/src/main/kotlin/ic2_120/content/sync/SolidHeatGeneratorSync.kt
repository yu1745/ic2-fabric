package ic2_120.content.sync

import ic2_120.content.syncs.SyncSchema

class SolidHeatGeneratorSync(
    schema: SyncSchema,
    private val heatFlow: HeatFlowSync? = null
) {
    var burnTime by schema.int("BurnTime")
    var burnTotal by schema.int("BurnTotal")

    /** 获取同步的滤波后产热速率（HU/t） */
    fun getSyncedGeneratedHeat(): Long = heatFlow?.getSyncedGeneratedHeat() ?: 0L

    /** 获取同步的滤波后输出速率（HU/t） */
    fun getSyncedOutputHeat(): Long = heatFlow?.getSyncedOutputHeat() ?: 0L
}

