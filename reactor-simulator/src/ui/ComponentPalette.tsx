// 元件栏：按 category 分组。
// - 单击：放到首个空格（旧行为保留）
// - 双击：进入「选中态」，之后在网格空格单击即可连续放置该元件；再次双击同一元件或双击别的元件切换选中
// 拖放仍保留（HTML5 drag&drop）。

import { type JSX } from 'preact';
import { PALETTE } from '../sim';
import type { ComponentId } from '../sim';

const CATEGORY_LABEL: Record<string, string> = {
  fuel: '燃料棒',
  vent: '散热片',
  exchanger: '热交换器',
  coolant: '冷却单元',
  condensator: '冷凝器',
  plating: '隔板',
  reflector: '中子反射板',
};

interface Props {
  /** 当前选中的元件（双击态），null = 未选中 */
  selected: ComponentId | null;
  onPick?: (id: ComponentId) => void;       // 单击：放首个空格
  onSelect?: (id: ComponentId) => void;     // 双击：进入/切换选中态
}

export function ComponentPalette({ selected, onPick, onSelect }: Props): JSX.Element {
  return (
    <div className="palette">
      {selected && (
        <div className="palette-selected-hint">
          已选中「{nameOf(selected)}」— 单击网格空格放置，再次双击或按 ESC 取消
        </div>
      )}
      {PALETTE.map((group) => (
        <div key={group.category} className="palette-group">
          <div className="palette-group-title">{CATEGORY_LABEL[group.category] ?? group.category}</div>
          <div className="palette-items">
            {group.items.map((meta) => (
              <div
                key={meta.id}
                className={'palette-item' + (selected === meta.id ? ' palette-item-selected' : '')}
                draggable
                onDragStart={(e) => {
                  e.dataTransfer?.setData('text/component-id', meta.id);
                  e.dataTransfer && (e.dataTransfer.effectAllowed = 'copy');
                }}
                onClick={() => onPick?.(meta.id)}
                onDblClick={() => onSelect?.(meta.id)}
                title={`${meta.name}（单击放置 · 双击选中后连续放置 · 可拖动）`}
              >
                <img src={`./textures/${meta.texture}.png`} alt={meta.name} draggable={false} />
                <span className="palette-item-name">{meta.name}</span>
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}

import { getMeta } from '../sim';
function nameOf(id: ComponentId): string {
  try { return getMeta(id).name; } catch { return id; }
}
