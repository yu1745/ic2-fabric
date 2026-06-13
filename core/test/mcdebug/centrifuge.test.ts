// 离心机 (Centrifuge) 测试。
//
// 离心机是热能机器，机器本身需要预热到 100 才开始工作；高速配方
// （如粉碎矿→三产物）需要 500+。能量侧使用 CESU（300 000 EU）保证
// 完整一轮配方耗能无忧。
//
// 槽位布局（与 `CentrifugeBlockEntity` 保持一致）：
//   slot 0    = 输入 (SLOT_INPUT)
//   slot 1..3 = 输出 (SLOT_OUTPUT_1..3)        — 不同产物的桶位
//   slot 4    = 放电槽 (SLOT_DISCHARGING)      — 这里通过 `EnergyStored` 预充
//   slot 5..8 = 升级槽 (SLOT_UPGRADE_0..3)
import {
  assertBlockId,
  assertSlotEmpty,
  assertSlotHas,
  defineTest,
  defineTests,
  insertItem,
  invItemEquals,
  place,
  setBeField,
  setBlocks,
  setSlot,
  waitTicks,
  waitUntil,
} from "@yu1745/mcdebug";
import { setHeat, type TestContext } from "./helpers.js";

/** 标准搭建：东侧 CESU 供电，机器自身预充少量能量，热量清零。 */
async function setupCentrifuge(ctx: TestContext): Promise<void> {
  const cesu = ctx.origin.east();
  await setBlocks(ctx, [{ pos: cesu, block: 'ic2_120:cesu', props: { facing: 'west' } }]);
  await setBeField(ctx, cesu, 'EnergyStored', 300_000);
  await place(ctx, ctx.origin, 'ic2_120:centrifuge');
  await setBeField(ctx, ctx.origin, 'EnergyStored', 40_000);
}

/** 在标准搭建上把热量升到 `minHeat + 200`，确保越过对应阈值。 */
async function setupCentrifugeHot(ctx: TestContext, minHeat: number): Promise<void> {
  await setupCentrifuge(ctx);
  await setHeat(ctx, minHeat + 200);
}

export const centrifugeTests = defineTests([
  // 基础放置。
  defineTest('centrifuge:place', async (ctx) => {
    await place(ctx, ctx.origin, 'ic2_120:centrifuge');
    await assertBlockId(ctx, ctx.origin, 'ic2_120:centrifuge');
  }),

  // 100 热：圆石 → 石粉（单产物）。
  defineTest('centrifuge:cobblestone to stone_dust', async (ctx) => {
    await setupCentrifugeHot(ctx, 100);
    await insertItem(ctx, ctx.origin, 'minecraft:cobblestone', 1, 0);
    await waitUntil(ctx, invItemEquals(ctx.origin, 1, 'ic2_120:stone_dust'), 40 * 20);
    await assertSlotHas(ctx, ctx.origin, 1, 'ic2_120:stone_dust');
    await assertSlotEmpty(ctx, ctx.origin, 0);
  }),

  // 500 热：粉碎铜 → 锡粉 + 铜粉 + 石粉（同时落三个槽，验证多槽位布局）。
  defineTest('centrifuge:crushed_copper to 3 outputs', async (ctx) => {
    await setupCentrifugeHot(ctx, 500);
    await insertItem(ctx, ctx.origin, 'ic2_120:crushed_copper', 1, 0);
    await waitUntil(ctx, invItemEquals(ctx.origin, 2, 'ic2_120:copper_dust'), 40 * 20);
    await assertSlotHas(ctx, ctx.origin, 1, 'ic2_120:small_tin_dust');
    await assertSlotHas(ctx, ctx.origin, 2, 'ic2_120:copper_dust');
    await assertSlotHas(ctx, ctx.origin, 3, 'ic2_120:stone_dust');
  }),

  // 无电闲置。
  defineTest('centrifuge:no_power:idle', async (ctx) => {
    await place(ctx, ctx.origin, 'ic2_120:centrifuge');
    await insertItem(ctx, ctx.origin, 'minecraft:cobblestone', 1, 0);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'minecraft:cobblestone');
    await assertSlotEmpty(ctx, ctx.origin, 1);
  }),

  // 热量不足：没预热到 100 时即便有电也不工作。
  defineTest('centrifuge:heat_starve:cold_centrifuge', async (ctx) => {
    await setupCentrifuge(ctx);
    await insertItem(ctx, ctx.origin, 'minecraft:cobblestone', 1, 0);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'minecraft:cobblestone');
    await assertSlotEmpty(ctx, ctx.origin, 1);
  }),

  // 非法输入：泥土不参与任何配方。
  defineTest('centrifuge:invalid_input:dirt', async (ctx) => {
    await setupCentrifugeHot(ctx, 100);
    await insertItem(ctx, ctx.origin, 'minecraft:dirt', 1, 0);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'minecraft:dirt');
    await assertSlotEmpty(ctx, ctx.origin, 1);
  }),

  // 输出三槽全满 → 阻塞新配方。
  defineTest('centrifuge:output_full:blocks_next', async (ctx) => {
    await setupCentrifugeHot(ctx, 100);
    await setSlot(ctx, ctx.origin, 1, 'ic2_120:stone_dust', 64);
    await setSlot(ctx, ctx.origin, 2, 'ic2_120:stone_dust', 64);
    await setSlot(ctx, ctx.origin, 3, 'ic2_120:stone_dust', 64);
    await insertItem(ctx, ctx.origin, 'minecraft:cobblestone', 1, 0);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'minecraft:cobblestone');
  }),
]);
