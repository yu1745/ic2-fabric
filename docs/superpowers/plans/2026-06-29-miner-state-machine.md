# 采矿机状态机重写 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `MinerBlockEntity.tick()`（258 行、6 个散落 boolean 隐式状态机）重写为基于显式 `MinerState` 枚举的 `when(state)` 分派，`sync.running` 降为派生量，玩家可观察行为不变。

**Architecture:** 新增 `private enum class MinerState { IDLE, REDSTONE_WAITING, PIPE_RECOVERING, PIPE_RECYCLING, SCANNING }` 作为单一权威状态字段。`tick()` 缩为 ~40 行：公共预处理 → `when(minerState)` 分派到各 `tickXxx()`（每个返回新状态）→ 末尾派生 `sync.running`。`SCANNING` 内部按子阶段拆方法。删除 `recoveringPipes`/`recyclingPipes`/`manualStoppedForRecovery`/`redstoneChangeRequired` 四个 boolean，语义并入状态。NBT 兼容旧存档（读旧 key 推导初始状态 + 新增 `NBT_MINER_STATE` 持久化）。

**Tech Stack:** Kotlin 1.x、Fabric 1.20.1、Loom。验证用 `./gradlew :core:compileKotlin`（快速）+ `./gradlew build`（服务端+客户端双检，AGENTS.md §2 强制）。无配方改动，**不需要** `runDatagen`。

**Spec:** `docs/superpowers/specs/2026-06-29-miner-state-machine-design.md`（必读，本计划的每条转移条件都在 spec §2.4 / §4）

**关键参考文件：**
- `core/src/main/kotlin/ic2_120/content/block/machines/MinerBlockEntity.kt`（被改文件，行号基于本计划写作时状态）
- `core/src/main/kotlin/ic2_120/content/block/machines/MachineBlockEntity.kt:86`（`setActiveState(world, pos, state, active)` 签名）

---

## 文件结构

只改一个文件：

- **Modify**: `core/src/main/kotlin/ic2_120/content/block/machines/MinerBlockEntity.kt`
  - 职责：采矿机 BlockEntity。本次改动集中在 `BaseMinerBlockEntity` 的状态字段、`tick()`、外部入口方法（`restartScan`/`startPipeRecovery`/`triggerPipeRecycling`）、NBT 读写。

- **Create**（验证用，可选）: `core/test/mcdebug/miner.test.ts`
  - 职责：最小回归 smoke test——通电后普通机能挖一格矿石。不覆盖全部状态转移（mcdebug 15s 硬约束 + 从零写状态机测试风险高），只防「重构后完全不工作」回归。

**为什么单文件**：状态机逻辑自包含在 MinerBlockEntity 内，不跨类。`MinerSync`（`MinerSync.kt`）字段定义不动；`MinerScreenHandler`/`MinerScreen` 不动；注册链路不动。

---

## Task 0: 准备 + 基线编译

**目的**：确认起点编译干净，建立基线。

**Files:**
- Read: `core/src/main/kotlin/ic2_120/content/block/machines/MinerBlockEntity.kt`
- Read: `docs/superpowers/specs/2026-06-29-miner-state-machine-design.md`

- [ ] **Step 1: 读 spec 全文**

通读 `docs/superpowers/specs/2026-06-29-miner-state-machine-design.md`，特别是 §2.4（转移边表）、§4（各状态规约）、§6（NBT 兼容）、§7（行为差异汇总）。

- [ ] **Step 2: 读现状 tick() 全文**

读 `MinerBlockEntity.kt` 第 536-794 行（`tick()`）和 824-900 行（`startPipeRecovery`/`triggerPipeRecycling`/`tryAutoResumeAfterPipeRefill`）。这是重写对象。

- [ ] **Step 3: 基线编译**

Run: `./gradlew :core:compileKotlin`
Expected: BUILD SUCCESSFUL。若失败，先修现状的编译错误再开始（不在本计划范围）。

- [ ] **Step 4: 创建工作分支**

```bash
git checkout -b refactor/miner-state-machine
```

---

## Task 1: 新增 `MinerState` 枚举 + 状态字段（不接线）

**目的**：先把状态枚举和字段加上，但还不接入 `tick()`。纯增量，编译必过。

**Files:**
- Modify: `core/src/main/kotlin/ic2_120/content/block/machines/MinerBlockEntity.kt`（companion object 内 + 字段区）

- [ ] **Step 1: 在 companion object 内加 NBT key 常量**

在 `companion object` 内、现有 `NBT_LAST_REDSTONE_ACTIVE` 之后（约第 181 行后）加：

```kotlin
private const val NBT_MINER_STATE = "MinerState"
```

- [ ] **Step 2: 在 `BaseMinerBlockEntity` 内、`PipeReachResult` enum 附近（约第 1047 行）加状态枚举**

```kotlin
/**
 * 采矿机显式状态机。替换原散落 boolean（recoveringPipes/recyclingPipes/
 * manualStoppedForRecovery/redstoneChangeRequired）。
 * 转移只通过 tick() 的 when 分支返回值发生（外部入口方法除外，见 spec §3）。
 */
private enum class MinerState {
    /** 等待自动恢复（普通机）；或红石未激活（高级机）。 */
    IDLE,
    /** 仅高级机：管道回收完成，等红石信号变化才重启。 */
    REDSTONE_WAITING,
    /** 终局回收（普通机到底 / 玩家手动 / 高级机回收）。 */
    PIPE_RECOVERING,
    /** 仅普通机：缺管时回收分支管道。 */
    PIPE_RECYCLING,
    /** 正常工作：扫描+挖掘，内部含 pendingBreakEnergy 子阶段。 */
    SCANNING
}
```

- [ ] **Step 3: 在字段区（约第 249 行 `private var redstoneChangeRequired` 附近）加状态字段**

```kotlin
/** 状态机权威字段。见 [MinerState]。 */
private var minerState: MinerState = MinerState.IDLE
```

注意：此时旧 boolean 字段（`recoveringPipes`/`recyclingPipes`/`manualStoppedForRecovery`/`redstoneChangeRequired`）**先保留不动**，本 Task 不删，避免一次性改太多编译不过。后续 Task 逐步替换引用后再删。

- [ ] **Step 4: 编译验证**

Run: `./gradlew :core:compileKotlin`
Expected: BUILD SUCCESSFUL（新增未使用的字段/枚举不会报错）。

- [ ] **Step 5: Commit**

```bash
git add core/src/main/kotlin/ic2_120/content/block/machines/MinerBlockEntity.kt
git commit -m "refactor(miner): 新增 MinerState 枚举与 minerState 字段（未接线）

为状态机重写做准备，旧 boolean 字段暂保留。"
```

---

## Task 2: NBT 兼容读写（新旧共存）

**目的**：让 `readNbt`/`writeNbt` 同时支持新旧格式。读时优先 `NBT_MINER_STATE`，其次旧 boolean 推导。写时只写新 key（保留旧 key 写 false 以向前兼容一段时间，见 spec §6.1）。

**Files:**
- Modify: `core/src/main/kotlin/ic2_120/content/block/machines/MinerBlockEntity.kt`（`readNbt` 约 440-493、`writeNbt` 约 495-530）

- [ ] **Step 1: 在 `readNbt` 末尾（约第 492 行 `lastRedstoneActive = ...` 之后）加状态推导**

