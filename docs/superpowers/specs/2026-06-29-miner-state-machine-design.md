# 采矿机状态机重写设计

- **日期**：2026-06-29
- **目标文件**：`core/src/main/kotlin/ic2_120/content/block/machines/MinerBlockEntity.kt`
- **范围**：重写 `BaseMinerBlockEntity.tick()`（原 536-794 行，258 行）及其驱动的状态逻辑。**不动** Screen/ScreenHandler/Sync 字段定义/注册链路/NBT 字段命名（值保留兼容）。
- **等价边界**：重新设计状态机。玩家可观察行为（挖矿/铺管/回收/红石/能量）以现有行为为基准，允许在 spec 明确标注处做最小修正（每处写明「旧→新+理由」）。

---

## 1. 问题诊断

`tick()` 混乱的根因是**没有一个显式状态机**。当前由 6 个散落 boolean 隐式表达 8 个逻辑状态：

| 隐式状态 | 当前承载 |
|---|---|
| 管道回收中 | `recoveringPipes` |
| 管道循环回收（普通机缺管）| `recyclingPipes` |
| 红石等待（高级机回收后）| `redstoneChangeRequired` |
| 红石未激活（高级机）| 内联判断 `!shouldRun` |
| 缺料/手动停机 | `manualStoppedForRecovery` + `sync.running==0` |
| 攒能待挖 | `pendingBreakEnergy > 0` |
| 正常扫描/挖掘 | 默认分支 |
| 到底（普通机）| 主循环局部变量 `reachedBottom` |

症状：
1. `sync.running` 既驱动逻辑又驱动 UI，在 tick 内被赋值 8+ 处，是纠缠核心。
2. `startPipeRecovery` 末尾（原 841-850）有打补丁注释防止 `manualStoppedForRecovery` 永久锁死——典型状态机不闭合。
3. 高级机/普通机差异通过 `acceptsAdvancedScanner` 在各处内联分支，而非通过状态入口/出口区分。
4. `pendingBreakEnergy` 子状态机与主扫描循环各占一段，但其实是同一「工作」态的两个阶段。

---

## 2. 状态枚举与拓扑

### 2.1 权威状态字段

```kotlin
private enum class MinerState {
    IDLE,                 // 等待自动恢复（普通机）；或红石未激活（高级机）
    REDSTONE_WAITING,     // 仅高级机：管道回收完成，等红石信号变化才重启
    PIPE_RECOVERING,      // 终局回收（普通机到底 / 玩家手动 / 高级机回收）
    PIPE_RECYCLING,       // 仅普通机：缺管时回收分支管道
    SCANNING              // 正常工作：扫描+挖掘，内部含 pendingEnergy 子阶段
}
private var minerState: MinerState = MinerState.IDLE
```

### 2.2 字段去留

**删除**（语义并入 `minerState`）：
- `recoveringPipes` → `state == PIPE_RECOVERING`
- `recyclingPipes` → `state == PIPE_RECYCLING`
- `manualStoppedForRecovery` → 由状态本身表达（`REDSTONE_WAITING` / `PIPE_RECOVERING`）
- `redstoneChangeRequired` → `state == REDSTONE_WAITING`
- `lastRedstoneActive` → 仍保留（`REDSTONE_WAITING` 检测变化需要），但语义降为「上次红石快照」

**降级为派生量**：
- `sync.running`：tick 末尾由 `minerState == SCANNING` 计算（SCANNING=1，其余=0）。**状态机内部不再读写 `sync.running` 做控制判断。**

**保留为数据**（非状态）：
- `pendingBreakEnergy`：`SCANNING` 内部子阶段数据
- `cursorIndex` / `cursorInitialized` / `lastPlacedPipeY` / `lastRecycledCursorY` / 渲染坐标 / stall 观察字段
- `knownPipePositions` / `pendingPipeRecovery` / `pipePlacementBudget` 等

### 2.3 状态转移图

