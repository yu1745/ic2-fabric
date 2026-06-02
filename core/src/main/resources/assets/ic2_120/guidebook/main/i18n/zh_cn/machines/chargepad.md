---
navigation:
  title: 充电座
  parent: index.md
  position: 47
  icon: ic2_120:batbox_chargepad
item_ids:
  - ic2_120:batbox_chargepad
  - ic2_120:cesu_chargepad
  - ic2_120:mfe_chargepad
  - ic2_120:mfsu_chargepad
---

# 充电座

<BlockImage id="ic2_120:batbox_chargepad" scale="4" />

充电座是带顶部充电面的储电设备。它和对应等级的储电箱、CESU、MFE 或 MFSU 拥有相同容量、分面输入输出规则、充电槽和燃料槽，并额外支持给站在上方的玩家自动充电。

## 等级对比

| 等级 | 名称 | 容量 | 输出 | 电压 |
|------|------|------|------|------|
| 1 | 储电箱充电座 | 40,000 EU | 32 EU/t | 低压 |
| 2 | CESU 充电座 | 300,000 EU | 128 EU/t | 中压 |
| 3 | MFE 充电座 | 4,000,000 EU | 512 EU/t | 高压 |
| 4 | MFSU 充电座 | 40,000,000 EU | 2,048 EU/t | 超高压 |

## 使用方式

- 站在充电座上方的方块空间内，即可给物品栏和护甲槽中的电动物品充电。
- 充电物品等级必须不高于充电座等级。
- 充电会消耗充电座内部缓存。
- 和普通储电设备一样，EU 从五个非正面输入，从正面输出。
- GUI 仍然有物品充电槽和燃料槽。红石提供 800 EU，能量粉提供 16,000 EU，且需要缓存有足够剩余空间才会消耗。
- 给玩家充电时方块会显示为激活状态。

## 布置

让输出面朝向需要接收多余电力的电缆或机器，并保持顶部可站人。MFE 和 MFSU 充电座也可以作为传送机的相邻储能方块，同时继续承担踩踏式护甲充电功能。

## 配方

<Recipe id="ic2_120:batbox_chargepad" />
<Recipe id="ic2_120:cesu_chargepad" />
<Recipe id="ic2_120:mfe_chargepad" />
<Recipe id="ic2_120:mfsu_chargepad" />