```kotlin
// 状态机初始化：优先读新 key，其次旧 boolean 推导（向前兼容）
minerState = when {
    nbt.contains(NBT_MINER_STATE) -> {
        val name = nbt.getString(NBT_MINER_STATE)
        MinerState.entries.firstOrNull { it.name == name } ?: MinerState.IDLE
    }
    nbt.getBoolean(NBT_REDSTONE_CHANGE_REQUIRED) || nbt.getBoolean(NBT_MANUAL_STOPPED_FOR_RECOVERY) -> {
        if (acceptsAdvancedScanner) MinerState.REDSTONE_WAITING else MinerState.IDLE
    }
    else -> MinerState.IDLE
}
```

说明：旧存档 `recoveringPipes`/`recyclingPipes` 未持久化（spec §6.2），重启后本就回 IDLE/SCANNING，这里统一回 IDLE（下一 tick 若条件满足会自动进 SCANNING）。

- [ ] **Step 2: 在 `writeNbt` 末尾（约第 529 行 `nbt.putBoolean(NBT_LAST_REDSTONE_ACTIVE, lastRedstoneActive)` 之后）加新 key 写出**

```kotlin
nbt.putString(NBT_MINER_STATE, minerState.name)
// 旧 key 写固定值，向前兼容旧版本加载（新代码读时优先 NBT_MINER_STATE）
nbt.putBoolean(NBT_REDSTONE_CHANGE_REQUIRED, false)
nbt.putBoolean(NBT_MANUAL_STOPPED_FOR_RECOVERY, false)
```

注意：保留对 `NBT_REDSTONE_CHANGE_REQUIRED` / `NBT_MANUAL_STOPPED_FOR_RECOVERY` 原有的 `nbt.putBoolean(...)` 行会重复——**删除原 491、492 行**（`redstoneChangeRequired = nbt.getBoolean(...)` / `lastRedstoneActive = ...` 中的 redstoneChangeRequired 读取），以及 writeNbt 中原 528 行 `nbt.putBoolean(NBT_REDSTONE_CHANGE_REQUIRED, redstoneChangeRequired)`。`lastRedstoneActive` 的读写保留（spec §2.2 保留该字段）。

具体：
- readNbt：删 `redstoneChangeRequired = nbt.getBoolean(NBT_REDSTONE_CHANGE_REQUIRED)`（原 491 行），保留 `lastRedstoneActive = nbt.getBoolean(NBT_LAST_REDSTONE_ACTIVE)`。
- writeNbt：删 `nbt.putBoolean(NBT_REDSTONE_CHANGE_REQUIRED, redstoneChangeRequired)`（原 528 行），保留 `nbt.putBoolean(NBT_LAST_REDSTONE_ACTIVE, lastRedstoneActive)`。

- [ ] **Step 3: 编译验证**

Run: `./gradlew :core:compileKotlin`
Expected: BUILD SUCCESSFUL。此时 `redstoneChangeRequired` 字段仍存在但不再被 NBT 读写引用——会触发「未使用」警告但不报错。后续 Task 删字段。

- [ ] **Step 4: Commit**

```bash
git add core/src/main/kotlin/ic2_120/content/block/machines/MinerBlockEntity.kt
git commit -m "refactor(miner): NBT 读写支持新 MinerState（含旧存档兼容推导）"
```

---

## Task 3: 重写 `tick()` 主体为 `when(minerState)` 分派（空壳版）

**目的**：先把 `tick()` 的骨架换掉，但各 `tickXxx()` 先写成「直接返回当前状态」的空壳，保证编译通过且行为暂时靠旧逻辑（本 Task 后旧 tick 逻辑已被删除，所以实际上空壳会让机器暂时不工作——**因此本 Task 与 Task 4 合并执行**，不能单独提交一个「机器不工作」的中间态）。

⚠️ **修订**：本 Task 不单独提交。改为**一次性**把 tick 骨架 + 所有 tickXxx 实现一起写（Task 3-8 合并为一个连续编辑 + 一次编译 + 一次提交）。下面 Task 4-8 是各 `tickXxx` 的实现细节，按顺序编辑同一文件，全部完成后统一编译 + 提交。

**Files:**
- Modify: `core/src/main/kotlin/ic2_120/content/block/machines/MinerBlockEntity.kt`（`tick()` 536-794）

- [ ] **Step 1: 用新骨架替换整个 `tick()` 方法（536-794 行）**

把原 `fun tick(world: World, pos: BlockPos, state: BlockState) { ... }` 整体替换为：

```kotlin
fun tick(world: World, pos: BlockPos, state: BlockState) {
    if (world.isClient) return
    val serverWorld = world as? ServerWorld ?: return

    // 公共预处理（每 tick 必做，与状态无关）—— spec §4.1
    tickCommonPre(world, pos)

    // 状态分派：转移只通过返回值发生（spec §3 约束 1）
    minerState = when (minerState) {
        MinerState.PIPE_RECOVERING  -> tickPipeRecovering(serverWorld, pos, state)
        MinerState.PIPE_RECYCLING   -> tickPipeRecycling(serverWorld, pos, state)
        MinerState.REDSTONE_WAITING -> tickRedstoneWaiting(world, pos, state)
        MinerState.IDLE             -> tickIdle(world, pos, state, serverWorld)
        MinerState.SCANNING         -> tickScanning(serverWorld, pos, state)
    }

    // 派生 sync.running（spec §3 约束 2）—— 状态机内部不读它做控制
    sync.running = if (minerState == MinerState.SCANNING) 1 else 0
    sync.syncCurrentTickFlow()
}
```

- [ ] **Step 2: 暂不编译**（此时引用的 `tickXxx` 都未定义，必然编译失败）。继续 Task 4-8。

---

## Task 4: 实现 `tickCommonPre`（公共预处理）

**目的**：抽出原 539-563 行的公共预处理。这是最直接的一段，几乎照搬。

**Files:**
- Modify: `core/src/main/kotlin/ic2_120/content/block/machines/MinerBlockEntity.kt`

- [ ] **Step 1: 在 `tick()` 方法下方加 `tickCommonPre`**

```kotlin
/**
 * 公共预处理：每 tick 必做，与状态无关（spec §4.1）。
 * 对应原 tick() 第 539-563 行。
 */
private fun tickCommonPre(world: World, pos: BlockPos) {
    sync.energy = sync.amount.toInt().coerceAtLeast(0)
    sync.pipeCount = getPipeCount()
    OverclockerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
    EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
    TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
    FluidPipeUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES)

    // 流体弹出升级：将储罐中的流体排到相邻方块
    if (fluidPipeProviderEnabled) {
        FluidPipeUpgradeComponent.ejectFluidToNeighbors(
            world, pos, fluidTankInternal, fluidPipeProviderFilter,
            fluidPipeProviderSides, upgradeCount = fluidPipeEjectorCount
        )
    }

    adjacentEnergyTransfer.tick()
    extractFromDischargingSlot()
    chargeScanner()
    sync.fluidBlocked = 0

    // 高级采矿机：缓存弹出
    if (acceptsAdvancedScanner) {
        tryEjectCache(world as? ServerWorld ?: return)
    }

    sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)
}
```

注意：原代码 559-561 的 `if (acceptsAdvancedScanner) tryEjectCache(...)` 在 `serverWorld` null 检查**之前**，但 `tryEjectCache` 需要 ServerWorld。原代码这里其实有隐患（`return` 会跳过后续），新代码保持原行为：`tryEjectCache(world as? ServerWorld ?: return)`——若非 ServerWorld 直接 return 整个 tick。这与原 559 `if (acceptsAdvancedScanner) tryEjectCache(world as? ServerWorld ?: return)` 一致。

- [ ] **Step 2: 暂不编译**（仍缺其他 tickXxx）。继续 Task 5。

---

## Task 5: 实现 `tickPipeRecovering` / `tickPipeRecycling`（管道回收两态）

**目的**：两个回收状态共用 `processPipeRecoveryTick`，但转移目标不同（spec §4.2 / §4.3）。

