# 核反应堆（Nuclear Reactor）

## 概述

核反应堆是 IC2 的高级发电方式，支持两种工作模式：

- **电模式**：燃料棒产热 → 堆温管理 → 散热片降温 → 发电输出 EU
- **热模式**：燃料棒产热翻倍 → 堆温管理 → 散热片加热冷却液 → 热能输出 HU

两种模式使用**完全相同的组件摆放**，热模式通过外层 5×5×5 结构自动判定。

---

## 多方块结构

核反应堆采用多方块结构：

- **中心**：核反应堆方块（`nuclear_reactor`）
- **扩展**：六面（上下前后左右）可各接触 0 或 1 个核反应仓（`reactor_chamber`）
- **外壳**（热模式）：反应堆为中心的 5×5×5 立方体外壳，由以下方块组成：
  - `reactor_vessel` — 反应堆压力容器
  - `reactor_fluid_port` — 流体接口（至少 2 个）
  - `reactor_redstone_port` — 红石接口
  - `reactor_access_hatch` — 访问接口

所有 NBT 数据均存储在反应堆 BE 上，反应仓本身无独立存储。

---

## 槽位布局与容量

### 布局

- 布局：9 行 × (3–9) 列，`index = col * 9 + row`
- 相邻：上下左右四方向 `(x±1, y)`、`(x, y±1)`

### 容量公式

```
当前有效容量 = 27 + 相邻反应仓数 × 9
```

| 常量 | 值 | 说明 |
|------|-----|------|
| `BASE_SLOTS` | 27 | 基础槽位，无反应仓时的容量 |
| `SLOTS_PER_CHAMBER` | 9 | 每个相邻反应仓增加的槽位 |
| `MAX_SLOTS` | 81 | 最大槽位数（6 个反应仓时） |

### 容量对照表

| 相邻反应仓数 | 有效容量 |
|-------------|---------|
| 0 | 27 |
| 1 | 36 |
| 2 | 45 |
| 3 | 54 |
| 4 | 63 |
| 5 | 72 |
| 6 | 81 |

### 容量变化时的行为

**容量减少（如反应仓被拆）**：
1. 立即掉落：`NuclearReactorBlock.neighborUpdate` 调用 `dropOverflowItems`
2. 每 tick 兜底：`NuclearReactorBlockEntity.tick()` 也会调用 `dropOverflowItems`

**容量增加（如放置反应仓）**：
仅增加可用槽位，原有物品保留。

---

## 计算周期与 Tick 机制

### Tick 率与偏移

- **Tick 率**：每 20 tick（每秒一次计算）
- **Tick 偏移**：BE 构造时随机 `0..19`，计算时使用 `(world.time + tickOffset) % 20 == 0`
- **目的**：分散全服核电计算，避免同一 tick 卡顿

### 双阶段遍历（processChambers）

每周期执行两次 Pass：

#### Pass 0（heatRun=false）：发电与中子脉冲

- 燃料棒：`acceptUraniumPulse` 向邻接传递脉冲，`addOutput(output)` 累加发电量
- 中子反射器：`acceptUraniumPulse` 返回 `true`，增加邻接燃料棒脉冲数
- 燃料棒耐久消耗，耗尽后替换为枯竭燃料棒

#### Pass 1（heatRun=true）：热量分配

**Pass 1 分两步执行**，先处理非散热片组件，再处理散热片组件，避免"先抽旧堆温、后产热"造成的顺序锁温。

- 燃料棒：`heat = triangularNumber(pulses) * 4 * cells`，优先传给邻接可储热组件，剩余 `addHeat`
- 散热片：`reactorVent` 吸堆温 → `alterHeat` 存自身 → `selfVent` 蒸发放热
- 热交换器：与堆/邻接组件按温度差交换热量

### 发电与能量输出

```
triangularNumber(n) = (n² + n) / 2
```

- `output` 为 float 累加值（铀每脉冲 +1.0，MOX 每脉冲 1.0–5.0 取决于堆温）
- 每 tick：`output × EU_PER_OUTPUT / 20` 转为 EU（`EU_PER_OUTPUT = 100`，即 5 EU/t × 20 tick/s）
- 等价于：**每 1 output = 5 EU/t**（与 IC2 原版一致）
- 输出上限：8192 EU/t（Tier 5）

#### 铀燃料棒发电量

