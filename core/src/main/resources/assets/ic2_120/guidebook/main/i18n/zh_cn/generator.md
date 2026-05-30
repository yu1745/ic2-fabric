---
navigation:
  title: 火力发电机
  parent: index.md
  position: 10
  icon: ic2_120:generator
item_ids:
  - ic2_120:generator
---

# 火力发电机

<BlockImage id="ic2_120:generator" p:facing="north" p:active="true" scale="4" />

火力发电机是早期最基础的固体燃料 EU 来源。它会燃烧熔炉燃料，将产生的电能暂存在内部缓冲区中，然后向相邻机器、电缆或可充电物品输出 EU。

## 输出

火力发电机在燃烧燃料时输出 **10 EU/t**。内部缓冲区容量为 **4,000 EU**，刚好对应一块煤炭产生的总电量。

燃料燃烧时间基于原版熔炉燃料时间，并除以 4。例如，煤炭在火力发电机中燃烧 400 tick，并产生 4,000 EU。

## 槽位

- 上方槽位：可充电电池或电动工具
- 下方槽位：固体燃料

火力发电机不接受 EU 输入。它会从除正面以外的所有方向输出 EU。

## 配方

<Recipe id="ic2_120:generator" />
