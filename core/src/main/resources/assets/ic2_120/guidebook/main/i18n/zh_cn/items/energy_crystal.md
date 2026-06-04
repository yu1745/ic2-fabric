---
navigation:
  title: 能量水晶与兰波顿水晶
  parent: index.md
  position: 321
  icon: ic2_120:energy_crystal
item_ids:
  - ic2_120:energy_crystal
  - ic2_120:lapotron_crystal
  - ic2_120:energy_crystal_wireless
  - ic2_120:lapotron_crystal_wireless
---

# 能量水晶与兰波顿水晶

<ItemImage id="ic2_120:energy_crystal" scale="4" />

<ItemLink id="ic2_120:energy_crystal" /> 与 <ItemLink id="ic2_120:lapotron_crystal" /> 是 IC2 中两个最高等级的手持可充电电池。它们是模组中耗电最高的电动装备（采矿镭射枪、喷气背包及类似终期工具）的标准电源。

## 概述

两种水晶在可于任意储电方块中充电这一点上与充电电池相同，但它们被设计用于持续的高功率放电。一颗兰波顿水晶充满后，足以让采矿镭射枪在多次挖掘隧道后才需要再次充电。

| 物品 | 等级 | 最大 EU | 说明 |
|------|:---:|---:|-------|
| <ItemLink id="ic2_120:energy_crystal" /> | 3 | 可配置 | 中后期电池，是能量背包的升级材料。 |
| <ItemLink id="ic2_120:lapotron_crystal" /> | 4 | 可配置 | 最高等级的手持电池，是兰波顿背包的升级材料。 |

默认值可在模组的 `Ic2Config` 中调整；具体容量与传输速率请查看对应版本配置文件。包含充电电池系列的标准阶梯见 [电池与移动供电](../reference/energy_items.md)。

## 方块视图

| 能量水晶 | 兰波顿水晶 |
|:---:|:---:|
| <ItemImage id="ic2_120:energy_crystal" scale="2" /> | <ItemImage id="ic2_120:lapotron_crystal" scale="2" /> |

## 使用方式

### 为高耗电工具供电

当某件工具的每 tick EU 消耗超出充电电池可舒适供应的范围时，应使用水晶。采矿镭射枪、喷气背包、电动链锯与采矿钻头都能从更高等级中受益——它们可以以更高功率拉电，并在多次使用之间续航更久。

### 电池背包升级

水晶还是最高等级电池背包的合成材料：

- <ItemLink id="ic2_120:energy_pack" />——在工作台中将 <ItemLink id="ic2_120:advanced_batpack" /> 与 <ItemLink id="ic2_120:energy_crystal" /> 合成来升级。
- <ItemLink id="ic2_120:lappack" />——将 <ItemLink id="ic2_120:energy_pack" /> 与 <ItemLink id="ic2_120:lapotron_crystal" /> 合成来升级。

将得到的背包装备在胸甲栏中，会自动为物品栏内等级不高于背包等级的电动工具补电。

### 无线型号

两种水晶都存在无线版本。只要它们在玩家物品栏中任意位置，就会被动从周围的 EU 网络中拉电，并为玩家手持或穿戴的电动工具补电。能量水晶与兰波顿水晶两个等级都有无线版本。

| 无线水晶 | 等级 | 说明 |
|------|:---:|-------|
| <ItemLink id="ic2_120:energy_crystal_wireless" /> | 3 | 与能量水晶同等级，具备被动补电行为。 |
| <ItemLink id="ic2_120:lapotron_crystal_wireless" /> | 4 | 与兰波顿水晶同等级，具备被动补电行为。 |

## 配方

水晶的配方定义在模组的数据驱动配方系统中。具体材料可使用 JEI 查看，或打开下方代码片段引用的配方文件。

<Recipe id="ic2_120:energy_crystal" />
<Recipe id="ic2_120:lapotron_crystal" />

## 相关

- [工具与防护装备](../reference/tools_armor.md)
- [电池与移动供电](../reference/energy_items.md)
- [可充电电池系列](re_battery.md)——较低的两个电池等级
- [采矿镭射枪](mining_laser.md)——这些水晶的典型高耗电使用者
