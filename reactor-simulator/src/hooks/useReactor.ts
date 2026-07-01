// 反应堆状态 hook：封装 grid、堆温、模式、腔室数、运行循环。
// 稳态视图每帧（或依赖变化时）重算一次 cycle；运行模式用 setInterval 推进。

import { useCallback, useEffect, useMemo, useReducer, useRef } from 'preact/hooks';
import {
  type ComponentId,
  type CycleStats,
  type Grid,
  type ReactorMode,
  emptyGrid,
  gridCapacity,
  runCycles,
  simulateCycle,
} from '../sim';

const MAX_CHAMBERS = 6;
const STORAGE_KEY = 'ic2-reactor-simulator:v1';

/** 持久化的状态子集（不含 running/cycle/lastStats 等瞬态） */
interface Persisted {
  grid: Grid;
  chambers: number;
  mode: ReactorMode;
  heat: number;
  speed: number;
}

/** 从 localStorage 读取持久化布局，失败/无则返回 null */
function loadPersisted(): Persisted | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const p = JSON.parse(raw) as Persisted;
    // 基本合法性校验
    if (
      !Array.isArray(p.grid) ||
      typeof p.chambers !== 'number' ||
      (p.mode !== 'electric' && p.mode !== 'fluid') ||
      typeof p.heat !== 'number' ||
      typeof p.speed !== 'number'
    ) {
      return null;
    }
    // 网格容量与 chambers 不一致时，按 chambers 重建（防止脏数据越界）
    if (p.grid.length !== gridCapacity(p.chambers)) return null;
    return p;
  } catch {
    return null;
  }
}

/** 保存布局到 localStorage（静默失败，如隐私模式） */
function savePersisted(s: ReactorState): void {
  try {
    const p: Persisted = {
      grid: s.grid,
      chambers: s.chambers,
      mode: s.mode,
      heat: s.heat,
      speed: s.speed,
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(p));
  } catch {
    /* 忽略写入失败 */
  }
}

export interface ReactorState {
  grid: Grid;
  chambers: number;
  mode: ReactorMode;
  heat: number;          // 当前堆温（运行模式累计，稳态模式仅展示）
  running: boolean;
  speed: number;         // cycles/sec
  cycle: number;         // 已运行 cycle 数
  lastStats: CycleStats | null;
  lastGrid: Grid | null; // 上一次模拟后的 grid（用于展示耐久衰减等）
  exploded: boolean;
  /** 双击元件栏后进入「选中态」：之后单击网格空格连续放置该元件，null = 未选中 */
  selectedComponent: ComponentId | null;
}

type Action =
  | { type: 'set-chambers'; n: number }
  | { type: 'set-mode'; mode: ReactorMode }
  | { type: 'place'; index: number; id: ComponentId }
  | { type: 'clear'; index: number }
  | { type: 'clear-all' }
  | { type: 'reset-heat' }
  | { type: 'set-heat'; heat: number }
  | { type: 'tick'; stats: CycleStats; grid: Grid; heat: number }
  | { type: 'start'; speed: number }
  | { type: 'stop' }
  | { type: 'set-speed'; speed: number }
  | { type: 'select-component'; id: ComponentId | null };

function init(_chambers: number): ReactorState {
  // 优先从 localStorage 恢复布局（刷新不丢）
  const persisted = loadPersisted();
  return {
    grid: persisted?.grid ?? emptyGrid(_chambers),
    chambers: persisted?.chambers ?? _chambers,
    mode: persisted?.mode ?? 'electric',
    heat: persisted?.heat ?? 0,
    running: false,
    speed: persisted?.speed ?? 5,
    cycle: 0,
    lastStats: null,
    lastGrid: null,
    exploded: false,
    selectedComponent: null,
  };
}

function reducer(s: ReactorState, a: Action): ReactorState {
  switch (a.type) {
    case 'set-chambers': {
      // 改腔室数：保留旧 grid 在新范围内的格子，超出截断
      const newCap = gridCapacity(a.n);
      const oldCols = s.chambers + 3;
      const newCols = a.n + 3;
      const ng = emptyGrid(a.n);
      for (let x = 0; x < Math.min(oldCols, newCols); x++) {
        for (let y = 0; y < 9; y++) {
          ng[x * 9 + y] = s.grid[x * 9 + y] ?? null;
        }
      }
      void newCap;
      return { ...s, chambers: a.n, grid: ng, running: false, cycle: 0, heat: 0, exploded: false, lastStats: null, lastGrid: null };
    }
    case 'set-mode':
      return { ...s, mode: a.mode, running: false, cycle: 0, heat: 0, exploded: false, lastStats: null, lastGrid: null };
    case 'place': {
      // 已有元件的格子不覆盖（选中态单击放置时跳过占用格）
      if (s.grid[a.index]) return s;
      const ng = s.grid.slice();
      ng[a.index] = { id: a.id, use: 0 };
      return { ...s, grid: ng };
    }
    case 'clear': {
      const ng = s.grid.slice();
      ng[a.index] = null;
      return { ...s, grid: ng };
    }
    case 'clear-all':
      return { ...s, grid: emptyGrid(s.chambers), running: false, cycle: 0, heat: 0, exploded: false, lastStats: null, lastGrid: null };
    case 'reset-heat':
      return { ...s, heat: 0, running: false, cycle: 0, exploded: false, lastStats: null, lastGrid: null };
    case 'set-heat': {
      // 直接设置堆温（用户输入）。设值会重置运行状态，把该堆温作为后续模拟的起点。
      // 注意：不重置 grid（元件布局保留），只改起始堆温。
      const h = Math.max(0, Math.min(10000, Math.floor(a.heat)));
      return { ...s, heat: h, running: false, cycle: 0, exploded: false, lastStats: null, lastGrid: null };
    }
    case 'tick':
      return {
        ...s,
        grid: a.grid,
        heat: a.heat,
        cycle: s.cycle + 1,
        lastStats: a.stats,
        lastGrid: a.grid,
        running: a.stats.exploded ? false : s.running,
        exploded: a.stats.exploded,
      };
    case 'start':
      return { ...s, running: true, speed: a.speed, exploded: false };
    case 'stop':
      return { ...s, running: false };
    case 'set-speed':
      return { ...s, speed: a.speed };
    case 'select-component':
      return { ...s, selectedComponent: a.id };
    default:
      return s;
  }
}

