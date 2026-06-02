---
navigation:
  title: 放射性同位素温差发电机
  parent: index.md
  position: 16
  icon: ic2_120:rt_generator
item_ids:
  - ic2_120:rt_generator
---

# 放射性同位素温差发电机

<BlockImage id="ic2_120:rt_generator" p:facing="north" p:active="true" scale="4" />

放射性同位素温差发电机（RTG）直接把 <ItemLink id="ic2_120:rtg_pellet" /> 的热量转为 EU。它是发电版 RTG：不产生 HU，不需要再接斯特林发电机，也不使用热传递面连接。

机器有 6 个燃料球槽位。每个槽位只能放 1 个 RTG 燃料球，燃料球不会被消耗。放入后会持续发电，直到玩家或自动化将其取出。

## EU 输出

输出只取决于已填入的燃料球槽位数量：

| 燃料球数量 | 发电量 |
|------|------|
| 1 | 1 EU/t |
| 2 | 2 EU/t |
| 3 | 4 EU/t |
| 4 | 8 EU/t |
| 5 | 16 EU/t |
| 6 | 32 EU/t |

RTG 发电机内部最多储存 20,000 EU。缓存满时，新的 EU 不会继续加入，直到有能量被取出。机器电压等级为 1，整机每 tick 的总输出上限为 32 EU/t。

机器不接受 EU 输入。EU 可以从除方块正面以外的任意方向取出。

## 槽位

- 燃料球网格：6 个槽位，每槽 1 个 RTG 燃料球
- 没有输出槽：燃料球不会被消耗，也不会产生废料

## 自动化

物品自动化可以向空燃料球槽位插入 <ItemLink id="ic2_120:rtg_pellet" />。每个槽位最多 1 个燃料球。自动化也可以把燃料球取出，用来降低输出或关闭发电机。

能量自动化应把 RTG 发电机视为只能输出的 EU 源。取电时使用除正面以外的任意方向。

## RTG 加热机

<ItemLink id="ic2_120:rt_heat_generator" /> 使用同一种燃料球，但输出 HU 而不是 EU。相同燃料球数量下，加热机的数值是本发电机的 2 倍：1 个燃料球为 2 HU/t，6 个燃料球为 64 HU/t，并且没有热量缓存。需要给耗热机器供 HU 时使用加热机；需要直接储存并输出 EU 时使用本 RTG 发电机。

## 配方

<Recipe id="ic2_120:rt_generator" />
