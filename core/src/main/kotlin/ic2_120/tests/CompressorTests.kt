package ic2_120.tests.compressor

import ic2_120.tests.*

import com.mcdebug.test.McDebugTest
import com.mcdebug.test.McDebugTestApi
import com.mcdebug.test.SetBlockOp
import com.mcdebug.test.Pos

/**
 * Compressor tests — powered by adjacent BatBox.
 * Slot layout: 0 = input, 1 = output, 2 = discharging, 3..6 = upgrades.
 * All 55 recipes are N×→1× (consolidation: ingots→block, dust→gem, etc.).
 *
 * Layout:
 *   testPos(1,0,0) BatBox [40 000 EU]  ←  adjacent  ←  TEST_POS Compressor
 */
private val BATBOX_POS: Pos get() = testPos(1, 0, 0)

/** Place BatBox (40k EU) directly adjacent (east) to the compressor. */
private fun setupCompressor() {
    McDebugTestApi.setBlocks(listOf(
        SetBlockOp(BATBOX_POS, "ic2_120:batbox", stateProps = mapOf("facing" to "west"))
    ))
    McDebugTestApi.setBeField(BATBOX_POS, "EnergyStored", 40000)
    McDebugTestApi.place(TEST_POS, "ic2_120:compressor")
    // 2 overclocker upgrades → speed (1/0.7)^2 ≈ 2.04×, halves recipe time
    // so the slow 4×-input recipes complete well within the 15 s timeout
    // when the test runner is under parallel load.
    McDebugTestApi.insertItem(TEST_POS, "ic2_120:overclocker_upgrade", count = 2, slot = 3)
}

// --- placement + canonical recipe (1 of 55) ---

object CompressorPlaceTest : McDebugTest {
    override val name = "compressor:place"
    override fun run() {
        McDebugTestApi.place(TEST_POS, "ic2_120:compressor")
        McDebugTestApi.assertBlockId(TEST_POS, "ic2_120:compressor")
    }
}

/** 4× clay_ball → 1× clay: simplest non-tag N×→1× recipe. */
object CompressorCompressTest : McDebugTest {
    override val name = "compressor:4× clay_ball → clay [adjacent BatBox]"
    override fun run() {
        setupCompressor()
        McDebugTestApi.insertItem(TEST_POS, "minecraft:clay_ball", count = 4, slot = 0)
        McDebugTestApi.waitUntil(
            invItemEquals(TEST_POS, slot = 1, itemId = "minecraft:clay"),
            timeoutTicks = 15 * 20,
        )
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 1, "minecraft:clay")
    }
}

/** 9× iron_ingot → 1× iron_block: tag-ingredient N×→1× (covers 10 tag recipes). */
object CompressorTagCompressTest : McDebugTest {
    override val name = "compressor:9× iron_ingot → iron_block [tag ingredient]"
    override fun run() {
        setupCompressor()
        McDebugTestApi.insertItem(TEST_POS, "minecraft:iron_ingot", count = 9, slot = 0)
        McDebugTestApi.waitUntil(
            invItemEquals(TEST_POS, slot = 1, itemId = "minecraft:iron_block"),
            timeoutTicks = 15 * 20,
        )
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 1, "minecraft:iron_block")
    }
}

// --- edge cases ---

object CompressorNoPowerTest : McDebugTest {
    override val name = "compressor:no_power:idle"
    override fun run() {
        // Place compressor only — no BatBox.
        McDebugTestApi.place(TEST_POS, "ic2_120:compressor")
        McDebugTestApi.insertItem(TEST_POS, "minecraft:clay_ball", count = 4, slot = 0)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "minecraft:clay_ball")
        McDebugTestApi.assertSlotEmpty(TEST_POS, slot = 1)
    }
}

object CompressorInvalidInputTest : McDebugTest {
    override val name = "compressor:invalid_input:dirt"
    override fun run() {
        setupCompressor()
        McDebugTestApi.insertItem(TEST_POS, "minecraft:dirt", count = 1, slot = 0)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "minecraft:dirt")
        McDebugTestApi.assertSlotEmpty(TEST_POS, slot = 1)
    }
}

object CompressorOutputFullTest : McDebugTest {
    override val name = "compressor:output_full:blocks_next"
    override fun run() {
        setupCompressor()
        // Pre-fill output to 64 clay — next compress has nowhere to go.
        McDebugTestApi.setSlot(TEST_POS, slot = 1, itemId = "minecraft:clay", count = 64)
        McDebugTestApi.insertItem(TEST_POS, "minecraft:clay_ball", count = 4, slot = 0)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "minecraft:clay_ball")
    }
}
