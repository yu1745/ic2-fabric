# 能量流速同步设计（输入/输出/发电/耗能）

本文档说明本项目中四类 EU/t 指标的定义、服务端统计方式、客户端显示链路，以及机器接入时必须遵守的约束。

## 1. 指标定义

- 输入速率（`input`）：外部系统向机器注入的 EU/t。
- 输出速率（`output`）：机器向外部系统输出的 EU/t。
- 发电速率（`generate`）：机器内部“产生”能量的 EU/t（发电机使用）。
- 耗能速率（`consume`）：机器内部“消耗”能量的 EU/t（耗电机使用）。

注意：对“耗电机器”（电炉、提取机、压缩机、金属成型机、粉碎机、特斯拉线圈等），UI 应显示 `输入 + 耗能`，不显示 `输出`。

## 2. 服务端统计入口

核心容器：`src/main/kotlin/ic2_120/content/TickLimitedSidedEnergyContainer.kt`

### 2.1 事务路径（外部输入/输出）

- 通过 `EnergyStorage` 的 `insert/extract` + 事务提交触发 `onFinalCommit()`。
- `onFinalCommit()` 根据 `amount - lastCommittedAmount` 统计：
  - `delta > 0` => 记入 `insertedThisTick`
  - `delta < 0` => 记入 `extractedThisTick`

### 2.2 内部路径（发电/耗能/放电注能）

- 内部发电必须调用：`generateEnergy(amount)`。
- 内部耗能必须调用：`consumeEnergy(amount)`。
- 非事务路径的内部注能（如电池放电）必须调用：`insertEnergy(amount)`。
- 机器给电池/电动工具充电等内部取能必须调用：`extractEnergy(amount)`。

如果直接写 `sync.amount +=/-= x`，对应速率不会被正确记账。

## 3. tick 快照与滤波

### 3.1 跨 tick 快照

- `normalizeTickBudget()` 在 tick 变化时把 `*ThisTick` 结转到 `last*Amount`。
- `EnergyFlowSync.syncCurrentTickFlow()` 先调用 `finalizeFlowSnapshot()`，再读取 `last*Amount`，避免机器 tick 与电网 tick 顺序影响。

### 3.2 滑动平均

`EnergyFlowSync` 通过 `schema.intAveraged(...)` 同步三项平均值：

- `AvgInserted`
- `AvgExtract`
- `AvgConsume`

默认窗口 20 tick（约 1 秒）。

## 4. 同步到客户端链路

1. 机器服务端 tick 末尾调用 `sync.syncCurrentTickFlow()`。
2. `EnergyFlowSync` 将稳定快照写入 `SyncedData(PropertyDelegate)`。
3. `ScreenHandler.addProperties(propertyDelegate)` 发送到客户端。
4. 客户端 `SyncedDataView` 读取同索引数据。
5. `Screen` 调用 `handler.sync.getSynced...Amount()` 渲染。

## 5. 机器接入规范（必须满足）

每个机器 `BlockEntity.tick` 必须满足：

1. 所有提前 `return` 分支前，都调用一次 `sync.syncCurrentTickFlow()`。
2. 内部耗能只能走 `consumeEnergy()`。
3. 内部发电只能走 `generateEnergy()`。
4. 放电槽注能等内部注能只能走 `insertEnergy()`。
5. 机器内部给物品充电等取能只能走 `extractEnergy()`。
6. `readNbt` 恢复能量后优先使用 `restoreEnergy(...)`（或调用 `sync.syncCommittedAmount()`），避免首个事务 delta 误判。

## 6. UI 规范

- 发电机类：显示 `发电 + 输出`。
- 耗电机类：显示 `输入 + 耗能`。
- 储能/变压器可按设备语义显示 `输入 + 输出`。

## 7. 常见症状与根因

### 症状 A：缓存满后输入速率变 0

常见根因：

- 机器在“无配方/输出满/不工作”分支提前 `return`，未执行 `sync.syncCurrentTickFlow()`。

### 症状 B：耗能速率始终为 0

常见根因：

- 机器内部直接 `sync.amount -= need`，未调用 `consumeEnergy()`。

### 症状 C：刚进世界速率异常跳变

常见根因：

- `readNbt` 后未调用 `sync.syncCommittedAmount()`。

## 8. 最小自检清单

改完任意机器后，至少检查以下场景：

1. 刚接电线、缓存未满：输入速率非 0，耗能/发电符合预期。
2. 缓存已满但机器仍工作：输入与耗能不应长期卡成 0。
3. 无配方/输出槽满/红石关闭等早退分支：速率仍可平滑归零，不“卡值”。
4. 读档后首秒：无离谱峰值。

编译验证：

```powershell
./gradlew compileKotlin
```
