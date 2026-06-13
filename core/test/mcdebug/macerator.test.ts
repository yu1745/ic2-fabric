// 粉碎机 (Macerator) 测试。
//
// 粉碎机有两条供电路径需要分别覆盖：
//   1. 通过**线缆**中转（中间隔一格绝缘铜缆），覆盖电缆连接 + 能量传输链路；
//   2. 直接相邻 BatBox，覆盖面贴接供电。
//
// 槽位布局（与 `MaceratorBlockEntity` 保持一致）：
//   slot 0  = 输入 (SLOT_INPUT)
//   slot 1  = 输出 (SLOT_OUTPUT)
//   slot 2  = 放电槽 (SLOT_DISCHARGING)
//   slot 3+ = 升级槽 (SLOT_UPGRADE_0..3)
import {
  assertBlockId,
  assertSlotCount,
  assertSlotEmpty,
  assertSlotHas,
  defineTest,
  defineTests,
  insertItem,
  invItemEquals,
  place,
  placeAsPlayer,
  setBeField,
  setBlocks,
  setSlot,
  waitTicks,
  waitUntil,
} from "@yu1745/mcdebug";
import { BATBOX_EAST, type TestContext } from "./helpers.js";

/**
 * 标准搭建：东二格 BatBox + 东一格绝缘铜缆（让玩家放置以正确生成 facing），
 * 然后放置粉碎机。线缆中转用于验证能量网络能跨方块传导。
 */
async function setupMacerator(ctx: TestContext): Promise<void> {
  const batbox = ctx.origin.east(2);
  const cable = ctx.origin.east();
  await setBlocks(ctx, [{ pos: batbox, block: 'ic2_120:batbox', props: { facing: 'west' } }]);
  await setBeField(ctx, batbox, 'EnergyStored', 40000);
  await placeAsPlayer(ctx, cable, 'ic2_120:insulated_copper_cable', 'west', { neighbor: batbox, playerFacing: 'east' });
  await place(ctx, ctx.origin, 'ic2_120:macerator');
}