```
放置/加载 ──► IDLE

IDLE ──(普通机: tryAutoResume 通过)────────────► SCANNING
IDLE ──(高级机: 红石满足 且 非 REDSTONE_WAITING)─► SCANNING
IDLE ──(高级机: 红石不满足)───────────────────► IDLE（自环，每 tick 重判）

SCANNING ──(普通机 reachedBottom)─────────────► PIPE_RECOVERING
SCANNING ──(普通机 缺管 triggerPipeRecycling)──► PIPE_RECYCLING
SCANNING ──(缺料/到底 无管可铺 等)─────────────► IDLE
SCANNING ──(玩家手动 startPipeRecovery)───────► PIPE_RECOVERING

PIPE_RECYCLING ──(队列空, 有管可恢复)─────────► SCANNING
PIPE_RECYCLING ──(队列空, 无管)───────────────► IDLE

PIPE_RECOVERING ──(普通机 队列空)─────────────► IDLE
PIPE_RECOVERING ──(高级机 队列空)─────────────► REDSTONE_WAITING

REDSTONE_WAITING ──(红石信号变化)─────────────► SCANNING
REDSTONE_WAITING ──(无变化)───────────────────► REDSTONE_WAITING（自环）
```

### 2.4 转移边精确条件表

| from | to | 条件 |
|---|---|---|
| IDLE | SCANNING | 普通机：`tryAutoResume` 通过（扫描器+钻头+管齐全，cursorY ≥ NORMAL_MIN_Y）|
| IDLE | SCANNING | 高级机：红石满足（含反转）且 `minerState != REDSTONE_WAITING` |
| SCANNING | PIPE_RECOVERING | 普通机 `reachedBottom`；或任意时机玩家手动 `startPipeRecovery` |
| SCANNING | PIPE_RECYCLING | 仅普通机：`triggerPipeRecycling` 命中（缺管、cursorY < lastRecycledCursorY、有可回收分支管）|
| SCANNING | IDLE | 高级机：红石变不满足；普通机：缺料（扫描器/钻头/管缺失）或缓存满（高级）|
| PIPE_RECYCLING | SCANNING | 回收队列处理完毕（每 tick 处理一格直到空），恢复运行 |
| PIPE_RECYCLING | IDLE | 队列空且本次未回收到任何管（停机）|
| PIPE_RECOVERING | IDLE | 普通机：`pendingPipeRecovery` 处理完毕 |
| PIPE_RECOVERING | REDSTONE_WAITING | 高级机：`pendingPipeRecovery` 处理完毕 |
| REDSTONE_WAITING | SCANNING | 高级机：`hasPower != lastRedstoneActive`（红石信号发生变化）|

---

## 3. tick() 新结构

```kotlin
fun tick(world: World, pos: BlockPos, state: BlockState) {
    if (world.isClient) return
    val serverWorld = world as? ServerWorld ?: return

    // 公共预处理（每 tick 必做，与状态无关）
    tickCommonPre(world, pos)

    // 状态分派（每个方法返回新的 MinerState，禁止内部直改 minerState）
    minerState = when (minerState) {
        MinerState.PIPE_RECOVERING  -> tickPipeRecovering(serverWorld, pos, state)
        MinerState.PIPE_RECYCLING   -> tickPipeRecycling(serverWorld, pos, state)
        MinerState.REDSTONE_WAITING -> tickRedstoneWaiting(world, pos, state)
        MinerState.IDLE             -> tickIdle(world, pos, state, serverWorld)
        MinerState.SCANNING         -> tickScanning(serverWorld, pos, state)
    }

    // 派生 sync.running 并收尾
    sync.running = if (minerState == MinerState.SCANNING) 1 else 0
    sync.syncCurrentTickFlow()
}
```

**硬性约束**：
1. 状态转移**只**通过两种途径发生：
   - `tick()` 的 `when` 分支返回值（`tickXxx` 内部不直接写 `minerState`，只 return 目标状态）
   - **外部入口方法**（`restartScan` / `startPipeRecovery` 等玩家或跨代码触发的入口）可直接写 `minerState`，因为它们不是 tick 循环的一部分，不会破坏可读性。
2. `sync.running` **只**在 tick 末尾赋值，状态机内部不读它做控制判断。
3. `setActiveState` 由各 `tickXxx` 自行调用（亮灯语义因状态而异）。

---

## 4. 各状态逐项规约

### 4.1 `tickCommonPre(world, pos)` — 公共预处理

**对应原代码**：539-561

**每 tick 必做（与状态无关）**：
1. `sync.energy = sync.amount.toInt().coerceAtLeast(0)`
2. `sync.pipeCount = getPipeCount()`
3. 重应用升级：`OverclockerUpgradeComponent.apply` / `EnergyStorageUpgradeComponent.apply` / `TransformerUpgradeComponent.apply` / `FluidPipeUpgradeComponent.apply`
4. 流体弹出升级：若 `fluidPipeProviderEnabled`，调 `FluidPipeUpgradeComponent.ejectFluidToNeighbors`
5. `adjacentEnergyTransfer.tick()`
6. `extractFromDischargingSlot()`
7. `chargeScanner()`
8. `sync.fluidBlocked = 0`
9. 高级机：`tryEjectCache(serverWorld)`
10. `sync.energyCapacity = sync.getEffectiveCapacity().toInt()...`