export function useReactor() {
  const [state, dispatch] = useReducer(reducer, 3, init);
  const stateRef = useRef(state);
  stateRef.current = state;

  // 持久化：grid/chambers/mode/heat/speed 变化时写入 localStorage（运行中不写，
  // 避免每个 tick 频繁写盘；暂停后下次变化时再写）
  useEffect(() => {
    if (state.running) return;
    savePersisted(state);
  }, [state.grid, state.chambers, state.mode, state.heat, state.speed, state.running]);

  // 稳态视图：基于当前 grid + 当前 heat 算一个 cycle 的瞬时统计（不修改状态）
  const steadyStats = useMemo<CycleStats | null>(() => {
    // 运行中时用 lastStats，否则实时算稳态
    if (state.running) return state.lastStats;
    const res = simulateCycle(state.grid, state.chambers, state.mode, state.heat);
    return res.stats;
  }, [state.grid, state.chambers, state.mode, state.heat, state.running, state.lastStats]);

  // 运行循环：用 requestAnimationFrame，按速度决定每帧推进几个 cycle。
  // 高速（如 100 cycle/s）时每帧批量推进多个 cycle，避免 setInterval 高频触发的渲染抖动。
  useEffect(() => {
    if (!state.running) return;
    const speed = state.speed; // cycles/sec
    let raf = 0;
    let last = performance.now();
    let pending = 0; // 累积待跑的 cycle 数（分数）
    const frame = (now: number) => {
      const dt = (now - last) / 1000;
      last = now;
      pending += dt * speed;
      // 每帧最多跑 floor(pending) 个 cycle，单帧上限 200 防卡死
      let n = Math.min(200, Math.floor(pending));
      pending -= n;
      const s = stateRef.current;
      if (s.exploded) {
        dispatch({ type: 'stop' });
        return;
      }
      let grid = s.grid;
      let heat = s.heat;
      let stats = s.lastStats;
      let exploded = false;
      while (n > 0) {
        const res = simulateCycle(grid, s.chambers, s.mode, heat);
        grid = res.grid;
        heat = res.heat;
        stats = res.stats;
        n--;
        if (res.stats.exploded) { exploded = true; break; }
        if (!res.stats.hasFuelRods) break;
      }
      if (stats) {
        dispatch({ type: 'tick', stats, grid, heat });
        if (exploded) { dispatch({ type: 'stop' }); return; }
      }
      raf = requestAnimationFrame(frame);
    };
    raf = requestAnimationFrame(frame);
    return () => cancelAnimationFrame(raf);
  }, [state.running, state.speed]);

  const place = useCallback((index: number, id: ComponentId) => dispatch({ type: 'place', index, id }), []);
  const clear = useCallback((index: number) => dispatch({ type: 'clear', index }), []);
  const setChambers = useCallback((n: number) => dispatch({ type: 'set-chambers', n: Math.max(0, Math.min(MAX_CHAMBERS, n)) }), []);
  const setMode = useCallback((mode: ReactorMode) => dispatch({ type: 'set-mode', mode }), []);
  const start = useCallback(() => dispatch({ type: 'start', speed: state.speed }), [state.speed]);
  const pause = useCallback(() => dispatch({ type: 'stop' }), []);
  const step = useCallback(() => {
    const s = stateRef.current;
    const res = simulateCycle(s.grid, s.chambers, s.mode, s.heat);
    dispatch({ type: 'tick', stats: res.stats, grid: res.grid, heat: res.heat });
  }, []);
  const reset = useCallback(() => dispatch({ type: 'clear-all' }), []);
  const resetHeat = useCallback(() => dispatch({ type: 'reset-heat' }), []);
  const setHeat = useCallback((heat: number) => dispatch({ type: 'set-heat', heat }), []);
  const setSpeed = useCallback((speed: number) => dispatch({ type: 'set-speed', speed }), []);
  // 双击元件栏选中 / 再次双击或传 null 取消
  const selectComponent = useCallback((id: ComponentId | null) => dispatch({ type: 'select-component', id }), []);
  const runToCompletion = useCallback(() => {
    // 一次性跑到燃料耗尽或爆炸（用于「快进」预览），不进 setInterval
    const s = stateRef.current;
    const res = runCycles(s.grid, s.chambers, s.mode, s.heat, 20000);
    // 取最后一步的 stats（runCycles 没返回每步 stats，这里重新算末态展示用）
    dispatch({ type: 'tick', stats: res.finalStats, grid: res.grid, heat: res.heat });
    dispatch({ type: 'stop' });
  }, []);

  return {
    state,
    steadyStats,
    place,
    clear,
    setChambers,
    setMode,
    start,
    pause,
    step,
    reset,
    resetHeat,
    setHeat,
    setSpeed,
    selectComponent,
    runToCompletion,
  };
}
