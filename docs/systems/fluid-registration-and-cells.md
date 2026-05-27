# 流体注册与流体单元系统

本文档描述 IC2-120 中流体注册、流体单元（cell）物品、客户端渲染及 JEI 集成的完整链路。
补充 `docs/systems/fluid-system.md`（管道传输），这里聚焦"流体本身"的注册逻辑与物品化。

---

## 1. 相关代码文件

| 职责 | 路径 |
|------|------|
| 流体注册 | `core/src/main/kotlin/ic2_120/content/fluid/ModFluids.kt` |
| 流体单元/桶物品 | `core/src/main/kotlin/ic2_120/content/item/CellsAndBuckets.kt` |
| 客户端流体渲染 | `core/src/client/kotlin/ic2_120/client/ModFluidClient.kt` |
| 流体颜色工具 | `core/src/client/kotlin/ic2_120/client/FluidUtils.kt` |
| 流体单元物品着色 | `core/src/client/kotlin/ic2_120/client/colorprovider/FluidCellColorProvider.kt` |
| JEI 集成 | `core/src/main/kotlin/ic2_120/integration/jei/Ic2JeiPlugin.kt` |

---

## 2. 流体注册 (ModFluids.kt)

所有流体在 `ModFluids.register()` 中通过 `registerFluid()` 统一注册。

```kotlin
registerFluid("steam", "fluid_still", "fluid_flow", tintArgb = 0xFFD0D0D0.toInt(), withBucket = false, rises = true)
```

### 2.1 参数说明

| 参数 | 说明 |
|------|------|
| `name` | 注册名，同时作为 fluid id 和 tex key |
| `stillTex` | still 纹理路径（`assets/ic2/textures/block/fluid/{name}.png`） |
| `flowTex` | flow 纹理路径 |
| `tintArgb` | 流体在世界的着色（`FluidBlock` 的 tintindex），**不是物品颜色** |
| `withBucket` | 是否注册桶物品（蒸汽等不装桶设为 false） |
| `rises` | 是否上升（蒸汽类流体，使用 `SteamFluidBlock` 控制生命周期） |

### 2.2 注册的流体一览

| 流体 | withBucket | rises | tintArgb | 渲染纹理 |
|------|-----------|-------|----------|---------|
| coolant | true | false | 无 | `coolant_still`（专用彩图） |
| hot_coolant | true | false | 无 | `hot_coolant_still`（专用彩图） |
| uu_matter | true | false | 无 | `uu_matter_still`（专用彩图） |
| weed_ex | true | false | 无 | `weed_ex_still`（专用彩图） |
| pahoehoe_lava | true | false | 无 | `pahoehoe_lava_still`（专用彩图） |
| biofuel | true | false | 0xFF97CA00 | `fluid_still`（默认 + tint） |
| biomass | true | false | 0xFF476A3C | `fluid_still`（默认 + tint） |
| distilled_water | true | false | 无 | 复用原版 `water_still` |
| construction_foam | true | false | 0xFFB4B4AF | `fluid_still`（默认 + tint） |
| creosote | true | false | 0xFF4E2D14 | `fluid_still`（默认 + tint） |
| compressed_air | true | false | 0xFFB0D8F0 | `fluid_still`（默认 + tint） |
| **steam** | **false** | **true** | 0xFFD0D0D0 | `steam_rise_still` + tint |
| **superheated_steam** | **false** | **true** | 0xFFFFBEBE | `steam_rise_still` + tint |

### 2.3 注册链路

`registerFluid()` 依次完成：

1. **注册 Fluid**（Still + Flowing 两个 `FlowableFluid` 实例）
2. **存储 tint** 到 `fluidTintColors` 映射（供世界渲染 tintindex + `FluidUtils` 颜色查询）
3. **注册 FluidBlock**（普通 `FluidBlock` 或上升的 `SteamFluidBlock`）
4. **注册桶**（`withBucket=true` 时，注册 `Ic2BucketItem` + 加入创造栏）
5. **设置 Block/Bucket 引用**到伴生对象常量
6. **填充查找表** `stillFluidMap`/`flowingFluidMap`/`blockMap`/`bucketMap`（替代 `Ic2Fluid.Still/Flowing` 中繁重的 `when` 表达式，新增流体无需再改分支）

---

## 3. 流体单元物品 (CellsAndBuckets.kt)

