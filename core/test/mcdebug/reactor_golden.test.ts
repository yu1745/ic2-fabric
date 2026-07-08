// 核反应堆 golden value 提取测试。
//
// 目的：为 reactor-simulator/ 的 TS 移植提供对拍基准值。
// 这些测试**不做断言**（除了 sanity），只把每个布局跑出来的精确数值（HeatStored / EnergyStored / 各槽 NBT use）
// 打印到日志，供 simulator 的 vitest parity 测试对拍。
//
// 运行：./gradlew :core:mcdebugTest   然后从输出日志里抓 "GOLDEN" 行。
//
// 网格索引：slot = x*9 + y（x=列 0..2 基础反应堆，y=行 0..8）
import {
  defineTest,
  defineTests,
  getBeNumber,
  place,
  setBlocks,
  setSlot,
  waitTicks,
  waitUntil,
  beFieldGreaterThan,
} from "@yu1745/mcdebug";

const REACTOR = 'ic2_120:nuclear_reactor';

// 通用：放反应堆 + 相邻红石块（启动）→ 填槽 → 等至少 2 个完整 cycle（tickOffset 随机 0..19）
// 读取 HeatStored（堆温 0..10000）、EnergyStored（EU 缓存 0..1_000_000）
// 以及每个 grid 槽（0..80）里物品的 NBT "use"（= 已用 cycle 数 / 已存热量 / 已消耗脉冲数）。
async function readReactor(ctx: any, label: string, slotsToFill: Array<[number, string]>) {
  await place(ctx, ctx.origin, REACTOR);
  // 反应堆永远是有红石信号才运行（直接读 isReceivingRedstonePower，不响应反转升级）
  await setBlocks(ctx, [{ pos: ctx.origin.east(), block: 'minecraft:redstone_block' }]);
  for (const [slot, item] of slotsToFill) {
    await setSlot(ctx, ctx.origin, slot, item, 1);
  }
  // 等到产出 EU（电模式）；最多 80 tick（4 cycle，留余量给 tickOffset + EU 缓慢放出）
  await waitUntil(ctx, beFieldGreaterThan(ctx.origin, 'EnergyStored', 0), 80);
  // 再多等一个完整 cycle，让数值稳定
  await waitTicks(ctx, 20);

  const heat = await getBeNumber(ctx, ctx.origin, 'HeatStored');
  const energy = await getBeNumber(ctx, ctx.origin, 'EnergyStored');

  // 读取每个非空槽的 use 值
  const slotUses: Record<string, { item: string; use: number }> = {};
  for (const [slotIdx] of slotsToFill) {
    try {
      const stack = await (await import('@yu1745/mcdebug')).getSlot(ctx, ctx.origin, slotIdx);
      const use = (stack.nbt as any)?.use ?? (stack.nbt as any)?.Use ?? null;
      slotUses[slotIdx] = { item: stack.item ?? '(empty)', use: use === null ? -1 : Number(use) };
    } catch {
      // 槽可能因组件损坏而空
      slotUses[slotIdx] = { item: '(empty/burned)', use: -1 };
    }
  }

  // GOLDEN 行：simulator 解析用
  console.log(`GOLDEN\t${label}\theat=${heat}\tenergy=${energy}\tslots=${JSON.stringify(slotUses)}`);
  return { heat, energy, slotUses };
}

// ===== 布局 1：单根铀棒 (slot 0 = col0,row0)，无邻接脉冲 =====
export const reactorGoldenSingleRod = defineTest('reactor:golden single uranium rod', async (ctx) => {
  await readReactor(ctx, 'single_uranium_rod_slot0', [[0, 'ic2_120:uranium_fuel_rod']]);
});

// ===== 布局 2：两根相邻铀棒互相脉冲 (slot 0 = col0,row0 与 slot 1 = col0,row1 是上下邻接) =====
// slot index = x*9+y：slot0=(0,0), slot1=(0,1) → 同列相邻行，4邻接 ✓
export const reactorGoldenTwoAdjacentRods = defineTest('reactor:golden two adjacent uranium rods', async (ctx) => {
  await readReactor(ctx, 'two_adjacent_uranium_rods', [
    [0, 'ic2_120:uranium_fuel_rod'],
    [1, 'ic2_120:uranium_fuel_rod'],
  ]);
});

