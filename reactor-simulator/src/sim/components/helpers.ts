// 共享辅助：邻位元件查询。移植自 UraniumFuelRods.kt / ReactorHeatExchangers.kt 中的
// checkPulseable / checkHeatAcceptor 私有函数（两处实现完全一致，统一在此）。

import type { IReactor, Slot } from '../types';
import { getComponent } from './resolver';

/**
 * 检查 (x,y) 元件是否接受中子脉冲。返回 1 或 0。
 * 移植自 UraniumFuelRods.kt:30-51 checkPulseable。
 */
export function checkPulseable(
  reactor: IReactor,
  x: number,
  y: number,
  pulsingSlot: Slot,
  mex: number,
  mey: number,
  heatRun: boolean,
): number {
  const other = reactor.getItemAt(x, y);
  if (!other) return 0;
  const comp = getComponent(other.id);
  if (!comp) return 0;
  return comp.acceptUraniumPulse(other, reactor, pulsingSlot, x, y, mex, mey, heatRun) ? 1 : 0;
}

/**
 * 若 (x,y) 元件可储热，加入 out。移植自 UraniumFuelRods.kt:53-58 checkHeatAcceptor。
 */
export function checkHeatAcceptor(
  reactor: IReactor,
  x: number,
  y: number,
  out: Array<{ slot: Slot; x: number; y: number }>,
): void {
  const s = reactor.getItemAt(x, y);
  if (!s) return;
  const comp = getComponent(s.id);
  if (comp && comp.canStoreHeat(s, reactor, x, y)) {
    out.push({ slot: s, x, y });
  }
}
