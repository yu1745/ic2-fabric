package ic2_120.content.sync

import ic2_120.content.fluid.ModFluids
import ic2_120.content.syncs.SyncSchema
import net.minecraft.fluid.Fluid
import net.minecraft.fluid.Fluids

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
        const val NBT_INPUT_FLUID_TYPE = "InputFluidType"
        const val NBT_OUTPUT_FLUID_TYPE = "OutputFluidType"

        const val TANK_CAPACITY_MB = 8_000

        const val FLUID_TYPE_EMPTY = 0
        const val FLUID_TYPE_HOT_COOLANT = 1
        const val FLUID_TYPE_LAVA = 2
        const val FLUID_TYPE_COOLANT = 3
        const val FLUID_TYPE_PAHOEHOE_LAVA = 4
    }

    var inputFluidMb by schema.int(NBT_INPUT_FLUID)
    var outputFluidMb by schema.int(NBT_OUTPUT_FLUID)
    var isWorking by schema.int(NBT_IS_WORKING)
    var inputFluidType by schema.int(NBT_INPUT_FLUID_TYPE)
    var outputFluidType by schema.int(NBT_OUTPUT_FLUID_TYPE)

    /** 获取同步的滤波后产热速率（HU/t） */
    fun getSyncedGeneratedHeat(): Long = heatFlow?.getSyncedGeneratedHeat() ?: 0L
    /** 获取同步的滤波后输出速率（HU/t） */
    fun getSyncedOutputHeat(): Long = heatFlow?.getSyncedOutputHeat() ?: 0L

    fun fluidTypeToFluid(type: Int): Fluid? = when (type) {
        FLUID_TYPE_HOT_COOLANT -> ModFluids.HOT_COOLANT_STILL
        FLUID_TYPE_LAVA -> Fluids.LAVA
        FLUID_TYPE_COOLANT -> ModFluids.COOLANT_STILL
        FLUID_TYPE_PAHOEHOE_LAVA -> ModFluids.PAHOEHOE_LAVA_STILL
        else -> null
    }

    fun fluidToFluidType(fluid: Fluid?): Int = when {
        fluid == null || fluid == Fluids.EMPTY -> FLUID_TYPE_EMPTY
        fluid == ModFluids.HOT_COOLANT_STILL || fluid == ModFluids.HOT_COOLANT_FLOWING -> FLUID_TYPE_HOT_COOLANT
        fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA -> FLUID_TYPE_LAVA
        fluid == ModFluids.COOLANT_STILL || fluid == ModFluids.COOLANT_FLOWING -> FLUID_TYPE_COOLANT
        fluid == ModFluids.PAHOEHOE_LAVA_STILL || fluid == ModFluids.PAHOEHOE_LAVA_FLOWING -> FLUID_TYPE_PAHOEHOE_LAVA
        else -> FLUID_TYPE_EMPTY
    }
}
