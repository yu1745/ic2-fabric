// 元件栏：按 category 分组，可拖放到网格。

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
  onPick?: (id: ComponentId) => void; // 点击放置到首个空格（移动端备选）
}

export function ComponentPalette({ onPick }: Props): JSX.Element {
  return (
    <div className="palette">
      {PALETTE.map((group) => (
        <div key={group.category} className="palette-group">
          <div className="palette-group-title">{CATEGORY_LABEL[group.category] ?? group.category}</div>
          <div className="palette-items">
            {group.items.map((meta) => (
              <div
                key={meta.id}
                className="palette-item"
                draggable
                onDragStart={(e) => {
                  e.dataTransfer?.setData('text/component-id', meta.id);
                  e.dataTransfer && (e.dataTransfer.effectAllowed = 'copy');
                }}
                onClick={() => onPick?.(meta.id)}
                title={`${meta.name}（拖到网格或点击放置）`}
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
