// 移植自 core/src/main/kotlin/ic2_120/content/reactor/AbstractFiniteNeutronReflectorItem.kt
//   - neutron_reflector / thick_neutron_reflector：有限脉冲耐久，按邻接燃料棒 cells 数消耗
//   - iridium_neutron_reflector：永不枯竭（继承 AbstractReactorComponent，acceptUraniumPulse 恒 true）
// 耐久存放在 slot.use（= 已消耗脉冲数）。

import { REFLECTOR_PARAMS } from '../constants';
import type { ComponentId, IReactor, IReactorComponent, Slot } from '../types';
import { getComponent } from './resolver';

// 燃料棒 registry name → numberOfCells（移植自 Kotlin when 分支：U/Mox 燃料棒返回其 cells）
// 与 UraniumFuelRods.kt / FUEL_ROD_PARAMS 一致。
const ROD_CELLS: Record<string, number> = {
  uranium_fuel_rod: 1, dual_uranium_fuel_rod: 2, quad_uranium_fuel_rod: 4,
  mox_fuel_rod: 1, dual_mox_fuel_rod: 2, quad_mox_fuel_rod: 4,
};

// 邻接燃料棒消耗权重：移植自 AbstractFiniteNeutronReflectorItem.kt:90-97 neighborFuelDrainWeight。
// 源码：isOperationalFuelRod(stack) ? numberOfCells : 0。
// 「operational」= 未枯竭（use < maxUse-1），即 acceptUraniumPulse 返回 true。
function neighborFuelDrainWeight(slot: Slot | null): number {
  if (!slot) return 0;
  const cells = ROD_CELLS[slot.id];
  if (!cells) return 0;
  const comp = getComponent(slot.id);
  if (!comp) return 0;
  // 枯竭棒返回 false → 不消耗
  if (!comp.acceptUraniumPulse(slot, null as unknown as IReactor, slot, 0, 0, 0, 0, false)) return 0;
  return cells;
}

// ===== 有限反射板（neutron_reflector / thick_neutron_reflector） =====
class FiniteReflectorBehavior implements IReactorComponent {
  constructor(public readonly maxPulses: number) {}

  canStoreHeat(): boolean { return false; }
  getMaxHeat(): number { return 0; }
  getCurrentHeat(): number { return 0; }
  alterHeat(): number { return 0; }

  /** 移植自 AbstractFiniteNeutronReflectorItem.kt:32-49 processChamber */
  processChamber(slot: Slot, reactor: IReactor, x: number, y: number, heatRun: boolean): void {
    if (heatRun) return;
    if (!reactor.produceEnergy()) return;
    if (slot.use >= this.maxPulses) return;

    let drain = 0;
    drain += neighborFuelDrainWeight(reactor.getItemAt(x - 1, y));
    drain += neighborFuelDrainWeight(reactor.getItemAt(x + 1, y));
    drain += neighborFuelDrainWeight(reactor.getItemAt(x, y - 1));
    drain += neighborFuelDrainWeight(reactor.getItemAt(x, y + 1));
    if (drain === 0) return;

    const next = Math.min(slot.use + drain, this.maxPulses);
    slot.use = next;
    if (next >= this.maxPulses) {
      reactor.setItemAt(x, y, null);
    }
  }

  /** 移植自 :51-60：未枯竭时接受脉冲 */
  acceptUraniumPulse(slot: Slot): boolean {
    return slot.use < this.maxPulses;
  }

  influenceExplosion(): number { return 0; }
}

// ===== 铱反射板：永不枯竭 =====
class IridiumReflectorBehavior implements IReactorComponent {
  canStoreHeat(): boolean { return false; }
  getMaxHeat(): number { return 0; }
  getCurrentHeat(): number { return 0; }
  alterHeat(): number { return 0; }
  processChamber(): void {}
  acceptUraniumPulse(): boolean { return true; } // 恒 true
  influenceExplosion(): number { return 0; }
}

// ===== 工厂 =====
const finiteCache: Record<string, FiniteReflectorBehavior> = {};
const iridium = new IridiumReflectorBehavior();

export function makeReflectorBehavior(id: ComponentId): IReactorComponent {
  if (id === 'iridium_neutron_reflector') return iridium;
  const p = (REFLECTOR_PARAMS as Record<string, { maxPulses: number }>)[id];
  if (!p) throw new Error(`unknown reflector: ${id}`);
  if (!finiteCache[id]) finiteCache[id] = new FiniteReflectorBehavior(p.maxPulses);
  return finiteCache[id];
}
