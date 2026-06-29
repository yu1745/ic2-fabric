// 采矿机 (Miner) 回归测试。
//
// 状态机重写（refactor/miner-state-machine）后的回归防护：
//   1. miner:place              — 放置不崩
//   2. miner:enters scanning    — 通电+装备齐全后 IDLE→SCANNING（Running=1）
//   3. miner:mines ore below    — 真实挖掘：扫描起点放铁矿石，验证被挖出
//
// 槽位布局（与 BaseMinerBlockEntity companion 一致）：
//   slot 0  = 扫描器 (SLOT_SCANNER)
//   slot 1  = 钻头   (SLOT_DRILL)
//   slot 2  = 放电槽 (SLOT_DISCHARGING)
//   slot 3..17 = 物品槽 (SLOT_ITEM_START..END) ← 矿石掉落物入这里
//   slot 18..21 = 升级槽 (SLOT_UPGRADE_0..3)
//   slot 22..23 = 输出槽
//   slot 24 = 采矿管 (SLOT_PIPE)
//
// 能量：基础配置（BatBox tier1, 32 EU/t, 4 万 EU）足以扫描+挖一格铁矿石
// （scanCost=64，breakCost=500）。不用超频——曾试过 3 超频+MFE，但 miner
// 接受速率被 tier 限制，能量平衡不稳；基础配置慢（period 20t）但稳。
//
// 注意：SyncedData.int 字段在 NBT 里拆成 *_High/*_Low 两个 16 位 key，
// Running 值为 0/1，全部落在 Running_Low（见 SyncedData.int 实现）。
//
// ⚠️ 已知并发隔离限制（不改 mcdebug，靠测试内规避）：
// mcdebug 默认清理区域垂直仅 y:-1..+6（8格），但 OD 扫描器（radius=6）会
// 从 pos.y-1 向下扫描到 NORMAL_MIN_Y=0（深 ~64 格）。这意味着：
//   - miner 会扫到清理区外的「真实世界矿石」并挖掉（不可逆，破坏世界）；
//   - 在 y=0..62 铺的管道 + 挖的坑洞，cleanup 清不掉，残留污染。
// 单独跑 miner 测试时通常没事（测试矿石在扫描起点，螺旋优先挖到）。
// 但并发跑全量 mcdebug 时，若真实矿石恰好在更早的螺旋位置，或残留管道
// 干扰寻路，结果可能不稳定。彻底解决需要 mcdebug 支持「测试影响盒」或
// 串行跑 miner —— 当前不引入这些机制，接受该已知限制。
// 挖掘测试通过把矿石放在扫描起点（cursorIndex=1）+ assertBlockNotId 双重
// 验证，最大化稳定性。
import {
  assertBlockId,
  assertBlockNotId,
  assertSlotHas,
  beFieldEquals,
  beFieldGreaterThan,
  defineTest,
  defineTests,
  getBeNumber,
  insertItem,
  invItem,
  place,
  setBlocks,
  setBeField,
  setSlot,
  waitUntil,
} from "@yu1745/mcdebug";
import { setupAdjacentBatbox, type TestContext } from "./helpers.js";

/** 物品槽起始（矿石掉落物入这里，SLOT_ITEM_START）。 */
const SLOT_ITEM_START = 3;
/** 扫描器槽（SLOT_SCANNER）。 */
const SLOT_SCANNER = 0;
/** 采矿管槽（末端）。 */
const SLOT_PIPE = 24;

/** OD 扫描器 scanRadius=6，螺旋首格 (0,0) 即正下方，故正下方放矿可被秒扫到。 */

/**
 * 标准搭建：相邻 BatBox 供电 + OD 扫描器 + 铁钻头 + 采矿管。
 * 基础配置（无超频）：tier 1，scanCost=64，batbox 32 EU/t × period 20t = 640 EU/周期，够扫描+挖掘。
 * （超频配置见末尾注释掉的 setupFastMiner，待基础挖掘验证通过后再启用。）
 */
