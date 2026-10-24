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
 * 金属成型机 (Metal Former) 测试。
 * 槽位：0=输入, 1=输出, 2=放电槽, 3+=升级槽。
 * 模式（MetalFormerSync.Mode）：0=ROLLING 辊压, 1=CUTTING 切割, 2=EXTRUDING 挤压。
 */
@McDebugTest
class MetalFormerTest {

    /** 标准搭建：相邻 BatBox + 设定模式。 */
    private fun setupMetalFormer(ctx: TestContext, mode: Int) {
        setupAdjacentBatbox(ctx, "ic2_120:metal_former")
        setBeField(ctx, ctx.origin, "Mode", mode)
    }

    @Test
    fun `place`(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:metal_former")
        assertBlockId(ctx, ctx.origin, "ic2_120:metal_former")
    }

    /** 辊压：铁锭 → 铁板。 */
    @Test
    fun rollingIronIngotToIronPlate(ctx: TestContext) {
        setupMetalFormer(ctx, 0)
        moveItemStackTo(ctx, ctx.origin, "minecraft:iron_ingot", 1, 0)
        waitUntil(ctx, invItemEquals(ctx.origin, 1, "ic2_120:iron_plate"), 15 * 20)
        assertSlotHas(ctx, ctx.origin, 1, "ic2_120:iron_plate")
    }

    /** 切割：铁板 → 4 铁缆。 */
    @Test
    fun cuttingIronPlateTo4IronCable(ctx: TestContext) {
        setupMetalFormer(ctx, 1)
        moveItemStackTo(ctx, ctx.origin, "ic2_120:iron_plate", 1, 0)
        waitUntil(ctx, invItemEquals(ctx.origin, 1, "ic2_120:iron_cable"), 15 * 20)
        assertSlotHas(ctx, ctx.origin, 1, "ic2_120:iron_cable")
    }

    /** 挤压：铁锭 → 4 铁缆。 */
    @Test
    fun extrudingIronIngotTo4IronCable(ctx: TestContext) {
        setupMetalFormer(ctx, 2)
        moveItemStackTo(ctx, ctx.origin, "minecraft:iron_ingot", 1, 0)
        waitUntil(ctx, invItemEquals(ctx.origin, 1, "ic2_120:iron_cable"), 15 * 20)
        assertSlotHas(ctx, ctx.origin, 1, "ic2_120:iron_cable")
    }

    @Test
    fun noPowerIdle(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:metal_former")
        moveItemStackTo(ctx, ctx.origin, "minecraft:iron_ingot", 1, 0)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 0, "minecraft:iron_ingot")
        assertSlotEmpty(ctx, ctx.origin, 1)
    }

    /** 辊压模式下泥土无配方。 */
    @Test
    fun invalidInputDirtInRollingMode(ctx: TestContext) {
        setupMetalFormer(ctx, 0)
        moveItemStackTo(ctx, ctx.origin, "minecraft:dirt", 1, 0)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 0, "minecraft:dirt")
        assertSlotEmpty(ctx, ctx.origin, 1)
    }

    @Test
    fun outputFullBlocksNext(ctx: TestContext) {
        setupMetalFormer(ctx, 0)
        setSlot(ctx, ctx.origin, 1, "ic2_120:iron_plate", 64)
        moveItemStackTo(ctx, ctx.origin, "minecraft:iron_ingot", 1, 0)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 0, "minecraft:iron_ingot")
    }
}
