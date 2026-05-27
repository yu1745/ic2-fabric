# 声音系统实现说明

## 概述

当前机器声音系统采用“服务端状态驱动 + 客户端循环音控制”的混合方案：

1. 服务端负责机器 `ACTIVE` 状态切换与必要的一次性事件音（如 `start/stop`）。
2. 客户端根据方块状态创建/维护循环 `SoundInstance`，实现可控停止与淡出。
3. 循环音不再依赖服务端反复 `world.playSound(...)`，避免“停机后声音仍播完一段”的问题。

当前覆盖结论：

- 框架已可用：机器 start/stop、客户端循环音、物品交互音、喷气背包循环音都有明确入口。
- 覆盖面仍偏低：现有 `@ModBlockEntity` 机器约 58 个，显式配置 `soundConfig` 的文件约 14 个，其中 `CannerBlockEntity` 当前是 `MachineSoundConfig.none()`；实际机器声音覆盖主要集中在早期核心机器。
- 资源多于接入：`assets/ic2/sounds.json` 已注册大量上游声音事件，但不少事件尚未被代码触发。

---

## 文档位置

声音系统主文档只维护在本文件：

- `docs/systems/sound-system.md`

资源目录内另有一个上游资源命名说明：

- `core/src/main/resources/assets/ic2/sounds/README.txt`

该文件只描述 `assets/ic2` 声音资源命名规则，不作为系统实现文档维护。

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
- `core/src/main/kotlin/ic2_120/content/block/machines/MachineBlockEntity.kt`

行为：

1. `setActiveState(world, pos, state, active)` 统一更新 blockstate。
2. 仅服务端触发一次性事件：
- `START_STOP` 机型：`start` 与 `stop`。
- 传送机阶段事件：`machine.teleporter.charge`（充能阶段）与 `machine.teleporter.use`（传送成功）。
3. `LOOP/OPERATE` 类型不在服务端重复触发，由客户端循环控制器接管。

---

## 客户端循环音职责

核心文件：
- `core/src/client/kotlin/ic2_120/client/MachineLoopSoundController.kt`
- `core/src/client/kotlin/ic2_120/Ic2_120Client.kt`

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

## 喷气背包循环音

核心文件：

- `core/src/client/kotlin/ic2_120/client/JetpackSoundController.kt`
- `core/src/main/resources/assets/ic2_120/sounds.json`
- `core/src/main/resources/assets/ic2_120/sounds/jetpack/jetpack_loop.ogg`

当前覆盖事件：

- `item.jetpack.loop`

客户端根据玩家飞行状态创建一个跟随玩家位置的 `MovingSoundInstance(repeat=true)`，停止飞行后淡出并结束。

---

## 物品交互音职责

核心文件（服务端触发成功动作音）：
- `core/src/main/kotlin/ic2_120/content/RubberTreetapHandler.kt`
- `core/src/main/kotlin/ic2_120/content/WrenchHandler.kt`
- `core/src/main/kotlin/ic2_120/content/screen/ScannerScreenHandler.kt`

当前已覆盖事件：
- `item.treetap.use`
- `item.treetap.electric.use`
- `item.wrench.use`
- `item.scanner.use`

当前已发现问题：

- `item.laser.shoot` 已在 `assets/ic2/sounds.json` 注册，但 `MiningLaserItem` 当前未播放该事件。
- `LaserProjectileEntity` 播放了 `item.laser.hit`，但 `assets/ic2/sounds.json` 当前没有注册该事件。

---

## 已知覆盖缺口

机器运行音覆盖仍不完整。以下类型已实现机器逻辑，但当前没有稳定接入声音系统，后续补声音时应优先按机器活跃状态和原 IC2 资源匹配：

- 加工/生产机器：金属成型机、洗矿机、流体装罐机、固体装罐机、离心机、方块切割机、复制机、UU 扫描机、物质生成机等。
- 热能/流体链路：固体加热机、流体加热机、电力加热机、斯特林发电机、蒸汽发生器、流体热交换机、冷凝机、发酵机等。
- 动能链路：动能发电机、手动动能发电机、水力动能发电机、风力动能发电机、蒸汽动能发电机、拴绳动能发电机等。
- 农业/特殊机器：作物监管机、作物收割机、动物监管机、屠宰机、磁化机、特斯拉线圈等。
- 资源已注册但未接入或未完整接入：`machine.canner.reverse`、`machine.fabricator.loop`、`machine.fabricator.scrap`、`machine.electrolyzer.loop`、`machine.o_mat.operate`、`machine.terraformer.loop`、`generator.nuclear.power.low/medium/high`。

架构注意事项：

- `MachineSoundConfig` 在服务端 BlockEntity 中配置，`MachineLoopSoundController` 在客户端硬编码 block 类型与事件 ID；这两处目前是两套来源，后续扩展时需要同步维护。
- 新增循环音时必须确认对应方块有稳定 `ACTIVE` 属性，并且运行状态能同步到客户端。
- 新增事件时必须同时检查 `sounds.json` 事件 ID 与代码里的 `Identifier(namespace, id)` 完全一致。

---

## Access Widener

为支持在控制器中直接停止声音实例，已在以下文件放开可见性：

- `core/src/main/resources/ic2_120.accesswidener`

新增条目：

```text
accessible method net/minecraft/client/sound/MovingSoundInstance setDone ()V
```

该条目允许在客户端控制器内直接调用 `setDone()` 立即结束循环实例。

---

## 关键类

- 服务端配置模型：`core/src/main/kotlin/ic2_120/content/sound/MachineSoundConfig.kt`
- 服务端状态入口：`core/src/main/kotlin/ic2_120/content/block/machines/MachineBlockEntity.kt`
- 客户端循环控制：`core/src/client/kotlin/ic2_120/client/MachineLoopSoundController.kt`
- 喷气背包循环控制：`core/src/client/kotlin/ic2_120/client/JetpackSoundController.kt`
- 客户端初始化注册：`core/src/client/kotlin/ic2_120/Ic2_120Client.kt`

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
