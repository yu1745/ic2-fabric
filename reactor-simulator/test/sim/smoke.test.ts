// 冒烟测试：确认 sim 层能跑、数值合理（非对拍，对拍见 parity.test.ts）。
import { describe, it, expect } from 'vitest';
import { simulateCycle, emptyGrid, triangularNumber } from '../../src/sim';

describe('smoke: sim 层基本可用', () => {
  it('空网格不产热不发电', () => {
    const grid = emptyGrid(0); // 3x9
    const { stats } = simulateCycle(grid, 0, 'electric', 0);
    expect(stats.heatProduced).toBe(0);
    expect(stats.euPerTick).toBe(0);
    expect(stats.netHeat).toBe(0);
    expect(stats.hasFuelRods).toBe(false);
  });

  it('triangularNumber 与 Kotlin 一致：(p*p+p)*2', () => {
    // triangularNumber(1) = (1+1)*2 = 4
    expect(triangularNumber(1)).toBe(4);
    // triangularNumber(2) = (4+2)*2 = 12
    expect(triangularNumber(2)).toBe(12);
    // triangularNumber(4) = (16+4)*2 = 40
    expect(triangularNumber(4)).toBe(40);
  });

  it('单根铀棒（无邻接脉冲）：产热 = triangular(1)*1 = 4', () => {
    // 网格 3x9，把铀棒放 (0,0) = slot index 0
    const grid = emptyGrid(0);
    grid[0] = { id: 'uranium_fuel_rod', use: 0 };
    const { stats } = simulateCycle(grid, 0, 'electric', 0);
    // basePulses = 1 + 1/2 = 1, neighborPulses = 0, heat = triangular(1)*1 = 4
    expect(stats.heatProduced).toBe(4);
    // 发电：totalPulses = (1+0)*1 = 1, eu = 1*100/20 = 5 EU/t
    expect(stats.euPerTick).toBe(5);
  });

  it('网格列优先索引：(x,y) -> x*9+y', () => {
    // 放到 (1, 2) = slot 1*9+2 = 11
    const grid = emptyGrid(0);
    grid[11] = { id: 'uranium_fuel_rod', use: 0 };
    const { stats } = simulateCycle(grid, 0, 'electric', 0);
    expect(stats.heatProduced).toBe(4);
    // slot 11 应有产热记录
    expect(stats.slotHeat.get(11)?.produced).toBe(4);
  });
});
