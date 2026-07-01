// 移植自 core/src/main/kotlin/ic2_120/content/block/nuclear/NuclearReactorBlockEntity.kt
// 的 tick / processChambers / 热量结算 / 爆炸 / 流体模式冷却液转换。
// IReactor 接口实现 + 单 cycle 推进函数。

import {
  ENERGY_CAPACITY,
  EU_PER_OUTPUT,
  HEAT_CAPACITY,
  HEAT_EXPLODE_THRESHOLD,
  HU_PER_BUCKET,
  MAX_CHAMBERS,
  MAX_OUTPUT_PER_TICK,
  PLATING_HEAT_BONUS,
  HEAT_PLATING_HEAT_BONUS,
  ROWS,
  TICKS_PER_CYCLE,
} from './constants';
import { getMeta } from './components/registry';
import { getComponent } from './components/resolver';
import { isStorageVent } from './components/HeatVents';
import type { ComponentId, CycleStats, Grid, IReactor, ReactorMode, Slot } from './types';

/** 列优先网格大小 = (3 + chambers) 列 × 9 行 */
export function gridCapacity(chambers: number): number {
  return (3 + chambers) * ROWS;
}

/** 列数 */
export function gridCols(chambers: number): number {
  return 3 + chambers;
}

/** 创建空网格 */
export function emptyGrid(chambers: number): Grid {
  return new Array(gridCapacity(chambers)).fill(null);
}

/** 读取 (x=列, y=行)，越界返回 null。移植自 getItemAt（列优先 slot = x*9+y） */
export function getItemAt(grid: Grid, chambers: number, x: number, y: number): Slot | null {
  if (x < 0 || y < 0 || x >= gridCols(chambers) || y >= ROWS) return null;
  return grid[x * ROWS + y] ?? null;
}

function isFuelRod(id: ComponentId): boolean {
  return id.endsWith('_fuel_rod') && !id.startsWith('depleted');
}

/**
 * 单周期模拟器：内部实现 IReactor，跑完一个完整 cycle（pass0 + pass1 + 结算）。
 *
 * 设计：每次调用都基于传入的 grid 副本 + 起始堆温进行模拟，返回结算后的状态与统计。
 * 不直接持有跨 cycle 状态（除堆温外），便于「稳态视图」每帧无副作用地重算。
 */
export class CycleSimulator implements IReactor {
  // 输入
  private grid: Grid;
  private readonly chambers: number;
  private readonly mode: ReactorMode;
  private readonly _produceEnergy: boolean;

  // 堆温
  private _heat: number;
  private readonly _cycleStartHeat: number;

  // 累加器（每个 cycle 重置）
  private outputAccumulator = 0;
  private emitHeatBuffer = 0;
  private totalHeatProduced = 0;
  private totalHeatDissipated = 0;
  private ventDissipatedHeat = 0; // = totalHeatDissipated（流体模式用，源码里两者一致）
  private heatEffectModifier = 1;
  private slotHeat = new Map<number, { produced: number; dissipated: number; energy: number }>();

  // 流体模式：冷却液是否充足（模拟器假设无限冷却液供给，便于纯产热计算；UI 上可切换）
  private readonly _hasCoolant: boolean;

  constructor(grid: Grid, chambers: number, mode: ReactorMode, startHeat: number, opts?: { produceEnergy?: boolean; hasCoolant?: boolean }) {
    // 深拷贝 grid，避免模拟过程修改外部状态（稳态视图可重复调用）
    this.grid = grid.map((s) => (s ? { ...s } : null));
    this.chambers = chambers;
    this.mode = mode;
    this._produceEnergy = opts?.produceEnergy ?? true;
    this._hasCoolant = opts?.hasCoolant ?? true;
    this._heat = startHeat;
    this._cycleStartHeat = startHeat;
  }

