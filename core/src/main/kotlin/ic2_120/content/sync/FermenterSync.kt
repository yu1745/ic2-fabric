package ic2_120.content.sync

import ic2_120.content.fluid.ModFluids
import ic2_120.content.syncs.SyncSchema
import net.minecraft.fluid.Fluid
import net.minecraft.fluid.Fluids

class FermenterSync(
    schema: SyncSchema
) : SyncSchema by schema {

    companion object {
        const val NBT_INPUT_BIOMASS = "InputBiomass"
        const val NBT_OUTPUT_BIOGAS = "OutputBiogas"
        const val NBT_BUFFERED_HEAT = "BufferedHeat"
        const val NBT_PROGRESS = "Progress"
        const val NBT_IS_WORKING = "IsWorking"
        const val NBT_HEAT_INPUT_PER_TICK = "HeatInputPerTick"
        const val NBT_HEAT_CONSUME_PER_TICK = "HeatConsumePerTick"
        const val NBT_FERTILIZER_PROGRESS = "FertilizerProgress"
        const val NBT_INPUT_FLUID_TYPE = "InputFluidType"
        const val NBT_OUTPUT_FLUID_TYPE = "OutputFluidType"

        const val TANK_CAPACITY_MB = 8_000
        const val PROCESS_INTERVAL_TICKS = 40
        const val INPUT_MB_PER_CYCLE = 20
        const val OUTPUT_MB_PER_CYCLE = 400
        const val HEAT_PER_CYCLE = 4_000
        const val FERTILIZER_PER_BIOMASS_BUCKET_MB = 1_000

        const val FLUID_TYPE_EMPTY = 0
        const val FLUID_TYPE_BIOMASS = 1
        const val FLUID_TYPE_BIOFUEL = 2
    }

    var inputBiomassMb by schema.int(NBT_INPUT_BIOMASS)
    var outputBiogasMb by schema.int(NBT_OUTPUT_BIOGAS)
    var bufferedHeat by schema.int(NBT_BUFFERED_HEAT)
    var progress by schema.int(NBT_PROGRESS)
    var isWorking by schema.int(NBT_IS_WORKING)
    var heatInputPerTick by schema.int(NBT_HEAT_INPUT_PER_TICK)
    var heatConsumePerTick by schema.int(NBT_HEAT_CONSUME_PER_TICK)
    var fertilizerProgress by schema.int(NBT_FERTILIZER_PROGRESS)
    var inputFluidType by schema.int(NBT_INPUT_FLUID_TYPE)
    var outputFluidType by schema.int(NBT_OUTPUT_FLUID_TYPE)

    fun fluidTypeToFluid(type: Int): Fluid? = when (type) {
        FLUID_TYPE_BIOMASS -> ModFluids.BIOMASS_STILL
        FLUID_TYPE_BIOFUEL -> ModFluids.BIOFUEL_STILL
        else -> null
    }

    fun fluidToType(fluid: Fluid?): Int = when {
        fluid == null || fluid == Fluids.EMPTY -> FLUID_TYPE_EMPTY
        fluid == ModFluids.BIOMASS_STILL || fluid == ModFluids.BIOMASS_FLOWING -> FLUID_TYPE_BIOMASS
        fluid == ModFluids.BIOFUEL_STILL || fluid == ModFluids.BIOFUEL_FLOWING -> FLUID_TYPE_BIOFUEL
        else -> FLUID_TYPE_EMPTY
    }
}
