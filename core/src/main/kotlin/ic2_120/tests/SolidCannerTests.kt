package ic2_120.tests.solid_canner

import ic2_120.tests.*

import com.mcdebug.test.McDebugTest
import com.mcdebug.test.McDebugTestApi
import com.mcdebug.test.SetBlockOp
import com.mcdebug.test.Pos

/**
 * Solid Canner tests — Tier 1 (LV, 32 EU/t) 2-input → 1-output machine.
 *
 * Two input slots (0 = container, 1 = food/material), one output slot (2),
 * one discharging slot (3), four upgrade slots (4..7).
 *
 * PROGRESS_MAX=200 ticks (10 s). ENERGY_PER_TICK=2 EU/t. Energy capacity is
 * small (416 EU), so we expand it via `setBeField("EnergyCapacity", 4000)`
 * to keep the machine running without exhausting the buffer mid-recipe.
 *
 * Two recipe families:
 *   - 燃料棒装填 (datagen) : empty_fuel_rod + uranium → uranium_fuel_rod
 *   - 食物装罐 (runtime fallback) : tin_can + any food → filled_tin_can (count = food hunger)
 *
 * Powered by adjacent BatBox (Tier 1, 40 000 EU, 32 EU/t output).
 *
 * Layout:
 *   testPos(1, 0, 0) BatBox [40 000 EU]  ←  adjacent  ←  TEST_POS Solid Canner
 */
private val BATBOX_POS: Pos get() = testPos(1, 0, 0)

/**
 * Place BatBox (40 000 EU) + solid canner. Expand the machine's internal
 * energy capacity so the 10 s recipe completes without brownouts.
 */
private fun setupSolidCanner() {
    McDebugTestApi.setBlocks(listOf(
        SetBlockOp(BATBOX_POS, "ic2_120:batbox", stateProps = mapOf("facing" to "west"))
    ))
    McDebugTestApi.setBeField(BATBOX_POS, "EnergyStored", 40000)
    McDebugTestApi.place(TEST_POS, "ic2_120:solid_canner")
    McDebugTestApi.setBeField(TEST_POS, "EnergyCapacity", 4000)
    McDebugTestApi.setBeField(TEST_POS, "EnergyStored", 4000)
}

// --- placement ---

object SolidCannerPlaceTest : McDebugTest {
    override val name = "solid_canner:place"
    override fun run() {
        McDebugTestApi.place(TEST_POS, "ic2_120:solid_canner")
        McDebugTestApi.assertBlockId(TEST_POS, "ic2_120:solid_canner")
    }
}

// --- canonical recipes (covers 2-input + 1-output routing) ---

/**
 * 1× empty_fuel_rod + 1× uranium → 1× uranium_fuel_rod. Datagen-registered
 * 2-input SolidCannerRecipe.
 */
object SolidCannerFuelRodUraniumTest : McDebugTest {
    override val name = "solid_canner:fuel_rod_uranium empty_fuel_rod+uranium → uranium_fuel_rod"
    override fun run() {
        setupSolidCanner()
        McDebugTestApi.insertItem(TEST_POS, "ic2_120:fuel_rod", count = 1, slot = 0)
        McDebugTestApi.insertItem(TEST_POS, "ic2_120:uranium", count = 1, slot = 1)
        McDebugTestApi.waitUntil(
            invItemEquals(TEST_POS, slot = 2, itemId = "ic2_120:uranium_fuel_rod"),
            timeoutTicks = 15 * 20,
        )
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 2, "ic2_120:uranium_fuel_rod")
        McDebugTestApi.assertSlotEmpty(TEST_POS, slot = 0)
        McDebugTestApi.assertSlotEmpty(TEST_POS, slot = 1)
    }
}

/**
 * 1× empty_fuel_rod + 1× mox → 1× mox_fuel_rod. Exercises the second
 * SolidCannerRecipe (sister to the uranium recipe).
 */
object SolidCannerFuelRodMoxTest : McDebugTest {
    override val name = "solid_canner:fuel_rod_mox empty_fuel_rod+mox → mox_fuel_rod"
    override fun run() {
        setupSolidCanner()
        McDebugTestApi.insertItem(TEST_POS, "ic2_120:fuel_rod", count = 1, slot = 0)
        McDebugTestApi.insertItem(TEST_POS, "ic2_120:mox", count = 1, slot = 1)
        McDebugTestApi.waitUntil(
            invItemEquals(TEST_POS, slot = 2, itemId = "ic2_120:mox_fuel_rod"),
            timeoutTicks = 15 * 20,
        )
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 2, "ic2_120:mox_fuel_rod")
    }
}

// --- edge cases ---

/** No power: inputs preserved, no progress, no output. */
object SolidCannerNoPowerTest : McDebugTest {
    override val name = "solid_canner:no_power:idle"
    override fun run() {
        McDebugTestApi.place(TEST_POS, "ic2_120:solid_canner")
        McDebugTestApi.insertItem(TEST_POS, "ic2_120:fuel_rod", count = 1, slot = 0)
        McDebugTestApi.insertItem(TEST_POS, "ic2_120:uranium", count = 1, slot = 1)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "ic2_120:fuel_rod")
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 1, "ic2_120:uranium")
        McDebugTestApi.assertSlotEmpty(TEST_POS, slot = 2)
    }
}

/** Only one of the two input slots populated: machine must not progress. */
object SolidCannerMissingInputTest : McDebugTest {
    override val name = "solid_canner:missing_input:one_slot_empty"
    override fun run() {
        setupSolidCanner()
        // Only fuel_rod; no uranium. Recipe requires both.
        McDebugTestApi.insertItem(TEST_POS, "ic2_120:fuel_rod", count = 1, slot = 0)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "ic2_120:fuel_rod")
        McDebugTestApi.assertSlotEmpty(TEST_POS, slot = 2)
    }
}

/** Dirt in both slots: no recipe matches, no output. */
object SolidCannerInvalidInputTest : McDebugTest {
    override val name = "solid_canner:invalid_input:dirt+dirt"
    override fun run() {
        setupSolidCanner()
        McDebugTestApi.insertItem(TEST_POS, "minecraft:dirt", count = 1, slot = 0)
        McDebugTestApi.insertItem(TEST_POS, "minecraft:dirt", count = 1, slot = 1)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "minecraft:dirt")
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 1, "minecraft:dirt")
        McDebugTestApi.assertSlotEmpty(TEST_POS, slot = 2)
    }
}

/**
 * Pre-fill output with 64 uranium_fuel_rod: next craft has nowhere to go,
 * inputs must be preserved.
 */
object SolidCannerOutputFullTest : McDebugTest {
    override val name = "solid_canner:output_full:blocks_next"
    override fun run() {
        setupSolidCanner()
        McDebugTestApi.setSlot(TEST_POS, slot = 2, itemId = "ic2_120:uranium_fuel_rod", count = 64)
        McDebugTestApi.insertItem(TEST_POS, "ic2_120:fuel_rod", count = 1, slot = 0)
        McDebugTestApi.insertItem(TEST_POS, "ic2_120:uranium", count = 1, slot = 1)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "ic2_120:fuel_rod")
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 1, "ic2_120:uranium")
    }
}
