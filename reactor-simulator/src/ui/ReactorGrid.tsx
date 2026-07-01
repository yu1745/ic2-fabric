// 反应堆网格：9 行 × (3..9) 列。拖放入元件、右键移除、显示耐久条与产热色。
// 选中态下：单击空格放置；按住鼠标拖过空格可连续批量放置。
// 悬停格子显示详情：耐久、本周期产热/散热/发电、燃料棒全生命周期发电、其它元件损坏 cycle 预测。

import { type JSX } from 'preact';
import { useEffect, useMemo, useState } from 'preact/hooks';
import { ROWS, simulateCycle } from '../sim';
import type { ComponentId, CycleStats, Grid, ReactorMode, Slot } from '../sim';
import { getMeta } from '../sim';

interface Props {
  grid: Grid;
  chambers: number;
  stats: CycleStats | null;
  /** 当前模式（tooltip 损坏预测 simulateCycle 用） */
  mode: ReactorMode;
  /** 当前选中元件（连续放置态）：非 null 时空格单击/拖动直接放置该元件 */
  selected: ComponentId | null;
  onPlace: (index: number, id: ComponentId) => void;
  onClear: (index: number) => void;
  /** 全寿命模拟得到的每个燃料棒累计发电（slotIndex → EU），来自 App 级 simulateFullLife */
  perSlotLifeEu: Map<number, number> | null;
}

const cols = (chambers: number) => 3 + chambers;

export function ReactorGrid({ grid, chambers, stats, mode, selected, onPlace, onClear, perSlotLifeEu }: Props): JSX.Element {
  const c = cols(chambers);
  const slotHeat = stats?.slotHeat;
  // 当前按下的鼠标按钮：null=未按下, 0=左键(放置), 2=右键(移除)
  const [activeButton, setActiveButton] = useState<number | null>(null);
  // 当前 hover 的格子 slotIndex（列优先），null=未悬停
  const [hoverSlot, setHoverSlot] = useState<number | null>(null);

  // 全局 mouseup 释放（即便鼠标移出网格也能复位）
  useEffect(() => {
    const up = () => setActiveButton(null);
    window.addEventListener('mouseup', up);
    return () => window.removeEventListener('mouseup', up);
  }, []);

  // 选中态下尝试放置到空格（已占用格忽略，由 useReactor place action 保证）
  const tryPlace = (slotIndex: number, slot: Slot | null) => {
    if (selected && !slot) onPlace(slotIndex, selected);
  };

  // tooltip 的「损坏预测」：跑 1 个 cycle 取 hover slot 的 use 增量。
  // 仅在 hoverSlot 变化时重算（依赖 grid/chambers/mode 也变就重算）。
  const hoverInfo = useMemo(() => {
    if (hoverSlot === null) return null;
    const before = grid[hoverSlot];
    if (!before) return null;
    const beforeUse = before.use;
    const res = simulateCycle(grid, chambers, mode, 0);
    const after = res.grid[hoverSlot];
    const afterUse = after ? after.use : -1; // -1 = 这周期内烧毁了
    const delta = after ? afterUse - beforeUse : null;
    const maxUse = maxUseFor(before.id);
    // 损坏 cycle 预测：use 增量 > 0 时，(maxUse - 当前use) / 增量；≤0 则不会损坏
    let doom: number | null = null;
    if (maxUse > 0 && delta !== null && delta > 0) {
      doom = Math.floor((maxUse - beforeUse) / delta);
    }
    return { slot: before, beforeUse, delta, doom, burned: !after };
  }, [hoverSlot, grid, chambers, mode]);

  return (
    <div
      className={'reactor-grid' + (selected ? ' reactor-grid-placing' : '')}
      style={{ gridTemplateColumns: `repeat(${c}, 1fr)`, gridTemplateRows: `repeat(${ROWS}, 1fr)` }}
      onMouseLeave={() => setActiveButton(null)}
    >
      {Array.from({ length: c * ROWS }, (_v, idx) => {
        // CSS grid 行优先填充：idx = row*cols + col。
        // sim 内部是列优先：slot index = col*9 + row（col=x 列, row=y 行）。
        // 这里按行优先遍历渲染，再用 (col,row) 算出 sim 的列优先 index 读写 grid。
        const row = Math.floor(idx / c);
        const col = idx % c;
        const slotIndex = col * ROWS + row; // sim 列优先
        const slot = grid[slotIndex];
        const meta = slot ? getMeta(slot.id) : null;
        const heatInfo = slotHeat?.get(slotIndex);
        // 产热色：>0 红，散热 >0 蓝
        const borderColor = heatInfo
          ? heatInfo.produced > 0
            ? '#ff5555'
            : heatInfo.dissipated > 0
              ? '#5599ff'
              : '#444'
          : '#444';
        // 选中态下空格高亮（提示可放置）
        const placingEmpty = selected && !slot;
        return (
          <div
            key={idx}
            className={'grid-slot' + (placingEmpty ? ' grid-slot-placeable' : '')}
            style={{ borderColor }}
            onDragOver={(e) => e.preventDefault()}
            onDrop={(e) => {
              const id = e.dataTransfer?.getData('text/component-id') as ComponentId | '';
              if (id) onPlace(slotIndex, id);
            }}
            // 左键按下：选中态放置；右键按下：移除（记录按钮供拖动延续）
            onMouseDown={(e) => {
              setActiveButton(e.button);
              if (e.button === 0) {
                // 左键：选中态放置
                tryPlace(slotIndex, slot);
              } else if (e.button === 2) {
                // 右键：移除（单击或拖动）
                if (slot) onClear(slotIndex);
              }
            }}
            onMouseEnter={() => {
              setHoverSlot(slotIndex);
              if (activeButton === 0) tryPlace(slotIndex, slot);
              else if (activeButton === 2 && slot) onClear(slotIndex);
            }}
            onMouseLeave={() => setHoverSlot(null)}
            onContextMenu={(e) => {
              // 阻止右键菜单（右键用于移除）
              e.preventDefault();
            }}
            title={meta ? `${meta.name} (col ${col + 1}, row ${row + 1})` : `空 (col ${col + 1}, row ${row + 1})`}
          >
            {meta && slot && (
              <SlotContent id={slot.id} use={slot.use} maxUse={maxUseFor(slot.id)} />
            )}
          </div>
        );
      })}
      {hoverInfo && (
        <SlotTooltip
          info={hoverInfo}
          heatInfo={slotHeat?.get(hoverSlot ?? -1) ?? null}
          lifeEu={hoverSlot !== null ? (perSlotLifeEu?.get(hoverSlot) ?? null) : null}
        />
      )}
    </div>
  );
}

