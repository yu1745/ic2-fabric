package ic2_120.tests.ore_washing_plant

import ic2_120.tests.*

import com.mcdebug.test.McDebugTest
import com.mcdebug.test.McDebugTestApi
import com.mcdebug.test.SetBlockOp
import com.mcdebug.test.Pos

/**
 * Ore Washing Plant tests — powered by adjacent CESU (HV tier 2).
 *
 * Slot layout:
 *   0 = input ore (crushed)
 *   1 = water input (water cell / water bucket)
 *   2 = purified crushed ore
 *   3 = stone dust
 *   4 = small metal dust
 *   5 = empty cell / empty bucket
 *   6 = discharging
 *   7..10 = upgrades
 *
 * PROGRESS_MAX = 500, ENERGY_PER_TICK = 16 — 25 s base recipe.
 * waterNeed = 1 bucket (81 000 droplets) per cycle.
 *
 * Why CESU + 1 transformer upgrade + 2 overclocker:
 *   - 2 overclocker upgrades → energyMultiplier = 1.6^2 = 2.56×, so the
 *     machine wants 16 * 2.56 = 41 EU/t. A BatBox (tier 1, 32 EU/t) clamps
 *     that down and the 25 s recipe never finishes in the 15 s timeout.
 *   - 1 transformer upgrade bumps `voltageTierBonus` to 1, raising the
 *     machine's max-insert from tier 1 (32 EU/t) to tier 2 (128 EU/t).
 *   - Power source: CESU (tier 2, 128 EU/t) matches the new tier.
 *   - 2 overclocker + 1 transformer: speed = (1/0.7)^2 ≈ 2.04×, recipe
 *     25 s → 12.3 s — fits inside 15 s.
 *
 * Layout:
 *   testPos(1,0,0) CESU [40 000 EU]  ←  adjacent  ←  TEST_POS Ore Washer
 */
private val CESU_POS: Pos get() = testPos(1, 0, 0)

/** 1 bucket = 81 000 droplets (Fabric Transfer API). */
private const val WATER_BUCKET_DROPLETS = 81_000L

private fun setupOreWasher() {
    McDebugTestApi.setBlocks(listOf(
        SetBlockOp(CESU_POS, "ic2_120:cesu", stateProps = mapOf("facing" to "west"))
    ))
    McDebugTestApi.setBeField(CESU_POS, "EnergyStored", 40000)
    McDebugTestApi.place(TEST_POS, "ic2_120:ore_washing_plant")
    // slot 7 = transformer upgrade → +1 voltage tier (tier 1 → 2 = 128 EU/t)
    McDebugTestApi.insertItem(TEST_POS, "ic2_120:transformer_upgrade", count = 1, slot = 7)
    // slot 8-9 = 2 overclocker upgrades → ~2.04× speed
    McDebugTestApi.insertItem(TEST_POS, "ic2_120:overclocker_upgrade", count = 2, slot = 8)
    // Prime the water tank directly via FluidStorage. The cell-in-slot path
    // requires an item-stack-to-fluid conversion we don't need to test.
    val result = McDebugTestApi.fluidInsert(TEST_POS, "minecraft:water", WATER_BUCKET_DROPLETS)
    if (result.transferred < WATER_BUCKET_DROPLETS) {
        throw AssertionError(
            "Failed to prime water tank: transferred ${result.transferred} / $WATER_BUCKET_DROPLETS"
        )
    }
}

// --- placement + canonical recipe ---

object OreWasherPlaceTest : McDebugTest {
    override val name = "ore_washing_plant:place"
    override fun run() {
        McDebugTestApi.place(TEST_POS, "ic2_120:ore_washing_plant")
        McDebugTestApi.assertBlockId(TEST_POS, "ic2_120:ore_washing_plant")
    }
}

/**
 * 1× crushed_copper → purified_copper + small_copper_dust (×2) + stone_dust.
 * The OreWasher writes outputs in the order the recipe declares them,
 * not the on-disk slot indices — so the multi-output distribution is
 * (slot 2 = purified, slot 3 = small dust, slot 4 = stone dust).
 */
object OreWasherWashTest : McDebugTest {
    override val name = "ore_washing_plant:crushed_copper → purified + dusts"
    override fun run() {
        setupOreWasher()
        McDebugTestApi.insertItem(TEST_POS, "ic2_120:crushed_copper", count = 1, slot = 0)
        McDebugTestApi.waitUntil(
            invItemEquals(TEST_POS, slot = 2, itemId = "ic2_120:purified_copper"),
            timeoutTicks = 15 * 20,
        )
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 2, "ic2_120:purified_copper")
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 3, "ic2_120:small_copper_dust")
        McDebugTestApi.assertSlotCount(TEST_POS, slot = 3, expectedCount = 2)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 4, "ic2_120:stone_dust")
    }
}

// --- edge cases ---

object OreWasherNoPowerTest : McDebugTest {
    override val name = "ore_washing_plant:no_power:idle"
    override fun run() {
        McDebugTestApi.place(TEST_POS, "ic2_120:ore_washing_plant")
        McDebugTestApi.fluidInsert(TEST_POS, "minecraft:water", WATER_BUCKET_DROPLETS)
        McDebugTestApi.insertItem(TEST_POS, "ic2_120:crushed_copper", count = 1, slot = 0)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "ic2_120:crushed_copper")
        McDebugTestApi.assertSlotEmpty(TEST_POS, slot = 2)
    }
}

/**
 * No water in tank: progress should not advance; input stays put.
 * (Even with power, the tick short-circuits when waterTankInternal.consumeInternal < waterNeed.)
 *
 * Note: this test uses the *full* setup including water, then immediately
 * drains the water tank back to zero. Draining the tank after the initial
 * priming is more robust than the alternate approach of never priming it,
 * because the OreWasher's tick() has multiple water-related short-circuits
 * and a missing-tank state interacts poorly with assertion timing in the
 * mcdebug harness.
 */
object OreWasherNoWaterTest : McDebugTest {
    override val name = "ore_washing_plant:no_water:idle"
    override fun run() {
        setupOreWasher()
        // Drain the water tank so the recipe cannot progress.
        val tank = McDebugTestApi.fluidGet(TEST_POS, index = 0)
        if (tank.amount > 0) {
            McDebugTestApi.fluidExtract(TEST_POS, tank.amount, index = 0)
        }
        McDebugTestApi.insertItem(TEST_POS, "ic2_120:crushed_copper", count = 1, slot = 0)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "ic2_120:crushed_copper")
        McDebugTestApi.assertSlotEmpty(TEST_POS, slot = 2)
    }
}

object OreWasherInvalidInputTest : McDebugTest {
    override val name = "ore_washing_plant:invalid_input:dirt"
    override fun run() {
        setupOreWasher()
        McDebugTestApi.insertItem(TEST_POS, "minecraft:dirt", count = 1, slot = 0)
        McDebugTestApi.waitUntil("tick > 200", timeoutTicks = 220)
        McDebugTestApi.assertSlotHas(TEST_POS, slot = 0, "minecraft:dirt")
        McDebugTestApi.assertSlotEmpty(TEST_POS, slot = 2)
    }
}
