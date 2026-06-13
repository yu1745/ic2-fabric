package ic2_120.tests.metal_former

import ic2_120.tests.*

import com.mcdebug.test.McDebugTest
import com.mcdebug.test.McDebugTestApi
import com.mcdebug.test.SetBlockOp
import com.mcdebug.test.Pos

/**
 * Metal Former tests — powered by adjacent BatBox.
 *
 * Three modes, all checked:
 *   ROLLING    (0):  1× iron_ingot → 1× iron_plate
 *   CUTTING    (1):  1× iron_plate → 4× iron_cable
 *   EXTRUDING  (2):  1× iron_ingot → 4× iron_cable
 *
 * Slot layout: 0 = input, 1 = output, 2 = discharging, 3..6 = upgrades.
 * PROGRESS_MAX = 200, ENERGY_PER_TICK = 10 — 10 s base, no overclocker needed.
 *
 * Mode is selected via `setBeField("Mode", N)` where N is 0/1/2.
 * Default is ROLLING (0) on placement.
 *
 * Layout:
 *   testPos(1,0,0) BatBox [40 000 EU]  ←  adjacent  ←  TEST_POS Metal Former
 */
private val BATBOX_POS: Pos get() = testPos(1, 0, 0)

private fun setupMetalFormer(mode: Int) {
    McDebugTestApi.setBlocks(listOf(
        SetBlockOp(BATBOX_POS, "ic2_120:batbox", stateProps = mapOf("facing" to "west"))
    ))
    McDebugTestApi.setBeField(BATBOX_POS, "EnergyStored", 40000)
    McDebugTestApi.place(TEST_POS, "ic2_120:metal_former")
    // Pick the recipe family: 0 = ROLLING (ingot→plate),
    //                       1 = CUTTING (plate→cable),
    //                       2 = EXTRUDING (ingot→cable)
    McDebugTestApi.setBeField(TEST_POS, "Mode", mode)
}

object MetalFormerPlaceTest : McDebugTest {
    override val name = "metal_former:place"
    override fun run() {
        McDebugTestApi.place(TEST_POS, "ic2_120:metal_former")
        McDebugTestApi.assertBlockId(TEST_POS, "ic2_120:metal_former")
    }
}

/** 1× iron_ingot → 1× iron_plate (ROLLING mode, default). */
object MetalFormerRollingTest : McDebugTest {
    override val name = "metal_former:rolling iron_ingot → iron_plate"
    override fun run() {
        setupMetalFormer(mode = 0)
        McDebugTestApi.insertItem(TEST_POS, "minecraft:iron_ingot", count = 1, slot = 0)
        McDebugTestApi.waitUntil(
            invItemEquals(TEST_POS, slot = 1, itemId = "ic2_120:iron_plate"),
            timeoutTicks = 15 * 20,
        )
        McDebugTestApi.assertSlotCount(TEST_POS, slot = 1, expectedCount = 1)
    }
}

/** 1× iron_plate → 4× iron_cable (CUTTING mode). */
object MetalFormerCuttingTest : McDebugTest {
    override val name = "metal_former:cutting iron_plate → 4× iron_cable"
    override fun run() {
        setupMetalFormer(mode = 1)
        McDebugTestApi.insertItem(TEST_POS, "ic2_120:iron_plate", count = 1, slot = 0)
        McDebugTestApi.waitUntil(
            invItemEquals(TEST_POS, slot = 1, itemId = "ic2_120:iron_cable"),
            timeoutTicks = 15 * 20,
        )
        McDebugTestApi.assertSlotCount(TEST_POS, slot = 1, expectedCount = 4)
    }
}

/** 1× iron_ingot → 4× iron_cable (EXTRUDING mode). */
object MetalFormerExtrudingTest : McDebugTest {
    override val name = "metal_former:extruding iron_ingot → 4× iron_cable"
    override fun run() {
        setupMetalFormer(mode = 2)
        McDebugTestApi.insertItem(TEST_POS, "minecraft:iron_ingot", count = 1, slot = 0)
        McDebugTestApi.waitUntil(
            invItemEquals(TEST_POS, slot = 1, itemId = "ic2_120:iron_cable"),
            timeoutTicks = 15 * 20,
        )
        McDebugTestApi.assertSlotCount(TEST_POS, slot = 1, expectedCount = 4)
    }
}

// --- edge cases ---

object MetalFormerNoPowerTest : McDebugTest {
    override val name = "metal_former:no_power:idle"
    override fun run() {
        McDebugTestApi.place(TEST_POS, "ic2_120:metal_former")
        McDebugTestApi.insertItem(TEST_POS, "minecraft:iron_ingot", count = 1, slot = 0)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "minecraft:iron_ingot")
        McDebugTestApi.assertSlotEmpty(TEST_POS, slot = 1)
    }
}

/**
 * Wrong mode for the input: e.g. insert iron_plate while in ROLLING mode.
 * ROLLING has no plate→? recipe, so the input must be left untouched.
 */
object MetalFormerInvalidInputTest : McDebugTest {
    override val name = "metal_former:invalid_input:plate_in_rolling_mode"
    override fun run() {
        setupMetalFormer(mode = 0)
        McDebugTestApi.insertItem(TEST_POS, "ic2_120:iron_plate", count = 1, slot = 0)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "ic2_120:iron_plate")
        McDebugTestApi.assertSlotEmpty(TEST_POS, slot = 1)
    }
}

object MetalFormerOutputFullTest : McDebugTest {
    override val name = "metal_former:output_full:blocks_next"
    override fun run() {
        setupMetalFormer(mode = 0)
        // Pre-fill output to 64 iron_plate.
        McDebugTestApi.setSlot(TEST_POS, slot = 1, itemId = "ic2_120:iron_plate", count = 64)
        McDebugTestApi.insertItem(TEST_POS, "minecraft:iron_ingot", count = 1, slot = 0)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "minecraft:iron_ingot")
    }
}
