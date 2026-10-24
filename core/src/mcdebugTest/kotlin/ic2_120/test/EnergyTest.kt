package ic2_120.test

import com.mcdebug.runner.McDebugTest
import com.mcdebug.runner.TestContext
import com.mcdebug.runner.assertBlockId
import com.mcdebug.runner.assertSlotEmpty
import com.mcdebug.runner.beFieldGreaterThan
import com.mcdebug.runner.getBeNumber
import com.mcdebug.runner.insertItem
import com.mcdebug.runner.place
import com.mcdebug.runner.setBeField
import com.mcdebug.runner.waitUntil
import org.junit.jupiter.api.Test

/**
 * 发电机 + 储电箱 (Energy) 基础测试。
 * generator：slot 0=燃料输入；燃煤产生 EU。
 * batbox：可存储/读取能量。
 */
@McDebugTest
class EnergyTest {

    @Test
    fun generatorPlace(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:generator")
        assertBlockId(ctx, ctx.origin, "ic2_120:generator")
    }

    /** 燃煤发电：放入煤后 EnergyStored 应 > 0，燃料消耗。 */
    @Test
    fun generatorBurnCoalToEu(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:generator")
        moveItemStackTo(ctx, ctx.origin, "minecraft:coal", 1, 0)
        waitUntil(ctx, beFieldGreaterThan(ctx.origin, "EnergyStored", 0), 5 * 20)
        val energy = getBeNumber(ctx, ctx.origin, "EnergyStored")
        if (energy <= 0) throw AssertionError("expected generator to produce energy, got $energy")
        assertSlotEmpty(ctx, ctx.origin, 0)
    }

    @Test
    fun batboxPlace(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:batbox")
        assertBlockId(ctx, ctx.origin, "ic2_120:batbox")
    }

    /** 写入能量再读回。 */
    @Test
    fun batboxStoreAndReadbackEnergy(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:batbox")
        setBeField(ctx, ctx.origin, "EnergyStored", 12345)
        val read = getBeNumber(ctx, ctx.origin, "EnergyStored")
        if (read != 12345.0) throw AssertionError("expected EnergyStored=12345, got $read")
    }
}