**Files:**
- Modify: `core/src/main/kotlin/ic2_120/content/block/machines/MinerBlockEntity.kt`（`processPipeRecoveryTick` 约 1053-1089 需改）

- [ ] **Step 1: 改造 `processPipeRecoveryTick` —— 删除其内部的状态字段赋值**

原 `processPipeRecoveryTick`（1053-1089）末尾有一段（1069-1086）直接改 `recoveringPipes`/`recyclingPipes`/`manualStoppedForRecovery`/`redstoneChangeRequired`/游标。这部分要拆出来：`processPipeRecoveryTick` 只负责「处理队列里的一格」，返回 `Boolean`（本 tick 是否处理了一格），**不再改状态字段**。

把原方法替换为：

```kotlin
/**
 * 处理管道回收队列的一格。
 * @return true = 本 tick 回收了一格（队列可能仍非空）；false = 队列已空，回收完毕
 *
 * 注意：本方法只处理「回收一格」的副作用（setBlockState AIR + 入管槽），
 * 不再改 minerState / 旧 boolean。转移由调用方（tickPipeRecovering /
 * tickPipeRecycling）根据返回值决定。spec §4.2/§4.3。
 */
private fun processPipeRecoveryTick(world: ServerWorld): Boolean {
    // 管道槽满时暂停回收，等待槽位空出来后继续。
    if (!canAcceptRecoveredPipe()) return false

    while (pendingPipeRecovery.isNotEmpty()) {
        val pipePos = pendingPipeRecovery.removeFirst()
        val state = world.getBlockState(pipePos)
        if (state.block !is MiningPipeBlock) continue

        world.setBlockState(pipePos, net.minecraft.block.Blocks.AIR.defaultState, Block.NOTIFY_ALL)
        knownPipePositions.remove(pipePos)
        insertRecoveredPipeIntoSlot()
        markDirty()
        return true
    }

    // 队列空：回收完毕。游标/状态重置由调用方做。
    return false
}
```

关键删除：原 1069-1088 的整段（`recoveringPipes = false` 起到 `return false` 前的所有状态字段操作）。游标重置逻辑（原 1072-1078）移到 `tickPipeRecovering` 的「队列空」分支。

- [ ] **Step 2: 加 `resetCursorForRecovery()` 辅助方法**（游标重置逻辑，被多处复用）

```kotlin
/** 重置游标到初始位置（回收完成 / 重启扫描时调用）。 */
private fun resetCursorForRecovery() {
    sync.cursorX = 0
    sync.cursorZ = 0
    sync.cursorY = pos.y - 1
    cursorInitialized = false
    cursorIndex = 0
    pendingBreakEnergy = 0L
    resetPathFailState()
}
```

- [ ] **Step 3: 加 `tickPipeRecovering`**

```kotlin
/**
 * PIPE_RECOVERING 状态：终局回收（普通机到底 / 玩家手动 / 高级机回收）。spec §4.2。
 * @return 下一状态（PIPE_RECOVERING 自环 / IDLE 普通机回收完 / REDSTONE_WAITING 高级机回收完）
 */
private fun tickPipeRecovering(world: ServerWorld, pos: BlockPos, state: BlockState): MinerState {
    val processed = processPipeRecoveryTick(world)
    setActiveState(world, pos, state, processed)
    return if (processed) {
        MinerState.PIPE_RECOVERING  // 队列仍非空，继续
    } else {
        // 队列空：回收完毕，重置游标
        resetCursorForRecovery()
        if (acceptsAdvancedScanner) MinerState.REDSTONE_WAITING else MinerState.IDLE
    }
}
```

- [ ] **Step 4: 加 `tickPipeRecycling`**

```kotlin
/**
 * PIPE_RECYCLING 状态：仅普通机，缺管时回收分支管道。spec §4.3。
 * @return 下一状态（PIPE_RECYCLING 自环 / SCANNING 回收完恢复挖矿）
 */
private fun tickPipeRecycling(world: ServerWorld, pos: BlockPos, state: BlockState): MinerState {
    val processed = processPipeRecoveryTick(world)
    setActiveState(world, pos, state, processed)
    return if (processed) {
        MinerState.PIPE_RECYCLING
    } else {
        // 回收完毕，恢复采矿（原行为：sync.running 仍为 1）
        MinerState.SCANNING
    }
}
```

- [ ] **Step 5: 暂不编译**。继续 Task 6。

---

## Task 6: 实现 `tickRedstoneWaiting` + `tickIdle`

**目的**：两个「不工作」状态。红石门控逻辑（原 574-606）拆分到这两个方法。spec §4.4 / §4.5。

**Files:**
- Modify: `core/src/main/kotlin/ic2_120/content/block/machines/MinerBlockEntity.kt`

- [ ] **Step 1: 加红石判定辅助方法**

```kotlin
/**
 * 高级机红石门控：计算是否应该工作（含红石反转升级）。spec §4.4/§4.5。
 * @return true = 红石条件满足，可工作
 */
private fun isRedstoneActiveForWork(world: World, pos: BlockPos): Boolean {
    if (!acceptsAdvancedScanner) return true  // 普通机无红石门控
    val hasPower = world.isReceivingRedstonePower(pos)
    val hasInverter = SLOT_UPGRADE_INDICES.any { getStack(it).item is RedstoneInverterUpgrade }
    return if (hasInverter) !hasPower else hasPower
}

/** 读取当前红石信号原始值（用于 REDSTONE_WAITING 检测变化）。 */
private fun currentRedstonePower(world: World, pos: BlockPos): Boolean =
    world.isReceivingRedstonePower(pos)
```

- [ ] **Step 2: 加 `tickRedstoneWaiting`**

```kotlin
/**
 * REDSTONE_WAITING 状态：仅高级机，管道回收完成后等红石信号变化才重启。spec §4.4。
 * @return 下一状态（SCANNING 信号变化 / REDSTONE_WAITING 自环）
 */
private fun tickRedstoneWaiting(world: World, pos: BlockPos, state: BlockState): MinerState {
    val hasPower = currentRedstonePower(world, pos)
    if (hasPower != lastRedstoneActive) {
        // 红石信号变化，重启
        lastRedstoneActive = hasPower
        resetCursorForRecovery()
        setActiveState(world, pos, state, false)
        return MinerState.SCANNING
    }
    setActiveState(world, pos, state, false)
    return MinerState.REDSTONE_WAITING
}
```

注意 spec §4.4 行为差异 #3：`lastRedstoneActive` 仅在检测到变化时更新（原代码每 tick 无条件刷新）。这是允许的最小差异。

- [ ] **Step 3: 加 `tryAutoResume` 辅助（替换原 `tryAutoResumeAfterPipeRefill`）**

```kotlin
/**
 * IDLE 态自动恢复判定（替换原 tryAutoResumeAfterPipeRefill）。spec §4.5。
 * @return true = 满足恢复条件，可转 SCANNING
 */
private fun tryAutoResume(): Boolean {
    val minY = if (acceptsAdvancedScanner) ADVANCED_MIN_Y else NORMAL_MIN_Y
    if (sync.cursorY < minY) return false
    if (getScannerType(getStack(SLOT_SCANNER)) == null) return false
    if (getDrillBreakCost() == null) return false
    if (findPipeInInventory() == null) return false
    // 自动恢复按"新一轮"开始，避免沿用缺料前的待挖能量残留
    pendingBreakEnergy = 0L
    return true
}
```

注意：原方法首行 `if (sync.running != 0) return` 和 `if (manualStoppedForRecovery) return` 删除——新状态机里 IDLE 态自然满足「不工作」，且 `manualStoppedForRecovery` 场景对应 REDSTONE_WAITING/PIPE_RECOVERING，不会进 IDLE。

