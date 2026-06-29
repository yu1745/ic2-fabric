// 采矿机 (Miner) 回归测试。
//
// 状态机重写（refactor/miner-state-machine）后的回归防护：
//   1. miner:place              — 放置不崩
//   2. miner:enters scanning    — 通电+装备齐全后 IDLE→SCANNING（Running=1）
//   3. miner:mines ore below    — 真实挖掘：正下方 1、2 格放铁矿石，验证被挖出
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
// 能量平衡（见计划计算）：
//   miner baseTier=2 + 1 变压器升级 = tier 3（接受 512 EU/t）
//   + 3 超频升级 → speed ×2.92，period=6 tick；每周期挖掘支出 2048 EU，MFE 收入 3072 EU（富余 50%）
//   MFE (tier 3, 4M EU) 供电，容量远超挖两格矿所需。
//
// 注意：SyncedData.int 字段在 NBT 里拆成 *_High/*_Low 两个 16 位 key，
// Running 值为 0/1，全部落在 Running_Low（见 SyncedData.int 实现）。
import {
  assertBlockId,
  assertSlotHas,
  beFieldGreaterThan,
  defineTest,
  defineTests,
  insertItem,
  invItemEquals,
  place,
  setBlocks,
  waitUntil,
} from "@yu1745/mcdebug";
import { setupAdjacentBatbox, type TestContext } from "./helpers.js";

/** 物品槽起始（矿石掉落物入这里，SLOT_ITEM_START）。 */
const SLOT_ITEM_START = 3;
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

  // 真实挖掘：在 pos.y-1 层（垂直管柱所在层）的非中心格放铁矿石，验证被挖出。
  // 注意：垂直管柱中心格 (0,0) 会被管道占用（tryPlacePipeAt 清空原方块铺管，不掉落），
  //       所以矿石必须放在 ring 1 起首位置 (-1,+1) 等「会被钻头挖」的格子上。
  // OD 螺旋顺序：index 0=(0,0)中心柱(铺管)，index 1=(-1,+1)，index 2=(0,+1)，index 3=(+1,+1)...
  // 验证 SCANNING 主路径完整：铺垂直管柱→扫描 ring1→挖矿→掉落入物品槽。
  defineTest("miner:mines ore below", async (ctx) => {
    await setupFastMiner(ctx);
    // 矿石放在 pos.y-1 层 ring1 位置：(-1,+1) 与 (+1,-1)，避开中心柱 (0,0)。
    // ctx.origin.down() = pos.y-1 层；.west().south() = (-1,+1)；.east().north() = (+1,-1)。
    await setBlocks(ctx, [
      { pos: ctx.origin.down().west().south(), block: "minecraft:iron_ore" },
      { pos: ctx.origin.down().east().north(), block: "minecraft:iron_ore" },
    ]);
    // 铁钻头 → IRON_PICKAXE → 挖 iron_ore 掉 raw_iron，入 SLOT_ITEM_START(3)。
    // 3 超频 period=6t；先铺中心柱 + 扫到 ring1 约 ~5 秒；给 25 秒裕量。
    await waitUntil(ctx, invItemEquals(ctx.origin, SLOT_ITEM_START, "minecraft:raw_iron"), 25 * 20);
    await assertSlotHas(ctx, ctx.origin, SLOT_ITEM_START, "minecraft:raw_iron");
  }),
]);
