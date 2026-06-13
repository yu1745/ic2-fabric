package ic2_120.tests.induction_furnace

import ic2_120.tests.*

import com.mcdebug.test.McDebugTest
import com.mcdebug.test.McDebugTestApi
import com.mcdebug.test.SetBlockOp
import com.mcdebug.test.Pos

/**
 * Induction Furnace tests — powered by adjacent BatBox, heated via redstone torch.
 *
 * Heat mechanics: with redstone power + energy, heat rises by 5/tick (HEAT_CHANGE_PER_TICK)
 * at a cost of MAX_HEAT_ENERGY_PER_TICK=1 EU/tick. HEAT_MAX=10000 → 2000 ticks to fully
 * pre-heat from cold. Most tests bypass the 2000-tick heat-up by setting
 * Heat_Low/Heat_High directly (the SyncedData "Heat" field is stored as two 16-bit halves
 * in NBT). One test exercises the natural heat-up path to verify the mechanism works.
 *
 * Slot layout: 0,1 = input; 2,3 = output; 4 = discharging; 5,6 = upgrades.
 * Smelts vanilla furnace recipes (1×→1×: ore→ingot, raw_food→cooked, etc.).
 * BASE_TICKS_PER_OPERATION=10, EU_PER_OPERATION=150 → at max heat, ~10 ticks per smelt.
 *
 * Redstone torch placement: must be on a SIDE of the furnace (not on top), because
 * RedstoneTorchBlock.getWeakRedstonePower returns 0 for direction=UP. Torch placed at
 * (-1, 0, 0) (west of furnace); the cleaned area extends 1 block on the lower side of
 * origin so this position is included in the cleared region between tests.
 *
 * Layout:
 *   testPos(-1, 0, 0) Redstone torch  →  powers furnace from west
 *   testPos( 1, 0, 0) BatBox [40 000 EU]  ←  adjacent east, powers furnace
 */
private val BATBOX_POS: Pos get() = testPos(1, 0, 0)
private val TORCH_POS: Pos get() = testPos(-1, 0, 0)

/** Set the SyncedData "Heat" field (stored as two 16-bit halves in NBT). */
private fun setHeat(value: Int) {
    McDebugTestApi.setBeField(TEST_POS, "Heat_Low", value and 0xFFFF)
    McDebugTestApi.setBeField(TEST_POS, "Heat_High", (value ushr 16) and 0xFFFF)
}

/** Place furnace + power source + redstone torch. Charge furnace directly. */
private fun setupInductionFurnace() {
    McDebugTestApi.setBlocks(listOf(
        SetBlockOp(BATBOX_POS, "ic2_120:batbox", stateProps = mapOf("facing" to "west")),
        SetBlockOp(TORCH_POS, "minecraft:redstone_torch"),
    ))
    McDebugTestApi.setBeField(BATBOX_POS, "EnergyStored", 40000)
    McDebugTestApi.place(TEST_POS, "ic2_120:induction_furnace")
    // Charge the furnace itself so heat-up can drain energy each tick.
    McDebugTestApi.setBeField(TEST_POS, "EnergyStored", 40000)
}

/** Place furnace with power+heat pre-set, ready to smelt. */
private fun setupInductionFurnaceHot() {
    setupInductionFurnace()
    // Skip the 2000-tick heat-up phase by setting Heat=HEAT_MAX directly.
    setHeat(10000)
}

private fun beFieldEquals(pos: Pos, path: String, value: Long): String =
    "be[${pos.x},${pos.y},${pos.z}].$path == $value"

// --- placement + canonical recipe ---

object InductionFurnacePlaceTest : McDebugTest {
    override val name = "induction_furnace:place"
    override fun run() {
        McDebugTestApi.place(TEST_POS, "ic2_120:induction_furnace")
        McDebugTestApi.assertBlockId(TEST_POS, "ic2_120:induction_furnace")
    }
}

/** With heat=10000 and power, 1× iron_ore → 1× iron_ingot. */
object InductionFurnaceSmeltTest : McDebugTest {
    override val name = "induction_furnace:smelt [adjacent BatBox + heat=10000 + redstone]"
    override fun run() {
        setupInductionFurnaceHot()
        McDebugTestApi.insertItem(TEST_POS, "minecraft:iron_ore", count = 1, slot = 0)
        McDebugTestApi.waitUntil(
            invItemEquals(TEST_POS, slot = 2, itemId = "minecraft:iron_ingot"),
            timeoutTicks = 15 * 20,
        )
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 2, "minecraft:iron_ingot")
    }
}

