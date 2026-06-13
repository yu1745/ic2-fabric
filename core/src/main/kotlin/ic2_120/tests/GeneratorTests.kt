package ic2_120.tests.generator

import ic2_120.tests.*

import com.mcdebug.test.McDebugTest
import com.mcdebug.test.McDebugTestApi

/**
 * Generator tests — burns fuel to produce EU.
 * Slot layout: 0 = fuel, 1 = battery.
 */
object GeneratorPlaceTest : McDebugTest {
    override val name = "generator:place"
    override fun run() {
        McDebugTestApi.place(TEST_POS, "ic2_120:generator")
        McDebugTestApi.assertBlockId(TEST_POS, "ic2_120:generator")
    }
}

object GeneratorBurnCoalTest : McDebugTest {
    override val name = "generator:burn coal → EU"
    override fun run() {
        McDebugTestApi.place(TEST_POS, "ic2_120:generator")
        McDebugTestApi.insertItem(TEST_POS, "minecraft:coal", count = 1, slot = 0)
        McDebugTestApi.waitUntil(
            beFieldGreaterThan(TEST_POS, "EnergyStored", 0),
            timeoutTicks = 5 * 20,
        )
        val energy = McDebugTestApi.getBeField(TEST_POS, "EnergyStored").asLong
        if (energy <= 0) {
            throw AssertionError("expected generator to produce energy, got $energy")
        }
        McDebugTestApi.assertSlotEmpty(TEST_POS, slot = 0)
    }
}