- [ ] **Step 4: 加 `tickIdle`**

```kotlin
/**
 * IDLE 状态：等待自动恢复（普通机）；或红石未激活（高级机）。spec §4.5。
 * @return 下一状态（IDLE 自环 / SCANNING 恢复）
 */
private fun tickIdle(world: World, pos: BlockPos, state: BlockState, serverWorld: ServerWorld): MinerState {
    // 红石门控（仅高级机）：红石未激活则自环
    if (!isRedstoneActiveForWork(world, pos)) {
        lastRedstoneActive = currentRedstonePower(world, pos)
        setActiveState(world, pos, state, false)
        return MinerState.IDLE
    }

    // 自动恢复
    if (tryAutoResume()) {
        setActiveState(world, pos, state, false)
        return MinerState.SCANNING
    }

    setActiveState(world, pos, state, false)
    return MinerState.IDLE
}
```

注意：高级机 `tryAutoResume` 中 `getDrillBreakCost()` 对高级机恒返回非 null（spec §4.5），所以高级机只要红石满足 + 扫描器在位 + 有管就恢复。普通机需要扫描器+钻头+管齐全。

- [ ] **Step 5: 暂不编译**。继续 Task 7。

---

## Task 7: 实现 `tickScanning`（核心，含子阶段）

**目的**：最复杂的状态。原 623-793 拆成有序子方法。spec §4.6。

**Files:**
- Modify: `core/src/main/kotlin/ic2_120/content/block/machines/MinerBlockEntity.kt`

- [ ] **Step 1: 加 `tickScanning` 主入口（子阶段顺序分派）**

```kotlin
/**
 * SCANNING 状态：正常工作。按子阶段顺序判定，首个返回非 null 即转移。spec §4.6。
 * 子方法返回 MinerState?：非 null = 发生转移，null = 继续下一子阶段。
 * 例外：checkScanner 返回 Int?（scanRadius），null = 应回 IDLE。
 */
private fun tickScanning(world: ServerWorld, pos: BlockPos, state: BlockState): MinerState {
    // 4.6.1 红石再检查（仅高级机）—— 非 null 即转移
    checkRedstoneStillActive(world, pos, state)?.let { return it }

    // 4.6.2 扫描器存在性 —— checkScanner 返回 scanRadius（Int），null = 回 IDLE
    val scanRadius = checkScanner(world, pos, state)
    if (scanRadius == null) return MinerState.IDLE

    // 4.6.3 stall 观察（诊断，不转移）
    observeCursorStall(world, "pre_tick")

    // 4.6.4 游标初始化 + 垂直管柱延伸 —— 非 null 即转移
    //      「是否到底」通过侧信道 verticalHitBedrock 传递（见 Step 6）
    ensureCursorAndVerticalPipe(world, pos, state, scanRadius)?.let { return it }

    // 4.6.5 能量预算预检查（仅普通机）
    checkScanEnergyBudget(world, pos, state)?.let { return it }

    // 4.6.6 周期节流
    checkPeriodThrottle(world, pos, state)?.let { return it }

    // 4.6.7 攒能子阶段（需 scanRadius 推进游标）
    tickPendingBreakEnergy(world, pos, state, scanRadius)?.let { return it }

    // 4.6.8 主扫描/挖掘循环
    return runScanMineLoop(world, pos, state, scanRadius, verticalHitBedrock)
}
```

⚠️ 注意：`ensureCursorAndVerticalPipe` 需要同时表达「是否转移」和「是否到底」。采用类级侧信道字段 `verticalHitBedrock`（见 Step 2/6）传递「到底」信号，方法本身返回 `MinerState?`（null = 继续下一子阶段）。`tickScanning` 在调 `runScanMineLoop` 时直接读 `verticalHitBedrock` 作为 `reachedBottomIn`，不再用局部变量。这是本计划的可变状态侧信道之一，加注释说明（另两个见 Task 8 Step 3）。

- [ ] **Step 2: 加侧信道字段**（在 `minerState` 字段附近）

```kotlin
/** tickScanning 内部侧信道：垂直管柱遇到基岩。见 ensureCursorAndVerticalPipe。 */
private var verticalHitBedrock = false
```

- [ ] **Step 3: 加 `cursorVerticalHitrock` 读取器**

```kotlin
private fun cursorVerticalHitBedrock(): Boolean = verticalHitBedrock
```

- [ ] **Step 4: 加 `checkRedstoneStillActive`（4.6.1）**

```kotlin
/** 4.6.1：高级机每 tick 复查红石，断开立即回 IDLE。 */
private fun checkRedstoneStillActive(world: ServerWorld, pos: BlockPos, state: BlockState): MinerState? {
    if (!acceptsAdvancedScanner) return null
    if (isRedstoneActiveForWork(world, pos)) return null
    lastRedstoneActive = currentRedstonePower(world, pos)
    setActiveState(world, pos, state, false)
    return MinerState.IDLE
}
```

- [ ] **Step 5: 加 `checkScanner`（4.6.2）**

```kotlin
/** 4.6.2：扫描器不在位则回 IDLE。返回 scanRadius 或 null（转移）。 */
private fun checkScanner(world: ServerWorld, pos: BlockPos, state: BlockState): Int? {
    val scannerType = getScannerType(getStack(SLOT_SCANNER)) ?: run {
        setActiveState(world, pos, state, false)
        return null  // 调用方据此 return IDLE
    }
    return if (acceptsAdvancedScanner) ADVANCED_MAX_SCAN_RADIUS else scannerType.scanRadius
}
```

注意：`checkScanner` 返回 `Int?`——非 null 是 scanRadius，null 表示扫描器不在位、应回 IDLE。`tickScanning`（Step 1）已用显式 `if (scanRadius == null) return MinerState.IDLE` 处理（不能用 `?.let { return it }`，因为那是 MinerState 模式，这里是 Int）。

- [ ] **Step 6: 加 `ensureCursorAndVerticalPipe`（4.6.4）**

```kotlin
/** 4.6.4：游标初始化 + 垂直管柱延伸。返回 MinerState? = 转移目标（null=继续）。
 *  「到底」通过侧信道 verticalHitBedrock 传递（ensurePipeColumnReaches 内设置，见 Step 7）。 */
private fun ensureCursorAndVerticalPipe(world: ServerWorld, pos: BlockPos, state: BlockState, scanRadius: Int): MinerState? {
    verticalHitBedrock = false
    if (!cursorInitialized) {
        ensureAndGetCursorTarget(scanRadius)
    }
    if (cursorInitialized && sync.cursorY < pos.y) {
        val done = ensurePipeColumnReaches(sync.cursorY)
        if (!done) {
            setActiveState(world, pos, state, false)
            return MinerState.SCANNING  // 自环，下 tick 继续
        }
        // verticalHitBedrock 已由 ensurePipeColumnReaches 在遇基岩时设置（Step 7），
        // 这里不再读 sync.running（新状态机不读它做控制）。
    }
    return null  // 继续
}
```

注意：`ensurePipeColumnReaches` 遇基岩时原代码设 `sync.running = 0`（原 1212 行）。新状态机不读 sync.running 做控制，所以用 `verticalHitBedrock` 侧信道传递「到底」信号。**但 `ensurePipeColumnReaches` 内部仍会设 `sync.running = 0`——需要改**：见 Step 7。

- [ ] **Step 7: 改 `ensurePipeColumnReaches`（原 1193-1221）—— 删除内部 `sync.running = 0`**

