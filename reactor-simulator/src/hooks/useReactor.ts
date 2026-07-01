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
  | { type: 'tick'; stats: CycleStats; grid: Grid; heat: number }
  | { type: 'start'; speed: number }
  | { type: 'stop' }
  | { type: 'set-speed'; speed: number }
  | { type: 'select-component'; id: ComponentId | null };

function init(chambers: number): ReactorState {
  return {
    grid: emptyGrid(chambers),
    chambers,
    mode: 'electric',
    heat: 0,
    running: false,
    speed: 5,
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

  // 稳态视图：基于当前 grid + 当前 heat 算一个 cycle 的瞬时统计（不修改状态）
  const steadyStats = useMemo<CycleStats | null>(() => {
    // 运行中时用 lastStats，否则实时算稳态
    if (state.running) return state.lastStats;
    const res = simulateCycle(state.grid, state.chambers, state.mode, state.heat);
    return res.stats;
  }, [state.grid, state.chambers, state.mode, state.heat, state.running, state.lastStats]);

  // 运行循环
  useEffect(() => {
    if (!state.running) return;
    const interval = Math.max(50, Math.floor(1000 / state.speed));
    const timer = window.setInterval(() => {
      const s = stateRef.current;
      if (s.exploded) {
        dispatch({ type: 'stop' });
        return;
      }
      // 单步推进
      const res = simulateCycle(s.grid, s.chambers, s.mode, s.heat);
      dispatch({ type: 'tick', stats: res.stats, grid: res.grid, heat: res.heat });
    }, interval);
    return () => window.clearInterval(timer);
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
    setSpeed,
    selectComponent,
    runToCompletion,
  };
}
