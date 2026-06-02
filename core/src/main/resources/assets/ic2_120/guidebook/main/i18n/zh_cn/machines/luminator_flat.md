---
navigation:
  title: 日光灯
  parent: index.md
  position: 44
  icon: ic2_120:luminator_flat
item_ids:
  - ic2_120:luminator_flat
---

# 日光灯

<BlockImage id="ic2_120:luminator_flat" scale="4" />

日光灯是一种由 EU 供电的扁平灯。内部有电时发出 15 级亮度，供电中断后会很快熄灭。

## 工作方式

- **储能：** 100 EU
- **输入：** 最高 8192 EU/t
- **消耗：** 每 4 tick 消耗 1 EU，也就是 5 EU/s
- **亮度：** 工作时 15 级

日光灯没有 GUI、没有物品槽，也不算真正的电池；100 EU 缓冲只用于让它在电网供电间隙维持极短时间。接上 EU 线路就会亮，断开供电通常 4 tick 内熄灭。

它能接受很高电压但耗电极低，因此可以直接挂在已有电缆线上作为照明点，不需要专门为了灯具再布置变压链。

## 配方

<Recipe id="ic2_120:luminator_flat" />
