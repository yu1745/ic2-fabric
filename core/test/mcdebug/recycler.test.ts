// 回收机 (Recycler) 测试。
//
// 回收机需要电池供电（这里直接 setSlot 写入带 `Energy` 组件的充电电池，
// 跳过发电流程以专注测试消费逻辑）。
//
// 槽位布局（与 `RecyclerBlockEntity` 保持一致）：
//   slot 0  = 输入 (SLOT_INPUT)
//   slot 1  = 输出 (SLOT_OUTPUT)        — 偶尔吐出的废料/碎片
//   slot 2  = 放电槽 (SLOT_DISCHARGING) — 这里直接放 RE 电池
//   slot 3+ = 升级槽 (SLOT_UPGRADE_0..3)
import {
  assertBlockId,
  assertSlotHas,
  defineTest,
  defineTests,
  getSlot,
  insertItem,
  invCountLessThan,
  place,
  setSlot,
  waitTicks,
  waitUntil,
} from "@yu1745/mcdebug";
import { type TestContext } from "./helpers.js";

/** 标准搭建：放置回收机 + 写入一块带 10 000 EU 电量的 RE 电池。 */
async function setupRecyclerBattery(ctx: TestContext): Promise<void> {
  await place(ctx, ctx.origin, 'ic2_120:recycler');
  await setSlot(ctx, ctx.origin, 2, 'ic2_120:re_battery', 1, { Energy: 10000 });
}

export const recyclerTests = defineTests([
  // 基础放置。
  defineTest('recycler:place', async (ctx) => {
    await place(ctx, ctx.origin, 'ic2_120:recycler');
    await assertBlockId(ctx, ctx.origin, 'ic2_120:recycler');
  }),

  // 经典路径：放 10 个圆石，等 200 tick 后数量应被消耗（具体变成什么由
  // 概率决定，不做硬断言）。
  defineTest('recycler:consume input with battery', async (ctx) => {
    await setupRecyclerBattery(ctx);
    await insertItem(ctx, ctx.origin, 'minecraft:cobblestone', 10, 0);
    await waitUntil(ctx, invCountLessThan(ctx.origin, 0, 10), 15 * 20);
    const slot = await getSlot(ctx, ctx.origin, 0);
    if (slot.count >= 10) throw new Error(`expected recycler to consume input items, still ${slot.count}`);
  }),

  // 无电闲置。
  defineTest('recycler:no_power:idle', async (ctx) => {
    await place(ctx, ctx.origin, 'ic2_120:recycler');
    await insertItem(ctx, ctx.origin, 'minecraft:cobblestone', 4, 0);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'minecraft:cobblestone');
  }),

  // 非法输入：木棍不在回收白名单里。
  defineTest('recycler:invalid_input:stick', async (ctx) => {
    await setupRecyclerBattery(ctx);
    await insertItem(ctx, ctx.origin, 'minecraft:stick', 1, 0);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'minecraft:stick');
  }),
]);
