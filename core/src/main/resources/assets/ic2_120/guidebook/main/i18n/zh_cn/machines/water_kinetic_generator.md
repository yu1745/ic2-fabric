---
navigation:
  title: 水力动能发生机
  parent: index.md
  position: 23
  icon: ic2_120:water_kinetic_generator
item_ids:
  - ic2_120:water_kinetic_generator
---

# 水力动能发生机

<BlockImage id="ic2_120:water_kinetic_generator" p:facing="north" p:active="true" scale="4" />

水力动能发生机是一台转子型 KU 来源。把 <ItemLink id="ic2_120:wooden_rotor" />、<ItemLink id="ic2_120:iron_rotor" />、<ItemLink id="ic2_120:steel_rotor" /> 或 <ItemLink id="ic2_120:carbon_rotor" /> 装进唯一的转子槽，然后在机器正前方一格的转子平面中布置水。

转子位于机器正面。KU 只会从机器背面输出，因此传动轴或 <ItemLink id="ic2_120:kinetic_generator" /> 要接在背面。动能发电机必须让正面朝向输入的动能线路，并以 4 KU = 1 EU 的比例把 KU 转成 EU。

## 水环境

机器只在转子正前方平面的所有采样点都是水时运行。转子扫过区域缺水会停止发电。只要这些水方块中有任意一个是流动水，就会获得流动水加成。

- **全是静止水**：正常输出，正常水力磨损
- **采样转子区域内存在流动水**：1.5 倍输出，同时 1.5 倍磨损
- **采样转子区域缺水**：不产出 KU

所需水面会随转子半径增大。大转子输出更高，但正前方也需要更大的完整水面。

## KU 输出

**KU/t = floor(64 x 转子倍率 x 水流加成)**

| 转子 | 半径 | 倍率 | 静止水 | 流动水加成 |
|------|------|------|--------|------------|
| 木制 | 1.0 | 1x | 64 KU/t | 96 KU/t |
| 铁制 | 1.5 | 2x | 128 KU/t | 192 KU/t |
| 钢制 | 2.0 | 3x | 192 KU/t | 288 KU/t |
| 碳纤维 | 2.5 | 4x | 256 KU/t | 384 KU/t |

界面会分别显示生成 KU 和输出 KU。生成 KU 是当前水环境和转子能产生的量；输出 KU 是本 tick 实际被动能发电机或传动网络抽走的量。如果生成 KU 大于 0 但输出 KU 为 0，说明机器在转，但背面没有东西取走 KU。

## 放置与传动

水力动能发生机不会长期储存 KU。它每 tick 从背面提供当前生成的 KU，未被抽走的 KU 到下一 tick 就会消失。

直接接动能发电机时，把动能发电机放在水力机背后，并让动能发电机的正面对着水力机。使用传动轴时，KU 到达消费者前会受到线路容量和路径损耗影响：

- <ItemLink id="ic2_120:wood_transmission_shaft" />：128 KU/t，无路径损耗
- <ItemLink id="ic2_120:iron_transmission_shaft" />：512 KU/t，每段 2 KU 路径损耗
- <ItemLink id="ic2_120:steel_transmission_shaft" />：2,048 KU/t，每段 1 KU 路径损耗
- <ItemLink id="ic2_120:carbon_transmission_shaft" />：8,192 KU/t，无路径损耗
- <ItemLink id="ic2_120:bevel_gear" />：2,048 KU/t，3 KU 路径损耗

铁制及以上转子想完整送出输出，至少应使用铁传动轴。钢或碳纤维传动轴更适合多台来源或较长线路。

## 转子磨损

转子只在机器实际生成 KU 时消耗耐久。水力发电的基础磨损为每 tick 2 点耐久；存在流动水加成时，磨损再提高到 1.5 倍。转子耐久耗尽后会损坏，槽位变空。

GUI 中的剩余寿命按静止水条件显示，单位为小时。使用流动水加成时，同一转子的实际寿命约为界面显示静水寿命的三分之二。

## 阻塞检测

如果不透明完整方块进入叶片路径，或另一台动能转子的旋转平面与它重叠，转子会卡住。卡住时不产出 KU，但已安装转子会保留。清除阻挡或移开重叠的发生机后即可恢复。

## 槽位

- 转子槽：接受 1 个木制、铁制、钢制或碳纤维转子

手持有效转子右键方块可快速安装；空手右键可取下已安装转子。打开界面也能操作同一个槽位，Shift 点击有效转子会插入 1 个。

## 配方

<Recipe id="ic2_120:water_kinetic_generator" />

## 相关

- <ItemLink id="ic2_120:wooden_rotor" />
- <ItemLink id="ic2_120:iron_rotor" />
- <ItemLink id="ic2_120:steel_rotor" />
- <ItemLink id="ic2_120:carbon_rotor" />
- <ItemLink id="ic2_120:kinetic_generator" />
- <ItemLink id="ic2_120:bevel_gear" />
