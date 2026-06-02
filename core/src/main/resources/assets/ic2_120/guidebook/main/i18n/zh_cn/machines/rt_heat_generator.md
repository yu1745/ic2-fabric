---
navigation:
  title: 放射性同位素温差加热机
  parent: index.md
  position: 56
  icon: ic2_120:rt_heat_generator
item_ids:
  - ic2_120:rt_heat_generator
---

# 放射性同位素温差加热机

<BlockImage id="ic2_120:rt_heat_generator" scale="4" />

放射性同位素温差（RTG）加热机是一种被动 HU 热源，使用 <ItemLink id="ic2_120:rtg_pellet" /> 供热。它不接受 EU、流体、可燃燃料，也不接受铀燃料棒。

## 热量输出

机器有 6 个燃料球槽位。每个槽位只能放 1 个 RTG 燃料球，填入槽位越多，输出按倍数提升：

| 燃料球数量 | 输出 |
|------|------|
| 1 | 2 HU/t |
| 2 | 4 HU/t |
| 3 | 8 HU/t |
| 4 | 16 HU/t |
| 5 | 32 HU/t |
| 6 | 64 HU/t |

RTG 燃料球不会被消耗，也不会变成乏燃料。放入后会一直留在槽位中，直到玩家或自动化将其取出。

机器没有热量缓存。每 tick 的 HU 会直接尝试送入相邻耗热机；同一 tick 内耗热机没有接收的 HU 会直接损失。如果没有正确对准的耗热机连接，机器不会产生或传输 HU，燃料球也不会有任何消耗。

## 传热面

HU 只会从本机的单个热传递面输出，不会从所有面输出。将耗热机器紧贴这个面放置，并确保耗热机器自己的热传递面朝回 RTG 加热机。

只要槽位中至少有 1 个 RTG 燃料球，方块就会显示为工作状态；但真正传热仍然需要两台机器的传热面对准。

## 槽位

- 燃料球网格：6 个槽位，每槽 1 个 RTG 燃料球
- 没有输出槽：燃料球不会被消耗，也不会产生废料

## 自动化

物品自动化可以向空燃料球槽位插入 <ItemLink id="ic2_120:rtg_pellet" />。每个槽位最多 1 个燃料球。自动化也可以把燃料球取出，方便调整装置的热量输出。

常见耗热机器包括：

- <ItemLink id="ic2_120:steam_generator" />
- <ItemLink id="ic2_120:stirling_generator" />
- <ItemLink id="ic2_120:blast_furnace" />
- <ItemLink id="ic2_120:fermenter" />

## 配方

<Recipe id="ic2_120:rt_heat_generator" />