### 3.1 两种流体单元体系

**专用单元（ModFluidCell 子类）：**

每种流体有独立的物品类和三重模型（空/半满/满），直接在游戏内交互。

```kotlin
@ModItem(name = "coolant_cell", tab = CreativeTab.IC2_MATERIALS, group = "cells")
class CoolantCell : ModFluidCell(FabricItemSettings()) { ... }
```

已有专用单元的流体：water、distilled_water、lava、coolant、hot_coolant、uu_matter、weed_ex、pahoehoe_lava、biofuel、biomass。

**通用单元（FluidCellItem + NBT）：**

`fluid_cell` 是一个通用物品，通过 NBT 键 `"FluidVariant"` 存储任意流体：

```kotlin
@ModItem(name = "fluid_cell", tab = CreativeTab.IC2_MATERIALS, group = "cells")
class FluidCellItem : Item(FabricItemSettings()), FluidModificationItem { ... }
```

适用于：本模组无专用单元的流体 + 其他模组的流体。

### 3.2 流体到单元的映射

`getFilledFluidCell(fluid)` 实现映射逻辑（CellsAndBuckets.kt:114-131）：

- 查询预定义的 `fluid → cell_id` 映射表
- 有映射 → 返回对应的专用单元 ItemStack
- 无映射 → 返回 `fluid_cell` + 设置 NBT FluidVariant

### 3.3 桶到单元的映射

`bucketToFilledFluidCell(bucketStack)`（CellsAndBuckets.kt:136）和 `EmptyCell.onUseOnBlock` 中的桶→单元转换都使用同一张映射表：

```
water_bucket → water_cell
coolant_bucket → coolant_cell
…（对应关系同流体到单元）
steam_bucket → 不存在（steam 无桶）
其他模组的桶 → fluid_cell + NBT
```

### 3.4 Storage 注册

`CellAndBucketFluidRegistration.register()` 为单元物品注册 Fabric `FluidStorage`：

- `empty_cell` / `fluid_cell` → `FluidCellStorage`（支持任意流体，NBT 存取）
- 每个 `ModFluidCell` 子类 → `ModFluidCellStorage`（固定流体类型，满→空转换）
- `foam_sprayer`、`cf_pack` 等容器也注册对应存储

同时遍历所有流体，将满流体单元注册到创造模式物品栏（IC2 材料标签页）。

---

## 4. 客户端流体渲染 (ModFluidClient.kt)

### 4.1 两种纹理模式

```kotlin
// 模式 A：专用贴图（颜色已在 PNG 中）
registerFluid(COOLANT_STILL, COOLANT_FLOWING, "block/fluid/coolant_still", "block/fluid/coolant_flow")

// 模式 B：默认贴图（tint 从 ModFluids 自动读取，无需传参）
registerFluid(BIOFUEL_STILL, BIOFUEL_FLOWING, "block/fluid/fluid_still", "block/fluid/fluid_flow")
```

- **模式 A**：纹理 PNG 本身带颜色，适用 coolant、hot_coolant、uu_matter 等
- **模式 B**：使用 `fluid_still` / `fluid_flow` 灰度纹理，`SimpleFluidRenderHandler` 的 tint 颜色自动从 `ModFluids.getFluidTintOrNull()` 读取（单一数据源）

**注意：** `compressed_air` 也必须在 `ModFluidClient` 中注册渲染处理器。它虽然主要作为高炉内部流体使用，但 GUI、流体单元着色、JEI/Jade 或 Connector 兼容层都可能查询 `FluidRenderHandler`。客户端注册使用通用 `fluid_still` / `fluid_flow` 纹理，并通过 `ModFluids` 的 ARGB tint 提供半透明浅蓝颜色。

### 4.2 tint 颜色来源

`ModFluidClient` 的 `registerFluid()` 内部自动查询 `ModFluids.getFluidTintOrNull(still)`：
- 有 tint → 创建 `SimpleFluidRenderHandler(stillId, flowId, tint)`
- 无 tint → 创建 `SimpleFluidRenderHandler(stillId, flowId)`

调用方只需指定纹理路径，不再需要传 tint 参数。

### 4.3 半透明渲染层

所有流体方块统一注册 `RenderLayer.getTranslucent()`，确保流体在世界中正确半透明渲染。

---

## 5. 流体颜色系统 (FluidUtils.kt + FluidCellColorProvider.kt)

