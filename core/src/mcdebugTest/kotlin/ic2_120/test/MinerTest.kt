package ic2_120.test

import com.mcdebug.runner.McDebugTest
import com.mcdebug.runner.TestContext
import com.mcdebug.runner.assertBlockId
import com.mcdebug.runner.assertBlockNotId
import com.mcdebug.runner.assertSlotHas
import com.mcdebug.runner.beFieldEquals
import com.mcdebug.runner.beFieldGreaterThan
import com.mcdebug.runner.insertItem
import com.mcdebug.runner.invItem
import com.mcdebug.runner.place
import com.mcdebug.runner.setBeField
import com.mcdebug.runner.setBlocks
import com.mcdebug.runner.setSlot
import com.mcdebug.runner.waitUntil
import org.junit.jupiter.api.Test

/**
 * 采矿机 (Miner) 测试 —— 按状态机重构后的逻辑编写。
 *
 * 槽位布局（与 MinerBlockEntity companion 一致）：
 *   slot 0  = 扫描器 (SLOT_SCANNER)
 *   slot 1  = 钻头   (SLOT_DRILL)
 *   slot 2  = 放电槽 (SLOT_DISCHARGING)
 *   slot 3..17 = 物品槽 (SLOT_ITEM_START..END) ← 矿石掉落物入这里
 *   slot 18..21 = 升级槽
 *   slot 22..23 = 输出槽
 *   slot 24 = 采矿管 (SLOT_PIPE)
 *
 * 重构后的关键行为（与旧测试假设的差异）：
 *   - **钻头必须预充电**：drillCharger 在机器有电时优先抽电充钻头
 *     （canChargeNow = sync.amount > 0），裸钻头会让机器永远攒不到
 *     铺管所需的 3 EU → livelock（EnergyStored=0, Cursor 不动）。
 *   - **挖掘是逐方块 200 tick**（铁钻头 mode 0：euPerTick=6, duration=200）：
 *     路径上每块非空方块都要先挖掉，超时预算要按 200t/块 计算。
 *   - 中心柱（pos.y-1 正下方）先铺管，然后按层扫描找矿。
 *   - 铁钻头挖铁矿石掉落 raw_iron（非 silk touch）。
 *
 * 能量：BatBox（tier1, 32 EU/t, 4 万 EU）+ 预充钻头/扫描器足以扫描+挖一格
 * 铁矿石。扫描器 OD 满电 100000，钻头满电 100000。
 */
@McDebugTest
class MinerTest {

    private val SLOT_SCANNER = 0
    private val SLOT_DRILL = 1
    private val SLOT_ITEM_START = 3
    private val SLOT_PIPE = 24

    /** 标准搭建：相邻 BatBox 供电 + 预充的 OD 扫描器 + 预充铁钻头 + 采矿管。 */
    private fun setupFastMiner(ctx: TestContext) {
        setupAdjacentBatbox(ctx, "ic2_120:miner")
        // 钻头/扫描器都预充：避免 charger 组件抽走机器电导致 livelock。
        setSlot(ctx, ctx.origin, SLOT_SCANNER, "ic2_120:scanner", 1, mapOf("Energy" to 100_000))
        setSlot(ctx, ctx.origin, SLOT_DRILL, "ic2_120:drill", 1, mapOf("Energy" to 100_000))
        moveItemStackTo(ctx, ctx.origin, "ic2_120:mining_pipe", 32, SLOT_PIPE)
    }

    @Test
    fun `place`(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:miner")
        assertBlockId(ctx, ctx.origin, "ic2_120:miner")
    }

    /** 通电 + 装备齐全 → IDLE→SCANNING（Running_Low>0）。 */
    @Test
    fun entersScanningWhenPoweredAndEquipped(ctx: TestContext) {
        setupFastMiner(ctx)
        waitUntil(ctx, beFieldGreaterThan(ctx.origin, "Running_Low", 0), 8 * 20)
    }