```
basePulses = 1 + numberOfCells / 2
总脉冲数 = basePulses × numberOfCells × 邻接燃料棒加成
EU/t = 总脉冲数 × 100
```

| 类型 | 基础脉冲/格 | 循环次数 | 总脉冲 | EU/t |
|------|----------|---------|--------|------|
| 单联铀燃料棒 | 1 | 1 | 1 | **100** |
| 双联铀燃料棒 | 2 | 2 | 4 | **400** |
| 四联铀燃料棒 | 3 | 4 | 12 | **1200** |

> ⚠️ 表格为单格、无邻接加成时的基准值。邻接中子反射器和燃料棒会大幅增加脉冲数。

---

## 热量与堆温

反应堆可贮存热量，最多 **10,000 点**。贮存的热量即为**堆温**。

- 堆温范围：0 ~ 10,000
- 燃料棒发热优先传给邻接可储热组件，剩余进入堆温
- 散热片蒸发热量通过 `addEmitHeat(-evaporated)` 降低堆温
- 堆温 ≥ 10,000 时爆炸

### 热量阈值与环境影响

| 堆温阈值 | 影响范围 | 效果 |
|---------|---------|------|
| > 4,000 | 5×5×5 | 方块有几率着火 |
| > 5,000 | 5×5×5 | 水有几率蒸发 |
| > 7,000 | 7×7×7 | 生物有几率受到辐射伤害 |
| > 8,500 | 5×5×5 | 方块有几率变成岩浆 |
| ≥ 10,000 | — | **核反应堆爆炸** |

> 防化服（`hazmat`）可免疫堆温 > 7,000 时的辐射伤害（尚未实现）。

---

## 组件一览

### 反应堆组件 API

- **IReactor**：反应堆核心接口，供组件访问槽位、热量、输出等
- **IBaseReactorComponent**：可放入反应堆物品需实现，`canBePlacedIn` 判断合法性
- **IReactorComponent**：`processChamber`、`acceptUraniumPulse`、`canStoreHeat`、`alterHeat` 等
- **AbstractReactorComponent**：默认空实现基础类
- **AbstractDamageableReactorComponent**：带热容量（耐久）的组件基类，NBT `"use"` 存储当前热量

### 燃料棒

#### 铀燃料棒

- **单/双/四联铀燃料棒**：发电、发热，耗尽后变为枯竭燃料棒
- **枯竭铀燃料棒**：占位符，不可发电
- 发热量：`triangularNumber(pulses) * 4 * cells`
- 耐久：20,000 次脉冲

#### MOX 燃料棒

MOX 燃料棒（Mixed Oxide Fuel）的发电和发热特性与堆温正相关。

- **单/双/四联 MOX 燃料棒**：耗尽后变为枯竭 MOX 燃料棒
- 耐久：10,000 次脉冲（铀的一半）

**发电公式**（电模式，`isFluidCooled() == false`）：

```
breederEffectiveness = cycleStartHeat / maxHeat
每脉冲发电 = 4.0 × breederEffectiveness + 1.0
```

| 堆温 | 效率 | 每脉冲发电 | EU/t（单联） |
|------|------|-----------|-------------|
| 0 | 0% | 1.0 | 100 |
| 2,500 | 25% | 2.0 | 200 |
| 5,000 | 50% | 3.0 | 300 |
| 7,500 | 75% | 4.0 | 400 |
| 10,000 | 100% | 5.0 | 500 |

> 热模式下 MOX 不发电，输出为 0。

**发热公式**（热模式，`isFluidCooled() == true`）：

```
基础发热 = triangularNumber(pulses) * 4 * cells
breederEffectiveness = cycleStartHeat / maxHeat
if (breederEffectiveness > 0.5) 发热量 × 2
```

> MOX 发热翻倍条件：堆温 > 5,000（cycleStartHeat > 5,000）。翻倍后发热量 = `基础发热 × 2`。

#### 铀与 MOX 对比

| 特性 | 铀燃料棒 | MOX 燃料棒 |
|------|---------|-----------|
| 耐久 | 20,000 脉冲 | 10,000 脉冲 |
| 单联基准 EU/t | 100 | 100 ~ 500（随堆温变化） |
| 单联基准发热 | 4 HU/脉冲 | 4 HU/脉冲（液冷时堆温>5000 翻倍） |
| 最佳工作点 | 任意 | 高温（堆温越高发电越多） |