原方法第 1212 行 `sync.running = 0` 改为：通过返回值/侧信道表达。但 `ensurePipeColumnReaches` 返回 `Boolean`（是否完成）。**方案**：让它额外设置 `verticalHitBedrock = true` 替代 `sync.running = 0`：

把原 1211-1214 行：
```kotlin
if (!state.isAir && state.block !is MiningPipeBlock && state.getHardness(serverWorld, checkPos) < 0f) {
    sync.running = 0
    return true
}
```
改为：
```kotlin
if (!state.isAir && state.block !is MiningPipeBlock && state.getHardness(serverWorld, checkPos) < 0f) {
    verticalHitBedrock = true
    return true
}
```

- [ ] **Step 8: 加 `checkScanEnergyBudget`（4.6.5）**

```kotlin
/** 4.6.5：普通机扫描能量不足则自环等待。 */
private fun checkScanEnergyBudget(world: ServerWorld, pos: BlockPos, state: BlockState): MinerState? {
    if (acceptsAdvancedScanner) return null
    val scanCost = (MinerSync.SCAN_ENERGY_PER_STEP * energyMultiplier).toLong().coerceAtLeast(1L)
    if (sync.amount < scanCost) {
        setActiveState(world, pos, state, false)
        return MinerState.SCANNING  // 自环等能量
    }
    return null
}
```

- [ ] **Step 9: 加 `checkPeriodThrottle`（4.6.6）**

```kotlin
/** 4.6.6：周期间隔等待态仍亮灯。 */
private fun checkPeriodThrottle(world: ServerWorld, pos: BlockPos, state: BlockState): MinerState? {
    val effectivePeriod = getEffectivePeriodTicks()
    if (((world.time + workOffset) % effectivePeriod) != 0L) {
        setActiveState(world, pos, state, true)  // 等待态视为工作中
        return MinerState.SCANNING
    }
    return null
}
```

- [ ] **Step 10: 加 `tickPendingBreakEnergy`（4.6.7 攒能子阶段）**

对应原 661-706。返回 `MinerState?`：null = 继续主循环，非 null = 转移（都是 SCANNING 自环）。

```kotlin
/** 4.6.7：有待挖掘能量时攒能。返回非 null = 本 tick 结束（自环 SCANNING）。
 *  scanRadius 由 tickScanning 传入（推进游标需要）。 */
private fun tickPendingBreakEnergy(world: ServerWorld, pos: BlockPos, state: BlockState, scanRadius: Int): MinerState? {
    if (pendingBreakEnergy <= 0L) return null  // 无待挖，继续主循环

    val drillBreakCost = getDrillBreakCost()
    if (drillBreakCost == null) {
        pendingBreakEnergy = 0L
        return null  // 继续主循环
    }

    val silkMultiplier = if (sync.silkTouch != 0 && acceptsAdvancedScanner) MinerSync.SILK_TOUCH_MULTIPLIER else 2L
    val breakEnergy = (drillBreakCost * silkMultiplier / 2L * energyMultiplier).toLong().coerceAtLeast(1L)

    val toReserve = minOf(sync.amount, breakEnergy - pendingBreakEnergy)
    if (toReserve > 0L) {
        sync.consumeEnergy(toReserve)
        pendingBreakEnergy += toReserve
    }

    if (pendingBreakEnergy >= breakEnergy) {
        val targetPos = pos.add(sync.cursorX, sync.cursorY - pos.y, sync.cursorZ)
        val blockState = world.getBlockState(targetPos)
        if (!blockState.isAir && shouldMine(world, targetPos, blockState) && canMineWithCurrentDrill(blockState)) {
            ensurePipeReachesPosition(targetPos)
            if (isPipeAdjacentTo(targetPos)) {
                pendingBreakEnergy = 0L
                advanceCursor(scanRadius)
                mineBlock(world, targetPos, blockState)
                sync.energy = sync.amount.toInt().coerceAtLeast(0)
                setActiveState(world, pos, state, true)
                return MinerState.SCANNING
            } else {
                pendingBreakEnergy = 0L
            }
        } else {
            pendingBreakEnergy = 0L
        }
    } else {
        sync.energy = sync.amount.toInt().coerceAtLeast(0)
        setActiveState(world, pos, state, true)
        return MinerState.SCANNING  // 仍在攒能
    }
    sync.energy = sync.amount.toInt().coerceAtLeast(0)
    setActiveState(world, pos, state, false)
    return MinerState.SCANNING
}
```

注意：`tickPendingBreakEnergy` 接收 `scanRadius` 参数（推进游标 `advanceCursor(scanRadius)` 需要）。Step 1 中调用已是 `tickPendingBreakEnergy(world, pos, state, scanRadius)?.let { return it }`，无需 `currentScanRadius()` 辅助方法（已弃用此方案）。

- [ ] **Step 11: 加 `runScanMineLoop`（4.6.8 主循环）**

对应原 709-793。这是最长的子方法。返回 `MinerState`（最终转移目标）。

```kotlin
/** 4.6.8：主扫描/挖掘循环。返回最终转移目标。 */
private fun runScanMineLoop(world: ServerWorld, pos: BlockPos, state: BlockState, scanRadius: Int, reachedBottomIn: Boolean): MinerState {
    var active = false
    var scannedThisCycle = 0
    var reachedBottom = reachedBottomIn

    while (true) {
        val targetPos = ensureAndGetCursorTarget(scanRadius)
        val minY = if (acceptsAdvancedScanner) ADVANCED_MIN_Y else NORMAL_MIN_Y
        if (targetPos.y < world.bottomY || targetPos.y < minY) {
            reachedBottom = true
            break
        }

        val blockState = world.getBlockState(targetPos)

        // 高级采矿机：缓存满则停止
        if (acceptsAdvancedScanner && cacheItemCount >= MAX_CACHE_ITEMS) {
            sync.energy = sync.amount.toInt().coerceAtLeast(0)
            setActiveState(world, pos, state, active || scannedThisCycle > 0)
            return MinerState.IDLE
        }

        // 先检查是否可挖掘
        if (shouldMine(world, targetPos, blockState) && canMineWithCurrentDrill(blockState)) {
            // 确保管道延伸到目标方块
            val pipeReach = ensurePipeReachesPosition(targetPos)
            when (pipeReach) {
                PipeReachResult.REACHED -> {}
                PipeReachResult.RETRY_LATER -> {
                    logDecision(world, "pipe_path_not_ready", targetPos)
                    break  // 下一 tick 继续
                }
                PipeReachResult.UNREACHABLE -> {
                    logDecision(world, "pipe_path_unreachable", targetPos)
                    if (recordPathFailAndShouldSkip(targetPos)) {
                        advanceCursor(scanRadius)
                    }
                    break
                }
            }

            if (!isPipeAdjacentTo(targetPos)) {
                logDecision(world, "pipe_not_adjacent_after_path", targetPos)
                advanceCursor(scanRadius)
                break
            }

            val breakCost = getDrillBreakCost() ?: 0L
            if (breakCost <= 0L) break

            val silkMultiplier = if (sync.silkTouch != 0 && acceptsAdvancedScanner) MinerSync.SILK_TOUCH_MULTIPLIER else 2L
            val breakEnergy = (breakCost * silkMultiplier / 2L * energyMultiplier).toLong().coerceAtLeast(1L)

            if (sync.consumeEnergy(breakEnergy) > 0L) {
                advanceCursor(scanRadius)
                mineBlock(world, targetPos, blockState)
                active = true
            } else {
                // 能量不足，转入待挖掘池，下一 tick 继续攒
                pendingBreakEnergy = sync.amount
                if (pendingBreakEnergy > 0L) sync.consumeEnergy(pendingBreakEnergy)
            }
            break
        }

        // 非矿石方块，消耗扫描能量并前进
        val scanCost = (MinerSync.SCAN_ENERGY_PER_STEP * energyMultiplier).toLong().coerceAtLeast(1L)
        if (consumeScannerEnergy(scanCost) <= 0L) {
            logDecision(world, "insufficient_scan_energy", targetPos)
            break
        }

        advanceCursor(scanRadius)
        scannedThisCycle++
    }

    sync.energy = sync.amount.toInt().coerceAtLeast(0)
    setActiveState(world, pos, state, active || scannedThisCycle > 0)

    // tryPlacePipeAt 链的侧信道（Task 8 Step 3 设置）：检查是否请求回收/停机
    if (pipeRecyclingRequested) return MinerState.PIPE_RECYCLING
    if (haltRequested) return MinerState.IDLE

    // 到底处理：仅普通机自动回收管道（原 789-791 行 `if (reachedBottom && !acceptsAdvancedScanner && !recoveringPipes)`）。
    // 高级机不在此触发终局回收——它靠每层 recoverLayerPipes 循环挖矿，不「到底」。
    if (reachedBottom && !acceptsAdvancedScanner) {
        return startPipeRecoveryAndGetState()
    }

    // 缺料检测：原代码主循环不直接判缺料，依赖下 tick。保持——返回 SCANNING，下 tick 由子阶段判定（缺扫描器回 IDLE）。
    return MinerState.SCANNING
}
```

