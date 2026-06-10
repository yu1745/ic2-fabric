---
navigation:
  title: 夜视镜
  parent: index.md
  position: 313
  icon: ic2_120:night_vision_goggles
item_ids:
  - ic2_120:night_vision_goggles
---

# 夜视镜

<ItemImage id="ic2_120:night_vision_goggles" scale="4" />

夜视镜是一件佩戴于头盔槽位的头戴式设备，可按需为玩家提供夜视效果。它由内部 EU 缓冲供电，而非原版耐久度，因此只要缓冲有电就能持续工作。

## 物品视图

| 夜视镜 |
|:-------------------:|
| <ItemImage id="ic2_120:night_vision_goggles" scale="2" /> |

## 属性

| 属性 | 值 |
|----------|-------|
| 槽位 | 头盔 |
| 护甲值 | 1 |
| 耐久倍率 | 5x |
| 等级 | 2 |
| 内部容量 | 30,000 EU（可配置） |
| 每 tick 消耗 | 激活时 1 EU/t（默认） |
| 刷新间隔 | 220 ticks（`NIGHT_VISION_DURATION_TICKS`） |
| 原版耐久 | 禁用（`isDamageable = false`）|

内部容量与每 tick 消耗在构建时从 `Ic2Config.current.armor.nightVisionGoggles` 读取，因此服务器管理员无需修改物品即可重新平衡夜视镜。默认值下，满电可提供约 30,000 ticks（约 25 分钟）的持续夜视时间。

## 使用方式

### 装备

- 将夜视镜放入头盔槽位。
- 通过 BatBox、CESU、MFE、MFSU、充电板或任何能与 2 级电物品通信的 EU 源为内部缓冲充电。

### 切换夜视

- **客户端 / 服务端交互**——具体按键绑定由夜视镜的 UI 处理器提供。默认行为是 **Alt+N**（`toggleVisionKey`）翻转 `NIGHT_VISION_ENABLED` 标志位。
- 启用后，夜视镜会给穿戴者施加夜视状态效果。效果每 `NIGHT_VISION_DURATION_TICKS` ticks（220 ticks，即 11 秒）重新施加一次，因此玩家不会看到效果闪烁消失。
- 剩余可用时间显示在物品的 tooltip 中，按 `maxEnergy / euPerTick` 在满电时计算，并实时更新。

### 自动光照保护

如果玩家走进光线充足的区域，夜视镜会拒绝在玩家不需要的效果上浪费 EU：

- 当玩家眼部位置的环境光照等级 **达到 8 或以上**时，夜视镜会**立即关闭夜视**，并施加一个持续 **80 ticks（4 秒）** 的短暂失明效果，让玩家有时间重新适应。
- 当玩家重新走入较暗的区域时，恢复正常的切换行为。

这是安全覆盖逻辑，不是配置选项——无法通过配置项关闭。

### 无原版耐久

夜视镜设置 `isDamageable = false`，因此它不会受到原版耐久损耗，也不会显示耐久条。它唯一的”磨损”就是夜视激活期间 EU 缓冲的消耗。

## 配方

<Recipe id="ic2_120:night_vision_goggles" />

## 相关

- [工具与防护装备](../reference/tools_armor.md)
- [电池与移动供电](../reference/energy_items.md)
