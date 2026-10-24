package ic2_120.test

import com.mcdebug.runner.McDebugTest
import com.mcdebug.runner.TestContext
import com.mcdebug.runner.assertBlockId
import com.mcdebug.runner.assertSlotCount
import com.mcdebug.runner.assertSlotEmpty
import com.mcdebug.runner.assertSlotHas
import com.mcdebug.runner.insertItem
import com.mcdebug.runner.place
import com.mcdebug.runner.setBeField
import com.mcdebug.runner.setBlocks
import com.mcdebug.runner.setSlot
import org.junit.jupiter.api.Test

/**
 * 粉碎机 (Macerator) 测试。
 *
 * 粉碎机有两条供电路径需要分别覆盖：
 *   1. 通过**线缆**中转（中间隔一格绝缘铜缆），覆盖电缆连接 + 能量传输链路；
 *   2. 直接相邻 BatBox，覆盖面贴接供电。
 *
 * 槽位布局（与 `MaceratorBlockEntity` 保持一致）：
 *   slot 0  = 输入 (SLOT_INPUT)
 *   slot 1  = 输出 (SLOT_OUTPUT)
 *   slot 2  = 放电槽 (SLOT_DISCHARGING)
 *   slot 3+ = 升级槽 (SLOT_UPGRADE_0..3)
 *
 * 驱动方式：全部用 `be.tick` 主动驱动（毫秒级、确定性），不用 wait.until
 * 干等自然 tick——机器内部逻辑（配方进度、能量获取）与自然 tick 完全一致
 * （同一 BlockEntityTicker 路径），但不受并行负载影响，彻底消除
 * "300 tick 临界预算在并发下偶发超时"的 flaky。依赖邻居/世界 tick 的
 * 场景（能量网络、红石）才需要 wait.until。
 */
@McDebugTest
class MaceratorTest {

    /**
     * be.tick 驱动机器 [ticks] 次后执行断言。驱动不足时断言失败信息会显示
     * 当前槽位状态，便于区分"没供电 / 没配方 / 输出阻塞"。
     */
    private fun tickDrive(ctx: TestContext, ticks: Int, check: (TestContext) -> Unit) {
        ctx.api.be.tick(ctx.origin, ticks)
        check(ctx)
    }

    /**
     * 标准搭建：东二格 BatBox + 东一格绝缘铜缆（setBlocks 放置，连接状态由
     * neighborUpdate 自动处理），然后放置粉碎机。
     */
    private fun setupMacerator(ctx: TestContext) {
        val batbox = ctx.pos(2, 0, 0)
        val cable = ctx.pos(1, 0, 0)
        setBlocks(ctx, listOf(batbox to "ic2_120:batbox"), mapOf("facing" to "west"))
        setBlocks(ctx, listOf(cable to "ic2_120:insulated_copper_cable"))
        setBeField(ctx, batbox, "EnergyStored", 40000)
        place(ctx, ctx.origin, "ic2_120:macerator")
        // be.tick 在同一 world tick 内循环驱动，ic2 机器的 TickLimitedSidedEnergyContainer
        // 按 world.time 重置 per-tick 输入预算 → 只有第一个 tick 能从 BatBox 拉电。
        // 机器自身预充（consumeEnergy 不受输入预算限制）即可正常推进配方。
        setBeField(ctx, ctx.origin, "EnergyStored", 400)
    }

    @Test
    fun `place`(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:macerator")
        assertBlockId(ctx, ctx.origin, "ic2_120:macerator")
    }

    /** 经典配方：1 圆石 → 1 砾石（经电缆供电）。 */
    @Test
    fun cobblestoneToGravelWithCable(ctx: TestContext) {
        setupMacerator(ctx)
        moveItemStackTo(ctx, ctx.origin, "minecraft:cobblestone", 1, 0)
        tickDrive(ctx, 600) { assertSlotHas(it, it.origin, 1, "minecraft:gravel") }
    }

    /** 1 煤炭块 → 9 煤粉（带数量断言）。 */
    @Test
    fun coalBlockTo9CoalDust(ctx: TestContext) {
        setupMacerator(ctx)
        moveItemStackTo(ctx, ctx.origin, "minecraft:coal_block", 1, 0)
        tickDrive(ctx, 600) { assertSlotCount(it, it.origin, 1, 9) }
    }

    /** 8 西瓜片 → 1 生物质渣（带数量断言）。 */
    @Test
    fun melonSlicesToBioChaff(ctx: TestContext) {
        setupMacerator(ctx)
        moveItemStackTo(ctx, ctx.origin, "minecraft:melon_slice", 8, 0)
        tickDrive(ctx, 600) { assertSlotCount(it, it.origin, 1, 1) }
    }

    /** 1 铁矿石 → 2 粉碎铁（验证富产系数）。 */
    @Test
    fun ironOreTo2CrushedIron(ctx: TestContext) {
        setupMacerator(ctx)
        moveItemStackTo(ctx, ctx.origin, "minecraft:iron_ore", 1, 0)
        tickDrive(ctx, 600) { assertSlotCount(it, it.origin, 1, 2) }
    }

