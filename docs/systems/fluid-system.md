# 流体系统总览

本文档描述 IC2-120 中流体管道系统的实现，包括管道方块、泵附件、Provider/Receiver 参与规则、流量模型与约束。

---

## 1. 相关代码位置

| 模块 | 路径 |
|------|------|
| 管道方块 | `src/main/kotlin/ic2_120/content/block/pipes/PipeBlocks.kt` |
| 管道实体 | `src/main/kotlin/ic2_120/content/block/pipes/PipeBlockEntity.kt` |
| 管道网络 | `src/main/kotlin/ic2_120/content/block/pipes/PipeNetwork.kt` |
| 网络管理 | `src/main/kotlin/ic2_120/content/block/pipes/PipeNetworkManager.kt` |
| 流体升级 | `src/main/kotlin/ic2_120/content/upgrade/IFluidPipeUpgradeSupport.kt` |
| 升级组件 | `src/main/kotlin/ic2_120/content/upgrade/FluidPipeUpgradeComponent.kt` |
| 扳手交互 | `src/main/kotlin/ic2_120/content/WrenchHandler.kt` |
| Jade 渲染 | `src/main/kotlin/ic2_120/integration/jade/Ic2JadePlugin.kt` |

---

## 2. 机器侧流体能力

流体交互基于 Fabric Transfer (`FluidStorage.SIDED`)：
- 机器内部使用 `SingleVariantStorage<FluidVariant>` 作为储罐。
- 通过 `FluidStorage.SIDED.registerForBlockEntity(...)` 注册分面流体能力。

已注册 `FluidStorage` 的机器（均可被管道网络自动识别）：
- `SolarDistillerBlockEntity`
- `GeoGeneratorBlockEntity`
- `PumpBlockEntity`
- `OreWashingPlantBlockEntity`
- `SemifluidGeneratorBlockEntity`
- `FluidBottlerBlockEntity`
- `BlastFurnaceBlockEntity`
- `FluidHeatExchangerBlockEntity`
- `ReactorFluidPortBlockEntity`

说明：任何暴露 `FluidStorage` 的方块（包括第三方模组）都会被管道网络自动识别为 provider/receiver，通过 simulateInsertion（dry-run transaction）确认实际可提取/可接受的流体。

---

## 3. 管道方块与连通性

管道是特殊方块，不继承 `MachineBlock`。

### 3.1 普通管道

**规格：**
- 材质：`BRONZE`、`CARBON`
- 尺寸：`TINY`、`SMALL`、`MEDIUM`、`LARGE`
- 共 8 种：`bronze_pipe_tiny/small/medium/large`、`carbon_pipe_tiny/small/medium/large`

**连接：**
- 六面连接属性：`north/south/east/west/up/down`
- 放置时默认连接所有可连接邻居（管道或 FluidStorage 容器）。
- 邻居更新时重算连通状态。
- 扳手右键命中面可切换该面“禁连/可连”。

### 3.2 泵附件

**规格：**
- `bronze_pump_attachment`、`carbon_pump_attachment`（共 2 种）
- 尺寸固定为 TINY，材质倍率同普通管道。

**连接规则（与普通管道不同）：**
- 泵附件只有**两个方向**可连接：
  - **正面（FACING）**：仅连接**非管道**且拥有 FluidStorage 的方块（储罐、机器等）。
  - **背面**：仅连接**管道**，不连接机器。
- 其余四个方向不可连接。

**放置：**
- 泵附件需**正面朝向储罐**放置（右键点击储罐表面），才能正确建立与储罐的连接并作为 Provider 工作。
- 若先放在管道上再朝向管道，泵会朝向管道，无法从储罐抽取流体。

**配置：**
- 右键泵附件打开 GUI，可设置流体过滤（过滤样本槽）。
- 未设置过滤时，可抽取任意流体。

---

## 4. 管道网络生命周期

`PipeNetworkManager` 按维度维护 `pos -> network` 索引。

| 事件 | 行为 |
|------|------|
| 首次访问 | 惰性建网，BFS 收集连通管道 |
| 管道移除 | `invalidateAt` 清理映射 |
| 连接状态变化 | `invalidateConnectionCachesAt` 仅清拓扑缓存，不拆网 |
| 世界卸载 | `onWorldUnload` 清理该维度缓存 |

---

## 5. 流量模型与统计

### 5.1 基础流量（桶/s）

| 尺寸 | 流量 |
|------|------|
| tiny | 0.4 |
| small | 0.8 |
| medium | 2.4 |
| large | 4.8 |

### 5.2 材质倍率

| 材质 | 倍率 |
|------|------|
| bronze | 1 |
| carbon | 2 |

### 5.3 每 tick 上限

单管道每 tick 最大传输量：

