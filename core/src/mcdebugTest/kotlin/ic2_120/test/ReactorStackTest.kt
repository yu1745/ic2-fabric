package ic2_120.test

import com.mcdebug.runner.McDebugTest
import com.mcdebug.runner.TestContext
import com.mcdebug.runner.setBlocks
import com.mcdebug.runner.setSlot
import com.google.gson.JsonObject
import com.mcdebug.cli.gson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 反应堆器件（带寿命/热容组件）堆叠回归测试。
 *
 * 背景：vanilla 1.20.1 中 Settings.maxDamage 会把 maxCount 强制为 1，
 * 且 ItemStack.isStackable() 对"可损耗且已损耗"的物品恒为 false。
 * 修复见 ReactorComponentStackMixin：getMaxCount 返回 64、isStackable 恒 true；
 * 合并本身仍由 vanilla canCombine 把关（item + NBT 一致才合并）。
 *
 * 注意：合并用例走箱子 GUI shift-click（AbstractContainerMenu.insertItem 的真实
 * vanilla 路径），因为 inv.insert 的 areItemsEqual 只比 item 不比 NBT。
 */
@McDebugTest
class ReactorStackTest {

    private val CELL = "ic2_120:reactor_coolant_cell"

    /** 消耗一半的冷却单元（10k 容量，已用 5000）。 */
    private val halfUse = mapOf("use" to 5000, "Damage" to 5000)
    /** 另一档寿命：已用 6000。 */
    private val otherUse = mapOf("use" to 6000, "Damage" to 6000)

    private fun placeChest(ctx: TestContext) {
        setBlocks(ctx, listOf(ctx.origin to "minecraft:chest"))
    }

    /** 打开箱子界面并把 10 个带 NBT 的冷却单元放进玩家主背包第一格。 */
    private fun openChestWithPlayerCells(ctx: TestContext, nbt: Map<String, Int>?): Pair<String, Int> {
        val opened = ctx.api.screen.openBlock(ctx.origin, player = "fake").asJsonObject
        val screenId = opened.get("screenId").asString
        val stack = JsonObject().apply {
            addProperty("item", CELL)
            addProperty("count", 10)
            if (nbt != null) add("nbt", gson.toJsonTree(nbt))
        }
        ctx.api.screen.setPlayerSlot(screenId, 9, stack)
        val snap = ctx.api.screen.snapshot(screenId).asJsonObject
        val slots = snap.get("slots").asJsonArray
        val playerIdx = (0 until slots.size()).firstOrNull { i ->
            val s = slots.getValue().asJsonObject
            s.get("item").takeIf { it != null && !it.isJsonNull }?.asString == CELL && s.get("count").asInt == 10
        } ?: throw AssertionError("expected seeded player slot to be visible in chest screen")
        return screenId to playerIdx
    }

    /** getMaxCount 应为 64（getMaxCount mixin）。 */
    @Test
    fun componentMaxStackSizeIs64(ctx: TestContext) {
        placeChest(ctx)
        ctx.api.inv.setSlot(ctx.origin, 0, CELL, 32)
        val slot = ctx.api.inv.getSlot(ctx.origin, 0).asJsonObject
        val maxCount = slot.get("maxCount").asInt
        if (maxCount != 64) throw AssertionError("reactor component max stack should be 64, got $maxCount")
    }

    /** 寿命完全相同（NBT 一致）的已损耗组件经 GUI shift-click 合并。 */
    @Test
    fun componentSameLifespanStacksMerge(ctx: TestContext) {
        placeChest(ctx)
        ctx.api.inv.setSlot(ctx.origin, 0, CELL, 32, halfUse)

        val (screenId, playerIdx) = openChestWithPlayerCells(ctx, halfUse)
        ctx.api.screen.quickMove(screenId, playerIdx)

        val slot0 = ctx.api.inv.getSlot(ctx.origin, 0).asJsonObject.get("slot").asJsonObject
        if (slot0.get("count").asInt != 42) {
            throw AssertionError("shift-click should merge same-lifespan used cells onto chest stack, count=${slot0.get("count").asInt}")
        }
        val nbt = slot0.get("nbt").asJsonObject
        if (nbt.get("use").asInt != 5000) throw AssertionError("merged stack must keep the shared lifespan NBT: $nbt")
        ctx.api.screen.close(screenId)
    }

    /** 寿命不同（NBT 不同）的组件不得合并进已有堆叠，只能落空槽。 */
    @Test
    fun componentDifferentLifespanDoesNotMerge(ctx: TestContext) {
        placeChest(ctx)
        ctx.api.inv.setSlot(ctx.origin, 0, CELL, 32, halfUse)

        val (screenId, playerIdx) = openChestWithPlayerCells(ctx, otherUse)
        ctx.api.screen.quickMove(screenId, playerIdx)

        val slot0 = ctx.api.inv.getSlot(ctx.origin, 0).asJsonObject.get("slot").asJsonObject
        if (slot0.get("count").asInt != 32) {
            throw AssertionError("different-lifespan cells must NOT merge, chest slot count=${slot0.get("count").asInt}")
        }
        ctx.api.screen.close(screenId)
    }

    /** 全新与已损耗组件不合并。 */
    @Test
    fun componentFreshVsUsedDoesNotMerge(ctx: TestContext) {
        placeChest(ctx)
        ctx.api.inv.setSlot(ctx.origin, 0, CELL, 32, halfUse)

        val (screenId, playerIdx) = openChestWithPlayerCells(ctx, null)
        ctx.api.screen.quickMove(screenId, playerIdx)

        val slot0 = ctx.api.inv.getSlot(ctx.origin, 0).asJsonObject.get("slot").asJsonObject
        if (slot0.get("count").asInt != 32) {
            throw AssertionError("fresh vs used cells must NOT merge, chest slot count=${slot0.get("count").asInt}")
        }
        ctx.api.screen.close(screenId)
    }
}
