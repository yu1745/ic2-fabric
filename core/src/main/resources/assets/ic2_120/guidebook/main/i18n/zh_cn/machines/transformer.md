---
navigation:
  title: 变压器
  parent: index.md
  position: 48
  icon: ic2_120:lv_transformer
item_ids:
  - ic2_120:lv_transformer
  - ic2_120:mv_transformer
  - ic2_120:hv_transformer
  - ic2_120:ev_transformer
---

# 变压器

<BlockImage id="ic2_120:lv_transformer" scale="4" />

变压器用于在不同电压等级之间转换电能。它们对于安全地连接不同电压等级的机器和储电设备至关重要。每个变压器有一个低压面（带有标记点）和五个高压面。

## 等级对比

| 等级 | 名称 | 低压侧 | 高压侧 |
|------|------|--------|--------|
| 1 | 低压变压器 | 32 EU/t | 128 EU/t |
| 2 | 中压变压器 | 128 EU/t | 512 EU/t |
| 3 | 高压变压器 | 512 EU/t | 2,048 EU/t |
| 4 | 超高压变压器 | 2,048 EU/t | 8,192 EU/t |

## 模式

### 降压模式（默认）
电力从五个高压面流向带有标记点的单一面。变压器将一个高压包转换为四个低压包。例如，中压变压器在标记面接收 128 EU/t，将在其他五个面输出 32 EU/t × 4。

### 升压模式（潜行 + 扳手）
潜行右键使用扳手切换到升压模式。电力从标记面流向五个周围面。变压器将低压包合并为一个高压包。在此模式下，五个面输入 32 EU/t × 4 将转换为标记面输出 128 EU/t。

## 视觉指示

- 带有一个点的面是**低压**（标记）面。
- 纹理上的红石粉表示高压面。
- 将标记面朝向应接收或提供较低电压的一侧。

## 配方

<Recipe id="ic2_120:lv_transformer" />
<Recipe id="ic2_120:mv_transformer" />
<Recipe id="ic2_120:hv_transformer" />
<Recipe id="ic2_120:ev_transformer" />
