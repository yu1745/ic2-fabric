# Docs 导航（ic2_120）

本目录已按用途重排，目标是减少重复、降低新成员上手成本。

## 快速入口

- 新增一台机器：`docs/guides/machine-implementation-guide.md`
- 新增一个物品：`docs/guides/item-implemented.md`
- 注解注册系统：`docs/registry/CLASS_BASED_REGISTRY.md`
- UI DSL 总览：`docs/ui/compose-ui.md`

## 目录结构

### guides（实施指南）

- `docs/guides/machine-implementation-guide.md`
- `docs/guides/machine-composition-reuse.md`
- `docs/guides/item-implemented.md`

### systems（子系统设计）

- `docs/systems/energy-network.md`
- `docs/systems/energy-flow-sync.md`
- `docs/systems/fluid-system.md`
- `docs/systems/heat-system.md`
- `docs/systems/nuclear-power.md`
- `docs/systems/upgrade-system.md`
- `docs/systems/sync-system.md`
- `docs/systems/sound-system.md`
- `docs/systems/jei-integration.md`
- `docs/systems/crop-hybrid-system.md`
- `docs/systems/crop-growth-requirements.md`
- `docs/systems/crop-player-guide.md`

### ui（界面与槽位）

- `docs/ui/compose-ui.md`
- `docs/ui/slot-spec-system.md`
- `docs/ui/drawcontext-methods.md`
- `docs/ui/canner-ui-coordinates.md`
- `docs/compose-ui/quick-start.md`
- `docs/compose-ui/elements.md`
- `docs/compose-ui/containers.md`
- `docs/compose-ui/scrollview.md`
- `docs/compose-ui/slot-anchor-pipeline.md`
- `docs/compose-ui/architecture.md`

### registry（注册与变体）

- `docs/registry/CLASS_BASED_REGISTRY.md`
- `docs/registry/block-variants.md`
- `docs/registry/biome-colored-blocks.md`

### inventory（资源清单）

- `docs/inventory/assets-inventory.md`
- `docs/inventory/sounds-inventory.md`

### pitfalls（踩坑记录）

- `docs/pitfalls/common-pitfalls.md`

### archive（归档/TODO）

- `docs/archive/unique-gift-item-anti-dup-todo.md`
- `docs/archive/transmission_shaft.md`

## 重复内容处理约定

- 注解注册规则只在 `docs/registry/CLASS_BASED_REGISTRY.md` 维护。
- 机器实现文档只保留机器流程与模板，系统细节统一链接到 `docs/systems/*`。
- 清单型文档只放在 `docs/inventory/*`，避免在实施指南中重复维护同一份列表。
