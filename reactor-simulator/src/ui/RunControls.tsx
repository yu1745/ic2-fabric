// 运行控制条：播放/暂停/步进/快进/重置/速度。

import { type JSX } from 'preact';

interface Props {
  running: boolean;
  speed: number;
  onStart: () => void;
  onPause: () => void;
  onStep: () => void;
  onRunToCompletion: () => void;
  onReset: () => void;
  onResetHeat: () => void;
  onSpeed: (n: number) => void;
}

const SPEEDS = [1, 2, 5, 10, 20];

export function RunControls(props: Props): JSX.Element {
  const { running, speed, onStart, onPause, onStep, onRunToCompletion, onReset, onResetHeat, onSpeed } = props;
  return (
    <div className="run-controls">
      {running ? (
        <button onClick={onPause}>⏸ 暂停</button>
      ) : (
        <button onClick={onStart}>▶ 运行</button>
      )}
      <button onClick={onStep} disabled={running}>⏭ 单步</button>
      <button onClick={onRunToCompletion} disabled={running}>⏩ 快进到结束</button>
      <button onClick={onResetHeat}>↺ 重置堆温</button>
      <button onClick={onReset}>🗑 清空网格</button>
      <span className="speed-label">速度:</span>
      <select value={speed} onChange={(e) => onSpeed(Number((e.target as HTMLSelectElement).value))}>
        {SPEEDS.map((s) => (
          <option key={s} value={s}>{s} cycle/s</option>
        ))}
      </select>
    </div>
  );
}