### 5.1 颜色获取链路（单一数据源）

`FluidUtils.getFluidColor(fluid)` 用于流体单元物品的 tintindex 着色：

```
getFluidColor(fluid)
  ├─ ModFluids.getFluidTintOrNull(fluid) 返回非 null → 使用注册时的 tintArgb（单一数据源）
  └─ null → sampleColorFromFluidTexture(fluid)
                └─ 取 FluidRenderHandler 的 still 精灵图 → 采样非透明像素平均色
```

`ModFluids.fluidTintColors` 是所有流体颜色的单一数据源。颜色在 `registerFluid()` 的
`tintArgb` 参数中定义，同时服务于：

- 流体方块世界的 tintindex 渲染
- 流体单元物品在 JEI/物品栏中的颜色
- 后续任何需要流体颜色的场景

`FluidUtils` 不再维护独立硬编码表，直接查询 `ModFluids.getFluidTintOrNull()`。

**特例：** 对于使用 `SimpleFluidRenderHandler` + tint 的流体（biofuel、biomass、
construction_foam、creosote、steam、superheated_steam），`tintArgb` 在注册时已包含正确
颜色，`FluidUtils` 直接从 `fluidTintColors` 读取，不需要额外处理。

**特例：** 对于半透明流体（如 `compressed_air`），`tintArgb` 使用 ARGB，其中 alpha 分量
参与客户端流体渲染；`FluidUtils` 仍然直接从 `fluidTintColors` 读取颜色。

### 5.2 流体单元物品着色

`FluidCellColorProvider` 注册 `ColorProviderRegistry.ITEM`：

- **通用 fluid_cell**：tintindex=1 时调用 `FluidUtils.getFluidColor(fluid)`
- **专用 ModFluidCell**（如 weed_ex_cell）：tintindex=1 时调用 `FluidUtils.getFluidColor(item.getFluid())`

---

## 6. JEI 集成 (Ic2JeiPlugin.kt)

### 6.1 流体单元子类型

`FluidCellSubtypeInterpreter` 按 `FluidVariant` NBT 区分子类型：
- 空单元 → `"empty"`
- 有流体 → `RegistryKey(fluid).toString()`
- 配方匹配上下文 → 返回 `""`（忽略子类型，使所有类型都能参与配方匹配）

### 6.2 流体单元 JEI 条目

`registerExtraIngredients()` 为没有专用单元的流体注册 `fluid_cell` + NBT 变体：

```kotlin
val modFluidCells = setOf(
    water, lava, distilled_water_still,
    coolant_still, hot_coolant_still, uu_matter_still,
    weed_ex_still, pahoehoe_lava_still,
    biofuel_still, biomass_still
    // 注意：steam、superheated_steam、compressed_air 不在排除集中
)

extraStacks += Registries.FLUID
    .filter { fluid !in modFluidCells && fluid != EMPTY && ... }
    .map { ItemStack(fluidCell).apply { setFluidCellVariant(FluidVariant.of(it)) } }
```

在排除集中的流体已有专用单元（如 `coolant_cell`），不会重复注册为通用 fluid_cell。
不在排除集中的流体（如 steam、compressed_air、其他模组流体）会作为通用 fluid_cell NBT 变体显示。

---

## 7. 添加新流体的完整检查清单

1. **ModFluids.kt**：在 `register()` 中添加 `registerFluid(...)` 调用（`tintArgb`、`withBucket`、`rises` 在此定义）
2. **ModFluidClient.kt**：在 `register()` 中添加纹理注册（带 tint 或不带）
3. **CellsAndBuckets.kt**：决定使用专用单元还是通用 fluid_cell
   - 专用单元：创建 `ModFluidCell` 子类 + 注册 `@ModItem`
   - 通用单元：无操作（自动）
4. **Ic2JeiPlugin.kt**：如果使用专用单元，将其流体加入 `modFluidCells` 排除集
   - 如果使用通用单元，**不要**加入排除集（会自动显示）
5. **语言文件**：添加 `fluid.ic2_120.{name}` 翻译
6. **合成表**：如果需要，添加配方

**不再需要的手动操作：**
- ~~在 `Ic2Fluid.Still/Flowing` 的 `when` 中添加分支~~ → 查找表自动填充
- ~~在 `FluidUtils` 中添加硬编码颜色~~ → 从 `fluidTintColors` 自动读取
