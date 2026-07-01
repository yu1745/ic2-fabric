// 移植自 core/src/main/kotlin/ic2_120/content/item/UraniumFuelRods.kt
// 行为：发电 + 脉冲传播 + 产热分配 + 耐久衰减（满则转枯竭）。
// 关键公式：triangularNumber(p) = (p*p+p)*2，产热 = triangular(basePulses+neighborPulses) * cells。

import { FUEL_ROD_PARAMS } from '../constants';
import type { ComponentId, IReactor, IReactorComponent, Slot } from '../types';
import { checkHeatAcceptor, checkPulseable } from './helpers';
import { getComponent } from './resolver';

/** 移植自 UraniumFuelRods.kt:28 */
export function triangularNumber(x: number): number {
  return (x * x + x) * 2;
}

// ===== 铀燃料棒（含双联/四联） =====
class UraniumFuelRodBehavior implements IReactorComponent {
  constructor(public readonly cells: number, public readonly maxUse: number, public readonly depleted: ComponentId) {}

  processChamber(slot: Slot, reactor: IReactor, x: number, y: number, heatRun: boolean): void {
    if (!reactor.produceEnergy()) return;

    // 移植自 UraniumFuelRods.kt:65-69
    const basePulses = 1 + Math.floor(this.cells / 2);
    const neighborPulses =
      checkPulseable(reactor, x - 1, y, slot, x, y, heatRun) +
      checkPulseable(reactor, x + 1, y, slot, x, y, heatRun) +
      checkPulseable(reactor, x, y - 1, slot, x, y, heatRun) +
      checkPulseable(reactor, x, y + 1, slot, x, y, heatRun);

    if (!heatRun) {
      // 电力模式才发电（流体模式不直接产 EU）
      if (!reactor.isFluidCooled()) {
        const totalPulses = (basePulses + neighborPulses) * this.cells;
        reactor.addOutput(totalPulses);
        // pass0 记录真实单步发电（用于全生命周期估算/仪表盘）。铀棒：energy = totalPulses
        reactor.addSlotHeatInfo(x * 9 + y, 0, 0, totalPulses);
      }
    } else {
      const heat0 = triangularNumber(basePulses + neighborPulses) * this.cells;
      let heat = heat0;
      reactor.addHeatProduced(heat);
      // 热量阶段的 slotHeatInfo 仅记产热（发电已在 pass0 记录，这里 energy=0 避免重复累加）
      reactor.addSlotHeatInfo(x * 9 + y, heat, 0, 0);

      // 产热分配：四邻可储热元件平摊，溢出回流，最后剩余进堆温。
      // 移植自 UraniumFuelRods.kt:87-109
      const acceptors: Array<{ slot: Slot; x: number; y: number }> = [];
      checkHeatAcceptor(reactor, x - 1, y, acceptors);
      checkHeatAcceptor(reactor, x + 1, y, acceptors);
      checkHeatAcceptor(reactor, x, y - 1, acceptors);
      checkHeatAcceptor(reactor, x, y + 1, acceptors);

      while (acceptors.length > 0 && heat > 0) {
        const dheat = Math.floor(heat / acceptors.length);
        heat -= dheat;
        const a = acceptors.shift()!;
        const comp = getComponent(a.slot.id)!;
        const overflow = comp.alterHeat(a.slot, reactor, a.x, a.y, dheat);
        heat += overflow;
      }
      if (heat > 0) reactor.addHeat(heat);
    }

    // 耐久衰减（仅 pass 0）：移植自 UraniumFuelRods.kt:112-118
    if (!heatRun) {
      if (slot.use >= this.maxUse - 1) {
        reactor.setItemAt(x, y, { id: this.depleted, use: 0 });
      } else {
        slot.use += 1;
      }
    }
  }

  acceptUraniumPulse(slot: Slot): boolean {
    // 枯竭后不再接受脉冲。移植自 :123-135
    return slot.use < this.maxUse - 1;
  }

  influenceExplosion(): number { return 2 * this.cells; }
  canStoreHeat(): boolean { return false; }
  getMaxHeat(): number { return 0; }
  getCurrentHeat(): number { return 0; }
  alterHeat(): number { return 0; }
}

// ===== MOX 燃料棒：发电随堆温线性放大，流体模式高温下产热 ×2 =====
class MoxFuelRodBehavior implements IReactorComponent {
  constructor(public readonly cells: number, public readonly maxUse: number, public readonly depleted: ComponentId) {}