async function setupFastMiner(ctx: TestContext): Promise<void> {
  await setupAdjacentBatbox(ctx, "ic2_120:miner");
  await insertItem(ctx, ctx.origin, "ic2_120:scanner", 1, 0);
  await insertItem(ctx, ctx.origin, "ic2_120:drill", 1, 1);
  await insertItem(ctx, ctx.origin, "ic2_120:mining_pipe", 32, SLOT_PIPE);
}

export const minerTests = defineTests([
  // 基础放置：矿工方块存在。
  defineTest("miner:place", async (ctx) => {
    await place(ctx, ctx.origin, "ic2_120:miner");
    await assertBlockId(ctx, ctx.origin, "ic2_120:miner");
  }),

  // 通电 + 装备齐全 → IDLE→SCANNING（Running_Low>0）。
  // 验证状态机主转移（IDLE→SCANNING）在运行时实际生效。
  defineTest("miner:enters scanning when powered and equipped", async (ctx) => {
    await setupFastMiner(ctx);
    await waitUntil(ctx, beFieldGreaterThan(ctx.origin, "Running_Low", 0), 8 * 20);
  }),

  // 真实挖掘：把矿石放在 OD 螺旋扫描的「最早可挖格」，最大化命中速度。
  // OD 螺旋顺序：cursorIndex 0=(0,0) 中心柱（铺管位，不挖），1=(-1,+1) 第一个可挖格。
  // 所以矿石放在 pos.y-1 层的 (-1,+1)（origin.down().west().south()）——游标初始化后第一个就扫到。
  // 验证 SCANNING 主路径完整：铺中心柱→扫描首格→挖矿→掉落入物品槽。
  defineTest("miner:mines ore below", async (ctx) => {
    await setupFastMiner(ctx);
    // 矿石放在 pos.y-1 层扫描起点 (-1,+1)，避开中心柱 (0,0)（铺管位）。
    const ore = ctx.origin.down().west().south();
    await setBlocks(ctx, [{ pos: ore, block: "minecraft:iron_ore" }]);
    // 铁钻头 → IRON_PICKAXE → 挖 iron_ore 掉 raw_iron，入 SLOT_ITEM_START(3)。
    // 双重验证：物品槽出现 raw_iron（invItem 谓词）+ 矿石方块被挖走（assertBlockNotId）。
    await waitUntil(ctx, invItem(ctx.origin, SLOT_ITEM_START, "minecraft:raw_iron"), 25 * 20);
    await assertSlotHas(ctx, ctx.origin, SLOT_ITEM_START, "minecraft:raw_iron");
    // 矿石已被挖掉（不再是 iron_ore）。
    await assertBlockNotId(ctx, ore, "minecraft:iron_ore");
  }),

  // 注：管道回收（PIPE_RECOVERING 状态）的运行时测试搭建成本高，暂未覆盖。
  // 尝试过的方案：预铺管道 + bedrock 让矿机「铺管遇基岩→verticalHitBedrock→reachedBottom→
  // startPipeRecovery」快速触发回收。但 runScanMineLoop 的 reachedBottom 只在循环 break 后
  // 才检查，且矿机能量不足时会先在当前层 livelock（pipe_path_not_ready），链条不可靠。
  // 自然到底（扫完 3 层到 y<0）需要扫 ~500 格，单测试耗时 50s+ 不实用。
  // PIPE_RECOVERING 的 B1 修复（槽满暂停 vs 队列空完成）已在代码评审 + 编译层验证，
  // 完整运行时闭环（回收→管道进管槽→队列空退出）留作手测（见 spec §9 手测矩阵 #1）。
]);