// ===== 布局 3：铀棒 + 铀棒 + 铀棒 + 铀棒（一字排开，4 根同列相邻）=====
// slot 0,1,2,3 = col0,row0..3
export const reactorGoldenFourStackedRods = defineTest('reactor:golden four stacked uranium rods', async (ctx) => {
  await readReactor(ctx, 'four_stacked_uranium_rods', [
    [0, 'ic2_120:uranium_fuel_rod'],
    [1, 'ic2_120:uranium_fuel_rod'],
    [2, 'ic2_120:uranium_fuel_rod'],
    [3, 'ic2_120:uranium_fuel_rod'],
  ]);
});

// ===== 布局 4：单根铀棒 + 散热片邻接（测散热片从燃料棒吸热）=====
// slot0=(0,0) 铀棒, slot9=(1,0) 散热片（col1,row0，与铀棒左右相邻）
export const reactorGoldenRodWithVent = defineTest('reactor:golden uranium rod + heat vent', async (ctx) => {
  await readReactor(ctx, 'rod_plus_vent', [
    [0, 'ic2_120:uranium_fuel_rod'],
    [9, 'ic2_120:heat_vent'],
  ]);
});

// ===== 布局 5：双联铀棒（cells=2）=====
export const reactorGoldenDualRod = defineTest('reactor:golden dual uranium rod', async (ctx) => {
  await readReactor(ctx, 'dual_uranium_rod_slot0', [[0, 'ic2_120:dual_uranium_fuel_rod']]);
});

// ===== 布局 6：四联铀棒（cells=4）=====
export const reactorGoldenQuadRod = defineTest('reactor:golden quad uranium rod', async (ctx) => {
  await readReactor(ctx, 'quad_uranium_rod_slot0', [[0, 'ic2_120:quad_uranium_fuel_rod']]);
});

// ===== 布局 7：铀棒 + 中子反射板邻接（测反射板脉冲 +1 与耐久消耗）=====
// slot0=(0,0) 铀棒, slot9=(1,0) 中子反射板
export const reactorGoldenRodWithReflector = defineTest('reactor:golden uranium rod + neutron reflector', async (ctx) => {
  await readReactor(ctx, 'rod_plus_reflector', [
    [0, 'ic2_120:uranium_fuel_rod'],
    [9, 'ic2_120:neutron_reflector'],
  ]);
});

// ===== 布局 8：铀棒 + 10k 冷却单元（测冷却单元吸热与 use 增长）=====
// slot0=(0,0) 铀棒, slot9=(1,0) 10k 冷却单元
export const reactorGoldenRodWithCoolantCell = defineTest('reactor:golden uranium rod + 10k coolant cell', async (ctx) => {
  await readReactor(ctx, 'rod_plus_coolant_cell', [
    [0, 'ic2_120:uranium_fuel_rod'],
    [9, 'ic2_120:reactor_coolant_cell'],
  ]);
});

// ===== 布置 sanity：确认反应堆能放置且无元件时堆温恒为 0 =====
export const reactorGoldenEmpty = defineTest('reactor:golden empty (sanity)', async (ctx) => {
  await place(ctx, ctx.origin, REACTOR);
  await waitTicks(ctx, 40);
  const heat = await getBeNumber(ctx, ctx.origin, 'HeatStored');
  const energy = await getBeNumber(ctx, ctx.origin, 'EnergyStored');
  console.log(`GOLDEN\tempty_sanity\theat=${heat}\tenergy=${energy}\tslots={}`);
  if (heat !== 0) throw new Error(`empty reactor heat should be 0, got ${heat}`);
});

// 批量占位（确保 runner 一定 collect 到）
export const reactorGoldenBatch = defineTests([]);