**关键差异说明**（原代码用 `sync.running == 0` 作为循环退出条件，新代码改为 `while(true) + break`）：
- 原 `while (sync.running != 0)`：循环内多处设 `sync.running = 0` 来退出。新状态机不读 sync.running，所以改成显式 break。
- 原 `advanceCursor` 内部（原 1010-1012）会设 `sync.running = 0`（游标越界）——**需要改**：见 Step 12。
- **重要行为保留**：原代码仅普通机在 `reachedBottom` 时调 `startPipeRecovery`（原 789 行 `!acceptsAdvancedScanner`）。高级机不触发终局回收——这与 spec §2.4 一致（PIPE_RECOVERING 对高级机的入口只有「玩家手动」，`startPipeRecovery` 对高级机本就是玩家按钮触发）。新代码保持此区别。

`startPipeRecoveryAndGetState()` 辅助（Step 13）：因为 `startPipeRecovery` 原 void，需返回新状态。

- [ ] **Step 12: 改 `advanceCursor`（原 1001-1021）—— 删除内部 `sync.running = 0`**

原 1010-1012：
```kotlin
if (sync.cursorY < minY) {
    sync.running = 0
}
```
改为：通过返回值或让调用方检测 `sync.cursorY < minY`。**方案**：`advanceCursor` 改返回 `Boolean`（游标是否仍有效，false = 越界到底）：

```kotlin
/**
 * 推进游标。返回 false = 游标越界（到底），调用方应触发回收。
 * 高级机每层完成后回收水平管道（保留中心柱）。
 */
private fun advanceCursor(range: Int): Boolean {
    resetPathFailState()
    cursorIndex++
    val totalPositions = (2 * range + 1) * (2 * range + 1)
    if (cursorIndex >= totalPositions) {
        val completedY = sync.cursorY
        sync.cursorY -= 1
        cursorIndex = 0
        val minY = if (acceptsAdvancedScanner) ADVANCED_MIN_Y else NORMAL_MIN_Y
        if (sync.cursorY < minY) {
            return false  // 到底，不再设 sync.running
        }
        if (acceptsAdvancedScanner) {
            recoverLayerPipes(completedY, range)
        }
    }
    val (x, z) = spiralXY(cursorIndex, range)
    sync.cursorX = x
    sync.cursorZ = z
    return true
}
```

**重要**：`advanceCursor` 原返回 Unit，现在返回 Boolean。所有调用点都要处理返回值。调用点：
- `tickPendingBreakEnergy` Step 10：`advanceCursor(scanRadius)` → 改为 `advanceCursor(scanRadius)`（忽略返回值，因为攒能态挖完即 return，越界由下 tick 主循环的 minY 判定处理）
- `runScanMineLoop` Step 11：两处 `advanceCursor(scanRadius)` → 同样可忽略返回值（循环内的 advance 是路径失败/不可达时跳过，越界检测由循环顶部的 `targetPos.y < minY` 处理）

所以调用点**可以忽略返回值**（Kotlin 允许不接收返回值）。但为了语义清晰，加注释。实际上为减少改动面，`advanceCursor` 可以**仍返回 Unit**，把越界检测完全交给调用方的 `targetPos.y < minY` 判定（原主循环 713 行已有此判定）。**采用此方案**：`advanceCursor` 保持返回 Unit，仅删除内部 `sync.running = 0`：

```kotlin
private fun advanceCursor(range: Int) {
    resetPathFailState()
    cursorIndex++
    val totalPositions = (2 * range + 1) * (2 * range + 1)
    if (cursorIndex >= totalPositions) {
        val completedY = sync.cursorY
        sync.cursorY -= 1
        cursorIndex = 0
        // 越界（到底）由调用方通过 cursorY < minY 检测，不再设 sync.running
        if (acceptsAdvancedScanner) {
            recoverLayerPipes(completedY, range)
        }
    }
    val (x, z) = spiralXY(cursorIndex, range)
    sync.cursorX = x
    sync.cursorZ = z
}
```

注意删除了原 `if (sync.cursorY < minY) { sync.running = 0 }` 判断块（含 minY 计算）。越界后 `recoverLayerPipes` 仍会执行（高级机），这与原行为一致（原代码也是先 recoverLayerPipes 再判断 running）。

- [ ] **Step 13: 加 `startPipeRecoveryAndGetState` 辅助**

因为 `runScanMineLoop` 需要在到底时触发回收并返回新状态，而 `startPipeRecovery` 是 void 公开方法（玩家也调用）。加一个返回状态的内部包装：

```kotlin
/** 触发终局回收并返回新状态（供 runScanMineLoop 到底时调用）。 */
private fun startPipeRecoveryAndGetState(): MinerState {
    startPipeRecovery()
    // startPipeRecovery 内部已设 minerState（Task 8），但本方法在 tickScanning 的
    // when 分支返回值上下文中，需要返回目标状态。startPipeRecovery 不返回状态，
    // 所以这里根据其副作用推断：
    return if (pendingPipeRecovery.isNotEmpty()) MinerState.PIPE_RECOVERING
           else MinerState.IDLE
}
```

⚠️ **设计反思**：`startPipeRecovery` 既被玩家调用（void）又被内部循环调用（需状态返回），导致这个包装很丑。**更好方案**：Task 8 改造 `startPipeRecovery` 时让它返回 `MinerState`（玩家调用处忽略返回值）。本 Step 改为：

```kotlin
private fun startPipeRecoveryAndGetState(): MinerState = startPipeRecovery()
```

（依赖 Task 8 把 `startPipeRecovery` 返回类型改为 `MinerState`。）

- [ ] **Step 14: 暂不编译**。继续 Task 8。

---

## Task 8: 改造外部入口方法（`restartScan` / `startPipeRecovery` / `triggerPipeRecycling`）

**目的**：让外部入口操作 `minerState` 而非旧 boolean。spec §5。

**Files:**
- Modify: `core/src/main/kotlin/ic2_120/content/block/machines/MinerBlockEntity.kt`

- [ ] **Step 1: 改 `restartScan`（原 806-822）返回新签名**

```kotlin
fun restartScan() {
    minerState = MinerState.SCANNING
    lastRecycledCursorY = Int.MAX_VALUE
    pendingPipeRecovery.clear()
    sync.cursorX = 0
    sync.cursorZ = 0
    sync.cursorY = pos.y - 1
    cursorInitialized = false
    cursorIndex = 0
    pendingBreakEnergy = 0L
    resetPathFailState()
    markDirty()
}
```

