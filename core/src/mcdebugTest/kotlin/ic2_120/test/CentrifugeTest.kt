package ic2_120.test

import com.mcdebug.runner.McDebugTest
import com.mcdebug.runner.TestContext
import com.mcdebug.runner.assertBlockId
import com.mcdebug.runner.assertSlotEmpty
import com.mcdebug.runner.assertSlotHas
import com.mcdebug.runner.beFieldGreaterThan
import com.mcdebug.runner.insertItem
import com.mcdebug.runner.invItemEquals
import com.mcdebug.runner.place
import com.mcdebug.runner.setBeField
import com.mcdebug.runner.setBlocks
import com.mcdebug.runner.setSlot
import com.mcdebug.runner.waitTicks
import com.mcdebug.runner.waitUntil
import org.junit.jupiter.api.Test

/**
 * 离心机 (Centrifuge) 测试。
 * 槽位：slot 0=输入, slot 1=输出, slot 2=放电槽, slot 3+=升级槽。
 * 热量：heat_starve 用例验证冷离心机不工作。
 */
@McDebugTest
class CentrifugeTest {

    /** 标准搭建：东侧 CESU 供电，机器自身预充少量能量，热量清零。 */
    private fun setupCentrifuge(ctx: TestContext) {
        val cesu = ctx.batboxEast
        setBlocks(ctx, listOf(cesu to "ic2_120:cesu"), mapOf("facing" to "west"))
        setBeField(ctx, cesu, "EnergyStored", 300_000)
        place(ctx, ctx.origin, "ic2_120:centrifuge")
        setBeField(ctx, ctx.origin, "EnergyStored", 40_000)
    }

    /** 在标准搭建上把热量升到 `minHeat + 200`，确保越过对应阈值。 */
    private fun setupCentrifugeHot(ctx: TestContext, minHeat: Int) {
        setupCentrifuge(ctx)
        setHeat(ctx, minHeat + 200)
    }

    @Test
    fun `place`(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:centrifuge")
        assertBlockId(ctx, ctx.origin, "ic2_120:centrifuge")
    }

    @Test
    fun cobblestoneToStoneDust(ctx: TestContext) {
        // 配方 minHeat=100；PROGRESS_MAX=500 tick（25s），预算 40*20=800 tick（对齐 TS 版）
        setupCentrifugeHot(ctx, 100)
        moveItemStackTo(ctx, ctx.origin, "minecraft:cobblestone", 1, 0)
        waitUntil(ctx, invItemEquals(ctx.origin, 1, "ic2_120:stone_dust"), 40 * 20)
        assertSlotHas(ctx, ctx.origin, 1, "ic2_120:stone_dust")
        assertSlotEmpty(ctx, ctx.origin, 0)
    }

    @Test
    fun crushedCopperTo3Outputs(ctx: TestContext) {
        // 配方 minHeat=500；输出：slot 1 = small_tin_dust, slot 2 = copper_dust
        setupCentrifugeHot(ctx, 500)
        moveItemStackTo(ctx, ctx.origin, "ic2_120:crushed_copper", 1, 0)
        waitUntil(ctx, invItemEquals(ctx.origin, 2, "ic2_120:copper_dust"), 40 * 20)
        assertSlotHas(ctx, ctx.origin, 1, "ic2_120:small_tin_dust")
        assertSlotHas(ctx, ctx.origin, 2, "ic2_120:copper_dust")
    }

    @Test
    fun noPowerIdle(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:centrifuge")
        moveItemStackTo(ctx, ctx.origin, "minecraft:cobblestone", 1, 0)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 0, "minecraft:cobblestone")
        assertSlotEmpty(ctx, ctx.origin, 1)
    }

    /** 冷离心机：热量不足时不应启动（heat_starve）。 */
    @Test
    fun heatStarveColdCentrifuge(ctx: TestContext) {
        setupCentrifuge(ctx)
        moveItemStackTo(ctx, ctx.origin, "minecraft:cobblestone", 1, 0)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 0, "minecraft:cobblestone")
        assertSlotEmpty(ctx, ctx.origin, 1)
    }

    @Test
    fun invalidInputDirt(ctx: TestContext) {
        setupCentrifugeHot(ctx, 0)
        moveItemStackTo(ctx, ctx.origin, "minecraft:dirt", 1, 0)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 0, "minecraft:dirt")
        assertSlotEmpty(ctx, ctx.origin, 1)
    }

    @Test
    fun outputFullBlocksNext(ctx: TestContext) {
        setupCentrifugeHot(ctx, 0)
        setSlot(ctx, ctx.origin, 1, "ic2_120:stone_dust", 64)
        moveItemStackTo(ctx, ctx.origin, "minecraft:cobblestone", 1, 0)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 0, "minecraft:cobblestone")
    }
}
