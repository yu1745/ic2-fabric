package ic2_120.test

import com.mcdebug.runner.McDebugTest
import com.mcdebug.runner.TestContext
import com.mcdebug.runner.assertBlockId
import com.mcdebug.runner.assertSlotEmpty
import com.mcdebug.runner.assertSlotHas
import com.mcdebug.runner.insertItem
import com.mcdebug.runner.invItemEquals
import com.mcdebug.runner.place
import com.mcdebug.runner.setBeField
import com.mcdebug.runner.setSlot
import com.mcdebug.runner.waitTicks
import com.mcdebug.runner.waitUntil
import org.junit.jupiter.api.Test

/**
 * 固体装罐机 (Solid Canner) 测试。
 * 槽位：0=材料A, 1=材料B, 2=输出, 3=放电槽, 4+=升级槽。
 */
@McDebugTest
class SolidCannerTest {

    /** 标准搭建：相邻 BatBox 供电，并把机器容量/电量都顶到 4 000 EU。 */
    private fun setupSolidCanner(ctx: TestContext) {
        setupAdjacentBatbox(ctx, "ic2_120:solid_canner")
        setBeField(ctx, ctx.origin, "EnergyCapacity", 4000)
        setBeField(ctx, ctx.origin, "EnergyStored", 4000)
    }

    @Test
    fun `place`(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:solid_canner")
        assertBlockId(ctx, ctx.origin, "ic2_120:solid_canner")
    }

    /** 空燃料棒 + 铀 → 铀燃料棒。 */
    @Test
    fun fuelRodUraniumToUraniumFuelRod(ctx: TestContext) {
        setupSolidCanner(ctx)
        moveItemStackTo(ctx, ctx.origin, "ic2_120:fuel_rod", 1, 0)
        moveItemStackTo(ctx, ctx.origin, "ic2_120:uranium", 1, 1)
        waitUntil(ctx, invItemEquals(ctx.origin, 2, "ic2_120:uranium_fuel_rod"), 15 * 20)
        assertSlotHas(ctx, ctx.origin, 2, "ic2_120:uranium_fuel_rod")
        assertSlotEmpty(ctx, ctx.origin, 0)
        assertSlotEmpty(ctx, ctx.origin, 1)
    }

    /** 空燃料棒 + MOX → MOX 燃料棒。 */
    @Test
    fun fuelRodMoxToMoxFuelRod(ctx: TestContext) {
        setupSolidCanner(ctx)
        moveItemStackTo(ctx, ctx.origin, "ic2_120:fuel_rod", 1, 0)
        moveItemStackTo(ctx, ctx.origin, "ic2_120:mox", 1, 1)
        waitUntil(ctx, invItemEquals(ctx.origin, 2, "ic2_120:mox_fuel_rod"), 15 * 20)
        assertSlotHas(ctx, ctx.origin, 2, "ic2_120:mox_fuel_rod")
        assertSlotEmpty(ctx, ctx.origin, 0)
        assertSlotEmpty(ctx, ctx.origin, 1)
    }

    @Test
    fun noPowerIdle(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:solid_canner")
        moveItemStackTo(ctx, ctx.origin, "ic2_120:fuel_rod", 1, 0)
        moveItemStackTo(ctx, ctx.origin, "ic2_120:uranium", 1, 1)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 0, "ic2_120:fuel_rod")
        assertSlotEmpty(ctx, ctx.origin, 2)
    }

    /** 缺输入：只放一个材料槽应不工作。 */
    @Test
    fun missingInputOneSlotEmpty(ctx: TestContext) {
        setupSolidCanner(ctx)
        moveItemStackTo(ctx, ctx.origin, "ic2_120:fuel_rod", 1, 0)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 0, "ic2_120:fuel_rod")
        assertSlotEmpty(ctx, ctx.origin, 2)
    }

    /** 非法输入：dirt + dirt 无配方。 */
    @Test
    fun invalidInputDirtDirt(ctx: TestContext) {
        setupSolidCanner(ctx)
        moveItemStackTo(ctx, ctx.origin, "minecraft:dirt", 1, 0)
        moveItemStackTo(ctx, ctx.origin, "minecraft:dirt", 1, 1)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 0, "minecraft:dirt")
        assertSlotEmpty(ctx, ctx.origin, 2)
    }

    @Test
    fun outputFullBlocksNext(ctx: TestContext) {
        setupSolidCanner(ctx)
        setSlot(ctx, ctx.origin, 2, "ic2_120:uranium_fuel_rod", 64)
        moveItemStackTo(ctx, ctx.origin, "ic2_120:fuel_rod", 1, 0)
        moveItemStackTo(ctx, ctx.origin, "ic2_120:uranium", 1, 1)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 0, "ic2_120:fuel_rod")
    }
}
