---
navigation:
  title: 地热发电机
  parent: index.md
  position: 11
  icon: ic2_120:geo_generator
item_ids:
  - ic2_120:geo_generator
---

# 地热发电机

<BlockImage id="ic2_120:geo_generator" p:facing="north" p:active="true" scale="4" />

地热发电机通过燃烧岩浆产生 EU。它可接受岩浆桶、岩浆单元以及流体管道输入，内部最多可存储 8 桶岩浆。每桶岩浆可燃烧 25 秒（500 tick），总计产生 10,000 EU。

该发电机提供稳定的 20 EU/t 输出，是前期较为强劲的发电设备之一。内部能量缓存满时自动暂停消耗岩浆，避免浪费。

## 输出

- **EU 输出**：20 EU/t
- **能量缓存**：10,000 EU
- **等级**：1
- **岩浆消耗**：2 mB/t（每桶 25 秒）

## 槽位

- 燃料槽（上方）：岩浆桶或岩浆单元
- 空容器槽（中间）：燃料消耗后输出空桶或空单元
- 电池槽（下方）：可充电电池或电动工具

地热发电机不接受 EU 输入。它会从除正面以外的所有方向输出 EU。

## 配方

<Recipe id="ic2_120:geo_generator" />