  // ===== IReactor 实现 =====
  getItemAt(x: number, y: number): Slot | null {
    return getItemAt(this.grid, this.chambers, x, y);
  }
  setItemAt(x: number, y: number, slot: Slot | null): void {
    if (x < 0 || y < 0 || x >= gridCols(this.chambers) || y >= ROWS) return;
    this.grid[x * ROWS + y] = slot;
  }
  getHeat(): number { return this._heat; }
  setHeat(h: number): void { this._heat = h; }
  addHeat(amount: number): void {
    // 移植自 NuclearReactorBlockEntity.addHeat：累加并 clamp
    this._heat += amount;
    if (this._heat < 0) this._heat = 0;
    if (this._heat > this.getMaxHeat()) this._heat = this.getMaxHeat();
  }
  getMaxHeat(): number {
    // 移植自 getMaxHeat()：10000 + Σ 隔板加成
    let max = HEAT_CAPACITY;
    for (const s of this.grid) {
      if (!s) continue;
      if (s.id === 'reactor_plating') max += PLATING_HEAT_BONUS;
      else if (s.id === 'reactor_heat_plating') max += HEAT_PLATING_HEAT_BONUS;
    }
    return max;
  }
  getEffectiveHeatForDrain(): number {
    // 移植自 getEffectiveHeatForDrain：堆温 + emitHeatBuffer
    return this._heat + this.emitHeatBuffer;
  }
  addEmitHeat(heat: number): void { this.emitHeatBuffer += heat; }
  produceEnergy(): boolean { return this._produceEnergy; }
  addOutput(units: number): void { this.outputAccumulator += units; }
  addHeatProduced(heat: number): void { this.totalHeatProduced += heat; }
  addHeatDissipated(heat: number): void {
    this.totalHeatDissipated += heat;
    this.ventDissipatedHeat += heat;
  }
  addSlotHeatInfo(slotIndex: number, heatProduced: number, heatDissipated: number, energyOutput = 0): void {
    const cur = this.slotHeat.get(slotIndex) ?? { produced: 0, dissipated: 0, energy: 0 };
    cur.produced += heatProduced;
    cur.dissipated += heatDissipated;
    cur.energy += energyOutput;
    this.slotHeat.set(slotIndex, cur);
  }
  isFluidCooled(): boolean { return this.mode === 'fluid'; }
  hasCoolant(): boolean { return this._hasCoolant; }
  getCycleStartHeat(): number { return this._cycleStartHeat; }
  multiplyHeatEffectModifier(mod: number): void { this.heatEffectModifier *= mod; }

  // ===== 跑一个完整 cycle =====
  run(): { grid: Grid; heat: number; stats: CycleStats } {
    // 1. processChambers 两趟 pass（移植自 NuclearReactorBlockEntity.processChambers）
    //    pass 0：全网格，heatRun=false
    for (let y = 0; y < ROWS; y++) {
      for (let x = 0; x < gridCols(this.chambers); x++) {
        const s = this.getItemAt(x, y);
        if (!s) continue;
        const comp = getComponent(s.id);
        comp?.processChamber(s, this, x, y, false);
      }
    }
    //    pass 1：先非 vent，再 vent（避免温度被 vent 抢先吸走导致顺序锁定）
    //    非散热片
    for (let y = 0; y < ROWS; y++) {
      for (let x = 0; x < gridCols(this.chambers); x++) {
        const s = this.getItemAt(x, y);
        if (!s) continue;
        if (isStorageVent(s.id)) continue;
        const comp = getComponent(s.id);
        comp?.processChamber(s, this, x, y, true);
      }
    }
    //    散热片（component_heat_vent 也走这里：源码 ReactorHeatVentBase 子类 + ComponentHeatVentItem 都在 pass1 末尾处理）
    for (let y = 0; y < ROWS; y++) {
      for (let x = 0; x < gridCols(this.chambers); x++) {
        const s = this.getItemAt(x, y);
        if (!s) continue;
        // component_heat_vent 不是 storage vent，但源码里它在 vent 末尾阶段处理；
        // 这里把所有 *heat_vent* 都放到末尾跑，与 Kotlin 的 ReactorHeatVentBase 判定一致
        // （Kotlin 判定 `stack.item is ReactorHeatVentBase`，ComponentHeatVentItem 不是其子类，
        //   但 ComponentHeatVentItem 自身 processChamber 在 heatRun=true 时才生效，
        //   且非储热，先跑/后跑对它无影响——为忠实移植，它实际在「非 vent」遍历里已被处理。）
        if (!isStorageVent(s.id)) continue;
        const comp = getComponent(s.id);
        comp?.processChamber(s, this, x, y, true);
      }
    }

    // 2. emitHeatBuffer 折入堆温（移植自 tick() line ~1066）
    this._heat = clampInt(this._heat + this.emitHeatBuffer, 0, this.getMaxHeat());

    // 3. 统计 / 结算
    const maxHeat = this.getMaxHeat();
    let hotCoolantMb = 0;
    if (this.mode === 'fluid') {
      // 移植自 tick() 流体冷却转换：dissipatedHeat = min(产热, 散热)，按 HU_PER_BUCKET 转 mB
      const dissipatedHeat = Math.min(this.totalHeatProduced, this.ventDissipatedHeat);
      // 1 桶冷却液 = HU_PER_BUCKET HU = 1000 mB；故 mB = heat / HU_PER_BUCKET * 1000
      hotCoolantMb = (dissipatedHeat / HU_PER_BUCKET) * 1000;
    }

    // 电力输出：euTotal = outputAccumulator * EU_PER_OUTPUT（移植自 tick() line 1168-1172）
    // 一个 cycle 的 EU 总量；EU/t = euTotal / TICKS_PER_CYCLE（且不超过 MAX_OUTPUT_PER_TICK）
    const euTotal = Math.min(Math.floor(this.outputAccumulator * EU_PER_OUTPUT), ENERGY_CAPACITY);
    const euPerTick = Math.min(euTotal / TICKS_PER_CYCLE, MAX_OUTPUT_PER_TICK);

    // 爆炸判定（移植自 tick() heat effects / explode）
    let exploded = false;
    let explosionPower = 0;
    if (this._heat >= HEAT_EXPLODE_THRESHOLD) {
      exploded = true;
      explosionPower = this.computeExplosionPower();
    }

    // 是否还有可运行燃料棒（运行模式停止条件）
    let hasFuelRods = false;
    for (const s of this.grid) {
      if (s && isFuelRod(s.id)) { hasFuelRods = true; break; }
    }

    const stats: CycleStats = {
      heat: this._heat,
      maxHeat,
      euPerTick,
      heatProduced: this.totalHeatProduced,
      heatDissipated: this.totalHeatDissipated,
      ventDissipated: this.ventDissipatedHeat,
      netHeat: this.totalHeatProduced - this.totalHeatDissipated,
      slotHeat: this.slotHeat,
      exploded,
      explosionPower,
      hotCoolantOutputMb: hotCoolantMb,
      hasFuelRods,
    };

    return { grid: this.grid, heat: this._heat, stats };
  }

