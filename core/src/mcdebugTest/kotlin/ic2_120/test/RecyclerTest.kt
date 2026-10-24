package ic2_120.test

import com.mcdebug.runner.McDebugTest
import com.mcdebug.runner.TestContext
import com.mcdebug.runner.assertBlockId
import com.mcdebug.runner.assertSlotEmpty
import com.mcdebug.runner.assertSlotHas
import com.mcdebug.runner.getSlotCount
import com.mcdebug.runner.insertItem
import com.mcdebug.runner.invCountLessThan
import com.mcdebug.runner.place
import com.mcdebug.runner.setSlot
import com.mcdebug.runner.waitTicks
import com.mcdebug.runner.waitUntil
import org.junit.jupiter.api.Test

/**
 * 回收机 (Recycler) 测试。
 * 槽位：0=输入, 1=输出, 2=放电槽（RE 电池）。
 */
@McDebugTest
class RecyclerTest {

    /** 标准搭建：放置回收机 + 写入一块带 10 000 EU 电量的 RE 电池。 */
    private fun setupRecyclerBattery(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:recycler")
        setSlot(ctx, ctx.origin, 2, "ic2_120:re_battery", 1, mapOf("Energy" to 10000))
    }

    @Test
    fun `place`(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:recycler")
        assertBlockId(ctx, ctx.origin, "ic2_120:recycler")
    }

    /** 电池供电：输入物品应被消耗（回收概率产出可忽略，只断言消耗）。 */
    @Test
    fun consumeInputWithBattery(ctx: TestContext) {
        setupRecyclerBattery(ctx)
        moveItemStackTo(ctx, ctx.origin, "minecraft:cobblestone", 10, 0)
        waitUntil(ctx, invCountLessThan(ctx.origin, 0, 10), 15 * 20)
        if (getSlotCount(ctx, ctx.origin, 0) >= 10) {
            throw AssertionError("expected recycler to consume input items, still ${getSlotCount(ctx, ctx.origin, 0)}")
        }
    }

    /** 无电闲置：无电池时输入不应被消耗。 */
    @Test
    fun noPowerIdle(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:recycler")
        moveItemStackTo(ctx, ctx.origin, "minecraft:cobblestone", 10, 0)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 0, "minecraft:cobblestone")
        assertSlotEmpty(ctx, ctx.origin, 1)
    }

    /** 非法输入：木棍无回收价值（不消耗）。 */
    @Test
    fun invalidInputStick(ctx: TestContext) {
        setupRecyclerBattery(ctx)
        moveItemStackTo(ctx, ctx.origin, "minecraft:stick", 10, 0)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 0, "minecraft:stick")
    }
}
