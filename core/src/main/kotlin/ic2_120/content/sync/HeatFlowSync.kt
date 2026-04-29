package ic2_120.content.sync

import ic2_120.content.syncs.SyncSchema

/**
 * 热流速率同步组件，用于跟踪和平均热产生/输出速率。
 * 参考 EnergyFlowSync 实现，但专门用于热能单位（HU）。
 */
class HeatFlowSync(
    schema: SyncSchema,
    private val heatProducer: HeatProducer,
    windowSize: Int = 20
) {
    companion object {
        private const val DEFAULT_GENERATED_KEY = "AvgHeatGenerated"
        private const val DEFAULT_OUTPUT_KEY = "AvgHeatOutput"
    }

    interface HeatProducer {
        fun getLastGeneratedHeat(): Long
        fun getLastOutputHeat(): Long
    }

    private var avgGeneratedHeat by schema.intAveraged(DEFAULT_GENERATED_KEY, windowSize = windowSize)
    private var avgOutputHeat by schema.intAveraged(DEFAULT_OUTPUT_KEY, windowSize = windowSize)

    /**
     * 在 tick 结束时调用，同步当前 tick 的实际热产生/输出
     */
    fun syncCurrentTickFlow() {
        val generated = heatProducer.getLastGeneratedHeat()
        val output = heatProducer.getLastOutputHeat()
        avgGeneratedHeat = generated.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
        avgOutputHeat = output.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    }

    /** 获取同步的滤波后热产生速率（HU/t） */
    fun getSyncedGeneratedHeat(): Long = avgGeneratedHeat.toLong().coerceAtLeast(0L)

    /** 获取同步的滤波后热输出速率（HU/t） */
    fun getSyncedOutputHeat(): Long = avgOutputHeat.toLong().coerceAtLeast(0L)
}
