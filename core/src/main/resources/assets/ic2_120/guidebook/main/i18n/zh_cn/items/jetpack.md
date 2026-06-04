---
navigation:
  title: 喷气背包
  parent: index.md
  position: 311
  icon: ic2_120:jetpack
item_ids:
  - ic2_120:jetpack
  - ic2_120:electric_jetpack
---

# 喷气背包

<ItemImage id="ic2_120:jetpack" scale="4" />

两种胸甲槽飞行设备。经典喷气背包是基于 `JetpackItem` 抽象构建的生物燃料飞行装置。电力喷气背包是更高等级的 `ElectricArmorItem`，使用 EU 驱动，并带有可配置的开关。

## 方块视图

| 喷气背包 | 电力喷气背包 |
|:-------:|:----------------:|
| <ItemImage id="ic2_120:jetpack" scale="2" /> | <ItemImage id="ic2_120:electric_jetpack" scale="2" /> |

## 喷气背包（生物燃料）

经典喷气背包基于 `JetpackItem` 抽象构建，所有飞行行为与燃料消耗逻辑都实现在该抽象内部。它使用生物燃料而非 EU 驱动，因此在你尚未拥有任何电力设施时是可用的飞行方案。

- **燃料：** 生物燃料，装载到背包的内置燃料箱中。
- **飞行行为：** 由 `JetpackItem` 控制；具体的推力、下降与每 tick 消耗请参考源码。
- **装备槽位：** 胸甲。装备后，只要燃料箱未空，玩家即可悬停与上升。

## 电力喷气背包

电力喷气背包是一款由 EU 驱动的 `ElectricArmorItem`。只有在玩家将其穿戴在胸甲槽中且内置 EU 缓冲非空时才能工作。

### 默认配置

| 属性 | 默认值 | 配置键 |
|----------|--------:|------------|
| 最大电量 | 32,000 EU | `Ic2Config.current.armor.electricJetpack.maxEnergy` |
| 连续飞行时长 | 320 s | `Ic2Config.current.armor.electricJetpack.flightDurationSeconds` |
| 每 tick 消耗 | 1 EU/t | `Ic2Config.current.armor.electricJetpack.euPerTick` |

在满电情况下，默认配置可提供约 320 秒（5 分 20 秒）的连续动力飞行。耗电仅在背包实际产生推力时进行；如果玩家在地面上且开关关闭，则不消耗 EU。

### 开关与 NBT

- **右键** 喷气背包，或使用其绑定的快捷键，可翻转 `FLIGHT_ENABLED` NBT 标志。
- 该标志按物品保存在 NBT 中，并与**客户端同步**到服务器。在一个客户端上打开后再重新连接，状态依然保留。
- 当该标志关闭时，喷气背包仍处于装备状态但不产生任何效果——在下降时可以方便地回退到重力下落。
- 剩余飞行时间会显示在物品的 tooltip 中，按 `maxEnergy / euPerTick` 在满电时计算，并随缓冲消耗实时更新。

### Tooltip

```
Electric Jetpack
EU: 32000 / 32000
Flight: Enabled
Time remaining: 5m 20s
```

### 充电

电力喷气背包的充电方式与其他 `ElectricArmorItem` 相同：放入 BatBox、CESU、MFE、MFSU 或充电板；也可以直接在物品栏中使用能量水晶或更高等级电池来补充电量。其等级与 Lapotron 水晶一致，因此可由 IC2 任何标准电力源充电。

## 使用方式

1. 将喷气背包放入胸甲槽。
2. 充满电。
3. 打开飞行开关（右键或快捷键）。
4. 跳跃起飞；松开跳跃键则下降。
5. 想恢复重力下落或避免在地面空耗 EU 时，关闭开关即可。

长途飞行时，建议携带一块已充电的能量水晶或紫晶背包，以及供电来源，以便在两次飞行之间为喷气背包补电。

## 配方

| 喷气背包 | 电力喷气背包 |
|:-------:|:----------------:|
| <Recipe id="ic2_120:jetpack" /> | <Recipe id="ic2_120:electric_jetpack" /> |

**图样说明**

- **喷气背包**（3x3）：`CEC` / `CUC` / `R R`，其中 **C** = Casing（外壳），**E** = Circuit（电路板），**U** = Empty Cell（空单元），**R** = Redstone（红石）。
- **电力喷气背包**（3x3）：`CAC` / `CBC` / `G G`，其中 **C** = Casing（外壳），**A** = Advanced Circuit（高级电路板），**B** = BatBox（储电箱），**G** = Glowstone Dust（荧石粉）。

## 相关

- [工具与防护装备](../reference/tools_armor.md)
- [电池与移动供电](../reference/energy_items.md)
