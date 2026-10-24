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
    /** 服务端按秒计算的液位变化速率（mB/s） */
    var levelChange1s by schema.int("LevelChange1s")
    var levelChange5s by schema.int("LevelChange5s")
    var levelChange15s by schema.int("LevelChange15s")
    var levelChange30s by schema.int("LevelChange30s")
    var levelChange60s by schema.int("LevelChange60s")

    val fluidId: Identifier?
        get() {
            if (fluidRawId < 0) return null
            val fluid = Registries.FLUID.get(fluidRawId)
            return Registries.FLUID.getId(fluid)
        }

    val hasFluid: Boolean get() = fluidAmount > 0 && fluidRawId >= 0
}
