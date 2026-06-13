package ic2_120.tests.block_cutter

import ic2_120.tests.*

import com.mcdebug.test.McDebugTest
import com.mcdebug.test.McDebugTestApi
import com.mcdebug.test.SetBlockOp
import com.mcdebug.test.Pos

/**
 * Block Cutter tests — powered by adjacent BatBox.
 *
 * Slot layout:
 *   0 = blade (mandatory, must be a cutting blade)
 *   1 = input
 *   2 = discharging (battery)
 *   3 = output
 *   4..7 = upgrades
 *
 * PROGRESS_MAX = 450, ENERGY_PER_TICK = 4 — 22.5 s base.
 * 2 overclocker upgrades keep slow recipes inside the 15 s timeout.
 *
 * Layout:
 *   testPos(1,0,0) BatBox [40 000 EU]  ←  adjacent  ←  TEST_POS Block Cutter
 */
private val BATBOX_POS: Pos get() = testPos(1, 0, 0)

/** Place BatBox + cutter + iron blade + 2 overclocker upgrades. */
private fun setupBlockCutter() {
    McDebugTestApi.setBlocks(listOf(
        SetBlockOp(BATBOX_POS, "ic2_120:batbox", stateProps = mapOf("facing" to "west"))
    ))
    McDebugTestApi.setBeField(BATBOX_POS, "EnergyStored", 40000)
    McDebugTestApi.place(TEST_POS, "ic2_120:block_cutter")
    // Blade is mandatory — without it, the cutter refuses to consume input.
    McDebugTestApi.insertItem(TEST_POS, "ic2_120:iron_block_cutting_blade", count = 1, slot = 0)
    // 2 overclocker upgrades → (1/0.7)^2 ≈ 2.04×, halves the 22.5 s recipe time
    // so it completes well within the 15 s timeout under parallel load.
    McDebugTestApi.insertItem(TEST_POS, "ic2_120:overclocker_upgrade", count = 2, slot = 4)
}

// --- placement + canonical recipe ---

object BlockCutterPlaceTest : McDebugTest {
    override val name = "block_cutter:place"
    override fun run() {
        McDebugTestApi.place(TEST_POS, "ic2_120:block_cutter")
        McDebugTestApi.assertBlockId(TEST_POS, "ic2_120:block_cutter")
    }
}

/**
 * 1× stone → 9× stone_slab: simplest 1×→N× recipe. Iron blade hardness
 * (5.0) easily meets stone's material hardness (1.5).
 */
object BlockCutterStoneTest : McDebugTest {
    override val name = "block_cutter:stone → 9× stone_slab [adjacent BatBox]"
    override fun run() {
        setupBlockCutter()
        McDebugTestApi.insertItem(TEST_POS, "minecraft:stone", count = 1, slot = 1)
        McDebugTestApi.waitUntil(
            invItemEquals(TEST_POS, slot = 3, itemId = "minecraft:stone_slab"),
            timeoutTicks = 15 * 20,
        )
        McDebugTestApi.assertSlotCount(TEST_POS, slot = 3, expectedCount = 9)
    }
}

// --- edge cases ---

object BlockCutterNoPowerTest : McDebugTest {
    override val name = "block_cutter:no_power:idle"
    override fun run() {
        McDebugTestApi.place(TEST_POS, "ic2_120:block_cutter")
        McDebugTestApi.insertItem(TEST_POS, "ic2_120:iron_block_cutting_blade", count = 1, slot = 0)
        McDebugTestApi.insertItem(TEST_POS, "minecraft:stone", count = 1, slot = 1)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 1, "minecraft:stone")
        McDebugTestApi.assertSlotEmpty(TEST_POS, slot = 3)
    }
}

/** Missing blade must leave input untouched. */
object BlockCutterNoBladeTest : McDebugTest {
    override val name = "block_cutter:no_blade:idle"
    override fun run() {
        McDebugTestApi.setBlocks(listOf(
            SetBlockOp(BATBOX_POS, "ic2_120:batbox", stateProps = mapOf("facing" to "west"))
        ))
        McDebugTestApi.setBeField(BATBOX_POS, "EnergyStored", 40000)
        McDebugTestApi.place(TEST_POS, "ic2_120:block_cutter")
        // Deliberately leave slot 0 (blade) empty.
        McDebugTestApi.insertItem(TEST_POS, "minecraft:stone", count = 1, slot = 1)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 1, "minecraft:stone")
        McDebugTestApi.assertSlotEmpty(TEST_POS, slot = 3)
    }
}

object BlockCutterInvalidInputTest : McDebugTest {
    override val name = "block_cutter:invalid_input:dirt"
    override fun run() {
        setupBlockCutter()
        McDebugTestApi.insertItem(TEST_POS, "minecraft:dirt", count = 1, slot = 1)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 1, "minecraft:dirt")
        McDebugTestApi.assertSlotEmpty(TEST_POS, slot = 3)
    }
}

object BlockCutterOutputFullTest : McDebugTest {
    override val name = "block_cutter:output_full:blocks_next"
    override fun run() {
        setupBlockCutter()
        // Pre-fill output to 64 stone_slab — next cut has nowhere to go.
        McDebugTestApi.setSlot(TEST_POS, slot = 3, itemId = "minecraft:stone_slab", count = 64)
        McDebugTestApi.insertItem(TEST_POS, "minecraft:stone", count = 1, slot = 1)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 1, "minecraft:stone")
    }
}
