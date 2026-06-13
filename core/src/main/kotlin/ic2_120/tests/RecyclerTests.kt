package ic2_120.tests.recycler

import ic2_120.tests.*

import com.google.gson.JsonObject
import com.mcdebug.test.McDebugTest
import com.mcdebug.test.McDebugTestApi

/**
 * Recycler tests — powered by **battery in discharging slot** (slot 2).
 * Slot layout: 0 = input, 1 = output (scrap, probabilistic), 2 = discharging, 3..6 = upgrades.
 * ENERGY_PER_TICK = 1 EU, PROGRESS_MAX = 50 ticks (~2.5s).
 * Output is probabilistic, so most tests verify input consumption, not output.
 *
 * Blacklist (FALLBACK): minecraft:stick is the only hardcoded blocked item.
 */

object RecyclerPlaceTest : McDebugTest {
    override val name = "recycler:place"
    override fun run() {
        McDebugTestApi.place(TEST_POS, "ic2_120:recycler")
        McDebugTestApi.assertBlockId(TEST_POS, "ic2_120:recycler")
    }
}

/**
 * Power: Charged Re-Battery in discharging slot.
 * Insert a stack of items and verify the recycler processes them.
 * Does NOT assert output since scrap is probabilistic (1/8 chance).
 */
object RecyclerConsumeTest : McDebugTest {
    override val name = "recycler:consume input [battery]"
    override fun run() {
        McDebugTestApi.place(TEST_POS, "ic2_120:recycler")
        val chargedBattery = JsonObject().apply { addProperty("Energy", 10000) }
        McDebugTestApi.setSlot(TEST_POS, slot = 2, itemId = "ic2_120:re_battery", count = 1, nbt = chargedBattery)
        McDebugTestApi.insertItem(TEST_POS, "minecraft:cobblestone", count = 10, slot = 0)
        McDebugTestApi.waitUntil(
            invCountLessThan(TEST_POS, slot = 0, count = 10),
            timeoutTicks = 15 * 20,
        )
        val slot = McDebugTestApi.getSlot(TEST_POS, 0)
        if (slot.count >= 10) {
            throw AssertionError("expected recycler to consume input items, still ${slot.count}")
        }
    }
}

// --- edge cases ---

/** No battery in discharging slot: recycler must not consume input. */
object RecyclerNoPowerTest : McDebugTest {
    override val name = "recycler:no_power:idle"
    override fun run() {
        McDebugTestApi.place(TEST_POS, "ic2_120:recycler")
        // Deliberately leave slot 2 (discharging) empty.
        McDebugTestApi.insertItem(TEST_POS, "minecraft:cobblestone", count = 4, slot = 0)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "minecraft:cobblestone")
    }
}

/**
 * Blacklisted item (stick) must NOT be consumed. The recycler should
 * recognise the blacklist and leave the input untouched.
 */
object RecyclerBlacklistedInputTest : McDebugTest {
    override val name = "recycler:invalid_input:stick"
    override fun run() {
        McDebugTestApi.place(TEST_POS, "ic2_120:recycler")
        val chargedBattery = JsonObject().apply { addProperty("Energy", 10000) }
        McDebugTestApi.setSlot(TEST_POS, slot = 2, itemId = "ic2_120:re_battery", count = 1, nbt = chargedBattery)
        McDebugTestApi.insertItem(TEST_POS, "minecraft:stick", count = 1, slot = 0)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "minecraft:stick")
    }
}
