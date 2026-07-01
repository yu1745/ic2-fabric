// 仪表盘：堆温条、EU/t、产热/散热/净热、冷却液产出、爆炸预测、燃料状态。

import { type JSX } from 'preact';
import { HEAT_EXPLODE_THRESHOLD, HEAT_FIRE_THRESHOLD } from '../sim';
import type { CycleStats, ReactorMode } from '../sim';

interface Props {
  stats: CycleStats | null;
  heat: number;
  mode: ReactorMode;
  cycle: number;
  running: boolean;
  exploded: boolean;
  /** 全寿命累计总发电（EU），来自 estimateFullLifeOutput；null=无燃料棒 */
  lifeTotalEu: number | null;
  /** 直接设置起始堆温 */
  onSetHeat: (heat: number) => void;
}

export function Dashboard({ stats, heat, mode, cycle, running, exploded, lifeTotalEu, onSetHeat }: Props): JSX.Element {
  const maxHeat = stats?.maxHeat ?? HEAT_EXPLODE_THRESHOLD;
  const heatPct = Math.max(0, Math.min(100, (heat / maxHeat) * 100));
  const heatColor = heat >= HEAT_EXPLODE_THRESHOLD
    ? '#ff0000'
    : heat >= HEAT_FIRE_THRESHOLD
      ? '#ff8800'
      : '#44aaff';

  const euPerTick = stats?.euPerTick ?? 0;
  const produced = stats?.heatProduced ?? 0;
  const dissipated = stats?.heatDissipated ?? 0;
  const net = stats?.netHeat ?? 0;
  const netColor = net > 0 ? '#ff5555' : net < 0 ? '#55ff55' : '#aaaaaa';

  return (
    <div className="dashboard">
      <div className="dashboard-row">
        <span className="label">堆温</span>
        <div className="heat-bar">
          <div className="heat-fill" style={{ width: `${heatPct}%`, background: heatColor }} />
          <span className="heat-text">{Math.round(heat)} / {maxHeat}</span>
        </div>
        <input
          className="heat-input"
          type="number"
          min={0}
          max={10000}
          value={Math.round(heat)}
          title="直接设置起始堆温（0-10000）"
          onChange={(e) => {
            const v = Number((e.target as HTMLInputElement).value);
            if (!Number.isNaN(v)) onSetHeat(v);
          }}
        />
      </div>

      <div className="dashboard-grid">
        {mode === 'electric' ? (
          <div className="stat">
            <span className="stat-label">EU/t</span>
            <span className="stat-value">{euPerTick.toFixed(1)}</span>
          </div>
        ) : (
          <div className="stat">
            <span className="stat-label">热冷却液 (mB/cycle)</span>
            <span className="stat-value">{(stats?.hotCoolantOutputMb ?? 0).toFixed(1)}</span>
          </div>
        )}
        <div className="stat">
          <span className="stat-label">产热 (HU/cycle)</span>
          <span className="stat-value">{produced}</span>
        </div>
        <div className="stat">
          <span className="stat-label">散热 (HU/cycle)</span>
          <span className="stat-value">{dissipated}</span>
        </div>
        <div className="stat">
          <span className="stat-label">净热 (HU/cycle)</span>
          <span className="stat-value" style={{ color: netColor }}>{net > 0 ? '+' : ''}{net}</span>
        </div>
      </div>

      {lifeTotalEu !== null && (
        <div className="stat stat-wide">
          <span className="stat-label">全寿命总发电 (EU)</span>
          <span className="stat-value">{formatEu(lifeTotalEu)}</span>
        </div>
      )}

      <div className="status-row">
        {exploded ? (
          <span className="status status-exploded">💥 已熔毁！爆炸威力 {stats?.explosionPower.toFixed(1)}</span>
        ) : stats && !stats.hasFuelRods ? (
          <span className="status status-idle">燃料已耗尽</span>
        ) : net > 0 ? (
          <span className="status status-warning">⚠ 产热 {'>'} 散热，将持续升温直至熔毁</span>
        ) : net < 0 ? (
          <span className="status status-ok">✓ 散热充足，堆温将下降</span>
        ) : running ? (
          <span className="status status-ok">运行中…</span>
        ) : (
          <span className="status status-idle">已就绪</span>
        )}
        <span className="cycle-count">cycle: {cycle}</span>
      </div>
    </div>
  );
}

// EU 格式化：>1e6 显示 M，>1e3 显示 k
function formatEu(eu: number): string {
  if (eu >= 1e6) return `${(eu / 1e6).toFixed(2)} M`;
  if (eu >= 1e3) return `${(eu / 1e3).toFixed(1)} k`;
  return `${Math.round(eu)}`;
}