function SlotContent({ id, use, maxUse }: { id: ComponentId; use: number; maxUse: number }): JSX.Element {
  const meta = getMeta(id);
  const frac = maxUse > 0 ? 1 - use / maxUse : 1;
  const barColor = frac > 0.75 ? '#00ff00' : frac > 0.5 ? '#ffdd00' : frac > 0.25 ? '#ffaa00' : '#ff0000';
  // 耐久条仅对有 maxUse 的元件显示（燃料棒/散热片/交换器/冷却单元/冷凝器/反射板）
  const showBar = maxUse > 0;
  return (
    <div className="slot-content">
      <img src={`./textures/${meta.texture}.png`} alt={meta.name} draggable={false} />
      {showBar && (
        <div className="durability-bar">
          <div className="durability-fill" style={{ width: `${Math.max(0, Math.min(100, frac * 100))}%`, background: barColor }} />
        </div>
      )}
    </div>
  );
}

// hover 详情浮层。
interface HoverInfo { slot: Slot; beforeUse: number; delta: number | null; doom: number | null; burned: boolean }
function SlotTooltip({ info, heatInfo, lifeEu }: {
  info: HoverInfo;
  heatInfo: { produced: number; dissipated: number; energy: number } | null;
  lifeEu: number | null;
}): JSX.Element {
  const { slot, beforeUse, delta, doom, burned } = info;
  const meta = getMeta(slot.id);
  const maxUse = maxUseFor(slot.id);
  const isFuelRod = slot.id.endsWith('_fuel_rod') && !slot.id.startsWith('depleted');
  const lines: Array<{ label: string; value: string; color?: string }> = [];
  lines.push({ label: '元件', value: meta.name });
  if (maxUse > 0) {
    const pct = ((1 - beforeUse / maxUse) * 100).toFixed(1);
    lines.push({ label: '耐久', value: `${maxUse - beforeUse} / ${maxUse} (${pct}%)` });
  }
  if (heatInfo) {
    if (heatInfo.produced > 0) lines.push({ label: '产热/cycle', value: `${heatInfo.produced} HU`, color: '#ff8080' });
    if (heatInfo.dissipated > 0) lines.push({ label: '散热/cycle', value: `${heatInfo.dissipated} HU`, color: '#80c0ff' });
    if (heatInfo.energy > 0) lines.push({ label: '发电/cycle', value: `${heatInfo.energy} (×100 EU)` , color: '#ffe080' });
  }
  if (isFuelRod && lifeEu !== null) {
    lines.push({ label: '全生命周期发电', value: formatEuBig(lifeEu), color: '#ffe080' });
  }
  if (delta !== null && maxUse > 0 && !isFuelRod) {
    if (delta > 0) {
      lines.push({ label: '每周期耐久增量', value: `+${delta}`, color: '#ff8080' });
      if (doom !== null) lines.push({ label: '预计损坏', value: `${doom} cycle 后`, color: '#ff6060' });
    } else if (delta < 0) {
      lines.push({ label: '每周期耐久增量', value: `${delta}`, color: '#80ff80' });
      lines.push({ label: '预计损坏', value: '不会（在降温）', color: '#80ff80' });
    } else {
      lines.push({ label: '耐久变化', value: '稳定', color: '#a0a0a0' });
    }
  }
  if (isFuelRod) {
    if (delta !== null && delta > 0 && doom !== null) {
      lines.push({ label: '预计耗尽', value: `${doom} cycle 后`, color: '#ffe080' });
    }
  }
  if (burned) {
    lines.push({ label: '⚠ 本周期', value: '已烧毁', color: '#ff4040' });
  }
  return (
    <div className="slot-tooltip">
      {lines.map((ln, i) => (
        <div key={i} className="tooltip-row">
          <span className="tooltip-label">{ln.label}</span>
          <span className="tooltip-value" style={ln.color ? { color: ln.color } : undefined}>{ln.value}</span>
        </div>
      ))}
    </div>
  );
}

