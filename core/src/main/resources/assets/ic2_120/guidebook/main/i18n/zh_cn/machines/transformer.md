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

变压器用于在相邻 EU 电压等级之间转换，并不改变 EU 总量。正面是高一级电压侧，其余五个面是低一级电压侧。升压/降压模式通过 GUI 切换。

## 等级对比

| 等级 | 名称 | 低压侧 | 高压侧 |
|------|------|--------|--------|
| 1 | 低压变压器 | 32 EU/t | 128 EU/t |
| 2 | 中压变压器 | 128 EU/t | 512 EU/t |
| 3 | 高压变压器 | 512 EU/t | 2,048 EU/t |
| 4 | 超高压变压器 | 2,048 EU/t | 8,192 EU/t |

## 模式

变压器默认处于降压模式。

### 升压模式
五个非正面接收低一级 EU，正面输出高一级 EU。变压器会先积累到足够一次高压输出的能量，再从正面输出，减少低量零散输出造成的损耗。

### 降压模式
正面接收高一级 EU，其余五面输出低一级 EU。用它把高等级储能或发电线路安全降给低等级机器和电缆。

## 朝向与缓冲

- **正面：** 高一级电压侧。
- **其他五面：** 低一级电压侧。
- **模式切换：** 使用变压器 GUI。
- **内部缓冲：** 低压 512 EU，中压 2,048 EU，高压 8,192 EU，超高压 32,768 EU。

把变压器放在电缆和机器等级不匹配的位置。例如用低压变压器降压时，让正面朝向 CESU/中压线路，其余面接低压机器。

## 配方

<Recipe id="ic2_120:lv_transformer" />
<Recipe id="ic2_120:mv_transformer" />
<Recipe id="ic2_120:hv_transformer" />
<Recipe id="ic2_120:ev_transformer" />
