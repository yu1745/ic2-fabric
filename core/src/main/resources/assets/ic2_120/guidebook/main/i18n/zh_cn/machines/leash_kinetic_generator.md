---
navigation:
  title: 拴绳动能发生机
  parent: index.md
  position: 25
  icon: ic2_120:leash_kinetic_generator
item_ids:
  - ic2_120:leash_kinetic_generator
---

# 拴绳动能发生机

<BlockImage id="ic2_120:leash_kinetic_generator" p:facing="north" p:active="true" scale="4" />

拴绳动能发生机利用动物围绕机器行走的动能产生 KU。当动物被拴绳拴在机器上并绕圈移动时，旋转运动被转换为 KU。

输出功率取决于动物的角速度（绕圈速度）和拴绳长度。机器会持续追踪动物位置，计算平均角速度，并根据速度和距离产生 KU。KU 输出上限为 512 KU/t。

## 输出

- **KU 输出**：可变，最高 512 KU/t
- **要求**：有动物拴在机器上并围绕移动
- **最大拴绳范围**：10 格
- **等级**：1

### 功率因素

- 基础 KU：8
- 角速度：2 KU 每度/秒
- 拴绳长度：每 10 cm 水平距离 1 KU

## 角速度追踪

机器每 tick 采样拴绳动物的位置，并维护 40 个样本（约 2 秒）的历史记录。角速度根据角度随时间的变化计算得出。产生功率所需的最小角速度为 5 度/秒——缓慢行走可能无法产生 KU。

**KU = 8 + (平均角速度 × 2) + (水平距离_cm / 10)**

结果上限为 512 KU/t。

## 槽位

拴绳动能发生机没有物品槽。所有交互通过将动物直接拴在机器上进行。

可从任意面提取 KU。

## 配方

<Recipe id="ic2_120:leash_kinetic_generator" />

## 相关

- <ItemLink id="minecraft:lead" />
