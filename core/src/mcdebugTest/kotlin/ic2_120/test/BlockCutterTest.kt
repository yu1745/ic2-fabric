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
 * 切割机 (Block Cutter) 测试。
 * 槽位：0=锯片, 1=输入?, 2=输出, 4=升级槽。
 * 配方：石头 → 9 石台阶。
 */
@McDebugTest
class BlockCutterTest {

    /** 标准搭建：相邻 BatBox 供电 + 铁锯片 + 一个超频升级。 */
    private fun setupBlockCutter(ctx: TestContext) {
        setupAdjacentBatbox(ctx, "ic2_120:block_cutter")
        moveItemStackTo(ctx, ctx.origin, "ic2_120:iron_block_cutting_blade", 1, 0)
        moveItemStackTo(ctx, ctx.origin, "ic2_120:overclocker_upgrade", 2, 4)
    }

    @Test
    fun `place`(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:block_cutter")
        assertBlockId(ctx, ctx.origin, "ic2_120:block_cutter")
    }

    /** 石头 → 9 石台阶。 */
    @Test
    fun stoneTo9StoneSlab(ctx: TestContext) {
        setupBlockCutter(ctx)
        moveItemStackTo(ctx, ctx.origin, "minecraft:stone", 1, 1)
        waitUntil(ctx, invItemEquals(ctx.origin, 3, "minecraft:stone_slab"), 15 * 20)
        assertSlotCount(ctx, ctx.origin, 3, 9)
    }

    @Test
    fun noPowerIdle(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:block_cutter")
        moveItemStackTo(ctx, ctx.origin, "minecraft:stone", 1, 1)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 1, "minecraft:stone")
        assertSlotEmpty(ctx, ctx.origin, 3)
    }

    /** 无锯片：机器应不工作。 */
    @Test
    fun noBladeIdle(ctx: TestContext) {
        setupAdjacentBatbox(ctx, "ic2_120:block_cutter")
        moveItemStackTo(ctx, ctx.origin, "minecraft:stone", 1, 1)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 1, "minecraft:stone")
        assertSlotEmpty(ctx, ctx.origin, 3)
    }

    @Test
    fun invalidInputDirt(ctx: TestContext) {
        setupBlockCutter(ctx)
        moveItemStackTo(ctx, ctx.origin, "minecraft:dirt", 1, 1)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 1, "minecraft:dirt")
        assertSlotEmpty(ctx, ctx.origin, 3)
    }

    @Test
    fun outputFullBlocksNext(ctx: TestContext) {
        setupBlockCutter(ctx)
        setSlot(ctx, ctx.origin, 3, "minecraft:stone_slab", 64)
        moveItemStackTo(ctx, ctx.origin, "minecraft:stone", 1, 1)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 1, "minecraft:stone")
    }
}
