// 顶层布局：模式/腔室 → [元件栏 | 仪表盘+网格] → 运行控制。

import { type JSX } from 'preact';
import { useEffect } from 'preact/hooks';
import { useReactor } from '../hooks/useReactor';
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
          onSelect={r.selectComponent}
          onPick={(id) => {
            // 有选中态时：单击元件栏 = 切换选中到该元件；无选中态时：放到首个空格（旧行为）
            if (state.selectedComponent) {
              r.selectComponent(state.selectedComponent === id ? null : id);
            } else {
              const idx = state.grid.findIndex((s) => s === null);
              if (idx >= 0) r.place(idx, id);
            }
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
          />
          <div className="grid-wrapper">
            <ReactorGrid
              grid={state.lastGrid ?? state.grid}
              chambers={state.chambers}
              stats={steadyStats}
              selected={state.selectedComponent}
              onPlace={r.place}
              onClear={r.clear}
            />
            <div className="grid-hint">
              {state.selectedComponent
                ? `选中放置模式：单击空格放入「${state.selectedComponent}」· 再次双击或按 ESC 取消`
                : '拖入元件 · 双击元件栏进入连续放置 · 右键移除 · 红边=产热 · 蓝边=散热'}
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
