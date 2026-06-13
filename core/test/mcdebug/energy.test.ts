// 能源设备（发电机 / 储能）测试。
//
// 这些是简单的“放置 + 读写”类用例，不跑完整配方。
// `defineTest` 直接导出，runner 会自动 collect 进来。
import {
  assertBlockId,
  assertSlotEmpty,
  beFieldGreaterThan,
  defineTest,
  defineTests,
  getBeNumber,
  insertItem,
  place,
  setBeField,
  waitUntil,
} from "@yu1745/mcdebug";

// 发电机：基础放置。
export const generatorPlace = defineTest('generator:place', async (ctx) => {
  await place(ctx, ctx.origin, 'ic2_120:generator');
  await assertBlockId(ctx, ctx.origin, 'ic2_120:generator');
});

// 发电机烧煤：放 1 个煤，等若干 tick，EnergyStored 必须 > 0 且输入槽被烧光。
export const generatorBurnCoal = defineTest('generator:burn coal to EU', async (ctx) => {
  await place(ctx, ctx.origin, 'ic2_120:generator');
  await insertItem(ctx, ctx.origin, 'minecraft:coal', 1, 0);
  await waitUntil(ctx, beFieldGreaterThan(ctx.origin, 'EnergyStored', 0), 5 * 20);
  const energy = await getBeNumber(ctx, ctx.origin, 'EnergyStored');
  if (energy <= 0) throw new Error(`expected generator to produce energy, got ${energy}`);
  await assertSlotEmpty(ctx, ctx.origin, 0);
});

// BatBox：基础放置。
export const batboxPlace = defineTest('batbox:place', async (ctx) => {
  await place(ctx, ctx.origin, 'ic2_120:batbox');
  await assertBlockId(ctx, ctx.origin, 'ic2_120:batbox');
});

// BatBox：写入指定能量值并能原样读回（验证 BE NBT 字段通路）。
export const batboxEnergyReadback = defineTest('batbox:store and readback energy', async (ctx) => {
  await place(ctx, ctx.origin, 'ic2_120:batbox');
  await setBeField(ctx, ctx.origin, 'EnergyStored', 12345);
  const energy = await getBeNumber(ctx, ctx.origin, 'EnergyStored');
  if (energy !== 12345) throw new Error(`expected batbox EnergyStored = 12345, got ${energy}`);
});

// 中等与大型储能（CESU/MFE/MFSU）：批量跑“放置”用例。
// 用 `defineTests([...].map(...))` 共享同一份测试体，避免重复代码。
export const storagePlaceTests = defineTests(
  [
    ['cesu:place', 'ic2_120:cesu'],
    ['mfe:place', 'ic2_120:mfe'],
    ['mfsu:place', 'ic2_120:mfsu'],
  ].map(([name, block]) =>
    defineTest(name, async (ctx) => {
      await place(ctx, ctx.origin, block);
      await assertBlockId(ctx, ctx.origin, block);
    }),
  ),
);
