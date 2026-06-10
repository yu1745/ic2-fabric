---
navigation:
  title: 太阳能头盔
  parent: index.md
  position: 304
  icon: ic2_120:solar_helmet
item_ids:
  - ic2_120:solar_helmet
---

# 太阳能头盔

<ItemImage id="ic2_120:solar_helmet" scale="4" />

太阳能头盔是一件可穿戴的充电设备。当玩家在白天佩戴于头盔槽位、且头顶上方天空未被遮挡时，头盔会以 `1 EU/t`（每 tick 1 EU）的速度对玩家物品栏中任何可充电的 EU 物品进行涓流充电。充电进行时，玩家还会获得一个被动的 `Solar Generating` 状态效果。

## 物品视图

<ItemImage id="ic2_120:solar_helmet" scale="4" />

## 属性

| 槽位 | 物品 | 护甲值 | 耐久倍率 | 修复材料 |
|------|------|:---:|:---:|---|
| 头盔 | solar_helmet | 2 | 8x | <ItemLink id="ic2_120:bronze_ingot" /> |

太阳能头盔没有内部 EU 缓冲；它是被动发电机，会把电力直接推送给你的物品。它在发电时也不消耗 EU——能量来源是阳光。

## 使用方式

### 穿戴与充电

- 把太阳能头盔装备到头盔槽位。
- 站在**日光**下，头顶有清晰的天空视野。不透明方块、玻璃（除特定透明玻璃外）、头顶有水、以及在室内都会中断充电。
- 激活时，头盔会以 `1 EU/t` 的速度向玩家物品栏中第一个可充电物品推送电力。内部优先级与 IC2 标准电池充电顺序一致：先高等级低电量，再低等级，因此高等级电池在部分放电时优先于低等级电池被填满。
- 充电进行时，玩家会获得 `Solar Generating` 状态效果。该效果只是视觉/状态指示，本身没有任何游戏性效果。

### 何时停止充电

太阳能头盔在以下任一条件下都会停止推送 EU：

- 玩家处于没有天空的维度（例如下界或末地）——这些维度在 mod 中被视为“无阳光”。
- 玩家在地下、室内或头顶有非透明方块遮挡。
- 玩家手中只有不可充电的物品——没有可推送 EU 的目标。
- 玩家正在潜行（可配置；默认潜行时也保持充电）。

### 与其他充电源的协同

太阳能头盔可与 IC2 其他充电源叠加：站在[充电板](../machines/chargepad.md)上、穿戴[能量背包 / 锂能量背包](../reference/energy_items.md)、或物品栏中放有无线电池，这些方式可以并行充电。太阳能头盔始终只贡献自身的 `1 EU/t`。

## 配方

<Recipe id="ic2_120:solar_helmet" />

配方材料：铁锭、<ItemLink id="ic2_120:solar_generator" /> 和 <ItemLink id="ic2_120:insulated_copper_cable" />。

## 相关

- [工具与防护装备](../reference/tools_armor.md)
- [电池与移动供电](../reference/energy_items.md)
- [太阳能发电机](../machines/solar_generator.md)——同一思路的方块版本
- [青铜护甲](bronze_armor.md)