  /** 移植自 NuclearReactorBlockEntity 爆炸威力计算（boomPower 起始 10，加 Σ influence，乘 heatEffectModifier） */
  private computeExplosionPower(): number {
    let boomPower = 10;
    let boomMod = 1;
    for (const s of this.grid) {
      if (!s) continue;
      const comp = getComponent(s.id);
      if (!comp) continue;
      const infl = comp.influenceExplosion(s, this);
      if (infl > 0 && infl < 1) boomMod *= infl;       // 隔板（0.9/0.99）
      else if (infl >= 1) boomPower += infl;            // 燃料棒（2*cells）
    }
    let power = boomPower * this.heatEffectModifier * boomMod;
    if (power < 0) power = 0;
    if (power > 100) power = 100; // 硬上限（config.reactorExplosionPowerLimit 默认 100）
    return power;
  }
}

function clampInt(v: number, lo: number, hi: number): number {
  return v < lo ? lo : v > hi ? hi : v;
}

/**
 * 稳态计算：基于当前 grid + 堆温，跑一个 cycle 但不修改输入（用于仪表盘实时展示）。
 * 堆温从 startHeat 开始；若想看「平衡温度」可多次迭代。
 */
export function simulateCycle(
  grid: Grid,
  chambers: number,
  mode: ReactorMode,
  startHeat: number,
  opts?: { produceEnergy?: boolean; hasCoolant?: boolean },
): { grid: Grid; heat: number; stats: CycleStats } {
  const sim = new CycleSimulator(grid, chambers, mode, startHeat, opts);
  return sim.run();
}

/**
 * 「运行」时间模型：逐 cycle 推进，前一个 cycle 的 grid/堆温 作为下一个的输入，
 * 直到爆炸或燃料耗尽或达到 maxCycles。返回每步统计。
 */
export function runCycles(
  grid: Grid,
  chambers: number,
  mode: ReactorMode,
  startHeat: number,
  maxCycles: number,
  opts?: { produceEnergy?: boolean; hasCoolant?: boolean; onStep?: (step: number, grid: Grid, heat: number, stats: CycleStats) => boolean | void },
): { grid: Grid; heat: number; finalStats: CycleStats } {
  let cur: Grid = grid.map((s) => (s ? { ...s } : null));
  let heat = startHeat;
  let stats: CycleStats | null = null;
  for (let step = 0; step < maxCycles; step++) {
    const sim = new CycleSimulator(cur, chambers, mode, heat, opts);
    const res = sim.run();
    cur = res.grid;
    heat = res.heat;
    stats = res.stats;
    if (opts?.onStep) {
      const cont = opts.onStep(step, cur, heat, res.stats);
      if (cont === false) break;
    }
    if (res.stats.exploded) break;
    if (!res.stats.hasFuelRods) break;
  }
  return { grid: cur, heat, finalStats: stats! };
}

export const MAX_CHAMBERS_EXPORT = MAX_CHAMBERS;
export { getMeta };
