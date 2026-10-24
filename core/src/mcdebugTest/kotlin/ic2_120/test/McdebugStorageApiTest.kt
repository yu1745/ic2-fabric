package ic2_120.test

import com.mcdebug.runner.McDebugTest
import com.mcdebug.runner.TestContext
import com.mcdebug.runner.place
import com.mcdebug.runner.setBlocks
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * mcdebug storage/screen API 测试（验证 mcdebug 自身的泛型存储与界面能力）。
 */
@McDebugTest
class McdebugStorageApiTest {

    private val FABRIC_ITEM = "fabric:item"
    private val FABRIC_FLUID = "fabric:fluid"
    private val TEAMREBORN_ENERGY = "teamreborn:energy"

    private fun assertHandle(handles: com.google.gson.JsonArray, handle: String, kind: String) {
        val ok = (0 until handles.size()).any { i ->
            val h = handles.getValue().asJsonObject
            h.get("handle").asString == handle && h.get("kind").asString == kind
        }
        assertTrue(ok, "expected $kind storage handle $handle; got $handles")
    }

    private fun blockTarget(pos: List<Int>): com.google.gson.JsonElement =
        com.mcdebug.cli.gson.toJsonTree(mapOf("kind" to "block", "pos" to pos))

    private fun itemTarget(stack: Map<String, Any?>): com.google.gson.JsonElement =
        com.mcdebug.cli.gson.toJsonTree(mapOf("kind" to "item", "stack" to stack))

    /** BatBox 能量适配器：list/get/insert/extract 往返。 */
    @Test
    fun batboxEnergyAdapter(ctx: TestContext) {
        ctx.api.world.setBlock(ctx.origin, "ic2_120:batbox", mapOf("facing" to "west"))
        val target = blockTarget(ctx.origin)

        val list = ctx.api.storage.list(target).asJsonObject.get("handles").asJsonArray
        assertHandle(list, TEAMREBORN_ENERGY, "energy")

        val before = ctx.api.storage.get(target, TEAMREBORN_ENERGY).asJsonObject
        assertEquals("energy", before.get("kind").asString)
        assertTrue(before.get("capacity").asLong > 0, "expected positive BatBox energy capacity")

        val inserted = ctx.api.storage.insert(target, TEAMREBORN_ENERGY, mapOf("kind" to "energy"), 32).asJsonObject
        assertEquals(32L, inserted.get("inserted").asLong)

        val afterInsert = ctx.api.storage.get(target, TEAMREBORN_ENERGY).asJsonObject
        assertEquals(before.get("amount").asLong + 32, afterInsert.get("amount").asLong)

        val extracted = ctx.api.storage.extract(target, TEAMREBORN_ENERGY, mapOf("kind" to "energy"), 16).asJsonObject
        assertEquals(16L, extracted.get("extracted").asLong)

        val afterExtract = ctx.api.storage.get(target, TEAMREBORN_ENERGY).asJsonObject
        assertEquals(afterInsert.get("amount").asLong - 16, afterExtract.get("amount").asLong)
    }

    /** Macerator 物品存储路由。 */
    @Test
    fun maceratorItemStorageRoutes(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:macerator")
        val target = blockTarget(ctx.origin)

        val list = ctx.api.storage.list(target).asJsonObject.get("handles").asJsonArray
        assertHandle(list, "vanilla:inventory", "item")
        assertHandle(list, FABRIC_ITEM, "item")

        val inserted = ctx.api.storage.insert(target, FABRIC_ITEM, mapOf("kind" to "item", "item" to "minecraft:cobblestone"), 1).asJsonObject
        assertEquals(1L, inserted.get("inserted").asLong)

        val storage = ctx.api.storage.get(target, FABRIC_ITEM).asJsonObject
        val slots = storage.get("slots").asJsonArray
        val inputSlot = (0 until slots.size()).firstOrNull { i ->
            val s = slots.getValue().asJsonObject.get("stack").asJsonObject
            s.get("item").takeIf { it != null && !it.isJsonNull }?.asString == "minecraft:cobblestone"
        } ?: throw AssertionError("expected cobblestone in fabric:item slots: $slots")
        assertEquals(1, slots.getValue(inputSlot).asJsonObject.get("stack").asJsonObject.get("count").asInt)

        val invalid = ctx.api.storage.insert(
            target, FABRIC_ITEM, mapOf("kind" to "item", "item" to "minecraft:dirt"), 1, simulate = true,
        ).asJsonObject
        assertEquals(0L, invalid.get("inserted").asLong)
    }

    /** IC2 水单元作为物品 target 的流体存储。 */
    @Test
    fun ic2FluidCellItemTarget(ctx: TestContext) {
        val target = itemTarget(mapOf("item" to "ic2_120:water_cell", "count" to 1))

        val list = ctx.api.storage.list(target).asJsonObject.get("handles").asJsonArray
        assertHandle(list, FABRIC_FLUID, "fluid")

        val storage = ctx.api.storage.get(target, FABRIC_FLUID).asJsonObject
        val tanks = storage.get("tanks").asJsonArray
        val hasWater = (0 until tanks.size()).any { i ->
            val t = tanks.getValue().asJsonObject
            t.get("fluid").takeIf { it != null && !it.isJsonNull }?.asString == "minecraft:water" && t.get("amount").asLong == 81_000L
        }
        assertTrue(hasWater, "expected water cell to expose 81000 droplets; got $tanks")

        val extracted = ctx.api.storage.extract(target, FABRIC_FLUID, mapOf("kind" to "fluid", "fluid" to "minecraft:water"), 81_000).asJsonObject
        assertEquals(81_000L, extracted.get("extracted").asLong)
        val targetAfter = extracted.get("targetAfter").asJsonObject
        if (targetAfter.get("kind").asString == "item") {
            assertEquals("ic2_120:empty_cell", targetAfter.get("stack").asJsonObject.get("item").asString)
        }
    }

