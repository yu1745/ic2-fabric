// 变压器 (Transformer) 测试。
//
// 变压器用 BE 字段 `Mode` 切换升/降压：
//   mode = 0 → step-up   LV → MV
//   mode = 1 → step-down MV → LV
//
// 变压器需要两侧各接一个不同等级的储能：升压时西侧 BatBox（LV）放出，
// 东侧 CESU（MV）充入；降压时反过来。
import {
  assertBlockId,
  defineTest,
  defineTests,
  getBeNumber,
  place,
  type Pos,
  setBeField,
  setBlocks,
  waitTicks,
} from "@yu1745/mcdebug";
import { type TestContext } from "./helpers.js";

/**
 * 升压摆设：西 BatBox (LV) → 变压器 (mode=0) → 东 CESU (MV)。
 * 返回两端坐标方便用例在断言时直接引用。
 */
async function setupTransformerStepUp(ctx: TestContext): Promise<{ west: Pos; east: Pos }> {
  const west = ctx.origin.west();
  const east = ctx.origin.east();
  await setBlocks(ctx, [
    { pos: west, block: 'ic2_120:batbox', props: { facing: 'east' } },
    { pos: ctx.origin, block: 'ic2_120:lv_transformer', props: { facing: 'east' } },
    { pos: east, block: 'ic2_120:cesu', props: { facing: 'north' } },
  ]);
  await setBeField(ctx, west, 'EnergyStored', 10000);
  await setBeField(ctx, east, 'EnergyStored', 0);
  await setBeField(ctx, ctx.origin, 'Mode', 0);
  return { west, east };
}

export const transformerTests = defineTests([
  // 升压：等 100 tick 后 CESU 应有能量流入，BatBox 能量减少。
  // 失败时报错会附带两侧 BE state / energy 方便排查（e.g. facing 错误）。
  defineTest('transformer:step_up LV to MV', async (ctx) => {
    const { west, east } = await setupTransformerStepUp(ctx);
    const batboxState = await ctx.api.world.getBlock(west);
    const transformerState = await ctx.api.world.getBlock(ctx.origin);
    const cesuState = await ctx.api.world.getBlock(east);
    const startCesuEnergy = await getBeNumber(ctx, east, 'EnergyStored');
    await waitTicks(ctx, 100);
    const endCesuEnergy = await getBeNumber(ctx, east, 'EnergyStored');
    const endBatboxEnergy = await getBeNumber(ctx, west, 'EnergyStored');
    const endTransformerEnergy = await getBeNumber(ctx, ctx.origin, 'EnergyStored');
    if (endCesuEnergy <= startCesuEnergy) {
      throw new Error(`expected CESU to gain energy, before=${startCesuEnergy} after=${endCesuEnergy}
  batbox.state=${JSON.stringify(batboxState.state)}
  transformer.state=${JSON.stringify(transformerState.state)}
  cesu.state=${JSON.stringify(cesuState.state)}
  batbox.energy=${endBatboxEnergy}
  transformer.energy=${endTransformerEnergy}`);
    }
    if (endBatboxEnergy > 10000) throw new Error(`BatBox should have lost some energy, got ${endBatboxEnergy}`);
  }),

  // 降压：CESU (MV) → 变压器 (mode=1) → BatBox (LV)，等 100 tick 后 BatBox 应有能量流入。
  defineTest('transformer:step_down MV to LV', async (ctx) => {
    const west = ctx.origin.west();
    const east = ctx.origin.east();
    await setBlocks(ctx, [
      { pos: west, block: 'ic2_120:batbox', props: { facing: 'north' } },
      { pos: ctx.origin, block: 'ic2_120:lv_transformer', props: { facing: 'east' } },
      { pos: east, block: 'ic2_120:cesu', props: { facing: 'west' } },
    ]);
    await setBeField(ctx, west, 'EnergyStored', 0);
    await setBeField(ctx, east, 'EnergyStored', 10000);
    await setBeField(ctx, ctx.origin, 'Mode', 1);
    const startBatboxEnergy = await getBeNumber(ctx, west, 'EnergyStored');
    await waitTicks(ctx, 100);
    const endBatboxEnergy = await getBeNumber(ctx, west, 'EnergyStored');
    if (endBatboxEnergy <= startBatboxEnergy) {
      throw new Error(`expected BatBox to gain energy via step-down, before=${startBatboxEnergy} after=${endBatboxEnergy}`);
    }
  }),

  // 基础放置：连续放置 LV/MV/HV/EV 四种变压器，验证方块 id。
  defineTest('transformer:place lv mv hv ev', async (ctx) => {
    await place(ctx, ctx.origin, 'ic2_120:lv_transformer');
    await assertBlockId(ctx, ctx.origin, 'ic2_120:lv_transformer');
    await place(ctx, ctx.origin.east(2), 'ic2_120:mv_transformer');
    await assertBlockId(ctx, ctx.origin.east(2), 'ic2_120:mv_transformer');
    await place(ctx, ctx.origin.east(3), 'ic2_120:hv_transformer');
    await assertBlockId(ctx, ctx.origin.east(3), 'ic2_120:hv_transformer');
    await place(ctx, ctx.origin.east(4), 'ic2_120:ev_transformer');
    await assertBlockId(ctx, ctx.origin.east(4), 'ic2_120:ev_transformer');
  }),
]);
