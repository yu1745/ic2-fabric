# 流体系统总览（当前实现）

本文档汇总当前仓库中与流体相关的实现，包括：
- 机器流体储罐与 Fabric Transfer 接入
- 流体管道与管道网络
- 流体升级驱动的 provider / receiver 参与规则
- 单流体网络约束与停机规则

## 1. 相关代码位置

- 管道方块与网络
  - `src/main/kotlin/ic2_120/content/block/pipes/PipeBlocks.kt`
  - `src/main/kotlin/ic2_120/content/block/pipes/PipeBlockEntity.kt`
  - `src/main/kotlin/ic2_120/content/block/pipes/PipeNetworkManager.kt`
  - `src/main/kotlin/ic2_120/content/block/pipes/PipeNetwork.kt`
- 升级与机器接入
  - `src/main/kotlin/ic2_120/content/upgrade/IFluidPipeUpgradeSupport.kt`
  - `src/main/kotlin/ic2_120/content/upgrade/FluidPipeUpgradeComponent.kt`
  - `src/main/kotlin/ic2_120/content/item/Upgrades.kt`
  - `src/main/kotlin/ic2_120/content/block/machines/SolarDistillerBlockEntity.kt`
- 主初始化与扳手
  - `src/main/kotlin/ic2_120/Ic2_120.kt`
  - `src/main/kotlin/ic2_120/content/WrenchHandler.kt`

## 2. 机器侧流体能力

当前机器流体交互基于 Fabric Transfer (`FluidStorage.SIDED`)：
- 机器内部通常使用 `SingleVariantStorage<FluidVariant>` 作为储罐实现。
- 通过 `FluidStorage.SIDED.registerForBlockEntity(...)` 注册分面流体能力。

已接入“流体升级驱动管道参与”的机器：
- `SolarDistillerBlockEntity`（1 台）

说明：
- 其他流体机器（如 Geo/Water/OreWashing）已有流体储罐与外部交互能力，但尚未接入 `IFluidPipeUpgradeSupport`。

## 3. 管道方块与连通性

管道是特殊方块，不继承 `MachineBlock`。

### 3.1 规格
- 材质：`BRONZE`、`CARBON`
- 尺寸：`TINY`、`SMALL`、`MEDIUM`、`LARGE`
- 共 8 种方块：`bronze_*` + `carbon_*`

### 3.2 六面连接属性
每根管道都有六个连接属性：
- `north/south/east/west/up/down`

行为：
- 放置时默认连接所有可连接邻居。
- 邻居更新时重算连通状态。
- 可通过扳手右键命中面切换该面“禁连/可连”。

### 3.3 扳手交互
- 扳手右键管道会切换命中面的禁连状态。
- 切换后会触发管道网络连接缓存失效，下一 tick 按新拓扑工作。

## 4. 管道网络生命周期

`PipeNetworkManager` 按维度维护 `pos -> network` 索引。

- 惰性建网：首次访问时 BFS 收集连通管道。
- 拆网失效：管道移除时 `invalidateAt` 清理映射。
- 连接缓存失效：`invalidateConnectionCachesAt` 仅清拓扑缓存，不拆网。
- 世界卸载：`onWorldUnload` 清理该维度缓存。

## 5. 流量模型与统计

### 5.1 基础流量（桶/s）
- tiny: `0.4`
- small: `0.8`
- medium: `2.4`
- large: `4.8`

### 5.2 材质倍率
- bronze: `1`
- carbon: `2`

### 5.3 每 tick 上限
单管道每 tick 最大传输量：
- `floor(baseBucketsPerSecond * materialMultiplier * FluidConstants.BUCKET / 20)`

### 5.4 负载统计
- 每根管道记录 `pipeLoad`（本 tick 实际使用量）。
- 当前仅统计，不接 Jade 渲染。

## 6. Provider / Receiver 参与规则（升级驱动）

核心原则：
- 机器本身无主动推拉行为。
- 只有安装对应流体升级的机器才参与管道分配。

角色规则：
- 安装 `fluid_ejector_upgrade`：参与为 provider（需 `supportsExtraction()`）。
- 安装 `fluid_pulling_upgrade`：参与为 receiver（需 `supportsInsertion()`）。

过滤规则：
- 过滤存储在升级物品 NBT（每个升级物品独立）。
- 已设置过滤：仅匹配该流体。
- 未设置过滤：视为可匹配任意流体。

当前高级流体升级（`advanced_*`）尚未纳入实际生效逻辑。

## 7. 单流体网络硬约束

每个管道网同一 tick 只允许一种流体参与分配。

判定：
- 统计当前可参与的 provider 可输出流体种类集合。
- 若集合大小 `> 1`：整网本 tick 直接停机，不执行分配。

状态：
- `stalledByMixedProviders = true`
- 该状态当前仅内部可用，尚未渲染到 Jade/UI。

## 8. 流体升级物品交互（当前）

`fluid_ejector_upgrade` / `fluid_pulling_upgrade` 已实现手持右键配置：
- 右键：尝试从副手流体容器读取流体并写入过滤。
- 潜行右键：清空过滤。

说明：
- 当前为“快捷配置交互”，尚未做独立 GUI 窗口版 item UI。

## 9. 已知范围与后续扩展

已完成：
- 管道系统闭环（方块、网络、流量、混流停机、扳手切面）。
- 升级驱动参与逻辑闭环（provider/receiver + NBT 过滤）。
- `SolarDistiller` 接入。

待扩展：
- 将 `IFluidPipeUpgradeSupport` 扩展到更多流体机器（Geo/Water/OreWashing 等）。
- Jade 管道实时流量与停机原因渲染。
- 升级配置 GUI（物品界面）替代当前快捷交互。
