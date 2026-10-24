package ic2_120.test

import com.mcdebug.runner.McDebugTest
import com.mcdebug.runner.TestContext
import com.mcdebug.runner.assertBlockId
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
 * 压缩机 (Compressor) 测试。
 * 槽位：slot 0 = 输入, slot 1 = 输出, slot 2 = 放电槽, slot 3+ = 升级槽（两个超频）。
 */
@McDebugTest
class CompressorTest {

    /** 标准搭建：相邻 BatBox 供电 + 两个超频升级。 */
    private fun setupCompressor(ctx: TestContext) {
        setupAdjacentBatbox(ctx, "ic2_120:compressor")
        moveItemStackTo(ctx, ctx.origin, "ic2_120:overclocker_upgrade", 2, 3)
    }

    @Test
    fun `place`(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:compressor")
        assertBlockId(ctx, ctx.origin, "ic2_120:compressor")
    }

    /** 4 粘土球 → 1 粘土。 */
    @Test
    fun clayBallsToClay(ctx: TestContext) {
        setupCompressor(ctx)
        moveItemStackTo(ctx, ctx.origin, "minecraft:clay_ball", 4, 0)
        waitUntil(ctx, invItemEquals(ctx.origin, 1, "minecraft:clay"), 15 * 20)
        assertSlotHas(ctx, ctx.origin, 1, "minecraft:clay")
    }

    /** 9 铁锭 → 1 铁块。 */
    @Test
    fun ironIngotsToIronBlock(ctx: TestContext) {
        setupCompressor(ctx)
        moveItemStackTo(ctx, ctx.origin, "minecraft:iron_ingot", 9, 0)
        waitUntil(ctx, invItemEquals(ctx.origin, 1, "minecraft:iron_block"), 15 * 20)
        assertSlotHas(ctx, ctx.origin, 1, "minecraft:iron_block")
    }

    /** 无电闲置。 */
    @Test
    fun noPowerIdle(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:compressor")
        moveItemStackTo(ctx, ctx.origin, "minecraft:clay_ball", 4, 0)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 0, "minecraft:clay_ball")
        assertSlotEmpty(ctx, ctx.origin, 1)
    }

    /** 非法输入：泥土无对应配方。 */
    @Test
    fun invalidInputDirt(ctx: TestContext) {
        setupCompressor(ctx)
        moveItemStackTo(ctx, ctx.origin, "minecraft:dirt", 1, 0)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 0, "minecraft:dirt")
        assertSlotEmpty(ctx, ctx.origin, 1)
    }

    /** 输出槽塞满粘土 → 新一轮压缩应卡住不消耗输入。 */
    @Test
    fun outputFullBlocksNext(ctx: TestContext) {
        setupCompressor(ctx)
        setSlot(ctx, ctx.origin, 1, "minecraft:clay", 64)
        moveItemStackTo(ctx, ctx.origin, "minecraft:clay_ball", 4, 0)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 0, "minecraft:clay_ball")
    }

    /** 多对一配方缓存回归：先放 1 个（配方需 4 个）空转，再补足到 4 个应开始压缩。 */
    @Test
    fun multiInputPartialThenFill(ctx: TestContext) {
        setupCompressor(ctx)
        moveItemStackTo(ctx, ctx.origin, "minecraft:clay_ball", 1, 0)
        waitTicks(ctx, 20)
        assertSlotHas(ctx, ctx.origin, 0, "minecraft:clay_ball")
        moveItemStackTo(ctx, ctx.origin, "minecraft:clay_ball", 3, 0)
        waitUntil(ctx, invItemEquals(ctx.origin, 1, "minecraft:clay"), 15 * 20)
        assertSlotHas(ctx, ctx.origin, 1, "minecraft:clay")
    }

    /** 水容器压缩基线：满水 fluid_cell → 雪块（产出后 pending 返还 empty_cell）。 */
    @Test
    fun waterCellFullToSnow(ctx: TestContext) {
        setupCompressor(ctx)
        setSlot(ctx, ctx.origin, 0, "ic2_120:fluid_cell", 1, mapOf("FluidVariant" to mapOf("fluid" to "minecraft:water")))
        waitUntil(ctx, invItemEquals(ctx.origin, 1, "minecraft:snow_block"), 15 * 20)
        assertSlotHas(ctx, ctx.origin, 1, "minecraft:snow_block")
    }

    /**
     * 水容器配方缓存回归：先放空 fluid_cell（无 NBT）让机器缓存 null 结果，
     * 再原位替换为满水 fluid_cell（item/count 不变、仅 NBT 变化，不经过空槽中间态）。
     * 若按 item 缓存 null 结果，替换后机器也不会开始压缩——本用例防止该回归。
     */
    @Test
    fun waterCellEmptyThenFillInPlace(ctx: TestContext) {
        setupCompressor(ctx)
        moveItemStackTo(ctx, ctx.origin, "ic2_120:fluid_cell", 1, 0)
        waitTicks(ctx, 20)
        assertSlotHas(ctx, ctx.origin, 0, "ic2_120:fluid_cell")
        setSlot(ctx, ctx.origin, 0, "ic2_120:fluid_cell", 1, mapOf("FluidVariant" to mapOf("fluid" to "minecraft:water")))
        waitUntil(ctx, invItemEquals(ctx.origin, 1, "minecraft:snow_block"), 15 * 20)
        assertSlotHas(ctx, ctx.origin, 1, "minecraft:snow_block")
    }
}
