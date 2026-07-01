// Parity B: 热交换器白盒单周期测试。
//
// 热交换器是唯一「自派生状态」的元件（根据邻居温度演化），长周期才收敛，且 tickOffset 随机
// 导致 mcdebug 抓收敛点不稳定。故改用代数手算单周期 Δ 验证移植正确性——这比长周期对拍更精确，
// 也更能抓住分层节流那几行（最容易抄错）的 bug。
//
// 验证策略：构造「只有 switchReactor、switchSide=0」的 reactor_heat_exchanger，
// 使其只有一个交换对象（反应堆本体），把问题降为一维，按 ReactorHeatExchangers.kt:126-154
// 逐步手算 expected，与 simulator 单周期结果逐项比对。
//
// 关键公式（ReactorHeatExchangers.kt:126-154，注意注释误导但逻辑如此）：
//   mymed = use/maxUse * 100          （百分比，0..100）
//   reactorMed = heat/maxHeat * 100
//   add = round(maxHeat/100 * (reactorMed + mymed/2))，clamp 到 ±switchReactor
//   avg = reactorMed + mymed/2         （量纲 0..150）
//   if avg < 1.0: add = floor(switchReactor/2)
//   if avg < 0.75: add = floor(switchReactor/4)
//   if avg < 0.5: add = floor(switchReactor/8)
//   if avg < 0.25: add = 1
//   方向：reactorR > myR → add = -add（热量流入交换器）；相等 → add=0
import { describe, it, expect } from 'vitest';
import { simulateCycle, emptyGrid, HEAT_CAPACITY } from '../../src/sim';
import type { Slot } from '../../src/sim';

// 单独构造一个最小 reactor：只有 1 个 reactor_heat_exchanger 在 slot 0。
// switchSide=0 → 不与任何邻居元件交换（即使有邻居）；switchReactor=72 → 只与反应堆本体交换。
// 这样 myHeatDelta 完全由反应堆交换那一支决定，可手算。

describe('parity B: 热交换器白盒单周期（reactor_heat_exchanger, switchSide=0/switchReactor=72）', () => {
  // 用例：[堆温, 交换器初始 use, 描述]
  // 手算依据上面公式；reactor_heat_exchanger maxUse=5000。
  const maxUse = 5000;
  const switchReactor = 72;
  const maxHeat = HEAT_CAPACITY; // 无隔板，10000

  // 辅助手算函数（独立实现一份，与 sim 对照）
  function handCalc(reactorHeat: number, exUse: number): { add: number; expectedExUseAfter: number; expectedReactorHeatAfter: number } {
    const mymed = (exUse * 100.0) / maxUse;
    const reactorMed = (reactorHeat * 100.0) / maxHeat;
    let add = Math.round((maxHeat / 100.0) * (reactorMed + mymed / 2));
    add = Math.max(-switchReactor, Math.min(switchReactor, add));
    const avg = reactorMed + mymed / 2;
    if (avg < 1.0) add = Math.floor(switchReactor / 2);
    if (avg < 0.75) add = Math.floor(switchReactor / 4);
    if (avg < 0.5) add = Math.floor(switchReactor / 8);
    if (avg < 0.25) add = 1;
    const reactorR = Math.round(reactorMed * 10) / 10.0;
    const myR = Math.round(mymed * 10) / 10.0;
    if (reactorR > myR) add = -add;
    else if (reactorR === myR) add = 0;
    // add > 0：热量从交换器流向反应堆（交换器 use 减少，反应堆 heat 增加）
    // add < 0：热量从反应堆流入交换器（交换器 use 增加，反应堆 heat 减少）
    // myHeatDelta -= add → 交换器 use += myHeatDelta = -add
    // 反应堆 heat += add（clamp 0..maxHeat）
    return {
      add,
      expectedExUseAfter: exUse - add, // myHeatDelta = -add，alterHeat(myHeatDelta) → use += -add
      expectedReactorHeatAfter: Math.max(0, Math.min(maxHeat, reactorHeat + add)),
    };
  }

  const cases: Array<[number, number, string]> = [
    [5000, 0, '热堆(5000) 冷交换器(0)：avg 大，热量应流入交换器（add<0）'],
    [1000, 4000, '反应堆 1000，交换器 4000（80%）：交换器更热，热量流向反应堆'],
    [200, 100, '低温区：avg 可能 <1.0，触发 /2 档'],
    [50, 10, '极低温：avg <0.25，add=1'],
    [0, 2500, '冷堆热交换器：交换器更热，流向反应堆'],
    [3000, 3000, '接近平衡：reactorR 可能 == myR → add=0'],
  ];

  for (const [reactorHeat, exUse, desc] of cases) {
    it(`${desc} [堆温=${reactorHeat}, ex.use=${exUse}]`, () => {
      const expected = handCalc(reactorHeat, exUse);

      const grid = emptyGrid(0);
      const exSlot: Slot = { id: 'reactor_heat_exchanger', use: exUse };
      grid[0] = exSlot;

      // simulateCycle 会深拷贝 grid，不污染输入；起始堆温 = reactorHeat
      const res = simulateCycle(grid, 0, 'electric', reactorHeat);
      const after = res.grid[0];

      // 交换器 use 应精确等于手算
      expect(after?.use).toBe(expected.expectedExUseAfter);
      // 反应堆堆温应精确等于手算
      expect(res.heat).toBe(expected.expectedReactorHeatAfter);
    });
  }

  it('switchSide 通路：component_heat_exchanger(switchSide=36) 与冷却单元邻接交换', () => {
    // component_heat_exchanger: switchSide=36, switchReactor=0 → 只与邻居元件交换，不碰反应堆
    // 布局：slot0 = component_heat_exchanger(use=1000), slot9=(col1,row0) = 10k 冷却单元(use=5000)
    // 两元件左右相邻，都 canStoreHeat。
    // 手算（switchSide 通路，maxUse_ex=5000, maxUse_cc=10000）：
    const exUse0 = 1000, ccUse0 = 5000;
    const exMax = 5000, ccMax = 10000;
    const mymed = (exUse0 * 100.0) / exMax;       // 20
    const othermed = (ccUse0 * 100.0) / ccMax;    // 50
    const switchSide = 36;
    let add = Math.round((ccMax / 100.0) * (othermed + mymed / 2)); // 100*(50+10)=6000
    add = Math.max(-switchSide, Math.min(switchSide, add)); // clamp 36
    const avg = othermed + mymed / 2; // 60 → 不触发任何档位（都 >1.0）
    void avg;
    const otherR = Math.round(othermed * 10) / 10.0; // 50
    const myR = Math.round(mymed * 10) / 10.0;       // 20
    // otherR(50) > myR(20) → add = -add（热量流入交换器）
    if (otherR > myR) add = -add; // -36
    const expectedExUseAfter = exUse0 - add; // 1000 - (-36) = 1036
    const expectedCcUseAfter = ccUse0 + add; // 5000 + (-36) = 4964

    const grid = emptyGrid(0);
    grid[0] = { id: 'component_heat_exchanger', use: exUse0 };
    grid[9] = { id: 'reactor_coolant_cell', use: ccUse0 };
    const res = simulateCycle(grid, 0, 'electric', 0);

    expect(res.grid[0]?.use).toBe(expectedExUseAfter); // 1036
    expect(res.grid[9]?.use).toBe(expectedCcUseAfter); // 4964
    // switchReactor=0 → 堆温不变
    expect(res.heat).toBe(0);
  });
});
