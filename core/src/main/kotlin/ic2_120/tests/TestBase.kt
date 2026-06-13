package ic2_120.tests

import com.mcdebug.test.McDebugTestApi
import com.mcdebug.test.Pos

/**
 * Shared test infrastructure.
 *
 * The mcdebug dispatcher assigns each test an isolated world area and
 * force-loads it before the test runs. [TEST_POS] is the per-test origin.
 */
val TEST_POS: Pos get() = McDebugTestApi.testOrigin

fun testPos(dx: Int = 0, dy: Int = 0, dz: Int = 0): Pos = McDebugTestApi.pos(dx, dy, dz)

fun invItemEquals(pos: Pos, slot: Int, itemId: String): String =
    "inv[${pos.x},${pos.y},${pos.z}].$slot.item == \"$itemId\""

fun invCountLessThan(pos: Pos, slot: Int, count: Int): String =
    "inv[${pos.x},${pos.y},${pos.z}].$slot.count < $count"

fun beFieldGreaterThan(pos: Pos, path: String, value: Long): String =
    "be[${pos.x},${pos.y},${pos.z}].$path > $value"

/**
 * Kept as a no-op for older tests. Area setup and force-loading are now
 * handled centrally by the mcdebug dispatcher.
 */
fun ensureChunkLoaded() = Unit
