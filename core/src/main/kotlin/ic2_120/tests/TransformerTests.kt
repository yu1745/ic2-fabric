package ic2_120.tests.transformer

import ic2_120.tests.*

import com.mcdebug.test.McDebugTest
import com.mcdebug.test.McDebugTestApi
import com.mcdebug.test.SetBlockOp
import com.mcdebug.test.Pos

/**
 * Transformer tests — LV ↔ MV step-up / step-down across an LV transformer.
 *
 * Topology for step-up: BatBox (LV) ──west──► LV Transformer ──east──► CESU (MV)
 *   - BatBox is Tier 1 (32 EU/t, 40 000 EU cap), `facing=east` so its output
 *     face (the front) is east — directly toward the transformer.
 *   - LV Transformer is placed with `facing=east`, so its "front" (high-side)
 *     is east. In default STEP_UP mode the other 5 faces (including west) are
 *     low-side inputs. The west input is what receives the BatBox's LV
 *     energy. The east front output is what the CESU receives.
 *   - CESU is Tier 2 (128 EU/t, 300 000 EU cap), `facing=west` so it accepts
 *     energy from its west face.
 *
 * Energy conservation: 4 ticks × 32 EU = 128 EU on the low side, 1 tick ×
 * 128 EU = 128 EU on the high side. The transformer's internal buffer
 * (512 EU) holds at most 4 high-tick worth.
 *
 * For step-down: we reverse the chain. CESU (MV) ──west──► LV Transformer
 * ──east──► BatBox (LV). The transformer mode is set to STEP_DOWN via
 * setBeField("Mode", 1).
 *
 * Layout:
 *   testPos(-1, 0, 0)  low-tier storage   (BatBox for step-up / CESU for step-down)
 *   TEST_POS            LV Transformer
 *   testPos( 1, 0, 0)  high-tier storage  (CESU for step-up / BatBox for step-down)
 */
private val WEST_STORAGE: Pos get() = testPos(-1, 0, 0)
private val EAST_STORAGE: Pos get() = testPos( 1, 0, 0)

/**
 * Step-up: charge the BatBox (LV) → wait → assert CESU (MV) has gained
 * energy. The transformer should accumulate ~32 EU/t on its low side and
 * push it out as ~128 EU/t on the high side.
 */
object TransformerStepUpTest : McDebugTest {
    override val name = "transformer:step_up LV→MV [BatBox → LV transformer → CESU]"
    override fun run() {
        // Storage facing = the OUTPUT face (energy leaves the storage on this
        // side). For the BatBox to feed the transformer on the east, the
        // BatBox's east face must be its output → facing=east.
        // For the CESU to receive energy from the transformer on the west,
        // the CESU's west face must be an INPUT face (non-facing) →
        // facing=anything other than west (use north so west is non-facing
        // and accepts input).
        McDebugTestApi.setBlocks(listOf(
            SetBlockOp(WEST_STORAGE, "ic2_120:batbox", stateProps = mapOf("facing" to "east")),
            SetBlockOp(TEST_POS,    "ic2_120:lv_transformer", stateProps = mapOf("facing" to "east")),
            SetBlockOp(EAST_STORAGE, "ic2_120:cesu", stateProps = mapOf("facing" to "north")),
        ))
        McDebugTestApi.setBeField(WEST_STORAGE, "EnergyStored", 10_000)
        McDebugTestApi.setBeField(EAST_STORAGE, "EnergyStored", 0)
        // Default transformer mode = STEP_UP (0). Verify just in case.
        McDebugTestApi.setBeField(TEST_POS, "Mode", 0)

        val batboxState = McDebugTestApi.getBlock(WEST_STORAGE)
        val transformerState = McDebugTestApi.getBlock(TEST_POS)
        val cesuState = McDebugTestApi.getBlock(EAST_STORAGE)
        val startCesuEnergy = McDebugTestApi.getBeField(EAST_STORAGE, "EnergyStored").asLong
        // LV transformer maxes at 32 EU/t in + 128 EU/t out. A 5 s window
        // (100 ticks) is enough to confirm the chain works.
        McDebugTestApi.waitUntil("tick > 100", timeoutTicks = 120)
        val endCesuEnergy = McDebugTestApi.getBeField(EAST_STORAGE, "EnergyStored").asLong
        val endBatboxEnergy = McDebugTestApi.getBeField(WEST_STORAGE, "EnergyStored").asLong
        val endTransformerEnergy = McDebugTestApi.getBeField(TEST_POS, "EnergyStored").asLong
        check(endCesuEnergy > startCesuEnergy) {
            "expected CESU to gain energy, before=$startCesuEnergy after=$endCesuEnergy\n" +
                "  batbox.state=$batboxState\n" +
                "  transformer.state=$transformerState\n" +
                "  cesu.state=$cesuState\n" +
                "  batbox.energy=$endBatboxEnergy\n" +
                "  transformer.energy=$endTransformerEnergy"
        }
        // Total energy should be conserved: what the BatBox lost, the CESU
        // gained (modulo the transformer's 512 EU internal buffer).
        val delta = endCesuEnergy - startCesuEnergy
        check(delta > 0L) {
            "expected CESU energy to grow (delta=$delta)"
        }
        check(endBatboxEnergy <= 10_000L) {
            "BatBox should have lost some energy, got $endBatboxEnergy"
        }
    }
}

