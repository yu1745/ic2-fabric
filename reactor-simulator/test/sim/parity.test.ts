// Parity 测试：simulator 与游戏内 mcdebug golden values 对拍。
//
// 分两组：
//   A. 快照对拍（稳态元件：燃料棒/散热片/冷却单元/反射板）—— 2 cycle 已稳态，golden 可信。
//   B. 热交换器白盒单周期测试 —— 热交换器是唯一"自派生状态"的元件，长周期才收敛，
//      且 tickOffset 随机导致 mcdebug 抓收敛点不稳定；改用代数手算单周期 Δ 验证移植正确性。
import { describe, it, expect } from 'vitest';
import { simulateCycle, runCycles, emptyGrid } from '../../src/sim';
import type { ComponentId } from '../../src/sim';
import { GOLDEN } from './golden-values';

// 把 golden layout ([slot, "uranium_fuel_rod"]) 转成 simulator grid
function buildGrid(layout: Array<[number, string]>, chambers = 0): ReturnType<typeof emptyGrid> {
  const g = emptyGrid(chambers);
  for (const [slot, id] of layout) {
    g[slot] = { id: id as ComponentId, use: 0 };
  }
  return g;
}

// ===== A. 快照对拍：稳态元件 =====
describe('parity A: 稳态元件 2-cycle 快照对拍', () => {
  it('empty: 不产热不发电', () => {
    const c = GOLDEN.find((x) => x.label === 'empty_sanity')!;
    const g = buildGrid(c.layout);
    const { stats } = simulateCycle(g, 0, 'electric', 0);
    expect(stats.heatProduced).toBe(0);
    expect(stats.netHeat).toBe(0);
  });

  it('single_uranium_rod: 单周期产热 = 4 (= triangular(1)*1)', () => {
    // golden: 2 cycle 后堆温 8 → 每周期 +4。但单根棒无散热，堆温应线性增长。
    const c = GOLDEN.find((x) => x.label === 'single_uranium_rod_slot0')!;
    expect(c.heat).toBe(8); // sanity: golden 本身
    expect(c.cycles).toBe(2);
    const g = buildGrid(c.layout);
    // 跑 2 个 cycle，堆温应 = 8（无散热片，净热全进堆温）
    const res = runCycles(g, 0, 'electric', 0, 2);
    expect(res.heat).toBe(c.heat);
  });

  it('two_adjacent_uranium_rods: 互脉冲，每根 basePulses=1 + neighborPulses=1', () => {
    // 每根：pulses = 1 + 1(邻) = 2，heat = triangular(2)*1 = 12；两根共 24/周期
    const c = GOLDEN.find((x) => x.label === 'two_adjacent_uranium_rods')!;
    expect(c.heat).toBe(48); // 2 cycle * 24
    const g = buildGrid(c.layout);
    const res = runCycles(g, 0, 'electric', 0, 2);
    expect(res.heat).toBe(c.heat);
  });

  it('four_stacked_uranium_rods: 4 根同列相邻，端点 1 邻、中间 2 邻', () => {
    // slot0,1,2,3 = col0,row0..3（同列上下相邻）
    // slot0(端): 邻 slot1 → pulses=2, heat=triangular(2)=12
    // slot1(中): 邻 slot0,slot2 → pulses=3, heat=triangular(3)=(9+3)*2=24
    // slot2(中): 邻 slot1,slot3 → pulses=3, heat=24
    // slot3(端): 邻 slot2 → pulses=2, heat=12
    // 合计 12+24+24+12 = 72/周期；2 cycle = 144 ✓ 与 golden 一致
    const c = GOLDEN.find((x) => x.label === 'four_stacked_uranium_rods')!;
    expect(c.heat).toBe(144);
    const g = buildGrid(c.layout);
    const res = runCycles(g, 0, 'electric', 0, 2);
    expect(res.heat).toBe(c.heat);
  });

  it('dual_uranium_rod: cells=2，basePulses=1+1=2，heat=triangular(2)*2=24', () => {
    // 无邻接脉冲：pulses=2, heat = triangular(2)*2 = 12*2 = 24/周期；2 cycle = 48 ✓
    const c = GOLDEN.find((x) => x.label === 'dual_uranium_rod_slot0')!;
    expect(c.heat).toBe(48);
    const g = buildGrid(c.layout);
    const res = runCycles(g, 0, 'electric', 0, 2);
    expect(res.heat).toBe(c.heat);
  });

  it('quad_uranium_rod: cells=4，basePulses=1+2=3，heat=triangular(3)*4=48', () => {
    // pulses=3, heat = triangular(3)*4 = 24*4 = 96/周期；2 cycle = 192 ✓
    const c = GOLDEN.find((x) => x.label === 'quad_uranium_rod_slot0')!;
    expect(c.heat).toBe(192);
    const g = buildGrid(c.layout);
    const res = runCycles(g, 0, 'electric', 0, 2);
    expect(res.heat).toBe(c.heat);
  });

  it('rod_plus_reflector: 反射板给 +1 脉冲，pulses=2, heat=triangular(2)=12', () => {
    // slot0 铀棒，slot9=(col1,row0) 反射板，左右相邻
    // pulses = 1(base) + 1(reflector) = 2, heat = triangular(2)*1 = 12/周期；2 cycle = 24 ✓
    const c = GOLDEN.find((x) => x.label === 'rod_plus_reflector')!;
    expect(c.heat).toBe(24);
    const g = buildGrid(c.layout);
    const res = runCycles(g, 0, 'electric', 0, 2);
    expect(res.heat).toBe(c.heat);
    // 反射板 use = 2 cycle * cells(1) = 2 ✓
    expect(res.grid[9]?.use).toBe(c.slotUses['9'].use);
  });

  it('rod_plus_vent: 散热片吸走燃料棒产热，堆温=0', () => {
    // slot0 铀棒(产 4/cycle)，slot9 散热片(canStoreHeat，吸 4)
    // 燃料棒产热分配给散热片 → 散热片 selfVent=6 ≥ 4，每周期蒸发掉，不进堆温
    const c = GOLDEN.find((x) => x.label === 'rod_plus_vent')!;
    expect(c.heat).toBe(0);
    const g = buildGrid(c.layout);
    const res = runCycles(g, 0, 'electric', 0, 2);
    expect(res.heat).toBe(c.heat);
    // 散热片 use 应为 0（吸 4 蒸发 6，始终不满）
    expect(res.grid[9]?.use).toBe(c.slotUses['9'].use);
  });

  it('rod_plus_coolant_cell: 冷却单元吸走产热，堆温=0，冷却单元 use=8', () => {
    // slot0 铀棒(产 4/cycle)，slot9 10k 冷却单元(canStoreHeat)
    // 2 cycle 共产 8，冷却单元无散热，全吸收 → use=8 ✓
    const c = GOLDEN.find((x) => x.label === 'rod_plus_coolant_cell')!;
    expect(c.heat).toBe(0);
    const g = buildGrid(c.layout);
    const res = runCycles(g, 0, 'electric', 0, 2);
    expect(res.heat).toBe(c.heat);
    expect(res.grid[9]?.use).toBe(c.slotUses['9'].use); // 8
  });

  it('燃料棒耐久：2 cycle 后 use=2', () => {
    const c = GOLDEN.find((x) => x.label === 'single_uranium_rod_slot0')!;
    const g = buildGrid(c.layout);
    const res = runCycles(g, 0, 'electric', 0, 2);
    expect(res.grid[0]?.use).toBe(2);
    expect(res.grid[0]?.id).toBe('uranium_fuel_rod'); // 未枯竭
  });
});
