---
navigation:
  title: 动能发电机
  parent: index.md
  position: 20
  icon: ic2_120:kinetic_generator
item_ids:
  - ic2_120:kinetic_generator
---

# 动能发电机

<BlockImage id="ic2_120:kinetic_generator" p:facing="north" p:active="true" scale="4" />

动能发电机将动能传输网络中的动能（KU）转换为 EU。它从正面连接的传动轴、斜齿轮或其他动能发生器拉取 KU，转换比例为 4 KU = 1 EU。

该机器是动能系统与电力系统之间的主要接口。它属于 3 级设备，可处理最高 2,048 KU/t 的输入并转换为最高 512 EU/t 的输出。

## 输出

- **转换比例**：4 KU = 1 EU
- **最大 KU 输入**：2,048 KU/t
- **最大 EU 输出**：512 EU/t
- **能量缓存**：10,000 EU
- **等级**：3

## 槽位

动能发电机没有物品槽。它通过正面从动能网络接收 KU。

动能发电机从除正面以外的所有方向输出 EU。

## 配方

<Recipe id="ic2_120:kinetic_generator" />

## 相关

- <ItemLink id="ic2_120:wood_transmission_shaft" />
- <ItemLink id="ic2_120:iron_transmission_shaft" />
- <ItemLink id="ic2_120:steel_transmission_shaft" />
- <ItemLink id="ic2_120:carbon_transmission_shaft" />
- <ItemLink id="ic2_120:bevel_gear" />
