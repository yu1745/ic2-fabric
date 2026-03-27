# 杂交作物系统

本文档描述 `ic2_120` 当前的作物架/作物/杂交实现与扩展点。

**源码位置**：
- `src/main/kotlin/ic2_120/content/block/CropStickBlock.kt`
- `src/main/kotlin/ic2_120/content/block/CropBlock.kt`
- `src/main/kotlin/ic2_120/content/crop/CropSystem.kt`
- `src/main/kotlin/ic2_120/content/crop/CropCare.kt`

---

## 当前状态（2026-03-27）

- 已实现完整主链路：种植 -> 生长 -> 成熟 -> 收获 -> 杂交/扩散 -> 种子袋扫描 -> 监管机护理。
- 已完成一轮反编译常量对齐（`TileEntityCrop` / `TileEntityCropmatron` / `ItemCropSeed`）。
- 当前结论：**主循环与多数 CropCard 行为已完成对齐，仍有少量边角行为可继续打磨**。

### 与原版仍有差异（当前保留）

- `Cropmatron` 扫描空间：当前按项目需求为 `9x1x9（下方一层）` 一次扫描；原版为 `9x3x9` 分步扫描。
- 少量 CropCard 的复杂副作用（如 Eating Plant 的完整状态效果组合、极细致随机细节）仍可继续逐分支抠齐。

---

## 架构概览

系统采用「双方块 + 方块实体」结构：

- `crop_stick`：空作物架/杂交底座（`crossing_base`）
- `crop`：已种植作物（`crop_type + age`）
- `CropBlockEntity`：保存作物属性（`growth/gain/resistance`）与生长点数

状态流转：

1. 放置 `crop_stick`
2. 再放一次作物架 -> `crossing_base=true`（杂交底座）
3. 在普通作物架上使用基础种子 -> 变为 `crop`
4. `crop` 成熟后右键收获 -> 掉落收益并回到普通作物架
5. 杂交底座随机 tick -> 尝试 `crossing`（>=2 邻居）或 `spreading`（=1 邻居）
6. 普通空作物架存在低概率自发杂草（与原版方向一致）

---

## 方块状态

### `crop_stick`

- 属性：`crossing_base: Boolean`
- `false`：普通作物架贴图（`stick`）
- `true`：杂交底座贴图（`stick_upgraded`）

### `crop`

- 属性：`crop_type: CropType`、`age: Int(0..7)`
- `crop_type` 覆盖 37 个作物类型（含 `weed`）
- `age` 的可视上限由 `CropSystem.maxAge(type)` 控制

---

## 数据模型

`CropSystem` 中维护：

- `CropType`：作物枚举（37 种，含 `WEED`）
- `CropDefinition`：每种作物的
  - `maxVisualAge`
  - `tier`
  - `properties`（5 维属性，用于杂交权重）
  - `attributes`（标签，用于杂交加权）
  - `baseSeedItems`（可直接种植的基础种子）
  - `gainItem`（成熟收获物）
- 基础种子映射：`Item -> CropType`

`CropBlockEntity` 持久化字段：

- `growth`
- `gain`
- `resistance`
- `scan_level`
- `growth_points`
- `nutrients`
- `water`
- `weed_ex`
- `terrain_humidity`
- `terrain_nutrients`
- `terrain_air_quality`

---

## 生长逻辑

执行点：`CropBlockEntity.tick()`（服务端，每 `tickRate=256` tick 一次）

- 每 `tickRate*4` 周期分三相更新地形参数：
  - `terrain_humidity`
  - `terrain_nutrients`
  - `terrain_air_quality`
- 生长点按 IC2 同构公式计算：
  - `base = 3 + rand(0..6) + statGrowth`
  - `minQuality = (tier-1)*4 + statGrowth + statGain + statResistance`
  - `providedQuality = getWeightInfluence(...) * 5`
  - 根据 `providedQuality` 与 `minQuality` 差值进行增益/惩罚
