package ic2_120.content.sync

import ic2_120.content.syncs.SyncSchema

/**
 * 发酵机同步属性。
 */
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

        const val TANK_CAPACITY_MB = 10_000
        const val PROCESS_INTERVAL_TICKS = 40
        const val INPUT_MB_PER_CYCLE = 20
        const val OUTPUT_MB_PER_CYCLE = 400
        const val HEAT_PER_CYCLE = 4_000
    }

    var inputBiomassMb by schema.int(NBT_INPUT_BIOMASS)
    var outputBiogasMb by schema.int(NBT_OUTPUT_BIOGAS)
    var bufferedHeat by schema.int(NBT_BUFFERED_HEAT)
    var progress by schema.int(NBT_PROGRESS)
    var isWorking by schema.int(NBT_IS_WORKING)
    var heatInputPerTick by schema.int(NBT_HEAT_INPUT_PER_TICK)
    var heatConsumePerTick by schema.int(NBT_HEAT_CONSUME_PER_TICK)
}
