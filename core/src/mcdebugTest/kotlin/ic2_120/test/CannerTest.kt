package ic2_120.test

import com.mcdebug.runner.McDebugTest
import com.mcdebug.runner.TestContext
import com.mcdebug.runner.assertSlotEmpty
import com.mcdebug.runner.assertSlotHas
import com.mcdebug.runner.insertItem
import com.mcdebug.runner.invItemEquals
import com.mcdebug.runner.setBeField
import com.mcdebug.runner.waitUntil
import org.junit.jupiter.api.Test

/**
 * 流体/固体装罐机 (Canner) 测试。
 * 槽位：slot 0=容器输入, slot 1=混合材料, slot 2=物品输出, slot 3=放电槽, slot 4..7=升级槽。
 * Mode=3 (ENRICH_LIQUID)。
 *
 * 注意：fluid API 必须显式传 index（服务端对多 tank 机器强制要求），
 * 旧 TS helper 没传 index 是基线失败原因之一。
 */
@McDebugTest
class CannerTest {

    private val BUCKET = 81_000L

    private fun setupMixingCanner(ctx: TestContext) {
        setupAdjacentBatbox(ctx, "ic2_120:canner")
        // 注意：canner 的 Mode 是 SyncedData.schema.int 字段，NBT 里拆成 Mode_Low/Mode_High
        // 两个 16 位键——setField("Mode") 写孤儿键会被 readNbt 忽略（Mode 保持默认）。
        // 必须直接写低位（3 < 65536，高位默认 0）。metal_former 是例外：它有显式 NBT_MODE 键。
        setBeField(ctx, ctx.origin, "Mode_Low", 3) // ENRICH_LIQUID
        moveItemStackTo(ctx, ctx.origin, "ic2_120:transformer_upgrade", 1, 4)
        moveItemStackTo(ctx, ctx.origin, "ic2_120:overclocker_upgrade", 2, 5)
        val inserted = ctx.api.fluid.insert(ctx.origin, "minecraft:water", BUCKET, index = 0)
            .asJsonObject.get("inserted").asLong
        if (inserted != BUCKET) throw AssertionError("failed to insert water: $inserted/$BUCKET")
    }

    /** 混合：空单元 + 青金石粉 + 水 → 冷却液单元（直接填充输出槽）。 */
    @Test
    fun mixingFillsEmptyCellDirectly(ctx: TestContext) {
        setupMixingCanner(ctx)
        moveItemStackTo(ctx, ctx.origin, "ic2_120:empty_cell", 1, 0)
        moveItemStackTo(ctx, ctx.origin, "ic2_120:lapis_dust", 8, 1)

        waitUntil(ctx, invItemEquals(ctx.origin, 2, "ic2_120:coolant_cell"), 15 * 20)
        assertSlotHas(ctx, ctx.origin, 2, "ic2_120:coolant_cell")
        assertSlotEmpty(ctx, ctx.origin, 0)
        assertSlotEmpty(ctx, ctx.origin, 1)
    }
}