// ===========================================================================
// 高级采矿机 (AdvancedMiner) 测试
//
// 高级机与普通机的关键差异（状态机重写重点）：
//   - 红石门控：必须收到红石信号才工作（红石反转升级可反转），见 isRedstoneActiveForWork
//   - REDSTONE_WAITING 状态：管道回收完成后等红石信号「变化」才重启（原 bug 根源）
//   - 内置下界合金镐（不需钻头槽），OV 扫描器扫描耗扫描器电池（不耗机器电）
//   - baseTier=3（接受 512 EU/t）
//
// 槽位：SLOT_SCANNER=0（OV 扫描器，需充电）, SLOT_PIPE=24（采矿管）。无钻头槽需求。
// 扫描器充电：通过 setSlot 带 nbt {Energy: N}（IElectricTool.ENERGY_KEY="Energy"）。
//
// ⚠️ REDSTONE_WAITING 状态的入口（startPipeRecovery 回收完）需要铺管+回收或 GUI 按钮
// 触发，搭建成本高且受并发隔离限制（见顶部注释）。当前测试覆盖红石门控双向
// （tickIdle + checkRedstoneStillActive），这是高级机独有逻辑里最易出问题的部分。
// REDSTONE_WAITING 的完整闭环（回收→等待→红石变化→重启）留作手测。

/** 高级机供电箱位置（东侧一格，放 MFE tier 3，西面朝矿机）。 */
const ADV_POWER_EAST = (ctx: TestContext) => ctx.origin.east();
/** 高级机红石块位置（北侧一格，红石块向四周供电）。 */
const ADV_REDSTONE_NORTH = (ctx: TestContext) => ctx.origin.north();

/**
 * 高级机标准搭建：东侧 MFE（tier 3，西朝矿机）预充 + OV 扫描器（满电）+ 采矿管。
 * 不放红石块 —— 由各测试自行控制红石以验证门控。
 * 高级机 baseTier=3，MFE tier 3 输出匹配（512 EU/t）。
 */
async function setupAdvancedMiner(ctx: TestContext): Promise<void> {
  // 供电：MFE tier 3，西面朝矿机
  const mfe = ADV_POWER_EAST(ctx);
  await setBlocks(ctx, [{ pos: mfe, block: "ic2_120:mfe", props: { facing: "west" } }]);
  await setBeField(ctx, mfe, "EnergyStored", 2_000_000);
  // 矿机
  await place(ctx, ctx.origin, "ic2_120:advanced_miner");
  // OV 扫描器（slot 0），带满电 nbt（OV 容量 1_000_000）
  await setSlot(ctx, ctx.origin, SLOT_SCANNER, "ic2_120:advanced_scanner", 1, { Energy: 1_000_000 });
  // 采矿管
  await insertItem(ctx, ctx.origin, "ic2_120:mining_pipe", 32, SLOT_PIPE);
}

export const advancedMinerTests = defineTests([
  // 基础放置：高级机方块存在。
  defineTest("advanced_miner:place", async (ctx) => {
    await place(ctx, ctx.origin, "ic2_120:advanced_miner");
    await assertBlockId(ctx, ctx.origin, "ic2_120:advanced_miner");
  }),

  // 红石门控（双向）：这是高级机独有逻辑，原 bug 根源区域。
  // 给红石 → 进 SCANNING（Running=1）；断红石 → 回 IDLE（Running=0）。
  // 覆盖 tickIdle 的 isRedstoneActiveForWork + checkRedstoneStillActive。
  defineTest("advanced_miner:redstone gates scanning on and off", async (ctx) => {
    await setupAdvancedMiner(ctx);
    const redstoneBlock = ADV_REDSTONE_NORTH(ctx);

    // 1. 给红石块 → 高级机应进 SCANNING（Running_Low 0→1）
    await setBlocks(ctx, [{ pos: redstoneBlock, block: "minecraft:redstone_block" }]);
    await waitUntil(ctx, beFieldGreaterThan(ctx.origin, "Running_Low", 0), 8 * 20);
    let running = await getBeNumber(ctx, ctx.origin, "Running_Low");
    if (running !== 1) throw new Error(`expected Running=1 with redstone, got ${running}`);

    // 2. 移走红石块（设为空气）→ 高级机应回 IDLE（Running_Low 1→0）
    await setBlocks(ctx, [{ pos: redstoneBlock, block: "minecraft:air" }]);
    await waitUntil(ctx, beFieldEquals(ctx.origin, "Running_Low", 0), 8 * 20);
    running = await getBeNumber(ctx, ctx.origin, "Running_Low");
    if (running !== 0) throw new Error(`expected Running=0 without redstone, got ${running}`);
  }),
]);
