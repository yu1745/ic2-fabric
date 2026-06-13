package ic2_120.tests.electric_furnace

import ic2_120.tests.*

import com.mcdebug.test.McDebugTest
import com.mcdebug.test.McDebugTestApi
import com.mcdebug.test.SetBlockOp
import com.mcdebug.test.Pos

/**
 * Electric Furnace tests — powered by adjacent BatBox.
 * Slot layout: 0 = input, 1 = output, 2 = discharging, 3..6 = upgrades.
 * Smelts vanilla furnace recipes (1×→1×: ore→ingot, raw_food→cooked, etc.).
 *
 * Layout:
 *   testPos(1,0,0) BatBox [40 000 EU]  ←  adjacent  ←  TEST_POS Electric Furnace
 */
private val BATBOX_POS: Pos get() = testPos(1, 0, 0)

/** Place BatBox (40k EU) directly adjacent (east) to the furnace. */
private fun setupElectricFurnace() {
    McDebugTestApi.setBlocks(listOf(
        SetBlockOp(BATBOX_POS, "ic2_120:batbox", stateProps = mapOf("facing" to "west"))
    ))
    McDebugTestApi.setBeField(BATBOX_POS, "EnergyStored", 40000)
    McDebugTestApi.place(TEST_POS, "ic2_120:electric_furnace")
}

// --- placement + canonical recipe ---

object ElectricFurnacePlaceTest : McDebugTest {
    override val name = "electric_furnace:place"
    override fun run() {
        McDebugTestApi.place(TEST_POS, "ic2_120:electric_furnace")
        McDebugTestApi.assertBlockId(TEST_POS, "ic2_120:electric_furnace")
    }
}

/** 1× iron_ore → 1× iron_ingot: vanilla furnace recipe. */
object ElectricFurnaceSmeltTest : McDebugTest {
    override val name = "electric_furnace:smelt [adjacent BatBox]"
    override fun run() {
        setupElectricFurnace()
        McDebugTestApi.insertItem(TEST_POS, "minecraft:iron_ore", count = 1, slot = 0)
        McDebugTestApi.waitUntil(
            invItemEquals(TEST_POS, slot = 1, itemId = "minecraft:iron_ingot"),
            timeoutTicks = 15 * 20,
        )
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 1, "minecraft:iron_ingot")
    }
}

// --- edge cases ---

object ElectricFurnaceNoPowerTest : McDebugTest {
    override val name = "electric_furnace:no_power:idle"
    override fun run() {
        McDebugTestApi.place(TEST_POS, "ic2_120:electric_furnace")
        McDebugTestApi.insertItem(TEST_POS, "minecraft:iron_ore", count = 1, slot = 0)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "minecraft:iron_ore")
        McDebugTestApi.assertSlotEmpty(TEST_POS, slot = 1)
    }
}

object ElectricFurnaceInvalidInputTest : McDebugTest {
    override val name = "electric_furnace:invalid_input:dirt"
    override fun run() {
        setupElectricFurnace()
        McDebugTestApi.insertItem(TEST_POS, "minecraft:dirt", count = 1, slot = 0)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "minecraft:dirt")
        McDebugTestApi.assertSlotEmpty(TEST_POS, slot = 1)
    }
}

object ElectricFurnaceOutputFullTest : McDebugTest {
    override val name = "electric_furnace:output_full:blocks_next"
    override fun run() {
        setupElectricFurnace()
        // Pre-fill output to 64 iron_ingot — next smelt has nowhere to go.
        McDebugTestApi.setSlot(TEST_POS, slot = 1, itemId = "minecraft:iron_ingot", count = 64)
        McDebugTestApi.insertItem(TEST_POS, "minecraft:iron_ore", count = 1, slot = 0)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "minecraft:iron_ore")
    }
}
