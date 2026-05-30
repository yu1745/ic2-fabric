---
navigation:
  title: 半流质发电机
  parent: index.md
  position: 12
  icon: ic2_120:semifluid_generator
item_ids:
  - ic2_120:semifluid_generator
---

# 半流质发电机

<BlockImage id="ic2_120:semifluid_generator" p:facing="north" p:active="true" scale="4" />

半流质发电机通过燃烧生物质燃料和杂酚油等液体燃料产生 EU。它接受流体容器（桶和单元）输入，并支持流体管道连接。

不同燃料提供不同的能量值：生物质燃料提供 16 EU/t，每桶 32,000 EU；杂酚油提供 8 EU/t，每桶 3,200 EU。

## 输出

- **EU 输出**：10-16 EU/t（取决于燃料）
- **能量缓存**：10,000 EU
- **等级**：1
- **内部储罐**：8 桶

### 燃料参数

| 燃料 | EU/t | EU/桶 |
|------|------|-------|
| 生物质燃料 | 16 | 32,000 |
| 杂酚油 | 8 | 3,200 |

## 槽位

- 燃料槽（上方）：生物质燃料/杂酚油桶或单元
- 空容器槽（中间）：燃料消耗后输出空容器
- 电池槽（下方）：可充电电池或电动工具
- 升级槽（4 个）：支持流体管道等升级

半流质发电机不接受 EU 输入。它会从除正面以外的所有方向输出 EU。

## 配方

<Recipe id="ic2_120:semifluid_generator" />
