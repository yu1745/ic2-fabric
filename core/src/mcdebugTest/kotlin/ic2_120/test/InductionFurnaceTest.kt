package ic2_120.test

import com.mcdebug.runner.McDebugTest
import com.mcdebug.runner.TestContext
import com.mcdebug.runner.assertBlockId
import com.mcdebug.runner.assertSlotEmpty
import com.mcdebug.runner.assertSlotHas
import com.mcdebug.runner.insertItem
import com.mcdebug.runner.place
import com.mcdebug.runner.setBeField
import com.mcdebug.runner.setBlocks
import org.junit.jupiter.api.Test

/**
 * 感应炉 (Induction Furnace) 测试。
 *
 * 热能机器：需要 (1) 能量、(2) 热量 ≥ 100、(3) 红石信号三者同时满足才能熔炼。
 * 红石火把放西侧一格提供持续信号。
 *
 * 槽位：slot 0..1 = 输入（双输入共享进度）, slot 2..3 = 输出, slot 4 = 放电槽, slot 5..6 = 升级槽。
 *
 * 驱动方式：be.tick（机器自身预充能量——热量/熔炼/散热都在机器 tick 内，红石
 * 状态直接读世界，不依赖自然 tick）。
 */
@McDebugTest
class InductionFurnaceTest {

    private fun tickDrive(ctx: TestContext, ticks: Int, check: (TestContext) -> Unit) {
        ctx.api.be.tick(ctx.origin, ticks)
        check(ctx)
    }

    /** 标准搭建：东 BatBox + 西红石火把，机器自身预充 40 000 EU。 */
    private fun setupInductionFurnace(ctx: TestContext) {
        val batbox = ctx.pos(1, 0, 0)
        val torch = ctx.pos(-1, 0, 0)
        // 逐个放置：props 不能统一应用（torch 没有 facing 属性）
        ctx.api.world.setBlock(batbox, "ic2_120:batbox", mapOf("facing" to "west"))
        ctx.api.world.setBlock(torch, "minecraft:redstone_torch")
        setBeField(ctx, batbox, "EnergyStored", 40000)
        place(ctx, ctx.origin, "ic2_120:induction_furnace")
        setBeField(ctx, ctx.origin, "EnergyStored", 40000)
    }

    /** 在标准搭建上把热量直接拉到 10 000，绕过预热曲线。 */
    private fun setupInductionFurnaceHot(ctx: TestContext) {
        setupInductionFurnace(ctx)
        setHeat(ctx, 10000)
    }

    @Test
    fun `place`(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:induction_furnace")
        assertBlockId(ctx, ctx.origin, "ic2_120:induction_furnace")
    }

    /** 三要素齐备时的标准熔炼。 */
    @Test
    fun smeltWithHeatAndRedstone(ctx: TestContext) {
        setupInductionFurnaceHot(ctx)
        moveItemStackTo(ctx, ctx.origin, "minecraft:iron_ore", 1, 0)
        tickDrive(ctx, 600) { assertSlotHas(it, it.origin, 2, "minecraft:iron_ingot") }
    }

    /** 双输入并行：两个槽位同时放入铁矿石，应并行熔炼到两个输出槽。 */
    @Test
    fun dualSlot(ctx: TestContext) {
        setupInductionFurnaceHot(ctx)
        moveItemStackTo(ctx, ctx.origin, "minecraft:iron_ore", 1, 0)
        moveItemStackTo(ctx, ctx.origin, "minecraft:iron_ore", 1, 1)
        tickDrive(ctx, 600) {
            assertSlotHas(it, it.origin, 2, "minecraft:iron_ingot")
            assertSlotHas(it, it.origin, 3, "minecraft:iron_ingot")
        }
    }

    /** 无红石且无可加工输入：热量持续散热至 0（原版散热 -4/tick，100 热 25 tick 散完）。 */
    @Test
    fun noRedstoneNoHeatNoSmelt(ctx: TestContext) {
        val batbox = ctx.pos(1, 0, 0)
        setBlocks(ctx, listOf(batbox to "ic2_120:batbox"), mapOf("facing" to "west"))
        setBeField(ctx, batbox, "EnergyStored", 40000)
        place(ctx, ctx.origin, "ic2_120:induction_furnace")
        setBeField(ctx, ctx.origin, "EnergyStored", 40000)
        setHeat(ctx, 100)
        tickDrive(ctx, 200) {
            assertSlotEmpty(it, it.origin, 2)
            val heat = getHeat(it)
            if (heat != 0) throw AssertionError("expected heat = 0 (fully decayed), got $heat")
        }
    }

    /** 无热量：有电有红石但热量为 0，机器不能工作。 */
    @Test
    fun noHeatNoSmelt(ctx: TestContext) {
        val batbox = ctx.pos(1, 0, 0)
        val torch = ctx.pos(-1, 0, 0)
        ctx.api.world.setBlock(batbox, "ic2_120:batbox", mapOf("facing" to "west"))
        ctx.api.world.setBlock(torch, "minecraft:redstone_torch")
        setBeField(ctx, batbox, "EnergyStored", 40000)
        place(ctx, ctx.origin, "ic2_120:induction_furnace")
        setBeField(ctx, ctx.origin, "EnergyStored", 40000)
        moveItemStackTo(ctx, ctx.origin, "minecraft:iron_ore", 1, 0)
        tickDrive(ctx, 200) {
            assertSlotHas(it, it.origin, 0, "minecraft:iron_ore")
            assertSlotEmpty(it, it.origin, 2)
        }
    }

    /** 升温曲线：电+红石都在但未预热，热量应上升到 > 100。 */
    @Test
    fun heatUpWithRedstoneAndEnergy(ctx: TestContext) {
        setupInductionFurnace(ctx)
        tickDrive(ctx, 120) {
            val heat = getHeat(it)
            if (heat <= 100) throw AssertionError("expected heat > 100 after 120 ticks, got $heat")
        }
    }

    /** 非法输入：泥土无熔炼配方。 */
    @Test
    fun invalidInputDirt(ctx: TestContext) {
        setupInductionFurnaceHot(ctx)
        moveItemStackTo(ctx, ctx.origin, "minecraft:dirt", 1, 0)
        tickDrive(ctx, 200) {
            assertSlotHas(it, it.origin, 0, "minecraft:dirt")
            assertSlotEmpty(it, it.origin, 2)
        }
    }

    /** 32 位热量（Heat_Low/Heat_High 拆分字段拼回）。 */
    private fun getHeat(ctx: TestContext): Int {
        val low = ctx.api.be.getField(ctx.origin, "Heat_Low").asJsonObject.get("value").asInt
        val high = ctx.api.be.getField(ctx.origin, "Heat_High").asJsonObject.get("value").asInt
        return low + high * 65536
    }
}
