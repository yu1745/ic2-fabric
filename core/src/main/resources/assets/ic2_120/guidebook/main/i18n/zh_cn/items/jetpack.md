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

胸甲槽飞行装备复用 Minecraft 原版创造飞行控制。穿上可用的飞行装备后，服务器只授予飞行权限；双击跳跃开始或停止飞行，按住跳跃上升，按住潜行下降。落地会自动关闭飞行。脱下装备、燃料耗尽或电量耗尽时，会恢复到穿上前的飞行状态。

## 物品视图

| 喷气背包 | 电力喷气背包 | 量子胸甲 |
|:-------:|:----------------:|:--------:|
| <ItemImage id="ic2_120:jetpack" scale="2" /> | <ItemImage id="ic2_120:electric_jetpack" scale="2" /> | <ItemImage id="ic2_120:quantum_chestplate" scale="2" /> |

## 喷气背包（生物燃料）

经典喷气背包使用物品内部储存的生物燃料。它是早期飞行方案，适合已有燃料生产、但暂时不想为机动装备消耗 EU 的阶段。

- **容量：** 默认 30,000 mB。
- **时长：** 满燃料默认提供 750 秒主动飞行。
- **消耗：** 仅在玩家实际处于飞行状态时扣除燃料。
- **槽位：** 胸甲。

## 电力喷气背包

电力喷气背包是一款由 EU 驱动的 `ElectricArmorItem`。它与生物燃料喷气背包使用同一套飞行权限逻辑，只是消耗内部 EU 缓冲而非燃料箱。

### 默认配置

| 属性 | 默认值 | 配置键 |
|----------|--------:|------------|
| 最大电量 | 30,000 EU | `Ic2Config.current.armor.electricJetpack.maxEnergy` |
| 连续飞行时长 | 750 s | `Ic2Config.current.armor.electricJetpack.flightDurationSeconds` |
| 每 tick 消耗 | 最大电量 / 飞行时长 | `Ic2Config.getElectricJetpackEuPerTick()` |

在满电情况下，默认配置可提供 12 分 30 秒的主动动力飞行。只有 `flying` 处于开启状态时才消耗 EU。

### 量子胸甲

<ItemLink id="ic2_120:quantum_chestplate" /> 是终期飞行胸甲。它默认储存 10,000,000 EU，满电时提供 1,200 秒主动创造式飞行，同时保留量子护甲的防护与防火定位。

### 充电

电力喷气背包的充电方式与其他 `ElectricArmorItem` 相同：放入 BatBox、CESU、MFE、MFSU 或充电板；也可以在物品栏中使用兼容的已充电物品补充电量。量子胸甲使用量子护甲能量缓冲，需要高等级 EU 充电。

## 使用方式

1. 为装备加满燃料或电量。
2. 将其放入胸甲槽。
3. 双击跳跃进入飞行。
4. 按住跳跃上升，按住潜行下降。
5. 落地或再次双击跳跃即可停止飞行。

长途飞行时，建议携带备用燃料、已充电电池以及供电来源，以便在两次飞行之间补给。

## 配方

| 喷气背包 | 电力喷气背包 | 量子胸甲 |
|:-------:|:----------------:|:--------:|
| <Recipe id="ic2_120:jetpack" /> | <Recipe id="ic2_120:electric_jetpack" /> | <Recipe id="ic2_120:quantum_chestplate" /> |

## 相关

- [工具与防护装备](../reference/tools_armor.md)
- [电池与移动供电](../reference/energy_items.md)
- [量子护甲](quantum_armor.md)
