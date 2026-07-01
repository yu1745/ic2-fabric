// 移植自 core/src/main/kotlin/ic2_120/content/item/ReactorHeatExchangers.kt
// 热交换器：与四邻可储热元件、与反应堆本体交换热量。
// 关键且容易抄错的点：分层节流逻辑里 avgTemp = othermed + mymed/2（量纲 0-150），
// 却与字面常量 1.0/0.75/0.5/0.25 比较——源码注释把档位写反了，但逻辑就是这样，照抄。

import { EXCHANGER_PARAMS } from '../constants';
import type { ComponentId, IReactor, IReactorComponent, Slot } from '../types';
import { checkHeatAcceptor } from './helpers';
import { getComponent as resolveComp } from './resolver';

class HeatExchangerBehavior implements IReactorComponent {
  constructor(public readonly heatStorage: number, public readonly switchSide: number, public readonly switchReactor: number) {}

  acceptUraniumPulse(): boolean { return false; }
  canStoreHeat(): boolean { return true; }
  getMaxHeat(): number { return this.heatStorage; }
  getCurrentHeat(slot: Slot): number { return slot.use; }

  /** 移植自 ReactorHeatExchangers.kt:48-70 alterHeat */
  alterHeat(slot: Slot, reactor: IReactor, x: number, y: number, heat: number): number {
    let myHeat = this.getCurrentHeat(slot);
    myHeat += heat;
    const max = this.getMaxHeat();
    if (myHeat > max) {
      // 超限：组件损坏（消失），返回未能吸收的溢出
      reactor.setItemAt(x, y, null);
      return max - myHeat + heat;
    }
    if (myHeat < 0) {
      // 热量不足：无法释放请求量
      const overflow = myHeat;
      slot.use = 0;
      return overflow;
    }
    slot.use = myHeat;
    return 0;
  }

  /** 移植自 ReactorHeatExchangers.kt:76-158 processChamber */
  processChamber(slot: Slot, reactor: IReactor, x: number, y: number, heatRun: boolean): void {
    if (!heatRun) return;

    let myHeatDelta = 0;

    // ===== 与四邻可储热元件交换 =====
    if (this.switchSide > 0) {
      const scaledSwitchSide = this.switchSide;
      const acceptors: Array<{ slot: Slot; x: number; y: number }> = [];
      checkHeatAcceptor(reactor, x - 1, y, acceptors);
      checkHeatAcceptor(reactor, x + 1, y, acceptors);
      checkHeatAcceptor(reactor, x, y - 1, acceptors);
      checkHeatAcceptor(reactor, x, y + 1, acceptors);

      for (const a of acceptors) {
        const comp = resolveComp(a.slot.id)!;
        const mymed = (this.getCurrentHeat(slot) * 100.0) / this.getMaxHeat();
        const othermed = (comp.getCurrentHeat(a.slot) * 100.0) / comp.getMaxHeat();

        // 基础交换量 = othermax/100 * (othermed + mymed/2)，再 clamp 到 ±switchSide
        let add = Math.round((comp.getMaxHeat() / 100.0) * (othermed + mymed / 2));
        add = clampInt(add, -scaledSwitchSide, scaledSwitchSide);

        // ⚠️ 注意：源码注释把档位说成「高温/中温」，但 avgTemp = othermed + mymed/2 量纲是 0-150，
        // 与 1.0/0.75/0.5/0.25 比较时，绝大多数情况会落入 <1.0 → /2，<0.75 → /4 ... 链式覆盖。
        // 这里 1:1 照抄源码（ReactorHeatExchangers.kt:103-108），不要按注释改。
        const avgTemp = othermed + mymed / 2;
        if (avgTemp < 1.0) add = Math.floor(scaledSwitchSide / 2);
        if (avgTemp < 0.75) add = Math.floor(scaledSwitchSide / 4);
        if (avgTemp < 0.5) add = Math.floor(scaledSwitchSide / 8);
        if (avgTemp < 0.25) add = 1;

        // 方向：邻居更热则 add 取负（热量流入交换器）
        const otherR = Math.round(othermed * 10) / 10.0;
        const myR = Math.round(mymed * 10) / 10.0;
        if (otherR > myR) add = -add;
        else if (otherR === myR) add = 0;

        myHeatDelta -= add;
        const actualExchange = comp.alterHeat(a.slot, reactor, a.x, a.y, add);
        if (actualExchange !== 0) {
          myHeatDelta += actualExchange;
        }
      }
    }

    // ===== 与反应堆本体交换 =====
    if (this.switchReactor > 0) {
      const scaledSwitchReactor = this.switchReactor;
      const mymed = (this.getCurrentHeat(slot) * 100.0) / this.getMaxHeat();
      const reactorMed = (reactor.getHeat() * 100.0) / reactor.getMaxHeat();

      let add = Math.round((reactor.getMaxHeat() / 100.0) * (reactorMed + mymed / 2));
      add = clampInt(add, -scaledSwitchReactor, scaledSwitchReactor);

      const avg = reactorMed + mymed / 2;
      if (avg < 1.0) add = Math.floor(scaledSwitchReactor / 2);
      if (avg < 0.75) add = Math.floor(scaledSwitchReactor / 4);
      if (avg < 0.5) add = Math.floor(scaledSwitchReactor / 8);
      if (avg < 0.25) add = 1;

      const reactorR = Math.round(reactorMed * 10) / 10.0;
      const myR = Math.round(mymed * 10) / 10.0;
      if (reactorR > myR) add = -add;
      else if (reactorR === myR) add = 0;

      myHeatDelta -= add;
      reactor.setHeat(clampInt(reactor.getHeat() + add, 0, reactor.getMaxHeat()));
    }

    // 应用交换器自身热量变化
    this.alterHeat(slot, reactor, x, y, myHeatDelta);
  }

  influenceExplosion(): number { return 0; }
}

function clampInt(v: number, lo: number, hi: number): number {
  return v < lo ? lo : v > hi ? hi : v;
}

// ===== 工厂 =====
const cache: Record<string, HeatExchangerBehavior> = {};

export function makeHeatExchangerBehavior(id: ComponentId): IReactorComponent {
  if (!cache[id]) {
    const p = (EXCHANGER_PARAMS as Record<string, { heatStorage: number; switchSide: number; switchReactor: number }>)[id];
    if (!p) throw new Error(`unknown heat exchanger: ${id}`);
    cache[id] = new HeatExchangerBehavior(p.heatStorage, p.switchSide, p.switchReactor);
  }
  return cache[id];
}

// 导出供 FuelRods 复用（避免重复定义 checkHeatAcceptor）
export { checkHeatAcceptor };
