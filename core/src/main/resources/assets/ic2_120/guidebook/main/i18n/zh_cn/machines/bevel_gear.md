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
- 斜齿轮本身的 KU 吞吐量等同于系统中最低等级的传动轴。

## 传动轴布局

斜齿轮旁边的传动轴必须使用与接触方向一致的轴向：东西方向使用 `axis=x`，上下方向使用 `axis=y`，南北方向使用 `axis=z`。

两根传动轴的拐角连接：

<GameScene zoom="7" interactive={true} fullWidth={true}>
  <IsometricCamera yaw="45" pitch="30" />
  <ImportStructure src="bevel_gear_two_shafts.snbt" />
</GameScene>

三根传动轴的交汇连接：

<GameScene zoom="6" interactive={true} fullWidth={true}>
  <IsometricCamera yaw="45" pitch="30" />
  <ImportStructure src="bevel_gear_three_shafts.snbt" />
</GameScene>

## 配方

<Recipe id="ic2_120:bevel_gear_from_plates" />
