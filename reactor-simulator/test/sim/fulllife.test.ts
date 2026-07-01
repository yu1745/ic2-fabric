// 验证 estimateFullLifeOutput 的语义：单步发电 × 总寿命（满→空），含布局脉冲加成。
import { describe, it, expect } from 'vitest';
import { estimateFullLifeOutput, emptyGrid, EU_PER_OUTPUT } from '../../src/sim';

describe('全生命周期发电估算（单步×总寿命）', () => {
  it('单根铀棒：单步 energy=1，总寿命 20000 → 1*100*20000 = 2,000,000 EU', () => {
    const g = emptyGrid(0);
    g[0] = { id: 'uranium_fuel_rod', use: 0 };
    const { totalEu, perSlotEu } = estimateFullLifeOutput(g, 0, 'electric', 0);
    // 单根无邻接：basePulses=1, neighborPulses=0, totalPulses=(1+0)*1=1 → energy=1
    // 全生命周期 = 1 * EU_PER_OUTPUT(100) * maxUse(20000) = 2,000,000
    expect(totalEu).toBe(1 * EU_PER_OUTPUT * 20000);
    expect(perSlotEu.get(0)).toBe(1 * EU_PER_OUTPUT * 20000);
  });

  it('两根相邻铀棒：互脉冲，单根 energy=2 > 单根的 1', () => {
    const g = emptyGrid(0);
    g[0] = { id: 'uranium_fuel_rod', use: 0 };
    g[1] = { id: 'uranium_fuel_rod', use: 0 }; // 上下相邻（同列 row0/row1）
    const { totalEu, perSlotEu } = estimateFullLifeOutput(g, 0, 'electric', 0);
    // 每根：basePulses=1, neighborPulses=1（邻棒）→ totalPulses=(1+1)*1=2 → energy=2
    // 两根各自全生命周期 = 2*100*20000 = 4,000,000，合计 8,000,000
    const perRod = 2 * EU_PER_OUTPUT * 20000;
    expect(perSlotEu.get(0)).toBe(perRod);
    expect(perSlotEu.get(1)).toBe(perRod);
    expect(totalEu).toBe(perRod * 2);
    // 关键：必须大于单根布局（脉冲加成生效）
    expect(totalEu).toBeGreaterThan(1 * EU_PER_OUTPUT * 20000 * 2);
  });

  it('燃料棒耐久不影响结果（use=15000 仍按满寿命 20000 算）', () => {
    const g1 = emptyGrid(0);
    g1[0] = { id: 'uranium_fuel_rod', use: 0 };
    const g2 = emptyGrid(0);
    g2[0] = { id: 'uranium_fuel_rod', use: 15000 }; // 已用 75%
    const r1 = estimateFullLifeOutput(g1, 0, 'electric', 0);
    const r2 = estimateFullLifeOutput(g2, 0, 'electric', 0);
    // 无论当前耐久，全生命周期发电相同（都是满寿命潜力）
    expect(r2.totalEu).toBe(r1.totalEu);
  });

  it('铀棒 + 反射板：脉冲 +1，发电高于单独铀棒', () => {
    const g = emptyGrid(3);
    g[0] = { id: 'uranium_fuel_rod', use: 0 };       // (col0,row0)
    g[9] = { id: 'neutron_reflector', use: 0 };      // (col1,row0) 左右相邻
    const { totalEu } = estimateFullLifeOutput(g, 3, 'electric', 0);
    // 反射板给 +1 脉冲 → energy=2 → 全生命周期 = 2*100*20000
    expect(totalEu).toBe(2 * EU_PER_OUTPUT * 20000);
  });
});