**返回**：无（预处理不改变状态，由调用方继续分派）。

---

### 4.2 `tickPipeRecovering(serverWorld, pos, state)` → `MinerState`

**对应原代码**：566-571 + `processPipeRecoveryTick`（1053-1089）

**进入条件**：`minerState == PIPE_RECOVERING`

**动作**：
1. 调 `processPipeRecoveryTick(serverWorld)`（保持原逻辑：管槽满则暂停返回 false；每 tick 从队列移除一格设为 AIR 并入管槽；队列空时清理）。
2. `setActiveState(world, pos, state, recovered)`（recovered = processPipeRecoveryTick 返回值）。
3. 判断下一状态：
   - 若队列未空（`pendingPipeRecovery.isNotEmpty()` 或 `processPipeRecoveryTick` 返回 true）→ `return PIPE_RECOVERING`（自环继续）
   - 若队列已空（`processPipeRecoveryTick` 走到末尾清理分支）：
     - 普通机 → `return IDLE`
     - 高级机 → `return REDSTONE_WAITING`

**注意**：原 `processPipeRecoveryTick` 末尾的高级机分支（1070-1082）做了游标重置 + 清红石等待。重写后这部分拆分：
- 游标重置 / `pendingBreakEnergy = 0` / `resetPathFailState()`：**保留**，在队列空时执行
- `redstoneChangeRequired = false`：**删除**（改为状态转移到 `REDSTONE_WAITING` 表达）
- `manualStoppedForRecovery = false`：**删除**（字段已删）

**与原行为差异（需标注）**：原代码高级机回收完会 `redstoneChangeRequired = false` 然后**立即**在下一 tick 由红石门控判断——但因为 `startPipeRecovery` 设了 `redstoneChangeRequired = true`，实际效果就是「回收完进红石等待」。新状态机把这个隐式语义变成显式 `REDSTONE_WAITING` 状态，**玩家可观察行为不变**。

---

### 4.3 `tickPipeRecycling(serverWorld, pos, state)` → `MinerState`

**对应原代码**：共用 `processPipeRecoveryTick`，但转移目标不同

**进入条件**：`minerState == PIPE_RECYCLING`（仅普通机可达）

**动作**：
1. 同 4.2 调 `processPipeRecoveryTick(serverWorld)`。
2. `setActiveState(...)`。
3. 判断下一状态：
   - 队列未空 → `return PIPE_RECYCLING`
   - 队列空 → `return SCANNING`（普通机回收完恢复挖矿；原行为：`processPipeRecoveryTick` 末尾 `recyclingPipes = false` 后 `sync.running` 仍为 1，等价于回 SCANNING）

**注意**：原 `processPipeRecoveryTick` 末尾的 `if (recyclingPipes) { recyclingPipes = false }` 分支保留语义为「队列空时转移回 SCANNING」。

---

### 4.4 `tickRedstoneWaiting(world, pos, state)` → `MinerState`

**对应原代码**：574-606 的 `redstoneChangeRequired` 分支

**进入条件**：`minerState == REDSTONE_WAITING`（仅高级机）

**动作**：
1. 读 `hasPower = world.isReceivingRedstonePower(pos)`。
2. 若 `hasPower != lastRedstoneActive`（红石信号变化）：
   - `lastRedstoneActive = hasPower`
   - 重置游标：`sync.cursorY = pos.y - 1`、`cursorInitialized = false`、`cursorIndex = 0`、`pendingBreakEnergy = 0L`、`resetPathFailState()`
   - `setActiveState(world, pos, state, false)`
   - `return SCANNING`
3. 否则（无变化）：
   - `setActiveState(world, pos, state, false)`
   - `return REDSTONE_WAITING`（自环）

**注意**：原代码 583-589 的重置逻辑（清 `manualStoppedForRecovery`、`sync.running = 1` 等）收敛为「转移回 SCANNING」。`lastRedstoneActive` 在此处更新（原代码在 597 行无条件更新，新代码仅在检测变化时更新——**这是最小行为差异**：原代码每 tick 都刷新 `lastRedstoneActive`，会导致「红石变化检测窗口 = 1 tick」；新代码保持 `lastRedstoneActive` 不变直到检测到变化，窗口语义更明确。两者对玩家可观察行为一致，因为进入 `REDSTONE_WAITING` 后只有信号变化才转移）。