### 散热片

> 格式：`heatStorage, selfVent, reactorVent`

| 组件 | 热容量 | 自身蒸发 | 吸堆温 | 说明 |
|------|--------|----------|--------|------|
| 散热片 (heat_vent) | 1000 | 6 | 0 | 仅自身蒸发 |
| 反应堆散热片 (reactor_heat_vent) | 1000 | 5 | 5 | 吸堆温后蒸发 |
| 元件散热片 (component_heat_vent) | 0 | 0 | 0 | 向四方向邻接组件各蒸发 4 HU，**不算散失热量** |
| 高级散热片 (advanced_heat_vent) | 1000 | 12 | 0 | 仅自身蒸发 |
| 超频散热片 (overclocked_heat_vent) | 1000 | 20 | 36 | 大量吸堆温，大量蒸发 |

- **热容量**：超过后组件损坏（消失）
- **selfVent（自身蒸发）**：散热片自身的热量被"蒸发"，记录为散失热量（用于热模式冷却液转换）
- **reactorVent（吸堆温）**：从反应堆吸收热量存入自身，不是散失热量

> ⚠️ 元件散热片的向邻接散热**不算散失热量**，不会计入热模式冷却液转换。

### 热交换器

> 热交换器使用**基于温度差的可变交换速率**，温度越低交换越慢。
> 格式：`heatStorage, switchSide, switchReactor`

| 组件 | 热容量 | 邻接交换 | 堆交换 | 说明 |
|------|--------|----------|--------|------|
| 热交换器 (heat_exchanger) | 2500 | 12 | 4 | 邻接↔堆双向交换 |
| 反应堆热交换器 (reactor_heat_exchanger) | 5000 | 0 | 72 | 仅与堆交换 |
| 元件热交换器 (component_heat_exchanger) | 5000 | 36 | 0 | 仅邻接交换 |
| 高级热交换器 (advanced_heat_exchanger) | 10000 | 24 | 8 | 双向交换 |

**邻接/堆交换速率**（单位 HU/周期）：

| 双方平均温度% | 交换速率比例 |
|-------------|-------------|
| ≥ 100% | 100%（全速） |
| ≥ 75% | 50%（1/2） |
| ≥ 50% | 25%（1/4） |
| ≥ 25% | 12.5%（1/8） |
| < 25% | 最小值（1 HU） |

热量始终从**高温**流向**低温**。

### 中子反射器

- **neutron_reflector**、**thick_neutron_reflector**、**iridium_neutron_reflector**
- `acceptUraniumPulse` 恒返回 `true`（简化实现，无耐久消耗）
- 效果：增加邻接燃料棒的脉冲数（邻接数越多脉冲越多）

### 其他反应堆物品

以下物品在代码中存在但**功能尚未实现**（占位/未来扩展）：

- `reactor_coolant_cell`、`triple_reactor_coolant_cell`、`sextuple_reactor_coolant_cell` — 反应堆冷却单元
- `rsh_condensator`、`lzh_condensator` — 冷凝器
- `lithium_fuel_rod` — 锂燃料棒
- `heatpack` — 热包
- `reactor_plating`、`reactor_heat_plating`、`containment_reactor_plating` — 反应堆装甲板

---

## 热模式（液冷反应堆）

热模式是核反应堆的特殊工作模式，通过检测外层 5×5×5 结构自动判定。

### 模式判定

- **结构**：反应堆为中心的 5×5×5 立方体外壳
- **外壳组成**：压力容器 / 红石接口 / 流体接口 / 访问接口
- **必要条件**：外壳上至少 **2 个**流体接口（1 输入冷却液 + 1 输出热冷却液）
- **检测频率**：每 20 ticks（1 秒）通过 tick 偏移分散检测

### 热模式与电模式对比

| 特性 | 电模式 | 热模式 |
|------|--------|--------|
| 发热量 | 正常 | **× 2** |
| 发电量 | 正常输出 EU | **不发电（output = 0）** |
| 散热片 selfVent | 降低堆温 | **加热冷却液** |
| 元件散热片邻接散热 | 降低堆温 | **不计入冷却液转换** |
| 流体需求 | 无 | **冷却液 + 热冷却液** |
| 流体接口 | 无需 | **至少 2 个** |
| 散热片耐久修复 | 总是可自然修复 | **必须有冷却液** |
| 堆温超限 | ≥ 10,000 爆炸 | **同样爆炸** |

