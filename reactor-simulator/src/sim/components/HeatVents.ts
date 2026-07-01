// 移植自 core/src/main/kotlin/ic2_120/content/item/ReactorHeatVents.kt
// 散热片：从堆温吸收（reactorVent）+ 自身蒸发（selfVent，真正的散失热量）。
// component_heat_vent 不储热，向四邻可储热元件各蒸发 sideVent。
// 注意 reactorVent 用 getEffectiveHeatForDrain（堆温+emitBuffer）防止同周期多片重复扣。

import { COMPONENT_VENT_SIDE_VENT, VENT_PARAMS } from '../constants';
import type { ComponentId, IReactor, IReactorComponent, Slot } from '../types';

// ===== 储热型散热片（heat_vent / reactor_heat_vent / advanced_heat_vent / overclocked_heat_vent） =====
class HeatVentBehavior implements IReactorComponent {
  constructor(public readonly heatStorage: number, public readonly selfVent: number, public readonly reactorVent: number) {}

  hasSelfVent(): boolean { return this.selfVent > 0; }

  acceptUraniumPulse(): boolean { return false; }
  canStoreHeat(): boolean { return true; }
  getMaxHeat(): number { return this.heatStorage; }
  getCurrentHeat(slot: Slot): number { return slot.use; }

  /** 移植自 ReactorHeatVents.kt:44-62 alterHeat */
  alterHeat(slot: Slot, reactor: IReactor, x: number, y: number, heat: number): number {
    let myHeat = this.getCurrentHeat(slot);
    myHeat += heat;
    const max = this.getMaxHeat();
    if (myHeat > max) {
      reactor.setItemAt(x, y, null);
      return max - myHeat + heat;
    }
    if (myHeat < 0) {
      const overflow = myHeat;
      slot.use = 0;
      return overflow;
    }
    slot.use = myHeat;
    return 0;
  }

  /** 移植自 ReactorHeatVents.kt:64-103 processChamber */
  processChamber(slot: Slot, reactor: IReactor, x: number, y: number, heatRun: boolean): void {
    if (!heatRun) return;

    const isThermal = reactor.isFluidCooled();
    let totalDissipated = 0;

    // 从堆温吸收（reactorVent）：不是散热，只是把热搬到散热片自己身上
    if (this.reactorVent > 0) {
      const reactorDrain = Math.min(reactor.getEffectiveHeatForDrain(), this.reactorVent);
      if (this.alterHeat(slot, reactor, x, y, reactorDrain) > 0) return;
      reactor.setHeat(reactor.getHeat() - reactorDrain);
    }

    // 自身蒸发（selfVent）：真正散失的热量
    const dissipated = this.selfVent;
    if (dissipated > 0) {
      if (isThermal) {
        // 流体模式：仅在有冷却液时才修复耐久（散热），并记入 addHeatDissipated 供冷却液转换
        if (reactor.hasCoolant()) {
          this.alterHeat(slot, reactor, x, y, -dissipated);
          reactor.addHeatDissipated(dissipated);
        }
      } else {
        // 电力模式：正常蒸发，修复自身耐久
        this.alterHeat(slot, reactor, x, y, -dissipated);
        reactor.addHeatDissipated(dissipated);
      }
      totalDissipated += dissipated;
    }

    reactor.addSlotHeatInfo(x * 9 + y, 0, totalDissipated);
  }

  influenceExplosion(): number { return 0; }
}

// ===== 元件散热片（component_heat_vent）：无储热，向四邻各蒸发 sideVent =====
// 移植自 ReactorHeatVents.kt:178-213 ComponentHeatVentItem
class ComponentHeatVentBehavior implements IReactorComponent {
  acceptUraniumPulse(): boolean { return false; }
  canStoreHeat(): boolean { return false; }
  getMaxHeat(): number { return 0; }
  getCurrentHeat(): number { return 0; }
  alterHeat(): number { return 0; }
  influenceExplosion(): number { return 0; }

  processChamber(_slot: Slot, reactor: IReactor, x: number, y: number, heatRun: boolean): void {
    if (!heatRun) return;
    const isThermal = reactor.isFluidCooled();
    // 流体模式无冷却液时不散热
    if (isThermal && !reactor.hasCoolant()) {
      reactor.addSlotHeatInfo(x * 9 + y, 0, 0);
      return;
    }
    const sideVent = COMPONENT_VENT_SIDE_VENT;
    let totalDissipated = 0;
    totalDissipated += this.cool(reactor, x - 1, y, sideVent);
    totalDissipated += this.cool(reactor, x + 1, y, sideVent);
    totalDissipated += this.cool(reactor, x, y - 1, sideVent);
    totalDissipated += this.cool(reactor, x, y + 1, sideVent);

    reactor.addHeatDissipated(totalDissipated);
    reactor.addSlotHeatInfo(x * 9 + y, 0, 0);
  }

  /** 移植自 ReactorHeatVents.kt:204-213 cool */
  private cool(reactor: IReactor, targetX: number, targetY: number, sideVentAmount: number): number {
    const other = reactor.getItemAt(targetX, targetY);
    if (!other) return 0;
    // 用 resolver 延迟查询避免循环依赖
    const comp = lookupComponent(other.id);
    if (!comp || !comp.canStoreHeat(other, reactor, targetX, targetY)) return 0;
    comp.alterHeat(other, reactor, targetX, targetY, -sideVentAmount);
    reactor.addSlotHeatInfo(targetX * 9 + targetY, 0, sideVentAmount);
    return sideVentAmount;
  }
}

// 延迟导入 resolver，避免 helpers ↔ resolver ↔ 各 behavior 的初始化顺序问题
import { getComponent as lookupComponent } from './resolver';

// ===== 工厂 =====
const ventCache: Record<string, HeatVentBehavior> = {};
const componentVent = new ComponentHeatVentBehavior();

export function makeHeatVentBehavior(id: ComponentId): IReactorComponent {
  if (id === 'component_heat_vent') return componentVent;
  const p = (VENT_PARAMS as Record<string, { heatStorage: number; selfVent: number; reactorVent: number }>)[id];
  if (!p) throw new Error(`unknown heat vent: ${id}`);
  if (!ventCache[id]) ventCache[id] = new HeatVentBehavior(p.heatStorage, p.selfVent, p.reactorVent);
  return ventCache[id];
}

/** 判断 id 是否为储热型散热片（reactor.ts 在 pass 1 排序时用） */
export function isStorageVent(id: ComponentId): boolean {
  return id in VENT_PARAMS;
}
