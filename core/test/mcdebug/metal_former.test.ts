// 金属成型机 (Metal Former) 测试。
//
// 金属成型机通过 BE 字段 `Mode` 切换三种模式（与 `MetalFormerSync.Mode` 保持一致）：
//   mode = 0 → ROLLING   辊压  ：1 铁锭 → 1 铁板
//   mode = 1 → CUTTING   切割  ：1 铁板 → 4 铁线
//   mode = 2 → EXTRUDING 挤压  ：1 铁锭 → 4 铁线
//
// 槽位布局（与 `MetalFormerBlockEntity` 保持一致）：
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
  setBeField,
  setSlot,
  waitTicks,
  waitUntil,
} from "@yu1745/mcdebug";
import { setupAdjacentBatbox, type TestContext } from "./helpers.js";

/** 标准搭建：相邻 BatBox + 设定模式。 */
async function setupMetalFormer(ctx: TestContext, mode: number): Promise<void> {
  await setupAdjacentBatbox(ctx, 'ic2_120:metal_former');
  await setBeField(ctx, ctx.origin, 'Mode', mode);
}

export const metalFormerTests = defineTests([
  // 基础放置。
  defineTest('metal_former:place', async (ctx) => {
    await place(ctx, ctx.origin, 'ic2_120:metal_former');
    await assertBlockId(ctx, ctx.origin, 'ic2_120:metal_former');
  }),

  // 辊压：1 铁锭 → 1 铁板。
  defineTest('metal_former:rolling iron_ingot to iron_plate', async (ctx) => {
    await setupMetalFormer(ctx, 0);
    await insertItem(ctx, ctx.origin, 'minecraft:iron_ingot', 1, 0);
    await waitUntil(ctx, invItemEquals(ctx.origin, 1, 'ic2_120:iron_plate'), 15 * 20);
    await assertSlotCount(ctx, ctx.origin, 1, 1);
  }),

  // 切割：1 铁板 → 4 铁线。
  defineTest('metal_former:cutting iron_plate to 4 iron_cable', async (ctx) => {
    await setupMetalFormer(ctx, 1);
    await insertItem(ctx, ctx.origin, 'ic2_120:iron_plate', 1, 0);
    await waitUntil(ctx, invItemEquals(ctx.origin, 1, 'ic2_120:iron_cable'), 15 * 20);
    await assertSlotCount(ctx, ctx.origin, 1, 4);
  }),

  // 挤压：1 铁锭 → 4 铁线（与切割产物相同但走的配方不同）。
  defineTest('metal_former:extruding iron_ingot to 4 iron_cable', async (ctx) => {
    await setupMetalFormer(ctx, 2);
    await insertItem(ctx, ctx.origin, 'minecraft:iron_ingot', 1, 0);
    await waitUntil(ctx, invItemEquals(ctx.origin, 1, 'ic2_120:iron_cable'), 15 * 20);
    await assertSlotCount(ctx, ctx.origin, 1, 4);
  }),

  // 无电闲置。
  defineTest('metal_former:no_power:idle', async (ctx) => {
    await place(ctx, ctx.origin, 'ic2_120:metal_former');
    await insertItem(ctx, ctx.origin, 'minecraft:iron_ingot', 1, 0);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'minecraft:iron_ingot');
    await assertSlotEmpty(ctx, ctx.origin, 1);
  }),

  // 模式不匹配：辊压模式下塞铁板进去不算合法输入。
  defineTest('metal_former:invalid_input:plate_in_rolling_mode', async (ctx) => {
    await setupMetalFormer(ctx, 0);
    await insertItem(ctx, ctx.origin, 'ic2_120:iron_plate', 1, 0);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'ic2_120:iron_plate');
    await assertSlotEmpty(ctx, ctx.origin, 1);
  }),

  // 输出满 → 阻塞。
  defineTest('metal_former:output_full:blocks_next', async (ctx) => {
    await setupMetalFormer(ctx, 0);
    await setSlot(ctx, ctx.origin, 1, 'ic2_120:iron_plate', 64);
    await insertItem(ctx, ctx.origin, 'minecraft:iron_ingot', 1, 0);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'minecraft:iron_ingot');
  }),
]);
