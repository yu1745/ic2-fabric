// 顶层布局：模式/腔室 → [元件栏 | 仪表盘+网格] → 运行控制。

import { type JSX } from 'preact';
import { useEffect, useMemo } from 'preact/hooks';
import { useReactor } from '../hooks/useReactor';
import { estimateFullLifeOutput } from '../sim';
import { ModeToggle } from './ModeToggle';
import { ComponentPalette } from './ComponentPalette';
import { Dashboard } from './Dashboard';
import { ReactorGrid } from './ReactorGrid';
import { RunControls } from './RunControls';

export function App(): JSX.Element {
  const r = useReactor();
  const { state, steadyStats } = r;

  // ESC 退出选中态
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && state.selectedComponent) r.selectComponent(null);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [state.selectedComponent]);

  // 展示用的 grid：tick action 已把每次模拟结果同步进 state.grid（含耐久衰减），
  // 所以直接用 state.grid 即可——停止后用户编辑的就是最新状态，新元件立即显示。
  const displayGrid = state.grid;
  const life = useMemo(() => {
    // 运行中不重算（昂贵），用上次结果
    if (state.running) return null;
    if (!steadyStats?.hasFuelRods) return null;
    return estimateFullLifeOutput(displayGrid, state.chambers, state.mode, state.heat);
  }, [displayGrid, state.chambers, state.mode, state.heat, state.running, steadyStats?.hasFuelRods]);

  return (
    <div className="app">
      <header className="app-header">
        <h1>IC2 核反应堆模拟器</h1>
        <span className="subtitle">移植自 ic2_120 NuclearReactorBlockEntity</span>
      </header>

      <ModeToggle
        mode={state.mode}
        chambers={state.chambers}
        onMode={r.setMode}
        onChambers={r.setChambers}
      />

      <div className="main-area">
        <ComponentPalette
          selected={state.selectedComponent}
          onSelect={(id) => {
            // 单击元件栏：选中该元件；再次单击同一元件则取消选中
            r.selectComponent(state.selectedComponent === id ? null : id);
          }}
        />

        <div className="right-panel">
          <Dashboard
            stats={steadyStats}
            heat={state.heat}
            mode={state.mode}
            cycle={state.cycle}
            running={state.running}
            exploded={state.exploded}
            lifeTotalEu={life?.totalEu ?? null}
            onSetHeat={r.setHeat}
          />
          <div className="grid-wrapper">
            <ReactorGrid
              grid={displayGrid}
              chambers={state.chambers}
              stats={steadyStats}
              mode={state.mode}
              selected={state.selectedComponent}
              onPlace={r.place}
              onClear={r.clear}
              perSlotLifeEu={life?.perSlotEu ?? null}
            />
            <div className="grid-hint">
              {state.selectedComponent
                ? `选中放置模式：左键单击或按住拖动扫过空格批量放入「${state.selectedComponent}」· 右键拖动移除 · 再次单击该元件或按 ESC 取消`
                : '单击元件栏选中后连续放置 · 左键拖动放置 · 右键单击或拖动移除 · 悬停查看详情 · 红边=产热 · 蓝边=散热'}
            </div>
          </div>
        </div>
      </div>

      <RunControls
        running={state.running}
        speed={state.speed}
        onStart={r.start}
        onPause={r.pause}
        onStep={r.step}
        onRunToCompletion={r.runToCompletion}
        onReset={r.reset}
        onResetHeat={r.resetHeat}
        onSpeed={r.setSpeed}
      />
    </div>
  );
}