```
floor(baseBucketsPerSecond × materialMultiplier × FluidConstants.BUCKET / 20)
```

### 5.4 负载统计

- 每根管道记录 `pipeLoad`（本 tick 实际使用量）。
- Jade HUD 显示（需安装 Jade）：悬停管道方块显示 "Flow: X / Y mB/t" 进度条，混流停机时显示 "Stalled: Mixed Fluids"，泵附件额外显示 "Pump Attachment" 标签。

---

## 6. Provider / Receiver 参与规则（simulateInsertion 模式）

### 6.1 核心原则

管道网络在 `pushFluids()` 中使用 simulateInsertion 模式自动识别端点：

- **Provider** — 遍历边界，对每个邻居检查 `storage.supportsExtraction()`，然后用 dry-run transaction 确认可提取的流体种类。
- **Receiver** — 遍历边界，跳过泵正面连接，检查 `storage.supportsInsertion()`，然后用 simulateInsertion（dry-run `insert(variant, 1, tx)`）确认能接受当前网络的流体。

不需要机器预先安装升级或声明接口。任何实现了 Fabric `FluidStorage.SIDED` 的方块都会被自动识别。

### 6.2 Provider

| 途径 | 说明 |
|------|------|
| **自动识别** | 邻居方块的 `FluidStorage.supportsExtraction()` 为 true，且 dry-run 能提取到流体 |
| **泵附件** | 泵附件正面朝向的方块，检查 `supportsExtraction()`，额外应用泵的过滤配置 |

Provider 的过滤仅在泵附件场景生效。普通机器的 FluidStorage 自身控制哪些流体可被提取。

### 6.3 Receiver

| 途径 | 说明 |
|------|------|
| **自动识别** | 邻居方块的 `FluidStorage.supportsInsertion()` 为 true，且 simulateInsertion 确认能接受当前网络的流体 |

不再区分 MachineBlock 与非 MachineBlock。

### 6.4 方向

由 `FluidStorage.SIDED` 的分面注册天然解决。机器在特定方向注册不同能力的 Storage，PipeNetwork 无需额外方向判断。

---

## 7. 单流体网络硬约束

每个管道网同一 tick 只允许**一种流体**参与分配。

- 统计当前可参与的 provider 可输出流体种类集合。
- 若集合大小 > 1：整网本 tick 直接停机，不执行分配。
- 状态：`stalledByMixedProviders = true`（当前仅内部可用，尚未渲染到 Jade/UI）。

---

## 8. 物品与方块交互

### 8.1 流体升级物品

`fluid_ejector_upgrade` / `fluid_pulling_upgrade` 不再影响管道网络识别，改为控制机器自身主动推/拉行为：

- **`fluid_ejector_upgrade`**：机器主动将输出储罐的流体推送到相邻方块的 `FluidStorage`。
- **`fluid_pulling_upgrade`**：机器主动从相邻方块的 `FluidStorage` 抽取流体到输入储罐。

手持右键配置（当前为快捷交互，无独立 GUI）：
- **右键**：尝试从副手流体容器读取流体并写入过滤。
- **潜行右键**：循环切换方向（`任意 -> 下 -> 上 -> 北 -> 南 -> 西 -> 东 -> 任意`）。

说明：当前为快捷配置交互，尚未做独立 GUI 窗口版 item UI。

### 8.2 泵附件

- **右键**：打开 GUI，配置流体过滤（过滤样本槽）。
- 扳手可切换泵附件背面与管道的连接状态（与普通管道相同）。

---

## 9. 已知范围与后续扩展

**已完成：**
- 管道系统闭环（普通管道 + 泵附件、网络、流量、混流停机、扳手切面）。
- Provider/Receiver 使用 simulateInsertion 模式自动识别，无需升级。
- `fluid_ejector_upgrade` / `fluid_pulling_upgrade` 改为控制机器主动推拉。
- `SteamGenerator` / `SteamKineticGenerator` / `Condenser` 默认不主动拉取，需升级启用。
- Jade 管道实时流量与停机原因渲染（`Ic2JadePlugin`）。
- 升级配置 GUI（物品界面）替代当前快捷交互。

---

## 10. 事务约束（排障记录）

Fabric Transfer 的 `Transaction.openOuter()` 在同一线程上不允许嵌套开启“外层事务”。

**结论：**
- 物品/方块自定义交互逻辑里，不要用“全局外层事务对象”作为哨兵。
- 任何 `Storage.insert/extract` 实现都应直接使用调用方传入的 `TransactionContext`。
- 与第三方模组（如 AE2）交互时，优先走标准 `FluidStorage` 能力路径，避免额外拦截层重复开事务。

**备注：** 本仓库已修复一次由全局事务泄漏导致的跨模组流体交互崩溃（表现为 "outer transaction already active"）。