删除对 `recoveringPipes`/`recyclingPipes`/`manualStoppedForRecovery`/`redstoneChangeRequired` 的赋值（字段待删）。`lastRecycledCursorY` 重置保留。

- [ ] **Step 2: 改 `startPipeRecovery`（原 824-852）返回 `MinerState`**

```kotlin
/**
 * 触发终局管道回收。返回回收后的目标状态。
 * 普通机：到底或玩家手动；高级机：玩家手动。
 */
fun startPipeRecovery(): MinerState {
    val serverWorld = world as? ServerWorld ?: return minerState
    pendingBreakEnergy = 0L
    knownPipePositions.clear()
    pipeCacheDirty = true
    resetPathFailState()
    buildPipeRecoveryQueue(serverWorld)

    val newState = if (pendingPipeRecovery.isNotEmpty()) {
        MinerState.PIPE_RECOVERING
    } else {
        // 队列空：无管可回收。普通机回 IDLE；高级机也回 IDLE（交红石门控），避免锁死。
        MinerState.IDLE
    }
    minerState = newState
    markDirty()
    return newState
}
```

删除原方法内对 `sync.running`/`manualStoppedForRecovery`/`recyclingPipes`/`redstoneChangeRequired`/游标的赋值（游标重置移到 `resetCursorForRecovery`，由 `tickPipeRecovering` 在回收完成时调用，**不在触发时**——spec §4.2：游标重置在「队列空时执行」）。

注意原方法末尾（841-850）的高级机特殊分支（无管时撤销停机态）被简化为统一回 IDLE——spec §5.2 已说明这与原行为等价。

- [ ] **Step 3: 改 `triggerPipeRecycling`（原 859-868）返回 `Boolean`**

原方法是 `private fun` void，内部直接设 `recyclingPipes`/`sync.running`。改为返回是否触发了回收，**不改 minerState**（遵守 spec §5.3 约束：由调用层翻译）。

```kotlin
/**
 * 普通机缺管时尝试触发分支管道回收。
 * @return true = 已触发回收（调用方应转 PIPE_RECYCLING）；false = 未触发
 */
private fun triggerPipeRecycling(): Boolean {
    if (acceptsAdvancedScanner) return false  // 仅普通机
    // 防死循环：游标未前进过则不再回收
    if (sync.cursorY >= lastRecycledCursorY) return false
    lastRecycledCursorY = sync.cursorY
    val serverWorld = world as? ServerWorld ?: return false
    buildPipeRecyclingQueue(serverWorld)
    return pendingPipeRecovery.isNotEmpty()
}
```

注意：原方法在缺管时设 `sync.running = 0` 停机。新方案：若 `triggerPipeRecycling` 返回 false（无可回收管），调用方（`tryPlacePipeAt` 链）需要表达「停机」。**但 `tryPlacePipeAt` 链目前是 Boolean 返回**——需要侧信道。

**关键问题**：`triggerPipeRecycling` 被 `tryPlacePipeAt`（原 1178）调用，`tryPlacePipeAt` 又被 `ensurePipeReachesPosition`/`ensurePipeColumnReaches` 调用，最终在 `runScanMineLoop` 里。要让「触发回收」或「停机」信号冒泡到 `runScanMineLoop` 的返回值。

**方案**：加两个类级侧信道字段（与 `verticalHitBedrock` 同模式）：

```kotlin
/** tickScanning 侧信道：tryPlacePipeAt 链请求触发分支管道回收。 */
private var pipeRecyclingRequested = false
/** tickScanning 侧信道：tryPlacePipeAt 链请求停机（无管可铺）。 */
private var haltRequested = false
```

在 `tickScanning`（Task 7 Step 1）方法体第一行（`checkRedstoneStillActive` 调用之前）加重置：
```kotlin
// 侧信道重置（每 tick 进入 SCANNING 时清零，防止跨 tick 残留）
pipeRecyclingRequested = false
haltRequested = false
verticalHitBedrock = false
```
注意：`verticalHitBedrock` 在 `ensureCursorAndVerticalPipe`（Step 6）内也会重置，这里再重置一次是双保险（确保 `runScanMineLoop` 读到的 `verticalHitBedrock` 反映本 tick 的垂直管柱结果，而非上一 tick 残留）。

改 `tryPlacePipeAt` 中对 `triggerPipeRecycling` 的调用（原 1177-1180）：
```kotlin
// 原：
findPipeInInventory() ?: run {
    triggerPipeRecycling()
    return false
}
// 改为：
findPipeInInventory() ?: run {
    if (triggerPipeRecycling()) {
        pipeRecyclingRequested = true
    } else {
        haltRequested = true  // 无管可铺也无可回收，停机
    }
    return false
}
```

然后 `runScanMineLoop` 末尾（return 前）检查侧信道：
```kotlin
// 主循环结束后检查侧信道
if (pipeRecyclingRequested) return MinerState.PIPE_RECYCLING
if (haltRequested) return MinerState.IDLE
```

这两行加在 `runScanMineLoop` 的 `return MinerState.SCANNING`（最后那个）之前。

- [ ] **Step 4: 暂不编译**。继续 Task 9 删除旧字段。

---

## Task 9: 删除旧 boolean 字段 + 旧方法 + 编译修错

**目的**：清理所有被替换的旧字段和方法，修编译错误。

**Files:**
- Modify: `core/src/main/kotlin/ic2_120/content/block/machines/MinerBlockEntity.kt`

- [ ] **Step 1: 删除字段声明**

删除以下字段声明（约 237-249 行区域）：
- `private var recoveringPipes: Boolean = false`
- `private var manualStoppedForRecovery: Boolean = false`
- `private var recyclingPipes: Boolean = false`
- `private var redstoneChangeRequired: Boolean = false`

保留 `lastRedstoneActive`。

- [ ] **Step 2: 删除旧方法 `tryAutoResumeAfterPipeRefill`（原 888-900）**

已被 `tryAutoResume`（Task 6 Step 3）替代。整段删除。

- [ ] **Step 3: grep 确认无残留引用**

Run:
```bash
grep -n "recoveringPipes\|recyclingPipes\|manualStoppedForRecovery\|redstoneChangeRequired\|tryAutoResumeAfterPipeRefill" core/src/main/kotlin/ic2_120/content/block/machines/MinerBlockEntity.kt
```
Expected: 无输出（所有引用已在前序 Task 替换）。若有残留，逐个替换：
- `recoveringPipes` → `minerState == MinerState.PIPE_RECOVERING`
- `recyclingPipes` → `minerState == MinerState.PIPE_RECYCLING`
- `manualStoppedForRecovery` → 看上下文（多数可删，因状态机已表达）
- `redstoneChangeRequired` → `minerState == MinerState.REDSTONE_WAITING`

- [ ] **Step 4: 编译（预期会有错误，逐个修）**

Run: `./gradlew :core:compileKotlin`

预期错误类型：
1. **未解析引用**：旧字段被删后，遗漏的引用。按 Step 3 方案修。
2. **`tryPlacePipeAt` 内 `triggerPipeRecycling()` 调用**：确认 Task 8 Step 3 已改。
3. **`startPipeRecovery` 返回类型变化**：若有其他调用点（grep `startPipeRecovery(` 确认只有玩家 UI 和内部），玩家 UI 调用处忽略返回值即可。

逐个修复直到 BUILD SUCCESSFUL。

- [ ] **Step 5: Commit（Task 3-9 合并提交）**

