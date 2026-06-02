---
navigation:
  title: 固体加热机
  parent: index.md
  position: 53
  icon: ic2_120:solid_heat_generator
item_ids:
  - ic2_120:solid_heat_generator
---

# 固体加热机

<BlockImage id="ic2_120:solid_heat_generator" scale="4" />

固体加热机把固体燃料燃烧成热能单位（HU）。它是一种小型直接热源，用来给消耗 HU 而不是 EU 的机器供热。

## 热量输出

当它连接到有效耗热机时，固体加热机会以 **20 HU/t** 燃烧燃料。热量会在每 tick 产生后立刻尝试送出；机器**没有热量缓存**。同一 tick 内没有被相邻机器接收的 HU 会直接损失。

支持的燃料为**煤炭**、**木炭**和**焦炭**。本机使用这些燃料的熔炉燃烧时间除以 4，因此 1 个煤炭或木炭会燃烧 **400 tick**，总共提供 **8,000 HU**。

如果受热面没有连接有效耗热机，固体加热机不会开始燃烧。

## 受热面布置

热量只能在直接相邻的机器之间传递。放置固体加热机时，需要让它的传热面贴住接收机器的传热面，并且两个传热面相对朝向彼此。它不会从所有面输出 HU，也不会通过独立热网传输。

常用的接收机器包括：

- <ItemLink id="ic2_120:blast_furnace" />
- <ItemLink id="ic2_120:steam_generator" />
- <ItemLink id="ic2_120:stirling_generator" />

由于未使用的 HU 会被丢弃，最好先确认耗热机能够接收热量，再放入燃料。

## 槽位与自动化

- 燃料槽：放入固体加热燃料。
- 输出槽：界面中存在，但当前机器不会产生物品输出。

物品自动化会把有效燃料送入燃料槽。两个机器槽位都允许被自动化抽出，不过输出槽通常保持为空。

## 配方

<Recipe id="ic2_120:solid_heat_generator" />