---

### 4.5 `tickIdle(world, pos, state, serverWorld)` → `MinerState`

**对应原代码**：574-606（非红石等待部分）+ 608-620（`tryAutoResumeAfterPipeRefill` + 扫描器检查）

**进入条件**：`minerState == IDLE`

**动作**：
1. **红石门控（仅高级机）**：
   - 读 `hasPower`、`hasInverter`、计算 `shouldRun`
   - 若 `!shouldRun`：`setActiveState(..., false)`、`return IDLE`（红石未激活，自环）
2. **自动恢复（`tryAutoResume`）**：
   - 普通机：若扫描器+钻头+管齐全且 `cursorY >= NORMAL_MIN_Y` → 重置 `pendingBreakEnergy = 0L`、`return SCANNING`
   - 高级机：红石满足即 `return SCANNING`（高级机 IDLE 不做缺料判断，因为高级机用扫描器电池且挖矿工具内置；原 `tryAutoResumeAfterPipeRefill` 对高级机也会跑，但条件 `getDrillBreakCost()==null` 对高级机恒为 false——所以高级机实际上只要红石满足就 SCANNING。**保留此语义**。）
3. 不满足恢复条件：`setActiveState(..., false)`、`return IDLE`

**注意**：原 `tryAutoResumeAfterPipeRefill`（888-900）逻辑保留为 `tryAutoResume()` 辅助，返回 Boolean，由 `tickIdle` 据此决定转 SCANNING 还是留 IDLE。`manualStoppedForRecovery` 判断删除（原 890 行 `if (manualStoppedForRecovery) return`——新状态机里 `manualStoppedForRecovery` 场景对应 `REDSTONE_WAITING` 或 `PIPE_RECOVERING`，根本不会进 IDLE，所以这条 guard 自然失效）。

---

### 4.6 `tickScanning(serverWorld, pos, state)` → `MinerState`

**对应原代码**：623-793（最长一段，内部拆子方法）

**进入条件**：`minerState == SCANNING`

**子阶段顺序**（每个子方法返回 `MinerState`，首个非 null 即决定转移；否则继续）：

#### 4.6.1 红石再检查（仅高级机）`checkRedstoneStillActive()`
- 高级机每 tick 重判红石（原 574-606 在 SCANNING 之前的逻辑，但因新状态机把红石门控放在 tickIdle，SCANNING 态也需要每 tick 复查——**保留原行为**：红石断开立即回 IDLE）。
- 若 `!shouldRun` → `return IDLE`

#### 4.6.2 扫描器存在性 `checkScanner()`
- `getScannerType(getStack(SLOT_SCANNER)) == null` → `setActiveState(..., false)`、`return IDLE`
- 计算 `scanRadius`

#### 4.6.3 stall 观察 `observeCursorStall(world, "pre_tick")`
- 无状态转移（保留诊断逻辑）。

#### 4.6.4 游标初始化 + 垂直管柱延伸 `ensureCursorAndVerticalPipe(scanRadius)`
- `if (!cursorInitialized) ensureAndGetCursorTarget(scanRadius)`
- 若 `cursorInitialized && sync.cursorY < pos.y`：
  - `ensurePipeColumnReaches(sync.cursorY)` 失败 → `setActiveState(..., false)`、`return SCANNING`（自环，下 tick 继续）
  - 遇到不可破坏方块（`sync.running==0` 触发的 reachedBottom）→ 标记 `reachedBottom = true`
- 返回 `reachedBottom` 标志给调用方。

#### 4.6.5 能量预算预检查 `checkScanEnergyBudget(scanRadius)`
- 普通机：`sync.amount < scanCost` → `setActiveState(..., false)`、`return SCANNING`（自环等能量）
- 高级机：扫描器电池能量在 `consumeScannerEnergy` 时检查，此处不做预算 guard。

#### 4.6.6 周期节流 `checkPeriodThrottle(world)`
- `((world.time + workOffset) % effectivePeriod) != 0L` → `setActiveState(..., true)`（等待态仍亮灯）、`return SCANNING`

