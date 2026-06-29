// 采矿机 (Miner) 最小回归测试。
//
// 状态机重写（refactor/miner-state-machine）后的回归防护：验证普通采矿机
// 通电 + 装备齐全后能从 IDLE 进入 SCANNING（Running=1）。
// 不覆盖全部状态转移（mcdebug 15s 硬约束），只防「重构后完全不工作」。
//
// 槽位布局（与 BaseMinerBlockEntity companion 一致）：
//   slot 0  = 扫描器 (SLOT_SCANNER)
//   slot 1  = 钻头   (SLOT_DRILL)
//   slot 2  = 放电槽 (SLOT_DISCHARGING)
//   slot 3..17 = 物品槽 (SLOT_ITEM_START..END)
//   slot 18..21 = 升级槽
//   slot 22..23 = 输出槽
//   slot 24 = 采矿管 (SLOT_PIPE)
//
// 注意：sync.running 是状态机末尾派生量（SCANNING=1，其余=0），见 spec §3。
import {
  assertBlockId,
  beFieldGreaterThan,
  defineTest,
  defineTests,
  insertItem,
  place,
  waitUntil,
} from "@yu1745/mcdebug";
import { setupAdjacentBatbox, type TestContext } from "./helpers.js";

/** 矿工 SLOT_PIPE 的索引（见 BaseMinerBlockEntity companion: 末端槽位）。 */
const SLOT_PIPE = 24;

/** 标准搭建：相邻 BatBox 供电 + OD 扫描器 + 铁钻头 + 采矿管。 */
async function setupMiner(ctx: TestContext): Promise<void> {
  await setupAdjacentBatbox(ctx, "ic2_120:miner");
  await insertItem(ctx, ctx.origin, "ic2_120:od_scanner", 1, 0);
  await insertItem(ctx, ctx.origin, "ic2_120:drill", 1, 1);
  // 给足采矿管（铺垂直管柱用）
  await insertItem(ctx, ctx.origin, "ic2_120:mining_pipe", 32, SLOT_PIPE);
}

export const minerTests = defineTests([
  // 基础放置：矿工方块存在。
  defineTest("miner:place", async (ctx) => {
    await place(ctx, ctx.origin, "ic2_120:miner");
    await assertBlockId(ctx, ctx.origin, "ic2_120:miner");
  }),

  // 核心回归：通电 + 装备齐全 → tryAutoResume 通过 → IDLE→SCANNING（Running=1）。
  // 验证状态机主转移（IDLE→SCANNING）在运行时实际生效，未被打坏。
  // Running 是状态机派生量（SCANNING=1，其余=0），用 >0 等价判定 SCANNING。
  defineTest("miner:enters scanning when powered and equipped", async (ctx) => {
    await setupMiner(ctx);
    // 最多等 ~8 秒（160 tick）。
    await waitUntil(ctx, beFieldGreaterThan(ctx.origin, "Running", 0), 8 * 20);
  }),
]);
