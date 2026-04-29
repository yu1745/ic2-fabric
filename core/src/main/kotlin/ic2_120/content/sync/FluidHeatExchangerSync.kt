package ic2_120.content.sync

import ic2_120.content.syncs.SyncSchema

/**
 * 流体热交换机同步属性。
 */
class FluidHeatExchangerSync(
    schema: SyncSchema,
    private val heatFlow: HeatFlowSync? = null
) : SyncSchema by schema {

    companion object {
        const val NBT_INPUT_FLUID = "InputFluid"
        const val NBT_OUTPUT_FLUID = "OutputFluid"
        const val NBT_IS_WORKING = "IsWorking"

        const val TANK_CAPACITY_MB = 10_000
    }

    var inputFluidMb by schema.int(NBT_INPUT_FLUID)
    var outputFluidMb by schema.int(NBT_OUTPUT_FLUID)
    var isWorking by schema.int(NBT_IS_WORKING)

    /** 获取同步的滤波后产热速率（HU/t） */
    fun getSyncedGeneratedHeat(): Long = heatFlow?.getSyncedGeneratedHeat() ?: 0L

    /** 获取同步的滤波后输出速率（HU/t） */
    fun getSyncedOutputHeat(): Long = heatFlow?.getSyncedOutputHeat() ?: 0L
}
