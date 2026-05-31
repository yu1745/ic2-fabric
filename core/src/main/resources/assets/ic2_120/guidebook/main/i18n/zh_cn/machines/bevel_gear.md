---
navigation:
  title: 斜齿轮
  parent: index.md
  position: 63
  icon: ic2_120:bevel_gear
item_ids:
  - ic2_120:bevel_gear
---

# 斜齿轮

<BlockImage id="ic2_120:bevel_gear" scale="4" />

斜齿轮用于改变动能传输的方向。它将动能（KU）从一个方向转向另一个方向——例如，将水平传动轴线路转换为垂直方向。

## 使用方式

- 将斜齿轮放置在传动轴线路的拐角处。
- 动能从一面进入，从相邻面以 90 度角输出。
- 这样可以实现紧凑的传动轴布局，并让动能绕过角落。
- 斜齿轮本身的 KU 吞吐量为固定的 2048 KU，相当于钢制传动轴的级别。
- 斜齿轮可以使用三向连接进行串联，让动能在单一方向上沿斜齿轮链无限传递，直到达到吞吐上限。

## 传动轴布局

两根传动轴的拐角连接：

<GameScene zoom="7" interactive={true} fullWidth={true}>
  <IsometricCamera yaw="45" pitch="30" />
  <ImportStructure src="bevel_gear_two_shafts.snbt" />
</GameScene>

**重要：** 只有以上两种布局在物理上是可行的。任何其他连接方式都会导致斜齿轮卡死，无法转动。

三根传动轴的交汇连接：

<GameScene zoom="6" interactive={true} fullWidth={true}>
  <IsometricCamera yaw="45" pitch="30" />
  <ImportStructure src="bevel_gear_three_shafts.snbt" />
</GameScene>

## 配方

<Recipe id="ic2_120:bevel_gear_from_plates" />
