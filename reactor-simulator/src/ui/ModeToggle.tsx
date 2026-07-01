// 顶部：电力/流体模式切换 + 腔室数滑块。

import { type JSX } from 'preact';
import type { ReactorMode } from '../sim';

interface Props {
  mode: ReactorMode;
  chambers: number;
  onMode: (m: ReactorMode) => void;
  onChambers: (n: number) => void;
}

export function ModeToggle({ mode, chambers, onMode, onChambers }: Props): JSX.Element {
  return (
    <div className="mode-toggle">
      <div className="mode-buttons">
        <button className={mode === 'electric' ? 'active' : ''} onClick={() => onMode('electric')}>电力模式 (产 EU)</button>
        <button className={mode === 'fluid' ? 'active' : ''} onClick={() => onMode('fluid')}>流体冷却模式 (产热冷却液)</button>
      </div>
      <div className="chambers-control">
        <label>反应堆腔室: {chambers}（{3 + chambers}列 × 9行 = {(3 + chambers) * 9}格）</label>
        <input
          type="range"
          min={0}
          max={6}
          value={chambers}
          onChange={(e) => onChambers(Number((e.target as HTMLInputElement).value))}
        />
      </div>
    </div>
  );
}