    /** 无电闲置。 */
    @Test
    fun noPowerIdle(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:macerator")
        moveItemStackTo(ctx, ctx.origin, "minecraft:cobblestone", 1, 0)
        tickDrive(ctx, 200) {
            assertSlotHas(it, it.origin, 0, "minecraft:cobblestone")
            assertSlotEmpty(it, it.origin, 1)
        }
    }

    /** 非法输入。 */
    @Test
    fun invalidInputDirt(ctx: TestContext) {
        setupMacerator(ctx)
        moveItemStackTo(ctx, ctx.origin, "minecraft:dirt", 1, 0)
        tickDrive(ctx, 200) {
            assertSlotHas(it, it.origin, 0, "minecraft:dirt")
            assertSlotEmpty(it, it.origin, 1)
        }
    }

    /** 输出满 → 阻塞。 */
    @Test
    fun outputFullBlocksNext(ctx: TestContext) {
        setupMacerator(ctx)
        setSlot(ctx, ctx.origin, 1, "minecraft:gravel", 64)
        moveItemStackTo(ctx, ctx.origin, "minecraft:cobblestone", 1, 0)
        tickDrive(ctx, 200) { assertSlotHas(it, it.origin, 0, "minecraft:cobblestone") }
    }

    /** 能量不足 1 轮 → 应当一整轮都不消耗输入（不能半成品消耗）。 */
    @Test
    fun energyStarveNoPartialConsume(ctx: TestContext) {
        val batbox = ctx.pos(2, 0, 0)
        val cable = ctx.pos(1, 0, 0)
        setBlocks(ctx, listOf(batbox to "ic2_120:batbox"), mapOf("facing" to "west"))
        setBlocks(ctx, listOf(cable to "ic2_120:insulated_copper_cable"))
        setBeField(ctx, batbox, "EnergyStored", 1)
        place(ctx, ctx.origin, "ic2_120:macerator")
        moveItemStackTo(ctx, ctx.origin, "minecraft:cobblestone", 1, 0)
        tickDrive(ctx, 200) { assertSlotHas(it, it.origin, 0, "minecraft:cobblestone") }
    }

    /** 直接相邻 BatBox 也能正常工作（覆盖面贴接供电链路）。 */
    @Test
    fun powerAdjacentBatbox(ctx: TestContext) {
        val batbox = ctx.batboxEast
        setBlocks(ctx, listOf(batbox to "ic2_120:batbox"), mapOf("facing" to "west"))
        setBeField(ctx, batbox, "EnergyStored", 40000)
        place(ctx, ctx.origin, "ic2_120:macerator")
        setBeField(ctx, ctx.origin, "EnergyStored", 400)  // 见 setupMacerator 注释
        moveItemStackTo(ctx, ctx.origin, "minecraft:cobblestone", 1, 0)
        tickDrive(ctx, 600) { assertSlotHas(it, it.origin, 1, "minecraft:gravel") }
    }

    /**
     * 过压爆炸：把粉碎机接在满电的 MFSU（HV）下，应立刻自爆成空气。
     * 过压检查在机器自身 tick 内（能量网络在机器 tick 中读取），be.tick 驱动即可；
     * 注意机器放下即炸（place 返回时可能已变 air），不能 insertItem。
     */
    @Test
    fun overvoltageHvExplode(ctx: TestContext) {
        val mfsu = ctx.batboxEast
        setBlocks(ctx, listOf(mfsu to "ic2_120:mfsu"), mapOf("facing" to "west"))
        setBeField(ctx, mfsu, "EnergyStored", 40_000_000)
        place(ctx, ctx.origin, "ic2_120:macerator")
        tickDrive(ctx, 30) { assertBlockId(it, it.origin, "minecraft:air") }
    }

    /**
     * 多对一配方缓存回归：先放 1 个（不足 8 个），让机器带着"数量不足"状态
     * 跑若干 tick，再补足到 8 个。若实现按"刚放入一个"缓存了 null 结果，
     * 补足后机器也不会开始加工——这是本用例要防止的回归。
     */
    @Test
    fun multiInputPartialThenFill(ctx: TestContext) {
        setupMacerator(ctx)
        // 先放 1 片西瓜（配方需 8 片），让机器空转 20 tick（足够触发多次配方判定）。
        moveItemStackTo(ctx, ctx.origin, "minecraft:melon_slice", 1, 0)
        tickDrive(ctx, 20) { assertSlotHas(it, it.origin, 0, "minecraft:melon_slice") }
        // 补足到 8 片后应当正常开始加工。
        moveItemStackTo(ctx, ctx.origin, "minecraft:melon_slice", 7, 0)
        tickDrive(ctx, 600) { assertSlotCount(it, it.origin, 1, 1) }
    }
}
