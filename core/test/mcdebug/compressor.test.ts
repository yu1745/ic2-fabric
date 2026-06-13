// 压缩机 (Compressor) 测试。
//
// 槽位布局（与 `CompressorBlockEntity` 保持一致）：
//   slot 0  = 输入 (SLOT_INPUT)
//   slot 1  = 输出 (SLOT_OUTPUT)
//   slot 2  = 放电槽 (SLOT_DISCHARGING)
//   slot 3+ = 升级槽 (SLOT_UPGRADE_0..3) — 这里放两个超频加速
import {
  assertBlockId,
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

/** 标准搭建：相邻 BatBox 供电 + 两个超频升级。 */
async function setupCompressor(ctx: TestContext): Promise<void> {
  await setupAdjacentBatbox(ctx, 'ic2_120:compressor');
  await insertItem(ctx, ctx.origin, 'ic2_120:overclocker_upgrade', 2, 3);
}

export const compressorTests = defineTests([
  // 基础放置。
  defineTest('compressor:place', async (ctx) => {
    await place(ctx, ctx.origin, 'ic2_120:compressor');
    await assertBlockId(ctx, ctx.origin, 'ic2_120:compressor');
  }),

  // 4 粘土球 → 1 粘土。
  defineTest('compressor:4 clay_ball to clay', async (ctx) => {
    await setupCompressor(ctx);
    await insertItem(ctx, ctx.origin, 'minecraft:clay_ball', 4, 0);
    await waitUntil(ctx, invItemEquals(ctx.origin, 1, 'minecraft:clay'), 15 * 20);
    await assertSlotHas(ctx, ctx.origin, 1, 'minecraft:clay');
  }),

  // 9 铁锭 → 1 铁块。
  defineTest('compressor:9 iron_ingot to iron_block', async (ctx) => {
    await setupCompressor(ctx);
    await insertItem(ctx, ctx.origin, 'minecraft:iron_ingot', 9, 0);
    await waitUntil(ctx, invItemEquals(ctx.origin, 1, 'minecraft:iron_block'), 15 * 20);
    await assertSlotHas(ctx, ctx.origin, 1, 'minecraft:iron_block');
  }),

  // 无电闲置。
  defineTest('compressor:no_power:idle', async (ctx) => {
    await place(ctx, ctx.origin, 'ic2_120:compressor');
    await insertItem(ctx, ctx.origin, 'minecraft:clay_ball', 4, 0);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'minecraft:clay_ball');
    await assertSlotEmpty(ctx, ctx.origin, 1);
  }),

  // 非法输入：泥土无对应配方。
  defineTest('compressor:invalid_input:dirt', async (ctx) => {
    await setupCompressor(ctx);
    await insertItem(ctx, ctx.origin, 'minecraft:dirt', 1, 0);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'minecraft:dirt');
    await assertSlotEmpty(ctx, ctx.origin, 1);
  }),

  // 输出槽塞满粘土 → 新一轮压缩应卡住不消耗输入。
  defineTest('compressor:output_full:blocks_next', async (ctx) => {
    await setupCompressor(ctx);
    await setSlot(ctx, ctx.origin, 1, 'minecraft:clay', 64);
    await insertItem(ctx, ctx.origin, 'minecraft:clay_ball', 4, 0);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'minecraft:clay_ball');
  }),
]);
