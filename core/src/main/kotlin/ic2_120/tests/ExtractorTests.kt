package ic2_120.tests.extractor

import ic2_120.tests.*

import com.mcdebug.test.McDebugTest
import com.mcdebug.test.McDebugTestApi
import com.mcdebug.test.SetBlockOp
import com.mcdebug.test.Pos

/**
 * Extractor tests — powered by adjacent BatBox.
 * Slot layout: 0 = input, 1 = output, 2 = discharging, 3..6 = upgrades.
 * All 27 recipes are 1×→N× (splitting: blocks→balls, wool→white_wool, etc.).
 *
 * Layout:
 *   testPos(1,0,0) BatBox [40 000 EU]  ←  adjacent  ←  TEST_POS Extractor
 */
private val BATBOX_POS: Pos get() = testPos(1, 0, 0)

/** Place BatBox (40k EU) directly adjacent (east) to the extractor. */
private fun setupExtractor() {
    McDebugTestApi.setBlocks(listOf(
        SetBlockOp(BATBOX_POS, "ic2_120:batbox", stateProps = mapOf("facing" to "west"))
    ))
    McDebugTestApi.setBeField(BATBOX_POS, "EnergyStored", 40000)
    McDebugTestApi.place(TEST_POS, "ic2_120:extractor")
    // 2 overclocker upgrades → ~2.04× speed; keeps slow recipes inside the
    // 15 s timeout window under parallel test load.
    McDebugTestApi.insertItem(TEST_POS, "ic2_120:overclocker_upgrade", count = 2, slot = 3)
}

// --- placement + canonical recipe (1 of 27) ---

object ExtractorPlaceTest : McDebugTest {
    override val name = "extractor:place"
    override fun run() {
        McDebugTestApi.place(TEST_POS, "ic2_120:extractor")
        McDebugTestApi.assertBlockId(TEST_POS, "ic2_120:extractor")
    }
}

/** 1× clay_block → 4× clay_ball: 1×→4× representative. */
object ExtractorExtractTest : McDebugTest {
    override val name = "extractor:clay_block → 4× clay_ball [adjacent BatBox]"
    override fun run() {
        setupExtractor()
        McDebugTestApi.insertItem(TEST_POS, "minecraft:clay", count = 1, slot = 0)
        McDebugTestApi.waitUntil(
            invItemEquals(TEST_POS, slot = 1, itemId = "minecraft:clay_ball"),
            timeoutTicks = 20 * 20,
        )
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 1, "minecraft:clay_ball")
        McDebugTestApi.assertSlotCount(TEST_POS, slot = 1, expectedCount = 4)
    }
}

/**
 * 1× resin → 3× rubber. Only recipe with output_count=3 (most are 1 or 4);
 * verifies the count field is honored at craft time.
 */
object ExtractorResinTest : McDebugTest {
    override val name = "extractor:resin → 3× rubber"
    override fun run() {
        setupExtractor()
        McDebugTestApi.insertItem(TEST_POS, "ic2_120:resin", count = 1, slot = 0)
        McDebugTestApi.waitUntil(
            invItemEquals(TEST_POS, slot = 1, itemId = "ic2_120:rubber"),
            timeoutTicks = 20 * 20,
        )
        McDebugTestApi.assertSlotCount(TEST_POS, slot = 1, expectedCount = 3)
    }
}

// --- edge cases ---

object ExtractorNoPowerTest : McDebugTest {
    override val name = "extractor:no_power:idle"
    override fun run() {
        McDebugTestApi.place(TEST_POS, "ic2_120:extractor")
        McDebugTestApi.insertItem(TEST_POS, "minecraft:clay", count = 1, slot = 0)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "minecraft:clay")
        McDebugTestApi.assertSlotEmpty(TEST_POS, slot = 1)
    }
}

object ExtractorInvalidInputTest : McDebugTest {
    override val name = "extractor:invalid_input:dirt"
    override fun run() {
        setupExtractor()
        McDebugTestApi.insertItem(TEST_POS, "minecraft:dirt", count = 1, slot = 0)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "minecraft:dirt")
        McDebugTestApi.assertSlotEmpty(TEST_POS, slot = 1)
    }
}

object ExtractorOutputFullTest : McDebugTest {
    override val name = "extractor:output_full:blocks_next"
    override fun run() {
        setupExtractor()
        // Pre-fill output to 64 clay_ball — next split has nowhere to go.
        McDebugTestApi.setSlot(TEST_POS, slot = 1, itemId = "minecraft:clay_ball", count = 64)
        McDebugTestApi.insertItem(TEST_POS, "minecraft:clay", count = 1, slot = 0)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "minecraft:clay")
    }
}
