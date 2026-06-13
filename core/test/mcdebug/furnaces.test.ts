// 电炉 (Electric Furnace) + 铁炉 (Iron Furnace) 测试。
//
// 电炉槽位布局（与 `ElectricFurnaceBlockEntity` 保持一致）：
//   slot 0  = 输入 (SLOT_INPUT)
//   slot 1  = 输出 (SLOT_OUTPUT)
//   slot 2  = 放电槽 (SLOT_DISCHARGING)
//   slot 3+ = 升级槽 (SLOT_UPGRADE_0..3)
//
// 铁炉槽位布局（与 `IronFurnaceBlockEntity` 保持一致）— 燃料供电，无升级：
//   slot 0  = 输入 (SLOT_INPUT)
//   slot 1  = 燃料 (SLOT_FUEL)
//   slot 2  = 输出 (SLOT_OUTPUT)
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
  type TestContext,
  waitTicks,
  waitUntil,
} from "@yu1745/mcdebug";
import { setupAdjacentBatbox } from "./helpers.js";

/** 电炉标准搭建：相邻 BatBox 供电（无升级）。 */
async function setupElectricFurnace(ctx: TestContext): Promise<void> {
  await setupAdjacentBatbox(ctx, 'ic2_120:electric_furnace');
}

export const electricFurnaceTests = defineTests([
  // 基础放置。
  defineTest('electric_furnace:place', async (ctx) => {
    await place(ctx, ctx.origin, 'ic2_120:electric_furnace');
    await assertBlockId(ctx, ctx.origin, 'ic2_120:electric_furnace');
  }),

  // 标准熔炼：铁矿石 → 铁锭；用相邻 BatBox 供电。
  defineTest('electric_furnace:smelt with adjacent BatBox', async (ctx) => {
    await setupElectricFurnace(ctx);
    await insertItem(ctx, ctx.origin, 'minecraft:iron_ore', 1, 0);
    await waitUntil(ctx, invItemEquals(ctx.origin, 1, 'minecraft:iron_ingot'), 15 * 20);
    await assertSlotHas(ctx, ctx.origin, 1, 'minecraft:iron_ingot');
  }),

  // 无电闲置。
  defineTest('electric_furnace:no_power:idle', async (ctx) => {
    await place(ctx, ctx.origin, 'ic2_120:electric_furnace');
    await insertItem(ctx, ctx.origin, 'minecraft:iron_ore', 1, 0);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'minecraft:iron_ore');
    await assertSlotEmpty(ctx, ctx.origin, 1);
  }),

  // 非法输入：泥土没有熔炼配方。
  defineTest('electric_furnace:invalid_input:dirt', async (ctx) => {
    await setupElectricFurnace(ctx);
    await insertItem(ctx, ctx.origin, 'minecraft:dirt', 1, 0);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'minecraft:dirt');
    await assertSlotEmpty(ctx, ctx.origin, 1);
  }),

  // 输出满 → 阻塞。
  defineTest('electric_furnace:output_full:blocks_next', async (ctx) => {
    await setupElectricFurnace(ctx);
    await setSlot(ctx, ctx.origin, 1, 'minecraft:iron_ingot', 64);
    await insertItem(ctx, ctx.origin, 'minecraft:iron_ore', 1, 0);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'minecraft:iron_ore');
  }),
]);

// —— 铁炉 ——
export const ironFurnaceTests = defineTests([
  // 基础放置。
  defineTest('iron_furnace:place+state', async (ctx) => {
    await place(ctx, ctx.origin, 'ic2_120:iron_furnace');
    await assertBlockId(ctx, ctx.origin, 'ic2_120:iron_furnace');
  }),

  // 标准熔炼：铁煤各 1 个 → 铁锭。
  defineTest('iron_furnace:smelt iron_ore', async (ctx) => {
    await place(ctx, ctx.origin, 'ic2_120:iron_furnace');
    await insertItem(ctx, ctx.origin, 'minecraft:iron_ore', 1, 0);
    await insertItem(ctx, ctx.origin, 'minecraft:coal', 1, 1);
    await waitUntil(ctx, invItemEquals(ctx.origin, 2, 'minecraft:iron_ingot'), 15 * 20);
    await assertSlotHas(ctx, ctx.origin, 2, 'minecraft:iron_ingot');
  }),

  // 无燃料：燃料槽空时铁炉不工作，原材料保留。
  defineTest('iron_furnace:no_fuel:idle', async (ctx) => {
    await place(ctx, ctx.origin, 'ic2_120:iron_furnace');
    await insertItem(ctx, ctx.origin, 'minecraft:iron_ore', 1, 0);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'minecraft:iron_ore');
    await assertSlotEmpty(ctx, ctx.origin, 2);
  }),

  // 非法输入：泥土放进燃料槽也不能熔炼。
  defineTest('iron_furnace:invalid_input:dirt', async (ctx) => {
    await place(ctx, ctx.origin, 'ic2_120:iron_furnace');
    await insertItem(ctx, ctx.origin, 'minecraft:dirt', 1, 0);
    await insertItem(ctx, ctx.origin, 'minecraft:coal', 1, 1);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'minecraft:dirt');
    await assertSlotEmpty(ctx, ctx.origin, 2);
  }),

  // 输出满 → 阻塞；输入和燃料都不会被消耗。
  defineTest('iron_furnace:output_full:blocks_next', async (ctx) => {
    await place(ctx, ctx.origin, 'ic2_120:iron_furnace');
    await setSlot(ctx, ctx.origin, 2, 'minecraft:iron_ingot', 64);
    await insertItem(ctx, ctx.origin, 'minecraft:iron_ore', 1, 0);
    await insertItem(ctx, ctx.origin, 'minecraft:coal', 1, 1);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'minecraft:iron_ore');
  }),
]);
