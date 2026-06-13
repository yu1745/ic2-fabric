// 提取机 (Extractor) 测试。
//
// 槽位布局（与 `ExtractorBlockEntity` 保持一致）：
//   slot 0  = 输入 (SLOT_INPUT)
//   slot 1  = 输出 (SLOT_OUTPUT)
//   slot 2  = 放电槽 (SLOT_DISCHARGING)
//   slot 3+ = 升级槽 (SLOT_UPGRADE_0..3) — 这里放两个超频
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
  setSlot,
  waitTicks,
  waitUntil,
} from "@yu1745/mcdebug";
import { setupAdjacentBatbox, type TestContext } from "./helpers.js";

/** 标准搭建：相邻 BatBox + 两个超频。 */
async function setupExtractor(ctx: TestContext): Promise<void> {
  await setupAdjacentBatbox(ctx, 'ic2_120:extractor');
  await insertItem(ctx, ctx.origin, 'ic2_120:overclocker_upgrade', 2, 3);
}

export const extractorTests = defineTests([
  // 基础放置。
  defineTest('extractor:place', async (ctx) => {
    await place(ctx, ctx.origin, 'ic2_120:extractor');
    await assertBlockId(ctx, ctx.origin, 'ic2_120:extractor');
  }),

  // 1 粘土 → 4 粘土球。
  defineTest('extractor:clay to 4 clay_ball', async (ctx) => {
    await setupExtractor(ctx);
    await insertItem(ctx, ctx.origin, 'minecraft:clay', 1, 0);
    await waitUntil(ctx, invItemEquals(ctx.origin, 1, 'minecraft:clay_ball'), 20 * 20);
    await assertSlotHas(ctx, ctx.origin, 1, 'minecraft:clay_ball');
    await assertSlotCount(ctx, ctx.origin, 1, 4);
  }),

  // 1 树脂 → 3 橡胶。
  defineTest('extractor:resin to 3 rubber', async (ctx) => {
    await setupExtractor(ctx);
    await insertItem(ctx, ctx.origin, 'ic2_120:resin', 1, 0);
    await waitUntil(ctx, invItemEquals(ctx.origin, 1, 'ic2_120:rubber'), 20 * 20);
    await assertSlotCount(ctx, ctx.origin, 1, 3);
  }),

  // 无电闲置。
  defineTest('extractor:no_power:idle', async (ctx) => {
    await place(ctx, ctx.origin, 'ic2_120:extractor');
    await insertItem(ctx, ctx.origin, 'minecraft:clay', 1, 0);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'minecraft:clay');
    await assertSlotEmpty(ctx, ctx.origin, 1);
  }),

  // 非法输入：泥土无对应配方。
  defineTest('extractor:invalid_input:dirt', async (ctx) => {
    await setupExtractor(ctx);
    await insertItem(ctx, ctx.origin, 'minecraft:dirt', 1, 0);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'minecraft:dirt');
    await assertSlotEmpty(ctx, ctx.origin, 1);
  }),

  // 输出满 → 阻塞。
  defineTest('extractor:output_full:blocks_next', async (ctx) => {
    await setupExtractor(ctx);
    await setSlot(ctx, ctx.origin, 1, 'minecraft:clay_ball', 64);
    await insertItem(ctx, ctx.origin, 'minecraft:clay', 1, 0);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'minecraft:clay');
  }),
]);
