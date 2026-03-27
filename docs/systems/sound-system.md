# 声音系统实现说明

## 概述

当前机器声音系统采用“服务端状态驱动 + 客户端循环音控制”的混合方案：

1. 服务端负责机器 `ACTIVE` 状态切换与必要的一次性事件音（如 `start/stop`）。
2. 客户端根据方块状态创建/维护循环 `SoundInstance`，实现可控停止与淡出。
3. 循环音不再依赖服务端反复 `world.playSound(...)`，避免“停机后声音仍播完一段”的问题。

---

## 事件 ID 规则

`SoundEvent.of(Identifier(namespace, id))` 中的 `id` 必须是 `sounds.json` 的**事件 ID**，不是音频文件路径。

示例：

- 正确：`machine.furnace.electric.start`
- 错误：`machines/furnace/electric/start`

资源路径由 `sounds.json` 内部映射，例如：

- 事件：`machine.furnace.electric.start`
- 资源：`ic2:machines/furnace/electric/start`

---

## 服务端职责

核心文件：
- `src/main/kotlin/ic2_120/content/block/machines/MachineBlockEntity.kt`

行为：

1. `setActiveState(world, pos, state, active)` 统一更新 blockstate。
2. 仅服务端触发一次性事件：
- `START_STOP` 机型：`start` 与 `stop`。
 - 传送机阶段事件：`machine.teleporter.charge`（充能阶段）与 `machine.teleporter.use`（传送成功）。
3. `LOOP/OPERATE` 类型不在服务端重复触发，由客户端循环控制器接管。

---

## 客户端循环音职责

核心文件：
- `src/client/kotlin/ic2_120/client/MachineLoopSoundController.kt`
- `src/client/kotlin/ic2_120/Ic2_120Client.kt`

行为：

1. 在客户端 Tick 扫描玩家附近机器（按 `ACTIVE=true` 判断运行态）。
2. 为每个运行中机器维护一个 `MovingSoundInstance(repeat=true)`。
3. 机器停机或超出跟踪集合时执行 fade-out 并 `setDone()` 停止。
4. 避免重复创建同一位置同一事件的实例。

当前覆盖机器（循环音）：
- `generator.generator.loop`
- `generator.geothermal.loop`
- `generator.water.loop`
- `generator.wind.loop`
- `generator.nuclear.loop`
- `machine.furnace.iron.operate`
- `machine.furnace.electric.loop`
- `machine.furnace.induction.loop`
- `machine.macerator.operate`
- `machine.compressor.operate`
- `machine.extractor.operate`
- `machine.canner.operate`
- `machine.recycler.operate`
- `machine.pump.operate`
- `machine.miner.operate`

说明：`Canner` 当前循环音固定为 `machine.canner.operate`，`machine.canner.reverse` 尚未接入独立循环分支。

---

## 物品交互音职责

核心文件（服务端触发成功动作音）：
- `src/main/kotlin/ic2_120/content/RubberTreetapHandler.kt`
- `src/main/kotlin/ic2_120/content/WrenchHandler.kt`
- `src/main/kotlin/ic2_120/content/screen/ScannerScreenHandler.kt`
- `src/main/kotlin/ic2_120/content/item/MiningLaserItem.kt`

当前已覆盖事件：
- `item.treetap.use`
- `item.treetap.electric.use`
- `item.wrench.use`
- `item.scanner.use`
- `item.laser.shoot`

---

## Access Widener

为支持在控制器中直接停止声音实例，已在以下文件放开可见性：

- `src/main/resources/ic2_120.accesswidener`

新增条目：

```text
accessible method net/minecraft/client/sound/MovingSoundInstance setDone ()V
```

该条目允许在客户端控制器内直接调用 `setDone()` 立即结束循环实例。

---

## 关键类

- 服务端配置模型：`src/main/kotlin/ic2_120/content/sound/MachineSoundConfig.kt`
- 服务端状态入口：`src/main/kotlin/ic2_120/content/block/machines/MachineBlockEntity.kt`
- 客户端循环控制：`src/client/kotlin/ic2_120/client/MachineLoopSoundController.kt`
- 客户端初始化注册：`src/client/kotlin/ic2_120/Ic2_120Client.kt`

---

## 验证建议

1. 编译检查：

```bash
./gradlew compileKotlin compileClientKotlin
```

2. 指令验证事件是否可播：

```mcfunction
/playsound ic2:machine.furnace.electric.start master @p
/playsound ic2:machine.furnace.electric.loop master @p
```

3. 游戏内行为验证：
- 开机：应听到启动音（有 start 的机器）。
- 运行：应持续循环。
- 停机：应短暂淡出并停止，不应“无限挂声”。
