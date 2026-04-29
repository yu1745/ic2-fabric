package ic2_120.content.crop

import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos

interface CropCareTarget {
    // 施肥
    fun applyFertilizer(amount: Int, simulate: Boolean = false): Int
    // 浇水
    fun applyHydration(amount: Int, simulate: Boolean = false): Int
    // 除草剂
    fun applyWeedEx(amount: Int, simulate: Boolean = false): Int
}

data class CropCareOps(
    val fertilizer: Int = 0,
    val hydration: Int = 0,
    val weedEx: Int = 0,
)

data class CropCareResult(
    var touched: Int = 0,
    var fertilizerUsed: Int = 0,
    var hydrationUsed: Int = 0,
    var weedExUsed: Int = 0,
)

object CropCareService {
    fun applyInArea(world: ServerWorld, center: BlockPos, radius: Int, ops: CropCareOps): CropCareResult {
        val result = CropCareResult()
        val r = radius.coerceAtLeast(0)
        for (x in -r..r) {
            for (y in -1..1) {
                for (z in -r..r) {
                    val pos = center.add(x, y, z)
                    val be = world.getBlockEntity(pos) as? CropCareTarget ?: continue
                    result.touched++
                    if (ops.fertilizer > 0) result.fertilizerUsed += be.applyFertilizer(ops.fertilizer, simulate = false)
                    if (ops.hydration > 0) result.hydrationUsed += be.applyHydration(ops.hydration, simulate = false)
                    if (ops.weedEx > 0) result.weedExUsed += be.applyWeedEx(ops.weedEx, simulate = false)
                }
            }
        }
        return result
    }
}
