---
navigation:
  title: 建筑泡沫背包
  parent: index.md
  position: 312
  icon: ic2_120:cf_pack
item_ids:
  - ic2_120:cf_pack
---

# 建筑泡沫背包

<ItemImage id="ic2_120:cf_pack" scale="4" />

建筑泡沫背包（CF Pack，Construction Foam Backpack）是 <ItemLink id="ic2_120:foam_sprayer" /> 在胸甲槽配套使用的流体储存器。装备后，主手中的喷枪会自动保持满液，你就可以持续铺设泡沫墙体，而不必频繁回到储罐或流体管道旁。

## 方块视图

| 建筑泡沫背包 |
|:------:|
| <ItemImage id="ic2_120:cf_pack" scale="2" /> |

## 属性

| 属性 | 值 |
|----------|-------|
| 槽位 | 胸甲 |
| 护甲 | 2 |
| 耐久倍率 | 8x |
| 修复材料 | <ItemLink id="ic2_120:carbon_fibre" /> |
| 内部容量 | 80 桶建筑泡沫（`CAPACITY_DROPLETS = 80 * BUCKET`） |
| 接受流体 | 仅建筑泡沫 |

建筑泡沫背包是一件胸甲而非工具，因此占用电甲槽。除流体储存外，它不提供任何电力或其他功能。

## 使用方式

### 装备

- 将建筑泡沫背包放入胸甲槽。
- 可从任意建筑泡沫来源灌注：储罐、流体管道、输出泡沫的机器或建筑泡沫桶。

### 自动为喷枪补液

建筑泡沫背包装备后，每 tick 都会尝试为玩家物品栏中的 <ItemLink id="ic2_120:foam_sprayer" /> 补液。喷枪自身容量较小；建筑泡沫背包会从自己的内部储量中持续为其补满至喷枪自身的上限。

- 自动补液仅作用于泡沫喷枪，不会为物品栏中的其他流体容器补充。
- 建筑泡沫背包自身会保持你所灌注的液位。它只在为喷枪补液时才会消耗泡沫。
- 当建筑泡沫背包耗尽后，喷枪会按自身储量继续消耗；届时需要再次从泡沫源为建筑泡沫背包补液。

### 流体接口

建筑泡沫背包对外暴露 `FluidStorage` 接口，允许外部自动化设备进行灌入或抽取。该接口被过滤为**仅接受建筑泡沫**——任何其他流体均会被拒绝。这使得建筑泡沫背包可以安全地接入通用流体管道，而不必担心流体污染。

### Tooltip

```
CF Pack
Construction Foam: 0.00 buckets / 80 buckets
Refills Foam Sprayer in inventory while worn
```

桶数按 `CAPACITY_DROPLETS` 计算，并随泡沫的注入或抽出实时更新。

## 配方

<Recipe id="ic2_120:cf_pack" />

**图样说明**（3x3，从左到右、从上到下）：

- **第一行：** `x o x` —— **x** = 泡沫喷枪，**o** = 电路板
- **第二行：** `y z y` —— **y** = 空单元，**z** = 铁外壳
- **第三行：** `y _ y` —— **y** = 空单元（中央底部为空）

## 相关

- [工具与防护装备](../reference/tools_armor.md)
- [建筑与装饰](../reference/building_decoration.md)
