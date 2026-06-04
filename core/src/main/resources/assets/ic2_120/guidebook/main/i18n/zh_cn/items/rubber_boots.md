---
navigation:
  title: 橡胶靴
  parent: index.md
  position: 305
  icon: ic2_120:rubber_boots
item_ids:
  - ic2_120:rubber_boots
---

# 橡胶靴

<ItemImage id="ic2_120:rubber_boots" scale="4" />

橡胶靴是一件混合型实用物品：它既是护甲，也是发电机，还是[防化服](hazmat_armor.md)套装的关键组件。它在游戏早期就可以用 <ItemLink id="ic2_120:rubber" /> 制作，并且从前期一直有用到后期构筑——因为它在玩家行走时会对物品栏中的物品被动充电。

## 方块视图

<ItemImage id="ic2_120:rubber_boots" scale="4" />

## 属性

| 槽位 | 物品 | 护甲值 | 耐久倍率 | 修复材料 |
|------|------|:---:|:---:|---|
| 靴子 | rubber_boots | 1 | 5x | <ItemLink id="ic2_120:rubber" /> |

靴子自身不存储 EU；它通过移动产生电力，并实时推送到物品栏中的物品上。

## 使用方式

### 行走式充电器

穿戴时，靴子会监测玩家的行走距离。每行走 `1.0 m`（在 mod 的通用配置中可调；默认为每步 1.0 m）水平距离，靴子就会向玩家物品栏中的可充电物品推送 `4 EU`（可配置）。充电量随靴子自身耐久度缩放，玩家即使不冲刺也能持续工作。

- 冲刺玩家会更快填满电池，因为每 tick 的移动都会计入距离阈值。
- 充电优先级遵循 IC2 的标准排序：先高等级电池，再低等级，再部分放电的物品。
- 走过单个方块通常就足以触发一次充电循环。

### 移动与绝缘效果

- **防滑着陆**——从任意高度摔落到靴子上时，会像原版附魔“摔落保护”那样消除摔落伤害。靴子被视为柔软、带缓冲的表面。
- **电气绝缘**——接触裸露、未绝缘的导线不会受到电击伤害。这与[防化服](hazmat_armor.md)的效果一致，因为橡胶靴正是该套装的靴子槽位。
- **与防化服的套装加成**——与三件防化服同时穿戴时，玩家会获得完全的辐射免疫，且防化头盔可提供水下呼吸。

### 何时停止充电

行走式充电器在以下情况下不会推送 EU：

- 玩家静止不动或在空中（跳跃中、下落中）。
- 玩家正在潜行且相关配置项（`chargeWhileSneaking`）被设置为 `false`。
- 物品栏中没有可充电的物品。
- 靴子已损坏（耐久度为 0）。

## 配方

<Recipe id="ic2_120:rubber_boots" />

配方材料：<ItemLink id="ic2_120:rubber" /> 和 <ItemLink id="minecraft:white_wool" />。

## 相关

- [工具与防护装备](../reference/tools_armor.md)
- [电池与移动供电](../reference/energy_items.md)
- [防化服](hazmat_armor.md)——使用这双靴子作为第四个槽位
- [橡胶树与世界资源](../reference/rubber_and_worldgen.md)