/**
 * Step-down: flip the transformer into STEP_DOWN mode and reverse the
 * energy flow. The CESU is the high-side source; the BatBox is the low-side
 * sink. (The transformer itself decides the direction: high-side input →
 * low-side output.)
 */
object TransformerStepDownTest : McDebugTest {
    override val name = "transformer:step_down MV→LV [CESU → LV transformer → BatBox]"
    override fun run() {
        // Note: transformer front is east. In STEP_DOWN mode the front (east)
        // is the *high-side input*, the other 5 faces are *low-side outputs*.
        // Flow: CESU (high) ──west──► transformer (front=east receives, 5 sides
        // output low) ──west──► BatBox (low).
        //   CESU must output on its west face → facing=west (facing = output face).
        //   BatBox must accept input on its west face → facing≠west (e.g. north).
        McDebugTestApi.setBlocks(listOf(
            SetBlockOp(WEST_STORAGE, "ic2_120:batbox", stateProps = mapOf("facing" to "north")),
            SetBlockOp(TEST_POS,    "ic2_120:lv_transformer", stateProps = mapOf("facing" to "east")),
            SetBlockOp(EAST_STORAGE, "ic2_120:cesu", stateProps = mapOf("facing" to "west")),
        ))
        McDebugTestApi.setBeField(WEST_STORAGE, "EnergyStored", 0)
        McDebugTestApi.setBeField(EAST_STORAGE, "EnergyStored", 10_000)
        // Switch to STEP_DOWN (mode id 1).
        McDebugTestApi.setBeField(TEST_POS, "Mode", 1)

        val startBatboxEnergy = McDebugTestApi.getBeField(WEST_STORAGE, "EnergyStored").asLong
        McDebugTestApi.waitUntil("tick > 100", timeoutTicks = 120)
        val endBatboxEnergy = McDebugTestApi.getBeField(WEST_STORAGE, "EnergyStored").asLong
        check(endBatboxEnergy > startBatboxEnergy) {
            "expected BatBox to gain energy via step-down, before=$startBatboxEnergy after=$endBatboxEnergy"
        }
    }
}

/**
 * Place-only sanity: all four transformer tiers can be placed and produce
 * the correct block id.
 */
object TransformerPlaceTest : McDebugTest {
    override val name = "transformer:place (lv + mv + hv + ev)"
    override fun run() {
        McDebugTestApi.place(TEST_POS, "ic2_120:lv_transformer")
        McDebugTestApi.assertBlockId(TEST_POS, "ic2_120:lv_transformer")
        McDebugTestApi.place(testPos(2, 0, 0), "ic2_120:mv_transformer")
        McDebugTestApi.assertBlockId(testPos(2, 0, 0), "ic2_120:mv_transformer")
        McDebugTestApi.place(testPos(3, 0, 0), "ic2_120:hv_transformer")
        McDebugTestApi.assertBlockId(testPos(3, 0, 0), "ic2_120:hv_transformer")
        McDebugTestApi.place(testPos(4, 0, 0), "ic2_120:ev_transformer")
        McDebugTestApi.assertBlockId(testPos(4, 0, 0), "ic2_120:ev_transformer")
    }
}