### 冷却液储罐

- **容量**：`COOLANT_TANK_CAPACITY_MB = 16,000 mB`（16 桶）
- **输入罐**：存储冷却液（`coolant_still`）
- **输出罐**：存储热冷却液（`hot_coolant_still`）
- **转换比例**：每 **20,000 HU** 散失热量 → 加热 **1,000 mB** 冷却液

### 散失热量的定义

**只有散热片的 `selfVent`（自身蒸发）才是散失热量**，可用于冷却液转换：

| 来源 | 计入散失热量？ | 说明 |
|------|--------------|------|
| `selfVent`（散热片自身蒸发） | ✅ 是 | 真正的热损耗 |
| `reactorVent`（从堆吸热） | ❌ 否 | 热量转移到散热片内部，未消失 |
| 元件散热片邻接散热 | ❌ 否 | 热量转移到邻接组件，未消失 |
| 热交换器交换 | ❌ 否 | 热量转移，未消失 |

### 热量守恒

热模式下，未能成功转换为热冷却液的热量会**回灌给带 selfVent 的散热片**（通过提升其内部热量来等效降低耐久）。若仍有剩余，则回加到堆温。

```
热模式热量流程：
1. 燃料棒产热 × 2
2. 热量分配（与电模式相同）
3. selfVent 散热 → ventDissipatedHeat（上限：≤ 本周期总产热）
4. ventDissipatedHeat → 加热冷却液
5. 未转换热量 → 回灌散热片 → 回加堆温
```

### 散热片耐久修复

| 模式 | 耐久修复条件 |
|------|-------------|
| 电模式 | 总是可自然修复（selfVent 蒸发时修复） |
| 热模式 | **必须有冷却液**（`hasCoolant() == true`） |

### 流体接口配置

- **至少 2 个流体接口**
- 流体接口有 1 个升级槽，可安装流体管道升级
- **手动交互**：UI 中有 4 个流体槽：
  - 冷却液输入槽 / 冷却液输出槽（冷却液 ↔ 空桶/空单元）
  - 热冷却液输入槽 / 热冷却液输出槽（空桶/空单元 ↔ 热冷却液）

### UI 显示

- **冷却液条**（左侧，能量条外侧）：蓝色，显示输入罐冷却液量
- **热冷却液条**（右侧，温度条外侧）：橙色，显示输出罐热冷却液量
- **流体交互槽**：热模式时在反应堆槽右侧显示

---

## 关键常量一览

| 常量 | 值 | 说明 |
|------|-----|------|
| `REACTOR_TIER` | 5 | Tier 5，最大输出 8192 EU/t |
| `ENERGY_CAPACITY` | 1,000,000 | 能量缓冲 1M EU |
| `EU_PER_OUTPUT` | **100** | 每 1 output = 100 EU/s = 5 EU/t（×20 tick/s） |
| `HEAT_CAPACITY` | 10,000 | 堆温上限 |
| `COOLANT_TANK_CAPACITY_MB` | **16,000** | 输入/输出罐各 16,000 mB |
| `HU_PER_BUCKET` | 20,000 | 每桶冷却液可吸收 20,000 HU |

---

## 代码文件索引

### 核心逻辑

- 反应堆 API：`ic2_120.content.reactor`
- 主逻辑：`NuclearReactorBlockEntity.kt`
- 同步与常量：`NuclearReactorSync.kt`
- 客户端 UI：`NuclearReactorScreen.kt`、`NuclearReactorScreenHandler.kt`

### 反应堆组件

- 燃料棒：`UraniumFuelRods.kt`（含 MOX）
- 散热片：`ReactorHeatVents.kt`
- 热交换器：`ReactorHeatExchangers.kt`
- 中子反射器等：`ReactorParts.kt`

### 热模式方块

- 流体接口：`ReactorFluidPortBlock.kt`、`ReactorFluidPortBlockEntity.kt`
- 红石接口：`ReactorRedstonePortBlock.kt`、`ReactorRedstonePortBlockEntity.kt`
- 压力容器：`ReactorVesselBlock.kt`
- 访问接口：`ReactorAccessHatchBlock.kt`
