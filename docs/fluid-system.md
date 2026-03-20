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

已接入“流体升级驱动管道参与”的机器：
- `SolarDistillerBlockEntity`
- `GeoGeneratorBlockEntity`
- `PumpBlockEntity`
- `OreWashingPlantBlockEntity`

说明：`WaterGenerator` 等其它流体机器可通过自身 `FluidStorage` 交互，但未接入升级驱动的 provider/receiver 参与规则。

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

## 6. Provider / Receiver 参与规则

### 6.1 核心原则

- 管道网络**主动拉取**流体：从 Provider 提取，向 Receiver 插入。
- 机器/储罐本身无主动推拉行为，仅通过能力接口参与。

### 6.2 Provider（流体提供方）

满足以下**任一**条件即可作为 Provider：

| 来源 | 条件 |
|------|------|
| **泵附件强制** | 泵附件正面朝向该方块，且 `storage.supportsExtraction()` |
| **升级驱动** | 方块为 MachineBlock，安装 `fluid_ejector_upgrade`，且 `storage.supportsExtraction()` |

泵附件可强制**任意** FluidStorage 容器（如 AE2 储罐、第三方储罐）作为 Provider，无需在容器上安装升级。

### 6.3 Receiver（流体接收方）

满足以下**任一**条件即可作为 Receiver：

| 来源 | 条件 |
|------|------|
| **非 MachineBlock** | 方块不是 MachineBlock，且 `storage.supportsInsertion()` |
| **升级驱动** | 方块为 MachineBlock，安装 `fluid_pulling_upgrade`，且 `storage.supportsInsertion()` |

非 MachineBlock（如 AE2 储罐、原版储罐）只要有 `supportsInsertion()` 即可作为 Receiver，无需安装升级。  
MachineBlock 必须安装 `fluid_pulling_upgrade` 才能作为 Receiver。

### 6.4 过滤规则

- **泵附件**：过滤存储在 BlockEntity NBT，通过 GUI 配置。
- **升级**：过滤存储在升级物品 NBT，每个升级独立。
- 已设置过滤：仅匹配该流体。
- 未设置过滤：视为可匹配任意流体。

### 6.5 方向规则（仅升级驱动）

- 方向存储在升级物品 NBT。
- 已设置方向：仅该方向连接面参与。
- 未设置方向：视为任意面参与。
- 若 provider/receiver 一方设置方向、另一方未设置，则“未设置”的一方自动排除已设置方向（避免同一面既入又出）。

当前高级流体升级（`advanced_*`）尚未纳入实际生效逻辑。

---

## 7. 单流体网络硬约束

每个管道网同一 tick 只允许**一种流体**参与分配。

- 统计当前可参与的 provider 可输出流体种类集合。
- 若集合大小 > 1：整网本 tick 直接停机，不执行分配。
- 状态：`stalledByMixedProviders = true`（当前仅内部可用，尚未渲染到 Jade/UI）。

---

## 8. 物品与方块交互

### 8.1 流体升级物品

`fluid_ejector_upgrade` / `fluid_pulling_upgrade` 手持右键配置：
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
- Provider：泵附件强制 + 升级驱动。
- Receiver：非 MachineBlock 自动 + 升级驱动。
- `SolarDistiller` / `GeoGenerator` / `Pump` / `OreWashingPlant` 接入升级驱动。
- Jade 管道实时流量与停机原因渲染（`Ic2JadePlugin`）。

**待扩展：**
- 将 `IFluidPipeUpgradeSupport` 扩展到更多流体机器（如 WaterGenerator 等）。
- 升级配置 GUI（物品界面）替代当前快捷交互。

---

## 10. 事务约束（排障记录）

Fabric Transfer 的 `Transaction.openOuter()` 在同一线程上不允许嵌套开启“外层事务”。

**结论：**
- 物品/方块自定义交互逻辑里，不要用“全局外层事务对象”作为哨兵。
- 任何 `Storage.insert/extract` 实现都应直接使用调用方传入的 `TransactionContext`。
- 与第三方模组（如 AE2）交互时，优先走标准 `FluidStorage` 能力路径，避免额外拦截层重复开事务。

**备注：** 本仓库已修复一次由全局事务泄漏导致的跨模组流体交互崩溃（表现为 "outer transaction already active"）。
