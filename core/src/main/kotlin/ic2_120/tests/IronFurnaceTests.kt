package ic2_120.tests.iron_furnace

import ic2_120.tests.*

import com.mcdebug.test.McDebugTest
import com.mcdebug.test.McDebugTestApi

/**
 * Iron Furnace tests — fuel-based processing, no EU required.
 * Slot layout: 0 = input, 1 = fuel, 2 = output.
 * Smelts vanilla furnace recipes (1×→1×).
 */

object IronFurnacePlaceTest : McDebugTest {
    override val name = "iron_furnace:place+state"
    override fun run() {
        McDebugTestApi.place(TEST_POS, "ic2_120:iron_furnace")
        McDebugTestApi.assertBlockId(TEST_POS, "ic2_120:iron_furnace")
    }
}

/** 1× iron_ore + 1× coal → 1× iron_ingot: vanilla furnace recipe, coal-powered. */
object IronFurnaceSmeltTest : McDebugTest {
    override val name = "iron_furnace:smelt iron_ore"
    override fun run() {
        McDebugTestApi.place(TEST_POS, "ic2_120:iron_furnace")
        McDebugTestApi.insertItem(TEST_POS, "minecraft:iron_ore", count = 1, slot = 0)
        McDebugTestApi.insertItem(TEST_POS, "minecraft:coal", count = 1, slot = 1)
        McDebugTestApi.waitUntil(
            invItemEquals(TEST_POS, slot = 2, itemId = "minecraft:iron_ingot"),
            timeoutTicks = 15 * 20,
        )
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 2, "minecraft:iron_ingot")
    }
}

// --- edge cases ---

/** No fuel: input must not be consumed. */
object IronFurnaceNoFuelTest : McDebugTest {
    override val name = "iron_furnace:no_fuel:idle"
    override fun run() {
        McDebugTestApi.place(TEST_POS, "ic2_120:iron_furnace")
        McDebugTestApi.insertItem(TEST_POS, "minecraft:iron_ore", count = 1, slot = 0)
        // slot 1 (fuel) deliberately empty
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "minecraft:iron_ore")
        McDebugTestApi.assertSlotEmpty(TEST_POS, slot = 2)
    }
}

/** Fuel present, but no smeltable input. */
object IronFurnaceInvalidInputTest : McDebugTest {
    override val name = "iron_furnace:invalid_input:dirt"
    override fun run() {
        McDebugTestApi.place(TEST_POS, "ic2_120:iron_furnace")
        McDebugTestApi.insertItem(TEST_POS, "minecraft:dirt", count = 1, slot = 0)
        McDebugTestApi.insertItem(TEST_POS, "minecraft:coal", count = 1, slot = 1)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "minecraft:dirt")
        McDebugTestApi.assertSlotEmpty(TEST_POS, slot = 2)
    }
}

/** Output slot full: smelt must not consume next input. */
object IronFurnaceOutputFullTest : McDebugTest {
    override val name = "iron_furnace:output_full:blocks_next"
    override fun run() {
        McDebugTestApi.place(TEST_POS, "ic2_120:iron_furnace")
        McDebugTestApi.setSlot(TEST_POS, slot = 2, itemId = "minecraft:iron_ingot", count = 64)
        McDebugTestApi.insertItem(TEST_POS, "minecraft:iron_ore", count = 1, slot = 0)
        McDebugTestApi.insertItem(TEST_POS, "minecraft:coal", count = 1, slot = 1)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "minecraft:iron_ore")
    }
}