#### 4.6.7 攒能子阶段 `tickPendingBreakEnergy()`
- `if (pendingBreakEnergy > 0L)`：
  - 取 `drillBreakCost`，若 null → 清 `pendingBreakEnergy`，转 4.6.8
  - 计算 `breakEnergy`，从 `sync.amount` 攒入 `pendingBreakEnergy`
  - 攒够 → 校验目标方块可挖 + 管道相邻 → 挖、推进游标、`setActiveState(..., true)`、`return SCANNING`
  - 未攒够 → `setActiveState(..., true)`、`return SCANNING`
- 否则继续 4.6.8

#### 4.6.8 主扫描/挖掘循环 `runScanMineLoop(scanRadius)`
- 原 709-793 的 while 循环，逐格扫描/挖掘。
- 返回值：
  - 普通机 `reachedBottom` → `return PIPE_RECOVERING`
  - 普通机缺管触发 `triggerPipeRecycling` → `return PIPE_RECYCLING`
  - 缺料（扫描器/钻头/管缺失）→ `return IDLE`
  - 高级机缓存满 → `return IDLE`
  - 正常 → `setActiveState(..., active || scannedThisCycle>0)`、`return SCANNING`

**子方法返回值约定**：每个子方法返回 `MinerState?`——返回非 null 表示发生转移，调用方立即 return 该值；返回 null 表示继续下一子阶段。`tickScanning` 末尾默认 `return SCANNING`。

---

## 5. 外部入口方法状态化

以下公开/私有方法需改为操作 `minerState` 而非旧 boolean：

### 5.1 `restartScan()`（玩家点「重新扫描」按钮）
- 重置所有游标数据 + `pendingBreakEnergy = 0L` + `resetPathFailState()`
- **`minerState = SCANNING`**（无论之前什么状态，强制重启）
- 清 `pendingPipeRecovery`
- **删除**对 `manualStoppedForRecovery` / `redstoneChangeRequired` / `recoveringPipes` / `recyclingPipes` / `lastRecycledCursorY` 的赋值（字段已删；`lastRecycledCursorY` 重置为 `Int.MAX_VALUE` 保留，因为它是数据不是状态）

### 5.2 `startPipeRecovery()`（玩家手动 / 普通机到底触发）
- 构建回收队列 `buildPipeRecoveryQueue`
- 若队列非空：**`minerState = PIPE_RECOVERING`**
- 若队列空：
  - 高级机：**`minerState = IDLE`**（原 844-850 的「撤销停机态」语义：无管可回收就不锁死，交红石门控）
  - 普通机：**`minerState = IDLE`**（原行为保留）
- 重置游标数据
- **删除** `manualStoppedForRecovery` / `recyclingPipes` / `redstoneChangeRequired` 赋值

### 5.3 `triggerPipeRecycling()`（普通机缺管内部触发）
- 原 859-868。改为：满足条件时构建回收队列，**`minerState = PIPE_RECYCLING`**；否则不改状态。
- 注意：此方法在 `runScanMineLoop` 内部被 `tryPlacePipeAt` 调用（原 1178 行）。重写后由 `runScanMineLoop` 在调用后检查 `minerState` 是否被改为 `PIPE_RECYCLING`，若是则 `return PIPE_RECYCLING`。

**⚠️ 实现注意**：`triggerPipeRecycling` 被 `tryPlacePipeAt` 调用，而 `tryPlacePipeAt` 又被 `ensurePipeReachesPosition` / `ensurePipeColumnReaches` 调用——这些目前是「返回 Boolean」的纯函数式调用链。若让 `triggerPipeRecycling` 直接改 `minerState` 会违反「转移只在 when 返回值发生」约束。

**解决方案**：`triggerPipeRecycling` 改为**返回 `Boolean`（是否触发了回收）**，由 `runScanMineLoop` / `tickScanning` 的调用层把「触发了回收」翻译成 `return PIPE_RECYCLING`。`minerState` 的赋值仍只发生在 `when` 分支返回值。`tryPlacePipeAt` 增加一个 out 参数或返回值表示「需触发回收」，向上冒泡到 `tickScanning`。

---

## 6. NBT 兼容性

**目标**：旧存档（用旧 boolean 写的 NBT）加载后状态机正确初始化。

### 6.1 删除字段的 NBT key
- `NBT_MANUAL_STOPPED_FOR_RECOVERY`：**保留 key 读取但忽略值**（向前兼容旧存档），加载时根据旧值推导初始 `minerState`：
  - 旧 `manualStoppedForRecovery == true` 且高级机 → `minerState = REDSTONE_WAITING`
  - 旧 `manualStoppedForRecovery == true` 且普通机 → `minerState = IDLE`（原普通机 manualStopped 仅在 startPipeRecovery 设，对应回收后；新状态机普通机回收完回 IDLE，一致）
  - 否则 → 由其他字段推导
