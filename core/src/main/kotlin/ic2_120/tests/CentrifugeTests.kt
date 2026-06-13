package ic2_120.tests.centrifuge

import ic2_120.tests.*

import com.mcdebug.test.McDebugTest
import com.mcdebug.test.McDebugTestApi
import com.mcdebug.test.SetBlockOp
import com.mcdebug.test.Pos

/**
 * Centrifuge tests — Tier 2 (MV) thermal centrifuge.
 *
 * Energy: 48 EU/t processing + 1 EU/t heating, max 128 EU/t insert. PROGRESS_MAX=500
 * ticks = 25 s per recipe (no overclocker). Capacity 10 000 EU.
 *
 * Heat: 0–5000. Min heat per recipe is enforced; without redstone heat decays at
 * 1 HU/t toward `recipe.minHeat` (or 0 if no input). Setting `Heat` directly via
 * setBeField bypasses the 5000-tick natural warm-up and lets the test recipe
 * start immediately. Heat > min_heat decays slowly (1/t), so a 1500-tick test
 * can afford to start at any value above the recipe's min_heat.
 *
 * Slot layout: 0 = input, 1/2/3 = outputs, 4 = discharging, 5..8 = upgrades.
 *
 * Powered by adjacent CESU (Tier 2, 300 000 EU, 128 EU/t output) directly to
 * the east. No cable needed: AdjacentEnergyTransferComponent handles
 * machine-to-machine contact and matches tier.
 *
 * Layout:
 *   testPos(1, 0, 0) CESU [300 000 EU]  ←  adjacent  ←  TEST_POS Centrifuge
 */
private val CESU_POS: Pos get() = testPos(1, 0, 0)

/** Set the SyncedData "Heat" field (stored as two 16-bit halves in NBT). */
private fun setHeat(value: Int) {
    McDebugTestApi.setBeField(TEST_POS, "Heat_Low", value and 0xFFFF)
    McDebugTestApi.setBeField(TEST_POS, "Heat_High", (value ushr 16) and 0xFFFF)
}

/**
 * Place CESU (300 000 EU) + centrifuge. Charge the centrifuge directly so the
 * heating-rate logic (1 EU/t) doesn't drain the CESU's neighbor buffer during
 * the test.
 */
private fun setupCentrifuge() {
    McDebugTestApi.setBlocks(listOf(
        SetBlockOp(CESU_POS, "ic2_120:cesu", stateProps = mapOf("facing" to "west"))
    ))
    McDebugTestApi.setBeField(CESU_POS, "EnergyStored", 300_000)
    McDebugTestApi.place(TEST_POS, "ic2_120:centrifuge")
    McDebugTestApi.setBeField(TEST_POS, "EnergyStored", 40_000)
}

private fun setupCentrifugeHot(minHeat: Int) {
    setupCentrifuge()
    // Start a comfortable margin above minHeat so small decay during the
    // 25 s recipe run doesn't drop below the threshold.
    setHeat(minHeat + 200)
}

// --- placement ---

object CentrifugePlaceTest : McDebugTest {
    override val name = "centrifuge:place"
    override fun run() {
        McDebugTestApi.place(TEST_POS, "ic2_120:centrifuge")
        McDebugTestApi.assertBlockId(TEST_POS, "ic2_120:centrifuge")
    }
}

// --- canonical recipes (covers single-output + multi-output paths) ---

/**
 * 1× cobblestone → 1× stone_dust (min heat 100). Simplest 1→1 recipe.
 */
object CentrifugeCobbleTest : McDebugTest {
    override val name = "centrifuge:1×→1×:cobblestone → stone_dust [heat=300]"
    override fun run() {
        setupCentrifugeHot(minHeat = 100)
        McDebugTestApi.insertItem(TEST_POS, "minecraft:cobblestone", count = 1, slot = 0)
        McDebugTestApi.waitUntil(
            invItemEquals(TEST_POS, slot = 1, itemId = "ic2_120:stone_dust"),
            timeoutTicks = 40 * 20,
        )
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 1, "ic2_120:stone_dust")
        McDebugTestApi.assertSlotEmpty(TEST_POS, slot = 0)
    }
}

/**
 * 1× crushed_copper → 3 outputs (small_tin_dust, copper_dust, stone_dust).
 * Tests multi-output routing into slots 1/2/3.
 */
object CentrifugeMultiOutputTest : McDebugTest {
    override val name = "centrifuge:1×→3×:crushed_copper → 3 outputs [heat=700]"
    override fun run() {
        setupCentrifugeHot(minHeat = 500)
        McDebugTestApi.insertItem(TEST_POS, "ic2_120:crushed_copper", count = 1, slot = 0)
        McDebugTestApi.waitUntil(
            invItemEquals(TEST_POS, slot = 2, itemId = "ic2_120:copper_dust"),
            timeoutTicks = 40 * 20,
        )
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 1, "ic2_120:small_tin_dust")
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 2, "ic2_120:copper_dust")
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 3, "ic2_120:stone_dust")
    }
}

// --- edge cases ---

/** No power → no progress, input preserved. */
object CentrifugeNoPowerTest : McDebugTest {
    override val name = "centrifuge:no_power:idle"
    override fun run() {
        // Place centrifuge only — no CESU.
        McDebugTestApi.place(TEST_POS, "ic2_120:centrifuge")
        McDebugTestApi.insertItem(TEST_POS, "minecraft:cobblestone", count = 1, slot = 0)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "minecraft:cobblestone")
        McDebugTestApi.assertSlotEmpty(TEST_POS, slot = 1)
    }
}

/** Heat < min_heat → machine refuses to craft, even with full power. */
object CentrifugeHeatStarveTest : McDebugTest {
    override val name = "centrifuge:heat_starve:cold_centrifuge"
    override fun run() {
        setupCentrifuge()
        // Heat stays at 0 (default) — below the 100 min_heat for cobblestone.
        McDebugTestApi.insertItem(TEST_POS, "minecraft:cobblestone", count = 1, slot = 0)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "minecraft:cobblestone")
        McDebugTestApi.assertSlotEmpty(TEST_POS, slot = 1)
    }
}

/** Dirt is not a centrifuging recipe → input must not be consumed. */
object CentrifugeInvalidInputTest : McDebugTest {
    override val name = "centrifuge:invalid_input:dirt"
    override fun run() {
        setupCentrifugeHot(minHeat = 100)
        McDebugTestApi.insertItem(TEST_POS, "minecraft:dirt", count = 1, slot = 0)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "minecraft:dirt")
        McDebugTestApi.assertSlotEmpty(TEST_POS, slot = 1)
    }
}

/**
 * Output full: pre-fill all three output slots to capacity. The machine must
 * NOT consume another input (centrifuge checks `canAcceptOutputs` before
 * progressing), but it WILL still heat up and consume energy.
 */
object CentrifugeOutputFullTest : McDebugTest {
    override val name = "centrifuge:output_full:blocks_next"
    override fun run() {
        setupCentrifugeHot(minHeat = 100)
        // Block all three outputs.
        McDebugTestApi.setSlot(TEST_POS, slot = 1, itemId = "ic2_120:stone_dust", count = 64)
        McDebugTestApi.setSlot(TEST_POS, slot = 2, itemId = "ic2_120:stone_dust", count = 64)
        McDebugTestApi.setSlot(TEST_POS, slot = 3, itemId = "ic2_120:stone_dust", count = 64)
        McDebugTestApi.insertItem(TEST_POS, "minecraft:cobblestone", count = 1, slot = 0)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "minecraft:cobblestone")
    }
}
