// 移植自 core/src/main/kotlin/ic2_120/content/item/ReactorParts.kt 中的：
//   - ReactorCoolantCellBase（被动储热，无 processChamber 行为）
//   - CondensatorItem（RSH/LZH，只能正吸收，不可被散热片/交换器冷却）
//   - ReactorPlatingItem / ReactorHeatPlatingItem（不储热；heatRun 时乘 heatEffectModifier；
//     HEAT_BONUS 由 reactor.getMaxHeat() 读取）
//   - ContainmentReactorPlatingItem 是源码里的纯 Item（无 reactor 行为），这里给个空 behavior。

import { COOLANT_CELL_HEAT, CONDENSATOR_HEAT, PLATING_EXPLOSION_MODIFIER } from '../constants';
import type { ComponentId, IReactor, IReactorComponent, Slot } from '../types';

// ===== 冷却单元：纯被动储热（移植自 ReactorCoolantCellBase） =====
class CoolantCellBehavior implements IReactorComponent {
  constructor(public readonly heatStorage: number) {}
  canStoreHeat(): boolean { return true; }
  getMaxHeat(): number { return this.heatStorage; }
  getCurrentHeat(slot: Slot): number { return slot.use; }
  processChamber(): void {}
  acceptUraniumPulse(): boolean { return false; }
  influenceExplosion(): number { return 0; }

  alterHeat(slot: Slot, reactor: IReactor, x: number, y: number, heat: number): number {
    let myHeat = slot.use + heat;
    const max = this.heatStorage;
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
}

// ===== 冷凝器：只能正吸收（移植自 CondensatorItem.alterHeat: if (heat < 0) return heat） =====
class CondensatorBehavior implements IReactorComponent {
  constructor(public readonly heatStorage: number) {}
  canStoreHeat(): boolean { return true; }
  getMaxHeat(): number { return this.heatStorage; }
  getCurrentHeat(slot: Slot): number { return slot.use; }
  processChamber(): void {}
  acceptUraniumPulse(): boolean { return false; }
  influenceExplosion(): number { return 0; }

  alterHeat(slot: Slot, reactor: IReactor, x: number, y: number, heat: number): number {
    // 拒绝负热量（不可被散热片/交换器冷却，只能在工作台修复）
    if (heat < 0) return heat;
    let myHeat = slot.use + heat;
    const max = this.heatStorage;
    if (myHeat > max) {
      reactor.setItemAt(x, y, null);
      return max - myHeat + heat;
    }
    slot.use = myHeat;
    return 0;
  }
}

// ===== 隔板：不储热；heatRun 时把爆炸倍率乘到 heatEffectModifier（移植自 ReactorPlatingItem.processChamber） =====
class PlatingBehavior implements IReactorComponent {
  constructor(public readonly explosionModifier: number) {}
  canStoreHeat(): boolean { return false; }
  getMaxHeat(): number { return 0; }
  getCurrentHeat(): number { return 0; }
  alterHeat(): number { return 0; }
  acceptUraniumPulse(): boolean { return false; }

  processChamber(_slot: Slot, reactor: IReactor, _x: number, _y: number, heatRun: boolean): void {
    if (!heatRun) return;
    reactor.multiplyHeatEffectModifier(this.explosionModifier);
  }

  influenceExplosion(): number {
    // influenceExplosion 在源码里 plating 返回的是 EXPLOSION_MODIFIER（<1，会乘到 boomMod），
    // 而 reactor.ts 计算 boomPower 时也走同一通道。保持一致：返回倍率。
    return this.explosionModifier;
  }
}

// 占位（containment_reactor_plating 在源码里是纯 Item，无 reactor 行为）
class NoopBehavior implements IReactorComponent {
  processChamber(): void {}
  acceptUraniumPulse(): boolean { return false; }
  canStoreHeat(): boolean { return false; }
  getMaxHeat(): number { return 0; }
  getCurrentHeat(): number { return 0; }
  alterHeat(): number { return 0; }
  influenceExplosion(): number { return 0; }
}

// ===== 工厂 =====
const cache = new Map<ComponentId, IReactorComponent>();

export function makeCoolantOrPlatingBehavior(id: ComponentId): IReactorComponent {
  if (cache.has(id)) return cache.get(id)!;
  let b: IReactorComponent;
  if (id in COOLANT_CELL_HEAT) {
    b = new CoolantCellBehavior((COOLANT_CELL_HEAT as Record<string, number>)[id]);
  } else if (id in CONDENSATOR_HEAT) {
    b = new CondensatorBehavior((CONDENSATOR_HEAT as Record<string, number>)[id]);
  } else if (id in PLATING_EXPLOSION_MODIFIER) {
    b = new PlatingBehavior((PLATING_EXPLOSION_MODIFIER as Record<string, number>)[id]);
  } else {
    // containment_reactor_plating 等无行为元件
    b = new NoopBehavior();
  }
  cache.set(id, b);
  return b;
}