    /** Macerator 界面：open/snapshot/quickMove/close。 */
    @Test
    fun maceratorScreenHandler(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:macerator")
        val opened = ctx.api.screen.openBlock(ctx.origin, player = "fake").asJsonObject
        assertEquals("ic2_120:macerator", opened.get("handlerType").asString)
        assertEquals(43, opened.get("slots").asJsonArray.size())
        assertTrue(opened.get("properties").asJsonArray.size() > 0, "expected macerator ContainerData values")

        val afterQuickMove = ctx.api.screen.quickMove(opened.get("screenId").asString, 0).asJsonObject
        assertEquals(opened.get("screenId").asString, afterQuickMove.get("screenId").asString)

        val closed = ctx.api.screen.close(opened.get("screenId").asString).asJsonObject
        assertEquals(true, closed.get("closed").asBoolean)
    }

    /** 界面 shift-click 路由玩家物品到机器槽位。 */
    @Test
    fun maceratorScreenQuickMoveRoutesPlayerItems(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:macerator")
        val opened = ctx.api.screen.openBlock(ctx.origin, player = "fake").asJsonObject
        val screenId = opened.get("screenId").asString

        // 玩家主背包第一格放圆石 → quickMove(34) 应进机器输入槽 0
        ctx.api.screen.setPlayerSlot(screenId, 0, mapOf("item" to "minecraft:cobblestone", "count" to 1))
        val afterInputMove = ctx.api.screen.quickMove(screenId, 34).asJsonObject
        val slots = afterInputMove.get("slots").asJsonArray
        assertEquals("minecraft:cobblestone", slots.get(0).asJsonObject.get("item").asString)
        assertTrue(slots.get(34).asJsonObject.get("item").isJsonNull)

        // 超频升级 → quickMove(35) 应进升级槽 3
        ctx.api.screen.setPlayerSlot(screenId, 1, mapOf("item" to "ic2_120:overclocker_upgrade", "count" to 1))
        val afterUpgradeMove = ctx.api.screen.quickMove(screenId, 35).asJsonObject
        val slots2 = afterUpgradeMove.get("slots").asJsonArray
        assertEquals("ic2_120:overclocker_upgrade", slots2.get(3).asJsonObject.get("item").asString)
        assertTrue(slots2.get(35).asJsonObject.get("item").isJsonNull)

        // 泥土（无路由）→ quickMove(36) 应留在玩家槽
        ctx.api.screen.setPlayerSlot(screenId, 2, mapOf("item" to "minecraft:dirt", "count" to 1))
        val afterInvalidMove = ctx.api.screen.quickMove(screenId, 36).asJsonObject
        assertEquals("minecraft:dirt", afterInvalidMove.get("slots").asJsonArray.get(36).asJsonObject.get("item").asString)

        ctx.api.screen.close(screenId)
    }

    /** 储罐流体存储与转移（方块↔方块、水单元→方块）。 */
    @Test
    fun tankFluidStorageAndTransfer(ctx: TestContext) {
        val fromPos = ctx.origin
        val toPos = ctx.pos(1, 0, 0)
        place(ctx, fromPos, "ic2_120:bronze_tank")
        place(ctx, toPos, "ic2_120:bronze_tank")

        val from = blockTarget(fromPos)
        val to = blockTarget(toPos)
        val list = ctx.api.storage.list(from).asJsonObject.get("handles").asJsonArray
        assertHandle(list, FABRIC_FLUID, "fluid")

        val inserted = ctx.api.storage.insert(from, FABRIC_FLUID, mapOf("kind" to "fluid", "fluid" to "minecraft:water"), 162_000).asJsonObject
        assertEquals(162_000L, inserted.get("inserted").asLong)

        val moved = ctx.api.storage.transfer(from, to, mapOf("kind" to "fluid", "fluid" to "minecraft:water"), 81_000).asJsonObject
        assertEquals(81_000L, moved.get("transferred").asLong)

        val fromStorage = ctx.api.storage.get(from, FABRIC_FLUID).asJsonObject
        val toStorage = ctx.api.storage.get(to, FABRIC_FLUID).asJsonObject
        assertEquals(81_000L, fromStorage.get("tanks").asJsonArray.get(0).asJsonObject.get("amount").asLong)
        assertEquals(81_000L, toStorage.get("tanks").asJsonArray.get(0).asJsonObject.get("amount").asLong)

        // 水单元 → 储罐
        val cellTransfer = ctx.api.storage.transfer(
            itemTarget(mapOf("item" to "ic2_120:water_cell", "count" to 1)),
            to,
            mapOf("kind" to "fluid", "fluid" to "minecraft:water"),
            81_000,
        ).asJsonObject
        assertEquals(81_000L, cellTransfer.get("transferred").asLong)
        val fromAfter = cellTransfer.get("fromAfter").asJsonObject
        if (fromAfter.get("kind").asString == "item") {
            assertEquals("ic2_120:empty_cell", fromAfter.get("stack").asJsonObject.get("item").asString)
        }

        val finalTo = ctx.api.storage.get(to, FABRIC_FLUID).asJsonObject
        assertEquals(162_000L, finalTo.get("tanks").asJsonArray.get(0).asJsonObject.get("amount").asLong)
    }
}