- 当 `growthPoints >= CropSystem.growthDuration(type, age)` 时 `age + 1`
- `canGrow` 已按作物卡分支条件执行（光照区间、营养要求、地块要求、末阶段限制等）
- 特定作物行为在 tick 中独立处理（如 Nether/Terra 互转、毒性作物副作用）
- 金属作物已实现根系方块校验：末阶段前一龄需在下方 1..5 格找到匹配矿石/金属块

---

## 杂交逻辑

执行点：`CropStickBlock.randomTick()`（仅 `crossing_base=true`）

### 1. Crossing（杂交）

触发前置：

- 1/3 概率触发
- 四向邻居中至少 2 株可参与作物

邻居入选概率（每株）：

- 基础值 `4`
- `growth >= 16` +1
- `growth >= 30` +1
- `resistance >= 28` 时加 `(27 - resistance)`
- 若 `chance >= rand(0..15)` 则参与

目标作物选择：

- 对每个候选目标作物 `target`，累加所有来源作物 `source` 的 `calculateRatioFor(target, source)`
- 通过累计权重 + 随机数抽签得到子代类型

子代属性遗传：

- `growth/gain/resistance` 取来源均值
- 加入随机扰动：`rand(0..2n) - n`（`n`=来源数量）
- 结果 clamp 到 `[0, 31]`

结果：

- 杂交底座变为 `crop(type=child, age=0)`
- 写入子代三维属性

### 2. Spreading（扩散）

触发前置：

- 四向邻居恰好 1 株作物
- 通过与 crossing 相同的邻居概率判定

结果：

- 杂交底座变为该邻居同类型作物（`age=0`）
- 复制其三维属性

---

## 交互规则

`CropStickBlock.onUse()`：

- 空架 + 作物架物品：切换到 `crossing_base=true`
- 空架 + 基础种子：种植为 `crop`
- 空架 + `crop_seed_bag`：按种子袋内 `owner/id/growth/gain/resistance/scan` 种植
- 杂交底座 + 空手：拆回普通作物架并返还 1 根作物架

`CropBlock.onUse()`：

- 对作物使用 `Fertilizer`：增加 `nutrients` 储量
- 对作物使用 `Weed-EX Cell`：增加 `weed_ex` 储量并返还空单元
- 仅成熟时可收获
- 产物由 `CropBlockEntity.performHarvest()` 生成（含原版式掉落次数概率）
- 收获后按作物规则回退年龄；无回退规则时重置为普通作物架

---

## 种子扫描

- `crop_seed_bag` 使用原版风格字段：
  - `owner`
  - `id`
  - `growth`
  - `gain`
  - `resistance`
  - `scan`
- `cropnalyzer` 通过耗电扫描逐级提升 `scan`（0..4）
- 显示解锁规则：
  - `scan=0`：未知
  - `scan=1`：显示作物类型
  - `scan=2`：显示属性标签
  - `scan=3`：显示等级/成熟龄
  - `scan=4`：显示 `G/Ga/Re`

---

## 杂草与护理

- `WEED` 按 IC2 逻辑以 `rand(50) - growth <= 2` 判定触发扩散工作
- 普通空作物架存在低概率自发转为杂草
- `weed_ex > 0` 会抑制感染/扩散判定（不会直接清除已存在杂草）
- `nutrients/water` 会参与生长点增益计算
- `CropBlockEntity` 实现 `CropCareTarget`，支持统一护理操作接口

作物监管机联动接口：

- `CropCareService.applyInArea(world, center, radius, ops)` 可批量对范围内作物应用：
  - 施肥（`fertilizer`）
  - 补水（`hydration`）
  - 除草剂（`weedEx`）

---

## 资源与渲染

- `assets/ic2_120/blockstates/crop_stick.json`、`crop.json`
- `assets/ic2_120/models/item/crop_stick.json`、`crop.json`
- 方块渲染直接复用 `assets/ic2` 现有 crop 模型与贴图

---

## 兼容与后续

当前实现已具备可运行的完整主链路（种植、生长、成熟、杂交、扩散、收获、肥料/Weed-EX/杂草、监管机护理接口、种子袋扫描）。
仍可继续补强（原版级细节）：

- 个别 CropCard 的复杂副作用/随机细节
- 与原版配置项（tickRate、细粒度平衡参数）联动
- JEI/手册中的种子扫描说明与可视化提示

