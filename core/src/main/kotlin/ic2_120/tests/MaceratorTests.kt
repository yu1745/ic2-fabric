package ic2_120.tests.macerator

import ic2_120.tests.*

import com.mcdebug.test.McDebugTest
import com.mcdebug.test.McDebugTestApi
import com.mcdebug.test.SetBlockOp
import com.mcdebug.test.Pos

/**
 * Macerator tests.
 *
 * Tier = 1 (LV, max 32 EU/t). ENERGY_PER_TICK = 2. PROGRESS_MAX = 400 ticks (~20s).
 * Slot layout: 0 = input, 1 = output, 2 = discharging, 3..6 = upgrades.
 *
 * Powered layout (BatBox + insulated copper cable):
 *   testPos(2,0,0) BatBox  →  testPos(1,0,0) cable  →  TEST_POS Macerator
 */
private val BATBOX_POS: Pos get() = testPos(2, 0, 0)
private val CABLE_POS: Pos get() = testPos(1, 0, 0)

/**
 * Place BatBox (40 000 EU) + insulated copper cable + macerator with the
 * correct facings so power flows: batbox → cable → mac.
 */
private fun setupMacerator() {
    McDebugTestApi.setBlocks(listOf(
        SetBlockOp(BATBOX_POS, "ic2_120:batbox", stateProps = mapOf("facing" to "west"))
    ))
    McDebugTestApi.setBeField(BATBOX_POS, "EnergyStored", 40000)
    McDebugTestApi.placeAsPlayer(
        CABLE_POS,
        "ic2_120:insulated_copper_cable",
        face = "west",
        neighbor = BATBOX_POS,
        playerFacing = "east",
    )
    McDebugTestApi.place(TEST_POS, "ic2_120:macerator")
}

// --- placement + one canonical recipe ---

object MaceratorPlaceTest : McDebugTest {
    override val name = "macerator:place"
    override fun run() {
        McDebugTestApi.place(TEST_POS, "ic2_120:macerator")
        McDebugTestApi.assertBlockId(TEST_POS, "ic2_120:macerator")
    }
}

object MaceratorGrindTest : McDebugTest {
    override val name = "macerator:cobblestone → gravel [cable]"
    override fun run() {
        setupMacerator()
        McDebugTestApi.insertItem(TEST_POS, "minecraft:cobblestone", count = 1, slot = 0)
        McDebugTestApi.waitUntil(
            invItemEquals(TEST_POS, slot = 1, itemId = "minecraft:gravel"),
            timeoutTicks = 15 * 20,
        )
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 1, "minecraft:gravel")
    }
}

// --- recipe kinds (one per shape; covers 58 recipes) ---

/** 1 input → N output: output count > 1. */
object MaceratorOutputMultiTest : McDebugTest {
    override val name = "macerator:1×→N×:coal_block→9×coal_dust"
    override fun run() {
        setupMacerator()
        McDebugTestApi.insertItem(TEST_POS, "minecraft:coal_block", count = 1, slot = 0)
        McDebugTestApi.waitUntil(
            invItemEquals(TEST_POS, slot = 1, itemId = "ic2_120:coal_dust"),
            timeoutTicks = 15 * 20,
        )
        McDebugTestApi.assertSlotCount(TEST_POS, slot = 1, expectedCount = 9)
    }
}

/** N input → 1 output: inputCount > 1. */
object MaceratorInputMultiTest : McDebugTest {
    override val name = "macerator:8×→1×:melon_slice→bio_chaff"
    override fun run() {
        setupMacerator()
        McDebugTestApi.insertItem(TEST_POS, "minecraft:melon_slice", count = 8, slot = 0)
        McDebugTestApi.waitUntil(
            invItemEquals(TEST_POS, slot = 1, itemId = "ic2_120:bio_chaff"),
            timeoutTicks = 15 * 20,
        )
        McDebugTestApi.assertSlotCount(TEST_POS, slot = 1, expectedCount = 1)
    }
}

/** Tag-based ingredient: ensures ModTags.Compat.Items.ORES_IRON dispatch works. */
object MaceratorTagIngredientTest : McDebugTest {
    override val name = "macerator:tag_ingredient:iron_ore→2×crushed_iron"
    override fun run() {
        setupMacerator()
        McDebugTestApi.insertItem(TEST_POS, "minecraft:iron_ore", count = 1, slot = 0)
        McDebugTestApi.waitUntil(
            invItemEquals(TEST_POS, slot = 1, itemId = "ic2_120:crushed_iron"),
            timeoutTicks = 15 * 20,
        )
        McDebugTestApi.assertSlotCount(TEST_POS, slot = 1, expectedCount = 2)
    }
}

// --- edge cases ---

/** No power: input must not be consumed. */
object MaceratorNoPowerTest : McDebugTest {
    override val name = "macerator:no_power:idle"
    override fun run() {
        // Place mac only — no BatBox, no cable.
        McDebugTestApi.place(TEST_POS, "ic2_120:macerator")
        McDebugTestApi.insertItem(TEST_POS, "minecraft:cobblestone", count = 1, slot = 0)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "minecraft:cobblestone")
        McDebugTestApi.assertSlotEmpty(TEST_POS, slot = 1)
    }
}

