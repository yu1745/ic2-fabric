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

动能发电机是动能传输网络通向 EU 电网的桥梁。它是 KU 消费端：每 tick 从连接到自身正面的传动轴、伞齿轮或相邻动能机器拉取 KU，并按固定比例 4 KU = 1 EU 转换。

它只从机器朝向的那一面接收 KU。EU 会从除正面以外的任意面输出，因此搭建时应把正面留给动能线路，把电缆接在侧面、顶部、背面或底部。

## 转换

- **转换比例**：4 KU = 1 EU
- **最大 KU 输入**：2,048 KU/t
- **最大 EU 输出**：512 EU/t
- **能量缓存**：10,000 EU
- **等级**：3

当输入达到 64 KU/t 时，动能发电机会显示为运行状态；输入降到 48 KU/t 以下才会停止显示运行。这个启停滞回可以避免临界功率附近的频繁闪烁。

不能整除为 EU 的 KU 会作为少量内部余数保留，跨 tick 继续参与转换，不会直接丢失。

## 槽位

动能发电机没有物品槽。它的界面只显示状态和玩家背包。

由于机器本身没有库存，漏斗、管道和物品升级没有可插入或抽出的槽位。自动化的重点是摆放方向：从正面输入 KU，并用电缆或相邻耗电机器从任意非正面抽取 EU。

## 动能输入

传动轴只能沿自身轴向传递 KU。线路需要 90 度转向，或机器需要从传动轴侧面接入时，使用 <ItemLink id="ic2_120:bevel_gear" />。传动轴材质会限制整条路径的吞吐：

- <ItemLink id="ic2_120:wood_transmission_shaft" />：128 KU/t
- <ItemLink id="ic2_120:iron_transmission_shaft" />：512 KU/t
- <ItemLink id="ic2_120:steel_transmission_shaft" />：2,048 KU/t
- <ItemLink id="ic2_120:carbon_transmission_shaft" />：8,192 KU/t

路径损耗会在 KU 到达动能发电机前扣除。伞齿轮和部分传动轴材质会带来损耗，因此较长或多次转向的线路，实际送达的 KU 可能低于源机器产生的 KU。

## 动能来源

以下机器都可以通过同一套动能传输网络为动能发电机供能：

- <ItemLink id="ic2_120:steam_kinetic_generator" /> 将蒸汽经涡轮转换为 KU 后，可从任意面输出 KU。
- <ItemLink id="ic2_120:wind_kinetic_generator" /> 从背面输出 KU；转子位于正面。
- <ItemLink id="ic2_120:water_kinetic_generator" /> 同样从背面输出 KU，并且需要转子。
- <ItemLink id="ic2_120:manual_kinetic_generator" /> 在存有手摇动能时，可从水平侧面输出 KU。

风力和水力动能发生机需要在转子槽内安装转子后才能产生 KU。转子材质会影响产能和耐久，但动能发电机本身没有转子槽；它只接收源机器和传动网络已经送来的 KU。

## 配方

<Recipe id="ic2_120:kinetic_generator" />

## 相关

- <ItemLink id="ic2_120:wood_transmission_shaft" />
- <ItemLink id="ic2_120:iron_transmission_shaft" />
- <ItemLink id="ic2_120:steel_transmission_shaft" />
- <ItemLink id="ic2_120:carbon_transmission_shaft" />
- <ItemLink id="ic2_120:bevel_gear" />