  processChamber(slot: Slot, reactor: IReactor, x: number, y: number, heatRun: boolean): void {
    if (!reactor.produceEnergy()) return;

    const basePulses = 1 + Math.floor(this.cells / 2);
    const neighborPulses =
      checkPulseable(reactor, x - 1, y, slot, x, y, heatRun) +
      checkPulseable(reactor, x + 1, y, slot, x, y, heatRun) +
      checkPulseable(reactor, x, y - 1, slot, x, y, heatRun) +
      checkPulseable(reactor, x, y + 1, slot, x, y, heatRun);

    if (!heatRun) {
      if (!reactor.isFluidCooled()) {
        const totalPulses = (basePulses + neighborPulses) * this.cells;
        // 移植自 UraniumFuelRods.kt:223-226：MOX 输出 = totalPulses * (4*heatFrac + 1)
        const breedereffectiveness = reactor.getHeat() / reactor.getMaxHeat();
        const reaktorOutput = 4.0 * breedereffectiveness + 1.0;
        const moxOutput = totalPulses * reaktorOutput;
        reactor.addOutput(moxOutput);
        // pass0 记录真实单步发电（含 MOX 堆温倍率），用于全生命周期估算/仪表盘
        reactor.addSlotHeatInfo(x * 9 + y, 0, 0, moxOutput);
      }
    } else {
      const rawHeat = triangularNumber(basePulses + neighborPulses) * this.cells;
      const finalHeat = this.getFinalHeat(reactor, rawHeat);
      let heat = finalHeat;
      reactor.addHeatProduced(heat);
      // 热量阶段仅记产热（发电已在 pass0 记录，energy=0 避免重复累加）
      reactor.addSlotHeatInfo(x * 9 + y, heat, 0, 0);

      const acceptors: Array<{ slot: Slot; x: number; y: number }> = [];
      checkHeatAcceptor(reactor, x - 1, y, acceptors);
      checkHeatAcceptor(reactor, x + 1, y, acceptors);
      checkHeatAcceptor(reactor, x, y - 1, acceptors);
      checkHeatAcceptor(reactor, x, y + 1, acceptors);

      while (acceptors.length > 0 && heat > 0) {
        const dheat = Math.floor(heat / acceptors.length);
        heat -= dheat;
        const a = acceptors.shift()!;
        const comp = getComponent(a.slot.id)!;
        const overflow = comp.alterHeat(a.slot, reactor, a.x, a.y, dheat);
        heat += overflow;
      }
      if (heat > 0) reactor.addHeat(heat);
    }

    if (!heatRun) {
      if (slot.use >= this.maxUse - 1) {
        reactor.setItemAt(x, y, { id: this.depleted, use: 0 });
      } else {
        slot.use += 1;
      }
    }
  }

  /** 移植自 UraniumFuelRods.kt:266-274：流体模式且周期开始堆温占比 > 0.5 时产热翻倍 */
  private getFinalHeat(reactor: IReactor, heat: number): number {
    if (reactor.isFluidCooled()) {
      const breedereffectiveness = reactor.getCycleStartHeat() / reactor.getMaxHeat();
      if (breedereffectiveness > 0.5) return heat * 2;
    }
    return heat;
  }

  acceptUraniumPulse(slot: Slot): boolean {
    return slot.use < this.maxUse - 1;
  }
  influenceExplosion(): number { return 2 * this.cells; }
  canStoreHeat(): boolean { return false; }
  getMaxHeat(): number { return 0; }
  getCurrentHeat(): number { return 0; }
  alterHeat(): number { return 0; }
}

// ===== 枯竭燃料棒：无任何行为（移植自 Depleted*FuelRodItem : AbstractReactorComponent） =====
class DepletedFuelRodBehavior implements IReactorComponent {
  processChamber(): void {}
  acceptUraniumPulse(): boolean { return false; }
  canStoreHeat(): boolean { return false; }
  getMaxHeat(): number { return 0; }
  getCurrentHeat(): number { return 0; }
  alterHeat(): number { return 0; }
  influenceExplosion(): number { return 0; }
}

// ===== 工厂：根据 ComponentId 造出对应的 behavior 实例 =====
const uraniumMap: Record<string, UraniumFuelRodBehavior> = {};
const moxMap: Record<string, MoxFuelRodBehavior> = {};
const depleted = new DepletedFuelRodBehavior();

export function makeFuelRodBehavior(id: ComponentId): IReactorComponent {
  const p = (FUEL_ROD_PARAMS as Record<string, { maxUse: number; cells: number; depleted: ComponentId }>)[id];
  if (id.startsWith('depleted')) return depleted;
  if (!p) return depleted;
  if (id.startsWith('mox') || id.includes('_mox')) {
    if (!moxMap[id]) moxMap[id] = new MoxFuelRodBehavior(p.cells, p.maxUse, p.depleted);
    return moxMap[id];
  }
  if (!uraniumMap[id]) uraniumMap[id] = new UraniumFuelRodBehavior(p.cells, p.maxUse, p.depleted);
  return uraniumMap[id];
}
