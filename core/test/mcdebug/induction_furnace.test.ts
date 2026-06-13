// 感应炉 (Induction Furnace) 测试。
//
// 感应炉是热能机器：需要 (1) 能量、(2) 热量 ≥ 100、(3) 红石信号
// 三者同时满足才能熔炼。这里把红石火把放在西侧一格以提供持续红石信号。
//
// 槽位布局（与 `InductionFurnaceBlockEntity` 保持一致）：
//   slot 0..1 = 输入 (SLOT_INPUT_0, SLOT_INPUT_1)    — 双输入槽，可并行熔炼
//   slot 2..3 = 输出 (SLOT_OUTPUT_0, SLOT_OUTPUT_1)
//   slot 4    = 放电槽 (SLOT_DISCHARGING)
//   slot 5..6 = 升级槽 (SLOT_UPGRADE_0..1)
import {
  assertBlockId,
  assertSlotEmpty,
  assertSlotHas,
  defineTest,
  defineTests,
  getBeNumber,
  insertItem,
  invItemEquals,
  place,
  setBeField,
  setBlocks,
  waitTicks,
  waitUntil,
} from "@yu1745/mcdebug";
import { setHeat, type TestContext } from "./helpers.js";

/** 标准搭建：东 BatBox + 西红石火把，机器自身预充 40 000 EU（不预热）。 */
async function setupInductionFurnace(ctx: TestContext): Promise<void> {
  const batbox = ctx.origin.east();
  const torch = ctx.origin.west();
  await setBlocks(ctx, [
    { pos: batbox, block: 'ic2_120:batbox', props: { facing: 'west' } },
    { pos: torch, block: 'minecraft:redstone_torch' },
  ]);
  await setBeField(ctx, batbox, 'EnergyStored', 40000);
  await place(ctx, ctx.origin, 'ic2_120:induction_furnace');
  await setBeField(ctx, ctx.origin, 'EnergyStored', 40000);
}

/** 在标准搭建上把热量直接拉到 10 000，绕过预热曲线。 */
async function setupInductionFurnaceHot(ctx: TestContext): Promise<void> {
  await setupInductionFurnace(ctx);
  await setHeat(ctx, 10000);
}

export const inductionFurnaceTests = defineTests([
  // 基础放置。
  defineTest('induction_furnace:place', async (ctx) => {
    await place(ctx, ctx.origin, 'ic2_120:induction_furnace');
    await assertBlockId(ctx, ctx.origin, 'ic2_120:induction_furnace');
  }),

  // 三要素齐备时的标准熔炼。
  defineTest('induction_furnace:smelt with heat and redstone', async (ctx) => {
    await setupInductionFurnaceHot(ctx);
    await insertItem(ctx, ctx.origin, 'minecraft:iron_ore', 1, 0);
    await waitUntil(ctx, invItemEquals(ctx.origin, 2, 'minecraft:iron_ingot'), 15 * 20);
    await assertSlotHas(ctx, ctx.origin, 2, 'minecraft:iron_ingot');
  }),

  // 双输入并行：两个槽位同时放入铁矿石，应并行熔炼到两个输出槽。
  defineTest('induction_furnace:dual_slot', async (ctx) => {
    await setupInductionFurnaceHot(ctx);
    await insertItem(ctx, ctx.origin, 'minecraft:iron_ore', 1, 0);
    await insertItem(ctx, ctx.origin, 'minecraft:iron_ore', 1, 1);
    await waitUntil(ctx, invItemEquals(ctx.origin, 3, 'minecraft:iron_ingot'), 15 * 20);
    await assertSlotHas(ctx, ctx.origin, 2, 'minecraft:iron_ingot');
    await assertSlotHas(ctx, ctx.origin, 3, 'minecraft:iron_ingot');
  }),

  // 无红石信号：有电、有初始 100 热，但没红石 → 热量会因衰减降回 < 100，
  // 验证“无红石 → 不维持热量”的语义。
  defineTest('induction_furnace:no_redstone:no_heat_no_smelt', async (ctx) => {
    const batbox = ctx.origin.east();
    await setBlocks(ctx, [{ pos: batbox, block: 'ic2_120:batbox', props: { facing: 'west' } }]);
    await setBeField(ctx, batbox, 'EnergyStored', 40000);
    await place(ctx, ctx.origin, 'ic2_120:induction_furnace');
    await setBeField(ctx, ctx.origin, 'EnergyStored', 40000);
    await setHeat(ctx, 100);
    await insertItem(ctx, ctx.origin, 'minecraft:iron_ore', 1, 0);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'minecraft:iron_ore');
    await assertSlotEmpty(ctx, ctx.origin, 2);
    const heat = await getBeNumber(ctx, ctx.origin, 'Heat_Low') + (await getBeNumber(ctx, ctx.origin, 'Heat_High')) * 65536;
    if (heat >= 100) throw new Error(`expected heat < 100 (decayed), got ${heat}`);
  }),

  // 无热量：有电有红石但热量为 0，机器不能工作。
  defineTest('induction_furnace:no_heat:no_smelt', async (ctx) => {
    const batbox = ctx.origin.east();
    await setBlocks(ctx, [{ pos: batbox, block: 'ic2_120:batbox', props: { facing: 'west' } }]);
    await setBeField(ctx, batbox, 'EnergyStored', 40000);
    await place(ctx, ctx.origin, 'ic2_120:induction_furnace');
    await setBeField(ctx, ctx.origin, 'EnergyStored', 40000);
    await insertItem(ctx, ctx.origin, 'minecraft:iron_ore', 1, 0);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'minecraft:iron_ore');
    await assertSlotEmpty(ctx, ctx.origin, 2);
  }),

  // 升温曲线：电+红石都在但未预热的情况下，等 80 tick 后热量应上升到 > 100。
  defineTest('induction_furnace:heat_up:redstone+energy', async (ctx) => {
    await setupInductionFurnace(ctx);
    await waitTicks(ctx, 80);
    const heatLow = await getBeNumber(ctx, ctx.origin, 'Heat_Low');
    const heatHigh = await getBeNumber(ctx, ctx.origin, 'Heat_High');
    const energy = await getBeNumber(ctx, ctx.origin, 'EnergyStored');
    const heat = heatLow + heatHigh * 65536;
    const torchState = await ctx.api.world.getBlock(ctx.origin.west());
    if (heat <= 100) {
      throw new Error(`expected heat > 100 after 80 ticks, got ${heat} low=${heatLow} high=${heatHigh} energy=${energy} torch=${JSON.stringify(torchState.state)}`);
    }
  }),

  // 非法输入：泥土无熔炼配方。
  defineTest('induction_furnace:invalid_input:dirt', async (ctx) => {
    await setupInductionFurnaceHot(ctx);
    await insertItem(ctx, ctx.origin, 'minecraft:dirt', 1, 0);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'minecraft:dirt');
    await assertSlotEmpty(ctx, ctx.origin, 2);
  }),
]);