- `NBT_REDSTONE_CHANGE_REQUIRED`：同理，旧 `true` → 初始 `minerState = REDSTONE_WAITING`
- 写出时：**不再写这两个 key**（或写常量 false，保持 key 存在但无副作用——**推荐：不写，因为新存档不再需要**）

### 6.2 派生 `recoveringPipes` / `recyclingPipes`（原未持久化）
- 这两个 boolean 原 NBT 未保存（无对应 key），加载后默认 false。新状态机加载时：
  - 默认 `minerState = IDLE`（除非 6.1 的旧字段推导出别的）
  - **已知行为差异**：原代码若服务器在 `recoveringPipes==true` 时崩溃重启，回收状态会丢失（默认 false → 回 SCANNING/IDLE）。新状态机同样不持久化 `PIPE_RECOVERING/PIPE_RECYCLING`（**与原行为一致**，不算回归）。

### 6.3 `sync.running` 持久化
- `sync.running` 是 SyncedData 字段，有独立持久化。新状态机不读它做控制，但**保留写出**（派生值），保证客户端首次同步看到正确灯态。
- 加载时：**忽略 `sync.running` 的值**（不用于推导 `minerState`），第一 tick 末尾会重新派生。

### 6.4 新增 NBT key（可选，推荐）
- `NBT_MINER_STATE`（String）：持久化当前 `minerState.name()`，使 `REDSTONE_WAITING` 在重启后恢复。
- 加载优先级：`NBT_MINER_STATE` > 旧字段推导 > 默认 IDLE。
- **这是唯一一处「新行为」**：原代码 `REDSTONE_WAITING` 语义不持久化（靠 `redstoneChangeRequired` 持久化，新代码删了该字段）。新增 `NBT_MINER_STATE` 保持等价。

---

## 7. 行为差异汇总

| # | 旧行为 | 新行为 | 理由 | 玩家可观察？ |
|---|---|---|---|---|
| 1 | `sync.running` 在 tick 内多处赋值，驱动控制 | 仅末尾派生 | 解耦 | 否（UI 灯态一致）|
| 2 | 高级机回收完进红石等待靠 `redstoneChangeRequired` + `manualStoppedForRecovery` 双 boolean | 显式 `REDSTONE_WAITING` 状态 | 闭合状态机 | 否 |
| 3 | `lastRedstoneActive` 每 tick 无条件刷新 | 仅检测到变化时刷新 | 语义明确化 | 否（窗口语义对玩家不可见）|
| 4 | `REDSTONE_WAITING` 靠 `redstoneChangeRequired` 持久化 | 靠 `NBT_MINER_STATE` 持久化 | 字段重构 | 否（重启后状态一致）|
| 5 | `triggerPipeRecycling` 直接改 `recyclingPipes` boolean | 返回 Boolean，由调用层翻译成状态转移 | 遵守「转移只在 when 返回值」约束 | 否 |

**无任何数值/能量/挖矿规则变化。**

---

## 8. 不在本次范围

- 游标螺旋算法（`spiralXY`）——纯算法，不涉及状态，不动。
- A* 寻路（`findBestPath3D` / `collectExistingPipeStarts3D`）——纯算法，不动。
- 流体处理（`handleFluidBlockForPipe` / `findFluidSourceBFS`）——不动。
- 物品缓存 / 弹出（`itemCache` / `tryEjectCache`）——不动。
- `shouldMine` / `mineBlock` / `getDrillBreakCost` / `canMineWithCurrentDrill`——挖矿判定，不动。
- SyncedData 字段定义、ScreenHandler、Screen、注册链路——不动。

---

## 9. 验证

- `./gradlew :core:compileKotlin` 服务端 + 客户端编译通过。
- 旧存档加载测试（若有）：放置中的采矿机重启后状态正确。
- 手测矩阵（按状态）：
  - 普通机：正常挖矿 → 到底 → 管道回收 → 停机
  - 普通机：挖矿中缺管 → 分支回收 → 恢复
  - 高级机：红石激活挖矿 → 红石断开停机 → 重新激活恢复
  - 高级机：到底/手动 → 管道回收 → REDSTONE_WAITING → 拨红石 → 恢复
  - 两机型：缺扫描器/钻头 → IDLE → 补料 → 自动恢复
