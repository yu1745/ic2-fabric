package ic2_120.tests.energy_storage

import ic2_120.tests.*

import com.mcdebug.test.McDebugTest
import com.mcdebug.test.McDebugTestApi

/**
 * Energy storage tests — verify placement of the four tiered storages
 * (BatBox/CESU/MFE/MFSU). The BatBox also gets a full NBT round-trip test
 * (its tier-1 buffer is the canonical "40 000 EU" the other machines
 * expect to drain from).
 */

object BatboxPlaceTest : McDebugTest {
    override val name = "batbox:place"
    override fun run() {
        McDebugTestApi.place(TEST_POS, "ic2_120:batbox")
        McDebugTestApi.assertBlockId(TEST_POS, "ic2_120:batbox")
    }
}

/**
 * Charge the BatBox via setBeField and verify the stored energy reads back.
 * This tests the BE NBT round-trip (writeNbt → readNbt → getBeField).
 */
object BatboxEnergyStoreTest : McDebugTest {
    override val name = "batbox:store + readback energy"
    override fun run() {
        McDebugTestApi.place(TEST_POS, "ic2_120:batbox")
        McDebugTestApi.setBeField(TEST_POS, "EnergyStored", 12345)
        val energy = McDebugTestApi.getBeField(TEST_POS, "EnergyStored").asLong
        if (energy != 12345L) {
            throw AssertionError("expected batbox EnergyStored = 12345, got $energy")
        }
    }
}

object CesuPlaceTest : McDebugTest {
    override val name = "cesu:place"
    override fun run() {
        McDebugTestApi.place(TEST_POS, "ic2_120:cesu")
        McDebugTestApi.assertBlockId(TEST_POS, "ic2_120:cesu")
    }
}

object MfePlaceTest : McDebugTest {
    override val name = "mfe:place"
    override fun run() {
        McDebugTestApi.place(TEST_POS, "ic2_120:mfe")
        McDebugTestApi.assertBlockId(TEST_POS, "ic2_120:mfe")
    }
}

object MfsuPlaceTest : McDebugTest {
    override val name = "mfsu:place"
    override fun run() {
        McDebugTestApi.place(TEST_POS, "ic2_120:mfsu")
        McDebugTestApi.assertBlockId(TEST_POS, "ic2_120:mfsu")
    }
}