建议后续新增能力时优先在 `CropSystem` 扩展数据定义，保持 `Block/BlockEntity` 逻辑薄化。

---

## 反编译逐常量对表（IC2 对齐）

对照来源：
- `ic2/core/crop/TileEntityCrop.java`
- `ic2/core/block/machine/tileentity/TileEntityCropmatron.java`
- `ic2/core/item/ItemCropSeed.java`

| 常量/规则 | 反编译行为 | 当前实现 | 状态 | 处理 |
|---|---|---|---|---|
| 作物主 tick 周期 | `tickRate=256` | `CropBlockEntity.tick()` 使用 `256` | Match | 已对齐 |
| 地形三相更新 | `tickRate*4`，偏移 `+0/+256/+512` | 同步三相公式与偏移 | Match | 已对齐 |
| 生长基础值 | `3 + rand(7) + statGrowth` | 同公式 | Match | 已对齐 |
| 最低质量 | `(tier-1)*4 + G + Ga + Re` | 同公式 | Match | 已对齐 |
| 提供质量 | `weightInfluence * 5` | 同公式 | Match | 已对齐 |
| 低质量惩罚 | `penalty=(min-provided)*4`，`>100` 且 `rand(32)>res` 可重置 | 同公式 | Match | 已对齐 |
| 肥料（机器） | `storage<100` 时每份 `+90`（可超过 100） | `applyFertilizer(1)` 每份 `+90`，阈值 `100` | Match | 已对齐 |
| 肥料（手动） | `storage<100` 时 `+100`（可超过 100） | `applyFertilizerDirect()` 同语义 | Match | 已对齐 |
| 水分上限 | `applyHydration` 上限 `200` | 上限 `200` | Match | 已对齐 |
| Weed-EX 上限（机器） | 上限 `150` | `applyWeedEx()` 上限 `150` | Match | 已对齐 |
| Weed-EX 上限（手动） | 上限 `100` | `applyWeedExDirect()` 上限 `100` | Match | 已对齐 |
| Weed-EX 衰减 | 每作物 tick 1/10 概率 `-1` | 同逻辑 | Match | 已对齐 |
| `hasWeedEX` 消耗 | 每次判定 `-5` | 同逻辑 | Match | 已对齐 |
| 杂草扩散触发 | `isWeed && rand(50)-growth<=2` | 同公式 | Match | 已对齐 |
| crossing 总触发概率 | `1/3` | `random.nextInt(3)==0` | Match | 已对齐 |
| 邻居入选概率 | `4 + growth阈值修正 + resistance修正` 与 `rand(16)` 比较 | 同公式 | Match | 已对齐 |
| 遗传扰动 | `rand(0..2n)-n` 并 clamp `[0,31]` | 同公式 | Match | 已对齐 |
| spreading 规则 | 仅 1 邻居作物可扩散，复用同概率门槛 | 同逻辑 | Match | 已对齐 |
| Cropmatron 扫描能耗 | 每扫描点 `1 EU`，每次施加 `10 EU` | `ENERGY_PER_SCAN=1`，`ENERGY_PER_APPLY=10` | Match | 已对齐 |
| Cropmatron 扫描节拍 | 每 `10 tick` 执行扫描逻辑 | `WORK_INTERVAL_TICKS=10`（含随机偏移） | Match | 已对齐 |
| Cropmatron 扫描范围 | 原版是 9x3x9 分步扫描 | 当前为你指定的 9x1x9（下方一层）一次扫描 | Diff(预期) | 按项目需求保留 |
| Seed 字段键名 | `owner/id/growth/gain/resistance/scan` | 同键名写入/读取 | Match | 已对齐 |
| 扫描等级区间 | `scan 0..4` | 同区间与分级显示 | Match | 已对齐 |
| Weed-EX 对已存在杂草 | 原版主要为防护/抑制，不直接秒杀 | 当前不直接清除已存在杂草 | Match | 已对齐 |
| 金属作物根系校验 | 末阶段需满足根系方块要求（向下多层） | 已实现 1..5 格向下矿石/金属块校验 | Match | 已对齐 |
