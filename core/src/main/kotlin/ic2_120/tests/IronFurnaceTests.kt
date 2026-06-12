package ic2_120.tests

import com.mcdebug.test.McDebugTest
import com.mcdebug.test.McDebugTestApi
import com.mcdebug.test.Pos

/**
 * Slot layout for `IronFurnaceBlockEntity` (see
 * `ic2_120/content/block/machines/IronFurnaceBlockEntity.kt`):
 *   0 = input (smeltable)
 *   1 = fuel (coal, charcoal, etc.)
 *   2 = output
 *
 * Both tests use [Pos.TEST_ORIGIN] (100, 64, 100) — the same origin
 * every test object uses. The first test runs place the block; the
 * second relies on the smelt from the previous tick. For a larger
 * test suite we'll want per-test isolation; v0.1 doesn't need it.
 */
private val POS = Pos(100, 64, 100)

/**
 * Place the iron furnace block and verify the world reports it at
 * that position. This is the basic "the registry id exists and the
 * server can place it" smoke test.
 */
object IronFurnacePlaceTest : McDebugTest {
    override val name = "iron_furnace:place+state"

    override fun run() {
        McDebugTestApi.place(POS, "ic2_120:iron_furnace")
        McDebugTestApi.assertBlockId(POS, "ic2_120:iron_furnace")
    }
}

/**
 * Place a furnace, insert iron ore + coal, and wait for the smelting
 * tick to produce an iron ingot in the output slot.
 *
 * Predicate `inv[100,64,100].2.item == "minecraft:iron_ingot"` reads
 * slot 2 of the block-entity inventory at (100, 64, 100). The IC2
 * furnace ticks at the same rate as vanilla — typically a few seconds
 * per smelt. 20s timeout is a comfortable margin for cold start.
 */
object IronFurnaceSmeltTest : McDebugTest {
    override val name = "iron_furnace:smelt iron_ore"

    override fun run() {
        McDebugTestApi.place(POS, "ic2_120:iron_furnace")
        McDebugTestApi.insertItem(POS, "minecraft:iron_ore", count = 1, slot = 0)
        McDebugTestApi.insertItem(POS, "minecraft:coal",        count = 1, slot = 1)
        McDebugTestApi.waitUntil(
            """inv[100,64,100].2.item == "minecraft:iron_ingot"""",
            timeoutTicks = 20 * 20,  // 20 seconds
        )
        McDebugTestApi.assertSlotHas(POS, slot = 2, "minecraft:iron_ingot")
    }
}