function formatEuBig(eu: number): string {
  if (eu >= 1e6) return `${(eu / 1e6).toFixed(2)} M EU`;
  if (eu >= 1e3) return `${(eu / 1e3).toFixed(1)} k EU`;
  return `${Math.round(eu)} EU`;
}

// 各元件 maxUse（耐久/热量上限），用于耐久条。与 constants 对齐。
function maxUseFor(id: ComponentId): number {
  // 燃料棒
  const fuel: Record<string, number> = {
    uranium_fuel_rod: 20000, dual_uranium_fuel_rod: 20000, quad_uranium_fuel_rod: 20000,
    mox_fuel_rod: 10000, dual_mox_fuel_rod: 10000, quad_mox_fuel_rod: 10000,
  };
  if (id in fuel) return fuel[id];
  // 散热片（heatStorage）
  const vent: Record<string, number> = {
    heat_vent: 1000, reactor_heat_vent: 1000, advanced_heat_vent: 1000, overclocked_heat_vent: 1000,
  };
  if (id in vent) return vent[id];
  // 热交换器
  const ex: Record<string, number> = {
    heat_exchanger: 2500, reactor_heat_exchanger: 5000, component_heat_exchanger: 5000, advanced_heat_exchanger: 10000,
  };
  if (id in ex) return ex[id];
  // 冷却单元
  const cc: Record<string, number> = {
    reactor_coolant_cell: 10000, triple_reactor_coolant_cell: 30000, sextuple_reactor_coolant_cell: 60000,
  };
  if (id in cc) return cc[id];
  // 冷凝器
  const cond: Record<string, number> = { rsh_condensator: 20000, lzh_condensator: 100000 };
  if (id in cond) return cond[id];
  // 反射板
  const refl: Record<string, number> = { neutron_reflector: 30000, thick_neutron_reflector: 120000 };
  if (id in refl) return refl[id];
  return 0; // 隔板/铱反射板/枯竭棒/元件散热片 无耐久条
}
