// 固体装罐机 (Solid Canner) 测试。
//
// 装罐机把两样物品合到一块（如空燃料棒 + 铀 → 铀燃料棒）。
// 这里给机器直接预充 4 000 EU（容量也提到 4 000）以跳过初始充电等待。
//
// 槽位布局（与 `SolidCannerBlockEntity` 保持一致）：
//   slot 0  = 容器输入 (SLOT_TIN_CAN)        — 例如空燃料棒
//   slot 1  = 内容物   (SLOT_FOOD)          — 例如铀块
//   slot 2  = 输出     (SLOT_OUTPUT)        — 装好的产物
//   slot 3  = 放电槽   (SLOT_DISCHARGING)
//   slot 4+ = 升级槽   (SLOT_UPGRADE_0..3)
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
  setSlot,
  waitTicks,
  waitUntil,
} from "@yu1745/mcdebug";
import { setupAdjacentBatbox, type TestContext } from "./helpers.js";

/** 标准搭建：相邻 BatBox 供电，并把机器容量/电量都顶到 4 000 EU。 */
async function setupSolidCanner(ctx: TestContext): Promise<void> {
  await setupAdjacentBatbox(ctx, 'ic2_120:solid_canner');
  await setBeField(ctx, ctx.origin, 'EnergyCapacity', 4000);
  await setBeField(ctx, ctx.origin, 'EnergyStored', 4000);
}

export const solidCannerTests = defineTests([
  // 基础放置。
  defineTest('solid_canner:place', async (ctx) => {
    await place(ctx, ctx.origin, 'ic2_120:solid_canner');
    await assertBlockId(ctx, ctx.origin, 'ic2_120:solid_canner');
  }),

  // 空燃料棒 + 铀 → 铀燃料棒（输入两个槽全部消耗）。
  defineTest('solid_canner:fuel_rod_uranium to uranium_fuel_rod', async (ctx) => {
    await setupSolidCanner(ctx);
    await insertItem(ctx, ctx.origin, 'ic2_120:fuel_rod', 1, 0);
    await insertItem(ctx, ctx.origin, 'ic2_120:uranium', 1, 1);
    await waitUntil(ctx, invItemEquals(ctx.origin, 2, 'ic2_120:uranium_fuel_rod'), 15 * 20);
    await assertSlotHas(ctx, ctx.origin, 2, 'ic2_120:uranium_fuel_rod');
    await assertSlotEmpty(ctx, ctx.origin, 0);
    await assertSlotEmpty(ctx, ctx.origin, 1);
  }),

  // 空燃料棒 + MOX → MOX 燃料棒。
  defineTest('solid_canner:fuel_rod_mox to mox_fuel_rod', async (ctx) => {
    await setupSolidCanner(ctx);
    await insertItem(ctx, ctx.origin, 'ic2_120:fuel_rod', 1, 0);
    await insertItem(ctx, ctx.origin, 'ic2_120:mox', 1, 1);
    await waitUntil(ctx, invItemEquals(ctx.origin, 2, 'ic2_120:mox_fuel_rod'), 15 * 20);
    await assertSlotHas(ctx, ctx.origin, 2, 'ic2_120:mox_fuel_rod');
  }),

  // 无电闲置：两个输入槽的物品都应原样保留。
  defineTest('solid_canner:no_power:idle', async (ctx) => {
    await place(ctx, ctx.origin, 'ic2_120:solid_canner');
    await insertItem(ctx, ctx.origin, 'ic2_120:fuel_rod', 1, 0);
    await insertItem(ctx, ctx.origin, 'ic2_120:uranium', 1, 1);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'ic2_120:fuel_rod');
    await assertSlotHas(ctx, ctx.origin, 1, 'ic2_120:uranium');
    await assertSlotEmpty(ctx, ctx.origin, 2);
  }),

  // 缺一不可：只放容器、内容物为空时不应触发。
  defineTest('solid_canner:missing_input:one_slot_empty', async (ctx) => {
    await setupSolidCanner(ctx);
    await insertItem(ctx, ctx.origin, 'ic2_120:fuel_rod', 1, 0);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'ic2_120:fuel_rod');
    await assertSlotEmpty(ctx, ctx.origin, 2);
  }),

  // 非法输入：两个槽都放泥土。
  defineTest('solid_canner:invalid_input:dirt+dirt', async (ctx) => {
    await setupSolidCanner(ctx);
    await insertItem(ctx, ctx.origin, 'minecraft:dirt', 1, 0);
    await insertItem(ctx, ctx.origin, 'minecraft:dirt', 1, 1);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'minecraft:dirt');
    await assertSlotHas(ctx, ctx.origin, 1, 'minecraft:dirt');
    await assertSlotEmpty(ctx, ctx.origin, 2);
  }),

  // 输出满 → 阻塞；两个输入槽的物品都应保留。
  defineTest('solid_canner:output_full:blocks_next', async (ctx) => {
    await setupSolidCanner(ctx);
    await setSlot(ctx, ctx.origin, 2, 'ic2_120:uranium_fuel_rod', 64);
    await insertItem(ctx, ctx.origin, 'ic2_120:fuel_rod', 1, 0);
    await insertItem(ctx, ctx.origin, 'ic2_120:uranium', 1, 1);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'ic2_120:fuel_rod');
    await assertSlotHas(ctx, ctx.origin, 1, 'ic2_120:uranium');
  }),
]);
