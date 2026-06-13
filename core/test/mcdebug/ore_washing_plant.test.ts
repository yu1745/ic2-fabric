// 洗矿机 (Ore Washing Plant) 测试。
//
// 洗矿机需要 (1) 能量、(2) 流体槽内至少 1 桶水、(3) 粉碎矿输入。
// 这里在 `setupOreWasher` 中通过 `fluidInsert` 直接灌满 1 桶水
// （一桶水 = 81 000 droplets，1000 droplets/mB）。
//
// 槽位布局（与 `OreWashingPlantBlockEntity` 保持一致）：
//   slot 0  = 矿输入 (SLOT_INPUT_ORE)
//   slot 1  = 水输入 (SLOT_INPUT_WATER)        — 也可放水桶
//   slot 2  = 纯净粉碎矿 (SLOT_OUTPUT_1)
//   slot 3  = 小撮金属粉 (SLOT_OUTPUT_2)
//   slot 4  = 石粉       (SLOT_OUTPUT_3)
//   slot 5  = 空桶       (SLOT_OUTPUT_EMPTY)
//   slot 6  = 放电槽 (SLOT_DISCHARGING)
//   slot 7..10 = 升级槽 (SLOT_UPGRADE_0..3)   — 这里变压器+超频各一
import {
  assertBlockId,
  assertSlotCount,
  assertSlotEmpty,
  assertSlotHas,
  defineTest,
  defineTests,
  fluidExtract,
  fluidGet,
  fluidInsert,
  insertItem,
  invItemEquals,
  place,
  setBeField,
  setBlocks,
  waitTicks,
  waitUntil,
} from "@yu1745/mcdebug";
import { type TestContext } from "./helpers.js";

/** 一桶水对应的 droplet 数（1000 droplets/mB × 1000 mB）。 */
const WATER_BUCKET_DROPLETS = 81_000;

/** 标准搭建：东 CESU 供电 + 变压器/超频升级 + 流体槽灌满 1 桶水。 */
async function setupOreWasher(ctx: TestContext): Promise<void> {
  const cesu = ctx.origin.east();
  await setBlocks(ctx, [{ pos: cesu, block: 'ic2_120:cesu', props: { facing: 'west' } }]);
  await setBeField(ctx, cesu, 'EnergyStored', 40000);
  await place(ctx, ctx.origin, 'ic2_120:ore_washing_plant');
  await insertItem(ctx, ctx.origin, 'ic2_120:transformer_upgrade', 1, 7);
  await insertItem(ctx, ctx.origin, 'ic2_120:overclocker_upgrade', 2, 8);
  const inserted = await fluidInsert(ctx, ctx.origin, 'minecraft:water', WATER_BUCKET_DROPLETS);
  if (inserted < WATER_BUCKET_DROPLETS) throw new Error(`failed to prime water tank: ${inserted}/${WATER_BUCKET_DROPLETS}`);
}

export const oreWashingPlantTests = defineTests([
  // 基础放置。
  defineTest('ore_washing_plant:place', async (ctx) => {
    await place(ctx, ctx.origin, 'ic2_120:ore_washing_plant');
    await assertBlockId(ctx, ctx.origin, 'ic2_120:ore_washing_plant');
  }),

  // 经典配方：粉碎铜 → 纯净铜 + 2 小撮铜粉 + 石粉（三产物 + 数量断言）。
  defineTest('ore_washing_plant:crushed_copper to purified and dusts', async (ctx) => {
    await setupOreWasher(ctx);
    await insertItem(ctx, ctx.origin, 'ic2_120:crushed_copper', 1, 0);
    await waitUntil(ctx, invItemEquals(ctx.origin, 2, 'ic2_120:purified_copper'), 15 * 20);
    await assertSlotHas(ctx, ctx.origin, 2, 'ic2_120:purified_copper');
    await assertSlotHas(ctx, ctx.origin, 3, 'ic2_120:small_copper_dust');
    await assertSlotCount(ctx, ctx.origin, 3, 2);
    await assertSlotHas(ctx, ctx.origin, 4, 'ic2_120:stone_dust');
  }),

  // 无电闲置（有水也开不动）。
  defineTest('ore_washing_plant:no_power:idle', async (ctx) => {
    await place(ctx, ctx.origin, 'ic2_120:ore_washing_plant');
    await fluidInsert(ctx, ctx.origin, 'minecraft:water', WATER_BUCKET_DROPLETS);
    await insertItem(ctx, ctx.origin, 'ic2_120:crushed_copper', 1, 0);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'ic2_120:crushed_copper');
    await assertSlotEmpty(ctx, ctx.origin, 2);
  }),

  // 无水闲置：有电但流体槽抽空，应不工作。
  defineTest('ore_washing_plant:no_water:idle', async (ctx) => {
    await setupOreWasher(ctx);
    const tank = await fluidGet(ctx, ctx.origin, 0);
    if (tank.amount > 0) await fluidExtract(ctx, ctx.origin, tank.amount, 0);
    await insertItem(ctx, ctx.origin, 'ic2_120:crushed_copper', 1, 0);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'ic2_120:crushed_copper');
    await assertSlotEmpty(ctx, ctx.origin, 2);
  }),

  // 非法输入：泥土不是粉碎矿。
  defineTest('ore_washing_plant:invalid_input:dirt', async (ctx) => {
    await setupOreWasher(ctx);
    await insertItem(ctx, ctx.origin, 'minecraft:dirt', 1, 0);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 0, 'minecraft:dirt');
    await assertSlotEmpty(ctx, ctx.origin, 2);
  }),
]);