```bash
git add core/src/main/kotlin/ic2_120/content/block/machines/MinerBlockEntity.kt
git commit -m "refactor(miner): tick() 重写为 MinerState 状态机分派

- 新增 MinerState 枚举（IDLE/REDSTONE_WAITING/PIPE_RECOVERING/PIPE_RECYCLING/SCANNING）
- tick() 重写为 when(minerState) 分派，转移只通过返回值发生
- sync.running 降为末尾派生量
- 删除 recoveringPipes/recyclingPipes/manualStoppedForRecovery/redstoneChangeRequired
- NBT 兼容旧存档 + 新增 NBT_MINER_STATE 持久化
- 行为差异见 spec §7，无玩家可观察变化"
```

---

## Task 10: 全量构建（服务端 + 客户端双检）

**目的**：AGENTS.md §2 强制要求双检。

**Files:** 无（仅验证）

- [ ] **Step 1: 全量构建**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL。

若失败，常见原因：
- 客户端 source set 引用了被删字段（grep 客户端代码）
- SyncedData 序列化问题（`sync.running` 仍是 SyncedData 字段，未删，应无问题）

- [ ] **Step 2: 若有 Gradle lock 错误**

按 AGENTS.md §7：删除 `~/.gradle/wrapper/dists/gradle-*.zip.lck` 后重试。

---

## Task 11: 最小回归 smoke test（mcdebug，可选但推荐）

**目的**：防「重构后完全不工作」回归。只测最简单的路径：普通机通电挖一格矿石。

**Files:**
- Create: `core/test/mcdebug/miner.test.ts`
- Read: `core/test/mcdebug/compressor.test.ts`（参考结构）、`core/test/mcdebug/helpers.ts`

- [ ] **Step 1: 读参考测试结构**

读 `core/test/mcdebug/compressor.test.ts` 和 `helpers.ts`，了解 `TestContext`、`place`、`setBlocks`、`setBeField`、`setupAdjacentBatbox` 用法。

- [ ] **Step 2: 写 smoke test**

```typescript
// 采矿机最小回归测试：验证普通机通电后能进入工作状态（SCANNING）。
// 不覆盖全部状态转移（mcdebug 15s 硬约束），只防「重构后完全不工作」。
import { setupAdjacentBatbox, type TestContext } from "./helpers";
import { setBeField, sleep, getBeField } from "@yu1745/mcdebug";

export default async function (ctx: TestContext) {
  // 在 origin 放置普通采矿机，东侧 BatBox 供电
  await setupAdjacentBatbox(ctx, "ic2_120:miner");

  // 装入 OD 扫描器 + 铁钻头 + 采矿管
  // 槽位见 MinerBlockEntity companion: SLOT_SCANNER=0, SLOT_DRILL=1, SLOT_PIPE=...
  // 注：具体 setBeField 写槽位的 API 以 helpers/参考测试为准，这里用占位字段名
  await setBeField(ctx, ctx.origin, "ScannerSlot", /* od_scanner item */ );
  // 若 mcdebug 无直接设物品槽 API，跳过物品装入，仅验证机器放置不崩 + tick 不抛异常

  // 等 2 秒让机器 tick 若干次
  await sleep(ctx, 2000);

  // 验证机器仍存在且无异常（最弱断言，但能抓 NPE/状态机崩溃）
  const running = await getBeField(ctx, ctx.origin, "Running");
  // 不强断言 running==1（缺物品可能 IDLE），只验证字段可读 = BE 未崩溃
  if (running === undefined) throw new Error("Miner BE crashed during tick");
}
```

⚠️ **实现注意**：mcdebug 设物品槽的 API 需要确认（`setBeField` 是否支持 ItemStack，或需要专门 API）。读 `compressor.test.ts` 看它怎么装入物品。若 mcdebug 不支持直接设物品槽，本测试降级为「放置 + 通电 + tick 不崩溃」的最弱断言。**实现时根据实际 API 调整，不要照抄上面的占位代码**。

- [ ] **Step 3: 运行 mcdebug 测试**

Run: `./gradlew :core:mcdebugTest`
Expected: 测试通过（miner.test.ts 不抛异常）。

若 mcdebug 环境搭建复杂或超时，**可跳过本 Task**（标记为 optional），依赖 Task 10 编译 + 后续手测。在 commit message 注明。

- [ ] **Step 4: Commit（若执行了）**

```bash
git add core/test/mcdebug/miner.test.ts
git commit -m "test(miner): 最小回归 smoke test（放置+tick 不崩溃）"
```

---

## Task 12: 手测矩阵 + 合并

**目的**：人工验证各状态转移正确。这是行为等价性的最终保证。

**Files:** 无

- [ ] **Step 1: 启动客户端**

Run: `./gradlew :core:runClient`（或用 `printRunClientCommand` 获取实际命令）

- [ ] **Step 2: 手测矩阵**

按 spec §9 验证矩阵逐项测：

| 场景 | 机型 | 预期 |
|---|---|---|
| 正常挖矿 | 普通 | 扫描→铺管→挖矿→掉落入物品槽 |
| 到底回收 | 普通 | 挖到底→管道自动回收→停机 |
| 缺管回收 | 普通 | 挖矿中抽空管槽→分支回收→补管→恢复 |
| 缺料停机 | 普通 | 抽走扫描器→停机→放回→自动恢复 |
| 红石激活挖矿 | 高级 | 给红石→工作；断红石→停机 |
| 红石等待 | 高级 | 到底/手动回收完→等红石变化→拨红石→恢复 |
| 缓存满 | 高级 | 挖满 64 物品→停机→清空邻接箱子→恢复 |

每项观察：灯态（active blockstate）、声音、物品/管道数量。

- [ ] **Step 3: 旧存档兼容测试（若有旧存档）**

加载一个重构前保存的、采矿机正在工作的存档，确认机器状态正确恢复（不卡死、不丢失进度）。

- [ ] **Step 4: 合并到 main**

```bash
git checkout main
git merge --no-ff refactor/miner-state-machine
```

按 AGENTS.md §9.1：若本分支是功能 commit，合并后登记 `docs/branch-sync-status.md`（🔒 封存期标记）+ 单独提交同步状态。

- [ ] **Step 5: 同步状态登记**

在 `docs/branch-sync-status.md` 末尾追加条目（🔒 标记），单独 commit。

---

## 行为差异对照（实现时逐项确认）

实现完成后，逐项核对 spec §7 的 5 处差异，确认每处都与 spec 描述一致：

1. [ ] `sync.running` 仅末尾派生（grep `sync.running =` 应只有 tick 末尾一处）
2. [ ] 高级机回收完进 REDSTONE_WAITING（无双 boolean）
3. [ ] `lastRedstoneActive` 仅变化时刷新
4. [ ] `REDSTONE_WAITING` 靠 `NBT_MINER_STATE` 持久化
5. [ ] `triggerPipeRecycling` 返回 Boolean，状态转移在调用层

---

## 风险与回退

- **风险 1**：`tryPlacePipeAt` 链的侧信道（`pipeRecyclingRequested`/`haltRequested`）若遗漏重置，会导致状态错乱。**缓解**：`tickScanning` 开头集中重置，grep 确认无其他写入点。
- **风险 2**：`advanceCursor` 删除 `sync.running = 0` 后，越界检测依赖调用方的 `targetPos.y < minY`。若遗漏某调用路径，游标可能越界。**缓解**：`ensureAndGetCursorTarget` 内部已有 minY clamp（原 934-935 行），双重保险。
- **回退**：所有改动在 `refactor/miner-state-machine` 分支，未合并前可整体放弃（`git branch -D`）。合并后若有问题，`git revert <merge-commit>`。
