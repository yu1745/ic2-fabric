---
navigation:
  title: 手摇动能发生机
  parent: index.md
  position: 21
  icon: ic2_120:manual_kinetic_generator
item_ids:
  - ic2_120:manual_kinetic_generator
---

# 手摇动能发生机

<BlockImage id="ic2_120:manual_kinetic_generator" p:facing="north" p:active="true" scale="4" />

手摇动能发生机在玩家手持摇杆右键点击时产生动能（KU）。首先需手持摇杆右键将摇杆装入机器，安装后空手右键开始摇动，每 tick 产生 KU。

不同材质的摇杆提供不同的 KU 输出。产生的 KU 可输出至相邻的动能机器、传动轴或斜齿轮。

## 输出

- **KU 输出**：可变（取决于摇杆材质）
- **等级**：1
- **输出面**：水平方向

### 摇杆材质

| 材质 | KU/t |
|------|------|
| 木制 | 64 |
| 铁制 | 128 |
| 钢制 | 256 |
| 碳纤维 | 512 |

## 摇杆机制

安装摇杆后，空手右键点击动能发生机即可开始持续产生 KU。玩家需保持在机器附近，摇动期间每 tick 持续产出 KU。

更高等级的摇杆产出显著更多的动能，但制作成本也相应提高。

## 槽位

- 摇杆槽：存放已安装的摇杆

玩家可手持新摇杆右键点击来更换已安装的摇杆。

## 配方

<Recipe id="ic2_120:manual_kinetic_generator" />

## 相关

- <ItemLink id="ic2_120:wooden_crank_handle" />
- <ItemLink id="ic2_120:iron_crank_handle" />
- <ItemLink id="ic2_120:steel_crank_handle" />
- <ItemLink id="ic2_120:carbon_crank_handle" />
- <ItemLink id="ic2_120:wood_transmission_shaft" />
- <ItemLink id="ic2_120:iron_transmission_shaft" />
- <ItemLink id="ic2_120:steel_transmission_shaft" />
- <ItemLink id="ic2_120:carbon_transmission_shaft" />
- <ItemLink id="ic2_120:bevel_gear" />
