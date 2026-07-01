// 验证 MOX 全生命周期发电是否反映堆温（heatFrac = heat/maxHeat）。
import { describe, it, expect } from 'vitest';
import { estimateFullLifeOutput, emptyGrid, EU_PER_OUTPUT, HEAT_CAPACITY } from '../../src/sim';

describe('MOX 全生命周期发电与堆温', () => {
  it('MOX 单步发电随堆温线性放大：堆温 0 vs 5000 vs 10000', () => {
    // 单根 MOX：basePulses=1, neighborPulses=0, totalPulses=(1+0)*1=1
    // 输出 = totalPulses * (4*heatFrac + 1)
    //   heatFrac=0   → 1*(0+1)=1
    //   heatFrac=0.5 → 1*(2+1)=3
    //   heatFrac=1.0 → 1*(4+1)=5
    for (const [heat, expectedEnergy] of [[0, 1], [5000, 3], [10000, 5]] as Array<[number, number]>) {
      const g = emptyGrid(0);
      g[0] = { id: 'mox_fuel_rod', use: 0 };
      const { totalEu, perSlotEu } = estimateFullLifeOutput(g, 0, 'electric', heat);
      const expectedFullLife = expectedEnergy * EU_PER_OUTPUT * 10000; // MOX maxUse=10000
      console.log(`heat=${heat}: totalEu=${totalEu} 期望=${expectedFullLife} (单步energy=${expectedEnergy})`);
      expect(perSlotEu.get(0), `heat=${heat}`).toBe(expectedFullLife);
    }
  });

  it('MOX 堆温 5000 的全生命周期发电应大于堆温 0', () => {
    const g0 = emptyGrid(0); g0[0] = { id: 'mox_fuel_rod', use: 0 };
    const g5 = emptyGrid(0); g5[0] = { id: 'mox_fuel_rod', use: 0 };
    const r0 = estimateFullLifeOutput(g0, 0, 'electric', 0);
    const r5 = estimateFullLifeOutput(g5, 0, 'electric', 5000);
    expect(r5.totalEu).toBeGreaterThan(r0.totalEu);
  });

  it('对比：铀棒发电不随堆温变（仅 MOX 受堆温影响）', () => {
    const gu = emptyGrid(0); gu[0] = { id: 'uranium_fuel_rod', use: 0 };
    const r0 = estimateFullLifeOutput(gu, 0, 'electric', 0);
    const rh = estimateFullLifeOutput(gu, 0, 'electric', 8000);
    // 铀棒发电恒定，与堆温无关
    expect(rh.totalEu).toBe(r0.totalEu);
    void HEAT_CAPACITY;
  });
});