export const maceratorTests = defineTests([
  // 基础放置。
  defineTest('macerator:place', async (ctx) => {
    await place(ctx, ctx.origin, 'ic2_120:macerator');
    await assertBlockId(ctx, ctx.origin, 'ic2_120:macerator');
  }),

  // 经典配方：1 圆石 → 1 砾石（经电缆供电）。
  defineTest('macerator:cobblestone to gravel with cable', async (ctx) => {
    await setupMacerator(ctx);
    await insertItem(ctx, ctx.origin, 'minecraft:cobblestone', 1, 0);
    await waitUntil(ctx, invItemEquals(ctx.origin, 1, 'minecraft:gravel'), 15 * 20);
    await assertSlotHas(ctx, ctx.origin, 1, 'minecraft:gravel');
  }),

  // 1 煤炭块 → 9 煤粉（带数量断言）。
  defineTest('macerator:coal_block to 9 coal_dust', async (ctx) => {
    await setupMacerator(ctx);
    await insertItem(ctx, ctx.origin, 'minecraft:coal_block', 1, 0);
    await waitUntil(ctx, invItemEquals(ctx.origin, 1, 'ic2_120:coal_dust'), 15 * 20);
    await assertSlotCount(ctx, ctx.origin, 1, 9);
  }),

  // 8 西瓜片 → 1 生物质渣（带数量断言）。
  defineTest('macerator:8 melon_slice to bio_chaff', async (ctx) => {
    await setupMacerator(ctx);
    await insertItem(ctx, ctx.origin, 'minecraft:melon_slice', 8, 0);
    await waitUntil(ctx, invItemEquals(ctx.origin, 1, 'ic2_120:bio_chaff'), 15 * 20);
    await assertSlotCount(ctx, ctx.origin, 1, 1);
  }),

  // 1 铁矿石 → 2 粉碎铁（验证富产系数）。
  defineTest('macerator:iron_ore to 2 crushed_iron', async (ctx) => {
    await setupMacerator(ctx);
    await insertItem(ctx, ctx.origin, 'minecraft:iron_ore', 1, 0);
    await waitUntil(ctx, invItemEquals(ctx.origin, 1, 'ic2_120:crushed_iron'), 15 * 20);
    await assertSlotCount(ctx, ctx.origin, 1, 2);
  }),

  // 无电闲置。
  defineTest('macerator:no_power:idle', async (ctx) => {
    await place(ctx, ctx.origin, 'ic2_120:macerator');
    await insertItem(ctx, ctx.origin, 'minecraft:cobblestone', 1, 0);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'minecraft:cobblestone');
    await assertSlotEmpty(ctx, ctx.origin, 1);
  }),

  // 非法输入。
  defineTest('macerator:invalid_input:dirt', async (ctx) => {
    await setupMacerator(ctx);
    await insertItem(ctx, ctx.origin, 'minecraft:dirt', 1, 0);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'minecraft:dirt');
    await assertSlotEmpty(ctx, ctx.origin, 1);
  }),

  // 输出满 → 阻塞。
  defineTest('macerator:output_full:blocks_next', async (ctx) => {
    await setupMacerator(ctx);
    await setSlot(ctx, ctx.origin, 1, 'minecraft:gravel', 64);
    await insertItem(ctx, ctx.origin, 'minecraft:cobblestone', 1, 0);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'minecraft:cobblestone');
  }),

  // 能量不足 1 轮 → 应当一整轮都不消耗输入（不能半成品消耗）。
  defineTest('macerator:energy_starve:no_partial_consume', async (ctx) => {
    const batbox = ctx.origin.east(2);
    const cable = ctx.origin.east();
    await setBlocks(ctx, [{ pos: batbox, block: 'ic2_120:batbox', props: { facing: 'west' } }]);
    await setBeField(ctx, batbox, 'EnergyStored', 1);
    await placeAsPlayer(ctx, cable, 'ic2_120:insulated_copper_cable', 'west', { neighbor: batbox, playerFacing: 'east' });
    await place(ctx, ctx.origin, 'ic2_120:macerator');
    await insertItem(ctx, ctx.origin, 'minecraft:cobblestone', 1, 0);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'minecraft:cobblestone');
  }),

  // 直接相邻 BatBox 也能正常工作（覆盖面贴接供电链路）。
  defineTest('macerator:power:adjacent_batbox', async (ctx) => {
    const batbox = BATBOX_EAST(ctx);
    await setBlocks(ctx, [{ pos: batbox, block: 'ic2_120:batbox', props: { facing: 'west' } }]);
    await setBeField(ctx, batbox, 'EnergyStored', 40000);
    await place(ctx, ctx.origin, 'ic2_120:macerator');
    await insertItem(ctx, ctx.origin, 'minecraft:cobblestone', 1, 0);
    await waitUntil(ctx, invItemEquals(ctx.origin, 1, 'minecraft:gravel'), 15 * 20);
    await assertSlotHas(ctx, ctx.origin, 1, 'minecraft:gravel');
  }),

  // 过压爆炸：把粉碎机接在满电的 MFSU（HV）下，应在 15s 内自爆成空气，
  // 用于覆盖“过压爆炸”机制。
  defineTest('macerator:overvoltage:hv_explode', async (ctx) => {
    const mfsu = BATBOX_EAST(ctx);
    await setBlocks(ctx, [{ pos: mfsu, block: 'ic2_120:mfsu', props: { facing: 'west' } }]);
    await setBeField(ctx, mfsu, 'EnergyStored', 40_000_000);
    await place(ctx, ctx.origin, 'ic2_120:macerator');
    await insertItem(ctx, ctx.origin, 'minecraft:cobblestone', 1, 0);
    await waitUntil(ctx, `block[${ctx.origin[0]},${ctx.origin[1]},${ctx.origin[2]}].id == "minecraft:air"`, 15 * 20);
    await assertBlockId(ctx, ctx.origin, 'minecraft:air');
  }),
]);
