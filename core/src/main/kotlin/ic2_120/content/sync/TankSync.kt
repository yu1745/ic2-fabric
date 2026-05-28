package ic2_120.content.sync

import ic2_120.content.syncs.SyncSchema
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

class TankSync(
    schema: SyncSchema,
    capacityMbProvider: () -> Int
) {
    /** 流体储量（droplets） */
    var fluidAmount by schema.int("FluidAmountMb")
    /** 容量（droplets） */
    var capacity by schema.int("CapacityMb", default = capacityMbProvider())
    var fluidRawId by schema.int("FluidRawId", default = -1)

    val fluidId: Identifier?
        get() {
            if (fluidRawId < 0) return null
            val fluid = Registries.FLUID.get(fluidRawId)
            return Registries.FLUID.getId(fluid)
        }

    val hasFluid: Boolean get() = fluidAmount > 0 && fluidRawId >= 0
}