/** Two input slots work in parallel: 1× iron_ore + 1× iron_ore → 2× iron_ingot. */
object InductionFurnaceDualSlotTest : McDebugTest {
    override val name = "induction_furnace:dual_slot:2× ore → 2× ingot"
    override fun run() {
        setupInductionFurnaceHot()
        McDebugTestApi.insertItem(TEST_POS, "minecraft:iron_ore", count = 1, slot = 0)
        McDebugTestApi.insertItem(TEST_POS, "minecraft:iron_ore", count = 1, slot = 1)
        McDebugTestApi.waitUntil(
            invItemEquals(TEST_POS, slot = 3, itemId = "minecraft:iron_ingot"),
            timeoutTicks = 15 * 20,
        )
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 2, "minecraft:iron_ingot")
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 3, "minecraft:iron_ingot")
    }
}

// --- edge cases ---

/** No redstone signal → heat doesn't rise (and decays if any), no smelt. */
object InductionFurnaceNoRedstoneTest : McDebugTest {
    override val name = "induction_furnace:no_redstone:no_heat_no_smelt"
    override fun run() {
        // Power on, but no torch on top.
        McDebugTestApi.setBlocks(listOf(
            SetBlockOp(BATBOX_POS, "ic2_120:batbox", stateProps = mapOf("facing" to "west")),
        ))
        McDebugTestApi.setBeField(BATBOX_POS, "EnergyStored", 40000)
        McDebugTestApi.place(TEST_POS, "ic2_120:induction_furnace")
        McDebugTestApi.setBeField(TEST_POS, "EnergyStored", 40000)
        // Start with a small amount of heat; without redstone it will decay.
        setHeat(100)
        McDebugTestApi.insertItem(TEST_POS, "minecraft:iron_ore", count = 1, slot = 0)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "minecraft:iron_ore")
        McDebugTestApi.assertSlotEmpty(TEST_POS, slot = 2)
        val heat = McDebugTestApi.getBeField(TEST_POS, "Heat_Low").asLong +
            (McDebugTestApi.getBeField(TEST_POS, "Heat_High").asLong shl 16)
        check(heat < 100) { "expected heat < 100 (decayed), got $heat" }
    }
}

/** Heat=0 (no redstone → no heat-up) → no smelt, input stays. */
object InductionFurnaceNoHeatTest : McDebugTest {
    override val name = "induction_furnace:no_heat:no_smelt"
    override fun run() {
        // No torch anywhere — heat stays at 0, can't smelt.
        McDebugTestApi.setBlocks(listOf(
            SetBlockOp(BATBOX_POS, "ic2_120:batbox", stateProps = mapOf("facing" to "west")),
        ))
        McDebugTestApi.setBeField(BATBOX_POS, "EnergyStored", 40000)
        McDebugTestApi.place(TEST_POS, "ic2_120:induction_furnace")
        McDebugTestApi.setBeField(TEST_POS, "EnergyStored", 40000)
        McDebugTestApi.insertItem(TEST_POS, "minecraft:iron_ore", count = 1, slot = 0)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "minecraft:iron_ore")
        McDebugTestApi.assertSlotEmpty(TEST_POS, slot = 2)
    }
}

/** Heat rises over time when redstone + energy are present. */
object InductionFurnaceHeatUpTest : McDebugTest {
    override val name = "induction_furnace:heat_up:redstone+energy → heat rises"
    override fun run() {
        setupInductionFurnace()
        // Wait 80 ticks (5 heat/tick * 80 = 400 expected).
        McDebugTestApi.waitUntil("tick > 80", timeoutTicks = 100)
        val heatLow = McDebugTestApi.getBeField(TEST_POS, "Heat_Low").asLong
        val heatHigh = McDebugTestApi.getBeField(TEST_POS, "Heat_High").asLong
        val energy = McDebugTestApi.getBeField(TEST_POS, "EnergyStored").asLong
        val heat = (heatHigh shl 16) or heatLow
        // Inspect the torch block at TORCH_POS — is it lit?
        val torchState = McDebugTestApi.getBlock(TORCH_POS)
        val torchBlock = torchState.get("name").asString
        val torchProps = torchState.get("props")
        check(heat > 100) {
            "expected heat > 100 after 80 ticks, got $heat (low=$heatLow high=$heatHigh energy=$energy torch=$torchBlock props=$torchProps)"
        }
    }
}

/** Dirt is not a smelting recipe → input stays, no output. */
object InductionFurnaceInvalidInputTest : McDebugTest {
    override val name = "induction_furnace:invalid_input:dirt"
    override fun run() {
        setupInductionFurnaceHot()
        McDebugTestApi.insertItem(TEST_POS, "minecraft:dirt", count = 1, slot = 0)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "minecraft:dirt")
        McDebugTestApi.assertSlotEmpty(TEST_POS, slot = 2)
    }
}
