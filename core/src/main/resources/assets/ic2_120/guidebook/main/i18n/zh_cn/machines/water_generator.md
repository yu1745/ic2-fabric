---
navigation:
  title: 水力发电机
  parent: index.md
  position: 13
  icon: ic2_120:water_generator
item_ids:
  - ic2_120:water_generator
---

# 水力发电机

<BlockImage id="ic2_120:water_generator" p:facing="north" p:active="true" scale="4" />

水力发电机利用水来产生 EU。它有两种发电方式：消耗水桶或水单元以 1 EU/t 发电（每桶 500 EU），以及检测机器周围 3x3x3 范围内的水源方块，每个水源方块贡献 0.01 EU/t。

典型的水塔设计配合周围水源方块，除桶装供水外还可从环境获得约 0.25 EU/t 的额外电力。

## 输出

- **桶装输出**：1 EU/t（每桶 500 EU）
- **环境输出**：每个相邻水源方块 0.01 EU/t
- **能量缓存**：10,000 EU
- **等级**：1

## 槽位

- 燃料槽（上方）：水桶或水单元
- 空容器槽（中间）：水源消耗后输出空容器
- 电池槽（下方）：可充电电池或电动工具
- 升级槽（4 个）：支持流体管道等升级

水力发电机不接受 EU 输入。它会从除正面以外的所有方向输出 EU。

## 配方

<Recipe id="ic2_120:water_generator" />
