---
navigation:
  title: 电力加热机
  parent: index.md
  position: 55
  icon: ic2_120:electric_heat_generator
item_ids:
  - ic2_120:electric_heat_generator
---

# 电力加热机

<BlockImage id="ic2_120:electric_heat_generator" scale="4" />

电力加热机将储存的 EU 直接转换为热能单位（HU）。如果你的产线已经有 EU 供电，它就是一种可控的热源，可以给相邻的耗热机器供热。

## 输出

转换率为 **1 EU = 1 HU**。实际热输出取决于安装的线圈数量：

- 1 个线圈：**10 HU/t**
- 10 个线圈：**100 HU/t**

机器有 **10 个线圈槽**，每个槽只能放入 1 个 <ItemLink id="ic2_120:coil" />。它有 **10,000 EU** 的内部电量缓存，可从电网输入最高 **2,048 EU/t**，但不会向外输出 EU。

电力加热机本身不缓存热量。每 tick 中，它只会按已安装线圈可产生的热量消耗 EU，然后立即尝试把这些 HU 传给连接的耗热机器。已经产生但未被耗热机器接收的 HU 会直接损失。

只有在有 EU、至少安装 1 个线圈、红石控制允许运行，并且传热面连接了有效耗热机器时，它才会工作。

## 槽位

- 线圈区域：10 个槽位，每槽 1 个线圈
- 电池槽：接受 1 个可充电电池物品，并将其中电量放入内部 EU 缓存
- 升级槽：无

## 使用方式

将 EU 源连接到任意面，或把已充电的电池放入电池槽。自动化可以把线圈插入线圈槽、把电池插入电池槽；机器内所有槽位的物品都可以被抽出。

传热是机器与机器之间的直接传递，不是热能网络。电力加热机只会从自己的传热面输出 HU；这个传热面就是放置时机器朝向的一侧。把耗热机器紧贴在这一侧，并旋转耗热机器，让它的传热面正对电力加热机。若传热面没有对齐，电力加热机会保持未激活状态，也不会消耗 EU。

常用耗热机器包括：

- <ItemLink id="ic2_120:steam_generator" />
- <ItemLink id="ic2_120:stirling_generator" />
- <ItemLink id="ic2_120:blast_furnace" />
- <ItemLink id="ic2_120:fermenter" />

## 配方

<Recipe id="ic2_120:electric_heat_generator" />
