package ic2_120.test

import com.mcdebug.runner.McDebugTest
import com.mcdebug.runner.TestContext
import com.mcdebug.runner.assertBlockId
import com.mcdebug.runner.assertSlotCount
import com.mcdebug.runner.assertSlotEmpty
import com.mcdebug.runner.assertSlotHas
import com.mcdebug.runner.insertItem
import com.mcdebug.runner.invItemEquals
import com.mcdebug.runner.place
import com.mcdebug.runner.setSlot
import com.mcdebug.runner.waitTicks
import com.mcdebug.runner.waitUntil
import org.junit.jupiter.api.Test

/**
 * 提取机 (Extractor) 测试。
 * 槽位：slot 0 = 输入, slot 1 = 输出, slot 2 = 放电槽, slot 3+ = 升级槽（两个超频）。
 */
@McDebugTest
class ExtractorTest {

    /** 标准搭建：相邻 BatBox + 两个超频。 */
    private fun setupExtractor(ctx: TestContext) {
        setupAdjacentBatbox(ctx, "ic2_120:extractor")
        moveItemStackTo(ctx, ctx.origin, "ic2_120:overclocker_upgrade", 2, 3)
    }

    @Test
    fun `place`(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:extractor")
        assertBlockId(ctx, ctx.origin, "ic2_120:extractor")
    }

    /** 1 粘土 → 4 粘土球。 */
    @Test
    fun clayTo4ClayBalls(ctx: TestContext) {
        setupExtractor(ctx)
        moveItemStackTo(ctx, ctx.origin, "minecraft:clay", 1, 0)
        waitUntil(ctx, invItemEquals(ctx.origin, 1, "minecraft:clay_ball"), 20 * 20)
        assertSlotHas(ctx, ctx.origin, 1, "minecraft:clay_ball")
        assertSlotCount(ctx, ctx.origin, 1, 4)
    }

    /** 1 树脂 → 3 橡胶。 */
    @Test
    fun resinTo3Rubber(ctx: TestContext) {
        setupExtractor(ctx)
        moveItemStackTo(ctx, ctx.origin, "ic2_120:resin", 1, 0)
        waitUntil(ctx, invItemEquals(ctx.origin, 1, "ic2_120:rubber"), 20 * 20)
        assertSlotCount(ctx, ctx.origin, 1, 3)
    }

    /** 无电闲置。 */
    @Test
    fun noPowerIdle(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:extractor")
        moveItemStackTo(ctx, ctx.origin, "minecraft:clay", 1, 0)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 0, "minecraft:clay")
        assertSlotEmpty(ctx, ctx.origin, 1)
    }

    /** 非法输入：泥土无配方。 */
    @Test
    fun invalidInputDirt(ctx: TestContext) {
        setupExtractor(ctx)
        moveItemStackTo(ctx, ctx.origin, "minecraft:dirt", 1, 0)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 0, "minecraft:dirt")
        assertSlotEmpty(ctx, ctx.origin, 1)
    }

    /** 输出满 → 阻塞不消耗输入。 */
    @Test
    fun outputFullBlocksNext(ctx: TestContext) {
        setupExtractor(ctx)
        setSlot(ctx, ctx.origin, 1, "minecraft:clay_ball", 64)
        moveItemStackTo(ctx, ctx.origin, "minecraft:clay", 1, 0)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 0, "minecraft:clay")
    }

}
