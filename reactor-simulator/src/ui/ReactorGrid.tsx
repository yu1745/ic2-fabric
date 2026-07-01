// 反应堆网格：9 行 × (3..9) 列。拖放入元件、右键移除、显示耐久条与产热色。

import { type JSX } from 'preact';
import { ROWS } from '../sim';
import type { ComponentId, CycleStats, Grid } from '../sim';
import { getMeta } from '../sim';

interface Props {
  grid: Grid;
  chambers: number;
  stats: CycleStats | null;
  /** 当前选中的元件（双击态）：非 null 时空格单击直接放置该元件 */
  selected: ComponentId | null;
  onPlace: (index: number, id: ComponentId) => void;
  onClear: (index: number) => void;
}

const cols = (chambers: number) => 3 + chambers;

export function ReactorGrid({ grid, chambers, stats, selected, onPlace, onClear }: Props): JSX.Element {
  const c = cols(chambers);
  const slotHeat = stats?.slotHeat;

  return (
    <div
      className={'reactor-grid' + (selected ? ' reactor-grid-placing' : '')}
      style={{ gridTemplateColumns: `repeat(${c}, 1fr)`, gridTemplateRows: `repeat(${ROWS}, 1fr)` }}
    >
      {Array.from({ length: c * ROWS }, (_v, idx) => {
        const x = Math.floor(idx / ROWS);
        const y = idx % ROWS;
        const slot = grid[idx];
        const meta = slot ? getMeta(slot.id) : null;
        const heatInfo = slotHeat?.get(idx);
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
              if (id) onPlace(idx, id);
            }}
            onClick={() => {
              // 选中态：空格单击放置选中元件（已占用格忽略）
              if (selected && !slot) onPlace(idx, selected);
            }}
            onContextMenu={(e) => {
              e.preventDefault();
              if (slot) onClear(idx);
            }}
            title={meta ? `${meta.name} (col ${x + 1}, row ${y + 1})` : `空 (col ${x + 1}, row ${y + 1})`}
          >
            {meta && slot && (
              <SlotContent id={slot.id} use={slot.use} maxUse={maxUseFor(slot.id)} />
            )}
          </div>
        );
      })}
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
