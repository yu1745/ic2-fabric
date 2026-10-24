package ic2_120.test

import com.mcdebug.runner.McDebugTest
import com.mcdebug.runner.TestContext
import com.mcdebug.runner.getBeNumber
import com.mcdebug.runner.place
import com.mcdebug.runner.setBeField
import com.mcdebug.runner.setBlocks
import org.junit.jupiter.api.Test

/**
 * #30：物质生成机 / 复制机停工（无红石）时空耗电——机器持续从电网拉电填充内部缓冲
 * （物质生成机 4M / 复制机 400k，按 512 EU/t 要拉几分钟，表现为“停止工作仍固定 512 EU/t 耗电”）。
 *
 * 修复后语义：红石停机时机器拒绝外部供能（电缆推送与邻接直送均归零）；
 * 红石恢复后正常取电。同时验证抽入升级可向物质生成机抽入废料槽。
 */
@McDebugTest
class MatterIdleDrainTest {

    private fun batboxE(ctx: TestContext): Double = getBeNumber(ctx, ctx.batboxEast, "EnergyStored")
    private fun machineE(ctx: TestContext): Double = getBeNumber(ctx, ctx.origin, "EnergyStored")

    private fun setup(ctx: TestContext, machine: String) {
        setBlocks(ctx, listOf(ctx.batboxEast to "ic2_120:batbox"), mapOf("facing" to "west"))
        setBeField(ctx, ctx.batboxEast, "EnergyStored", 40000)
        place(ctx, ctx.origin, machine)
    }

    /** 物质生成机：无红石停工、内部缓冲远未满时，不应从电网拉电。 */
    @Test
    fun `idle matter generator does not drain network`(ctx: TestContext) {
        setup(ctx, "ic2_120:matter_generator")
        setBeField(ctx, ctx.origin, "EnergyStored", 100_000)

        ctx.api.be.tick(ctx.origin, 500)
        val e1Bat = batboxE(ctx)
        val e1Mach = machineE(ctx)
        ctx.api.be.tick(ctx.origin, 500)
        val e2Bat = batboxE(ctx)
        val e2Mach = machineE(ctx)

        // 停机门控生效后：BatBox 与机器内部能量都必须一分不差地保持不变
        // （修复前机器会持续从电网拉电填内部缓冲，即使 be.tick 冻结世界时间也会出现至少一次拉取）。
        if (e1Bat != 40000.0 || e2Bat != 40000.0 || e1Mach != 100_000.0 || e2Mach != 100_000.0) {
            throw AssertionError(
                "idle matter generator drains network: batbox $e1Bat->$e2Bat, machine $e1Mach->$e2Mach"
            )
        }
    }

    /** 复制机：无红石停工、内部缓冲远未满时，不应从电网拉电。 */
    @Test
    fun `idle replicator does not drain network`(ctx: TestContext) {
        setup(ctx, "ic2_120:replicator")
        setBeField(ctx, ctx.origin, "EnergyStored", 100_000)

        ctx.api.be.tick(ctx.origin, 500)
        val e1Bat = batboxE(ctx)
        val e1Mach = machineE(ctx)
        ctx.api.be.tick(ctx.origin, 500)
        val e2Bat = batboxE(ctx)
        val e2Mach = machineE(ctx)

        if (e1Bat != 40000.0 || e2Bat != 40000.0 || e1Mach != 100_000.0 || e2Mach != 100_000.0) {
            throw AssertionError(
                "idle replicator drains network: batbox $e1Bat->$e2Bat, machine $e1Mach->$e2Mach"
            )
        }
    }

    /**
     * 红石激活（运行许可）时机器必须仍能正常取电，防止门控误伤供能链路。
     * 用满 UU 罐让机器无工可做（提前返回、不消耗能量），红石开启 → 只进不出，电量应上升。
     */
    @Test
    fun `powered matter generator still charges`(ctx: TestContext) {
        setup(ctx, "ic2_120:matter_generator")
        setBeField(ctx, ctx.origin, "EnergyStored", 0)
        // 满 UU 罐（810_000 droplets）：红石开启也不会消耗能量，取电只增不减
        setBeField(ctx, ctx.origin, "TankAmount", 810_000)
        // 任意相邻红石块提供信号，机器获得运行许可
        setBlocks(ctx, listOf(ctx.pos(-1, 0, 0) to "minecraft:redstone_block"), emptyMap())

        com.mcdebug.runner.waitUntil(
            ctx,
            com.mcdebug.runner.beFieldGreaterThan(ctx.origin, "EnergyStored", 0),
            10 * 20
        )
        val mach = machineE(ctx)
        if (mach <= 0) {
            throw AssertionError("powered matter generator failed to charge, energy=$mach")
        }
    }

    /** #30 第二部分：抽入升级应能从相邻箱子把废料抽入物质生成机的废料槽。 */
    @Test
    fun `pulling upgrade pulls scrap into matter generator`(ctx: TestContext) {
        setup(ctx, "ic2_120:matter_generator")
        // 北侧箱子放废料，升级槽 3（SLOT_UPGRADE_0）放抽入升级
        val chest = ctx.pos(0, 0, -1)
        setBlocks(ctx, listOf(chest to "minecraft:chest"), emptyMap())
        com.mcdebug.runner.moveItemStackTo(ctx, chest, "ic2_120:scrap", 16, 0)
        com.mcdebug.runner.moveItemStackTo(ctx, ctx.origin, "ic2_120:pulling_upgrade", 1, 3)

        ctx.api.be.tick(ctx.origin, 40)
        com.mcdebug.runner.assertSlotHas(ctx, ctx.origin, 0, "ic2_120:scrap")
    }
}