    /**
     * 真实挖掘：矿石放在中心柱正西一格（(-1,0)）——Bresenham 清路路径从
     * (0,0) 直达 (-1,0)，**第一块方块就是矿石本身**，无需先挖路障，
     * 挖掘 200 tick 后掉落 raw_iron 入 SLOT_ITEM_START(3)。
     */
    @Test
    fun minesOreBelow(ctx: TestContext) {
        setupAdjacentBatbox(ctx, "ic2_120:miner")
        val ore = ctx.pos(-1, -1, 0)
        setBlocks(ctx, listOf(ore to "minecraft:iron_ore"))
        setupFastMinerEquipOnly(ctx)

        try {
            waitUntil(ctx, invItem(ctx.origin, SLOT_ITEM_START, "minecraft:raw_iron"), 30 * 20)
        } catch (e: Exception) {
            val minerNbt = ctx.api.be.getNbt(ctx.origin).asJsonObject.get("nbt")
            val oreState = ctx.api.world.getBlock(ore).asJsonObject.get("state")
            val itemSlot = ctx.api.inv.getSlot(ctx.origin, SLOT_ITEM_START)
            val pipeSlot = ctx.api.inv.getSlot(ctx.origin, SLOT_PIPE)
            throw AssertionError(
                "miner did not collect first ore: ore=${oreState}, itemSlot=$itemSlot, " +
                    "pipeSlot=$pipeSlot, nbt=$minerNbt",
                e,
            )
        }
        assertSlotHas(ctx, ctx.origin, SLOT_ITEM_START, "minecraft:raw_iron")
        assertBlockNotId(ctx, ore, "minecraft:iron_ore")
    }

    /** 只装备不搭供电（给已 setup 的矿机补装备，避免重复 setup）。 */
    private fun setupFastMinerEquipOnly(ctx: TestContext) {
        setSlot(ctx, ctx.origin, SLOT_SCANNER, "ic2_120:scanner", 1, mapOf("Energy" to 100_000))
        setSlot(ctx, ctx.origin, SLOT_DRILL, "ic2_120:drill", 1, mapOf("Energy" to 100_000))
        moveItemStackTo(ctx, ctx.origin, "ic2_120:mining_pipe", 32, SLOT_PIPE)
    }
}

/**
 * 高级采矿机 (AdvancedMiner) 测试。
 *
 * 与普通机差异：红石门控（必须收到红石信号才工作，可被红石反转升级反转）、
 * 内置下界合金镐（无钻头槽）、OV 扫描器耗扫描器电池（不耗机器电）、
 * baseTier=3（接受 512 EU/t）。
 *
 * 槽位：SLOT_SCANNER=0（OV 扫描器需充电）, SLOT_PIPE=24。
 */
@McDebugTest
class AdvancedMinerTest {

    private val SLOT_SCANNER = 0
    private val SLOT_PIPE = 24

    /** 高级机标准搭建：东侧 MFE（tier 3 西朝矿机）预充 + OV 扫描器（满电）+ 采矿管。 */
    private fun setupAdvancedMiner(ctx: TestContext) {
        val mfe = ctx.batboxEast
        setBlocks(ctx, listOf(mfe to "ic2_120:mfe"), mapOf("facing" to "west"))
        setBeField(ctx, mfe, "EnergyStored", 2_000_000)
        place(ctx, ctx.origin, "ic2_120:advanced_miner")
        setSlot(ctx, ctx.origin, SLOT_SCANNER, "ic2_120:advanced_scanner", 1, mapOf("Energy" to 1_000_000))
        moveItemStackTo(ctx, ctx.origin, "ic2_120:mining_pipe", 32, SLOT_PIPE)
    }

    @Test
    fun `place`(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:advanced_miner")
        assertBlockId(ctx, ctx.origin, "ic2_120:advanced_miner")
    }

    /** 红石门控（双向）：给红石 → SCANNING（Running=1）；断红石 → IDLE（Running=0）。 */
    @Test
    fun redstoneGatesScanningOnAndOff(ctx: TestContext) {
        setupAdvancedMiner(ctx)
        val redstoneBlock = ctx.pos(0, 0, -1)

        // 1. 给红石块 → 高级机应进 SCANNING（Running_Low 0→1）
        setBlocks(ctx, listOf(redstoneBlock to "minecraft:redstone_block"))
        waitUntil(ctx, beFieldGreaterThan(ctx.origin, "Running_Low", 0), 8 * 20)
        if (getRunning(ctx) != 1) throw AssertionError("expected Running=1 with redstone, got ${getRunning(ctx)}")

        // 2. 移走红石块 → 高级机应回 IDLE（Running_Low 1→0）
        setBlocks(ctx, listOf(redstoneBlock to "minecraft:air"))
        waitUntil(ctx, beFieldEquals(ctx.origin, "Running_Low", 0), 8 * 20)
        if (getRunning(ctx) != 0) throw AssertionError("expected Running=0 without redstone, got ${getRunning(ctx)}")
    }

    private fun getRunning(ctx: TestContext): Int =
        ctx.api.be.getField(ctx.origin, "Running_Low").asJsonObject.get("value").asInt
}