/** No recipe for input: input must not be consumed. */
object MaceratorInvalidInputTest : McDebugTest {
    override val name = "macerator:invalid_input:dirt"
    override fun run() {
        setupMacerator()
        McDebugTestApi.insertItem(TEST_POS, "minecraft:dirt", count = 1, slot = 0)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "minecraft:dirt")
        McDebugTestApi.assertSlotEmpty(TEST_POS, slot = 1)
    }
}

/** Output slot full: mac must block before consuming next input. */
object MaceratorOutputFullTest : McDebugTest {
    override val name = "macerator:output_full:blocks_next"
    override fun run() {
        setupMacerator()
        // Pre-fill output to 64 gravel — next craft has nowhere to go.
        McDebugTestApi.setSlot(TEST_POS, slot = 1, itemId = "minecraft:gravel", count = 64)
        McDebugTestApi.insertItem(TEST_POS, "minecraft:cobblestone", count = 1, slot = 0)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "minecraft:cobblestone")
    }
}

/** Energy < ENERGY_PER_TICK: mac must not start craft (no partial consume). */
object MaceratorEnergyStarveTest : McDebugTest {
    override val name = "macerator:energy_starve:no_partial_consume"
    override fun run() {
        // Build, but BatBox only has 1 EU (< ENERGY_PER_TICK = 2).
        McDebugTestApi.setBlocks(listOf(
            SetBlockOp(BATBOX_POS, "ic2_120:batbox", stateProps = mapOf("facing" to "west"))
        ))
        McDebugTestApi.setBeField(BATBOX_POS, "EnergyStored", 1)
        McDebugTestApi.placeAsPlayer(
            CABLE_POS,
            "ic2_120:insulated_copper_cable",
            face = "west",
            neighbor = BATBOX_POS,
            playerFacing = "east",
        )
        McDebugTestApi.place(TEST_POS, "ic2_120:macerator")
        McDebugTestApi.insertItem(TEST_POS, "minecraft:cobblestone", count = 1, slot = 0)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "minecraft:cobblestone")
    }
}

// --- power paths ---

/**
 * BatBox DIRECTLY adjacent to mac, no cable. Verifies the direct machine-to-
 * machine energy transfer path (AdjacentEnergyTransferComponent).
 */
object MaceratorAdjacentPowerTest : McDebugTest {
    override val name = "macerator:power:adjacent_batbox"
    override fun run() {
        // BatBox at testPos(1,0,0) — DIRECTLY adjacent to mac at TEST_POS.
        val ADJACENT_BATBOX = testPos(1, 0, 0)
        McDebugTestApi.setBlocks(listOf(
            SetBlockOp(ADJACENT_BATBOX, "ic2_120:batbox", stateProps = mapOf("facing" to "west"))
        ))
        McDebugTestApi.setBeField(ADJACENT_BATBOX, "EnergyStored", 40000)
        McDebugTestApi.place(TEST_POS, "ic2_120:macerator")
        McDebugTestApi.insertItem(TEST_POS, "minecraft:cobblestone", count = 1, slot = 0)
        McDebugTestApi.waitUntil(
            invItemEquals(TEST_POS, slot = 1, itemId = "minecraft:gravel"),
            timeoutTicks = 15 * 20,
        )
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 1, "minecraft:gravel")
    }
}

/**
 * MFSU (tier 4, 40M EU) directly adjacent to LV mac on MFSU's `facing=west`
 * output face. AdjacentEnergyTransferComponent.tick() detects the overvoltage
 * (4 > 1) and explodes the mac. Verify the mac is destroyed within a few
 * ticks. MFSU must face the mac so its output side supports extraction;
 * other faces are input-only.
 */
object MaceratorOvervoltageTest : McDebugTest {
    override val name = "macerator:overvoltage:hv_explode"
    override fun run() {
        // MFSU at testPos(1,0,0) facing west → output face = west = mac's east side.
        val MFSU_POS = testPos(1, 0, 0)
        McDebugTestApi.setBlocks(listOf(
            SetBlockOp(MFSU_POS, "ic2_120:mfsu", stateProps = mapOf("facing" to "west"))
        ))
        McDebugTestApi.setBeField(MFSU_POS, "EnergyStored", 40_000_000L)
        McDebugTestApi.place(TEST_POS, "ic2_120:macerator")
        McDebugTestApi.insertItem(TEST_POS, "minecraft:cobblestone", count = 1, slot = 0)
        // Mac should be destroyed within a handful of ticks.
        McDebugTestApi.waitUntil(
            "block[${TEST_POS.x},${TEST_POS.y},${TEST_POS.z}].id == \"minecraft:air\"",
            timeoutTicks = 15 * 20,
        )
        McDebugTestApi.assertBlockId(TEST_POS, "minecraft:air")
    }
}
