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
import com.mcdebug.runner.setBeField
import com.mcdebug.runner.setBlocks
import com.mcdebug.runner.setSlot
import com.mcdebug.runner.waitTicks
import com.mcdebug.runner.waitUntil
import org.junit.jupiter.api.Test

/**
 * 洗矿厂 (Ore Washing Plant) 测试。
 * 槽位：slot 0=输入, slot 1=流体输出?, slot 2=输出, slot 7=变压器升级, slot 8+=超频。
 * 需要水箱预充水（81_000 droplets = 1 桶）。
 */
@McDebugTest
class OreWashingPlantTest {

    private val WATER_BUCKET_DROPLETS = 81_000

    /** 标准搭建：相邻 BatBox + 变压器升级（提 tier 接受更多能量）+ 双超频 + 预充水箱。 */
    private fun setupOreWasher(ctx: TestContext) {
        setupAdjacentBatbox(ctx, "ic2_120:ore_washing_plant")
        moveItemStackTo(ctx, ctx.origin, "ic2_120:transformer_upgrade", 1, 7)
        moveItemStackTo(ctx, ctx.origin, "ic2_120:overclocker_upgrade", 2, 8)
        val inserted = ctx.api.fluid.insert(ctx.origin, "minecraft:water", WATER_BUCKET_DROPLETS.toLong(), index = 0)
            .asJsonObject.get("inserted").asLong
        if (inserted < WATER_BUCKET_DROPLETS) {
            throw AssertionError("failed to prime water tank: $inserted/$WATER_BUCKET_DROPLETS")
        }
    }

    @Test
    fun `place`(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:ore_washing_plant")
        assertBlockId(ctx, ctx.origin, "ic2_120:ore_washing_plant")
    }

    /** 经典配方：粉碎铜 → 纯净铜 + 2 小撮铜粉 + 石粉（多产物）。 */
    @Test
    fun crushedCopperToPurifiedAndDusts(ctx: TestContext) {
        setupOreWasher(ctx)
        moveItemStackTo(ctx, ctx.origin, "ic2_120:crushed_copper", 1, 0)
        waitUntil(ctx, invItemEquals(ctx.origin, 2, "ic2_120:purified_copper"), 20 * 20)
        assertSlotHas(ctx, ctx.origin, 2, "ic2_120:purified_copper")
    }

    /** 无电闲置（有水也开不动）。 */
    @Test
    fun noPowerIdle(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:ore_washing_plant")
        ctx.api.fluid.insert(ctx.origin, "minecraft:water", WATER_BUCKET_DROPLETS.toLong(), index = 0)
        moveItemStackTo(ctx, ctx.origin, "ic2_120:crushed_copper", 1, 0)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 0, "ic2_120:crushed_copper")
        assertSlotEmpty(ctx, ctx.origin, 2)
    }

    /** 无水闲置：有电但流体槽抽空，应不工作。 */
    @Test
    fun noWaterIdle(ctx: TestContext) {
        setupAdjacentBatbox(ctx, "ic2_120:ore_washing_plant")
        moveItemStackTo(ctx, ctx.origin, "ic2_120:crushed_copper", 1, 0)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 0, "ic2_120:crushed_copper")
        assertSlotEmpty(ctx, ctx.origin, 2)
    }

    @Test
    fun invalidInputDirt(ctx: TestContext) {
        setupOreWasher(ctx)
        moveItemStackTo(ctx, ctx.origin, "minecraft:dirt", 1, 0)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 0, "minecraft:dirt")
        assertSlotEmpty(ctx, ctx.origin, 2)
    }
}
