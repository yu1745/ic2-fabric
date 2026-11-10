# 弹出 / 抽入升级重新设计：从轮询改为变更驱动

日期：2026-11-10　分支：`1.21.1`

> 静态审查结论，**未做 profiling**。下面"为什么现有缓存没用"是按代码路径推出来的，
> 建议实施前用 spark 采一次样对照。

涉及文件：`content/upgrade/EjectorUpgradeComponent.kt`、`PullingUpgradeComponent.kt`、
`IValidInputItem.kt`、`content/RecipeInputLookupCache.kt`、`content/storage/RoutedItemStorage.kt`、
`content/block/machines/MachineBlockEntity.kt`，以及 28 个调用 `ejectIfUpgraded`、23 个调用 `pullIfUpgraded` 的 BE。

## 一、现有缓存为什么没用

现有三层优化（`BlockApiCache`、`RecipeInputLookupCache`、P5 退避）各自省掉了一小块，但都没碰到主要开销。

### 1. 退避在有负载时自动失效

规则是"干活 → 间隔 −2，空手 → 间隔 +1"。平衡点在"每次成功对应 2 次空手"，
即间隔 ≈ 加工周期 / 3，上限 20。

- 基础打粉机（130 tick 一次）：停在 20 tick 一轮，每次加工约 6 次轮询。
- 超频后（10 tick 一次）：约 3 tick 一轮。
- 输入槽在补货（速率 1 个/tick 填 64 个）：连续 64 tick 每 tick 成功，间隔钉在 1，**每 tick 全量扫描**。

所以退避只对闲置机器有效，而真正吃性能的是满负荷工厂。

### 2. 被缓存的不是贵的部分

每次触发仍然要付的固定成本：

| 开销 | 位置 |
|---|---|
| 每个升级读两次 NBT，每次 `copyTag()` 深拷贝整个 CompoundTag，再 `ResourceLocation.tryParse` + 注册表查找 | `readFilter` / `readDirections`，`EjectorUpgradeComponent.kt:228,247` |
| NBT 解析排在"输出槽全空 / 输入槽全满"检查**之前**，没活干也照付 | `doEject` `:142-161`、`doPull` `:103-120` |
| 抽入对 6 个方向的源容器**逐槽全量枚举**，输入槽已经装满后剩下的候选仍然继续枚举 | `doPull` `for (view in source)` |
| `isValidInputItem` 为每个候选物品 `new ItemStack(item, 1)` 只为判断是不是电池（7 个 BE 都这么写） | `MaceratorBlockEntity.kt:369` 等 |
| `canPlaceItem(slot, variant.toStack(1))` 每个候选 × 每个输入槽再分配一次 | `doPull` |
| 抽入每次成功抽取各开一个外层事务 | `doPull` `Transaction.openOuter().use` |
| 源是本模组机器时，`BlockApiCache.find` 仍调 provider，provider 走 `KProperty1.get` 反射 | `ClassScanner.kt:942` |

### 3. 没装升级的机器也在付费

- 每台机器每 tick 两次 `WeakHashMap.getOrPut(machine)`（弹出 + 抽入各一张表，每次还要清理过期弱引用）。
- `OverclockerUpgradeComponent.apply`、`EnergyStorageUpgradeComponent.apply`、`TransformerUpgradeComponent.apply` 每 tick 重扫升级槽并重算倍率（`repeat(count)` 连乘）。

### 根因

轮询模型的成本是 `机器数 × tick 数 × 邻居数 × 邻居槽位数`，和"实际发生了多少次物品移动"无关。
退避只是拿延迟换成本，它不知道什么时候有活。只要还是"到点就扫一遍看看"，加什么缓存都只能改常数。

## 二、新设计：变更戳 + 状态机

核心想法：**一次传输尝试的结果只取决于两端容器的内容。两端都没变，结果一定和上次一样，不用再试。**

### 2.1 变更戳（change stamp）

给每个 BlockEntity 一个单调递增的 `int` 计数器，内容变化时 +1。

- **本模组机器**：在 `MachineBlockEntity.setChanged()` 的**限流判断之前** `changeStamp++`
  （现在的 20 tick 限流会提前 return，戳必须在它前面）。
- **原版和其他模组容器**：加一个 mixin，在 `BlockEntity.setChanged()` 头部给 duck 接口字段 +1。
  - 原版容器、漏斗、玩家点槽位都会调 `setChanged()`。
  - Fabric Transfer API 的 `InventoryStorageImpl` 提交时调 `inventory.markDirty()`，大箱子两半都会调。
  - 成本是全局每次 `setChanged` 多一次整数自增，可忽略。

```java
@Mixin(BlockEntity.class)
abstract class BlockEntityChangeStampMixin implements ChangeStamped {
    @Unique private int ic2_120$stamp;
    @Inject(method = "setChanged()V", at = @At("HEAD"))
    private void ic2_120$bump(CallbackInfo ci) { ic2_120$stamp++; }
    @Override public int ic2_120$changeStamp() { return ic2_120$stamp; }
}
```

**戳不可信的情况**（内容会变但不调 `setChanged`，或者根本没有 BE）：

- 堆肥桶等无 BE 的方块存储；
- 虚拟库存（AE2 接口、末影类网络存储）；
- 大箱子：邻居位置只拿到一半的 BE，另一半被玩家或漏斗改动时本半的戳不变。

对这些目标标记为"无戳"，退回定时重试（见 2.4）。**最坏情况等于现在的行为，常见情况接近零成本。**

### 2.2 每台机器一个 `ItemIoController`

作为 BE 的普通字段（不再用 `WeakHashMap`），持有：

- **解析后的升级状态**：超频倍率、储能加成、电压加成、弹出配置列表、抽入配置列表（过滤物品、方向位掩码、速率）。
  只在自身变更戳变化时重新解析。这一条顺带消掉 Overclocker / EnergyStorage / Transformer 三个 `apply` 的每 tick 扫描和全部 NBT 拷贝。
- **6 个方向的 `Link`**：`BlockApiCache`、上次看到的对端戳、对端是否"无戳"。
- **两个状态机**（弹出、抽入）各自的 `nextWakeTick`。

每 tick 的快速路径只有一次比较：

```kotlin
fun tick(now: Long) {
    if (now < nextWake && machine.changeStamp == seenOwnStamp) return
    ...
}
```

### 2.3 弹出状态机

| 状态 | 条件 | 成本 | 唤醒条件 |
|---|---|---|---|
| 空闲 | 输出槽全空 | 0 | 自身戳变化（产出了东西） |
| 传输 | 有输出且上次成功 | 按配额传输 | 配额用尽则下一 tick 继续 |
| 阻塞 | 有输出但所有目标都拒收 | 0 | 自身戳变化，或任一目标戳变化；无戳目标按退避定时重试 |

效果：正常运转的机器每产出一次只尝试一次弹出，成本从 `O(tick 数)` 变成 `O(产出次数)`。

### 2.4 抽入状态机

| 状态 | 条件 | 成本 | 唤醒条件 |
|---|---|---|---|
| 满足 | 输入槽全满 | 0 | 自身戳变化（消耗了原料） |
| 补货 | 输入有空位且源里有货 | 按批次抽取 | 见下方"批量" |
| 饥饿 | 输入有空位但所有源都没有可用物品 | 0 | 任一源的戳变化；无戳源按退避定时重试（上限 20~40 tick） |

三处配套改动：

1. **按需求直接抽，不枚举源**。输入槽已有物品 X 且有空位时，直接 `source.extract(X的variant, n, tx)`，
   不遍历源的所有槽位。只有输入槽为空、不知道该要什么时才枚举，而且有了戳之后每次源变化只枚举一次。
2. **批量由冷热间隔自然形成**（见第五节）。最小间隔是 5 tick，配额仍按"速率 × 流逝 tick"补偿，
   所以热态下每 5 tick 抽一批、吞吐量不变，不需要单独的批量参数。
3. **一次触发一个事务**，和弹出侧已有的 P4 做法对齐。

### 2.5 顺手清掉的分配

- `IValidInputItem.isValidInputItem(item)` 里的 `ItemStack(item, 1)` 改成 `item is IBatteryItem`。
- `canPlaceItem(slot, variant.toStack(1))` 只在快速否定通过后、真正要抽之前调一次，不放在槽位循环里。
- `readFilter` / `readDirections` 用 `CustomData.contains` / `getUnsafe()` 只读访问，不 `copyTag()`
  （有了 2.2 之后它们只在升级槽变化时才跑，这条影响已经很小）。

## 三、行为差异和风险

- **速率语义不变**：每候选每 tick `4^(n-1)` 个、起始方向轮转、过滤和方向配置都保留。
- **批量抽入会改变节奏**：同样的总量，从"每 tick 1 个"变成"每 16 tick 16 个"。
  多台机器共享一个源箱子时分配会更"成块"。如果要严格保持原节奏，可以把批量设为 1，只保留戳跳过。
- **原地修改不调 `setChanged` 的代码会让戳漏更新**。本模组 BE 里有直接 `stack.shrink()` / `grow()` 的写法
  （如 `MaceratorBlockEntity.kt:266-268`，后面跟了 `markDirtyThrottled()`，是安全的），需要逐个 BE 核对。
  兜底：每 100 tick 无条件重新校验一次。
- **大箱子**：按"无戳"处理，或者在 `Link` 里解析出另一半的 BE 一起看戳。先做前者。
- **Sinytra Connector / Forge**：`BlockEntity.setChanged()` 两边都有，现有 mixin 已有 Fabric/Forge 双份的先例。
- **GUI 里改升级配置**（写过滤、写方向）是原地改 `CUSTOM_DATA`，需确认对应的 C2S 处理里调了 `setChanged()`。

## 四、冷热变速：照搬 AE2 数值

数值来自 AE2 `main` 分支（1.21.x）源码，`forge/1.20.1` 对照一致。关键常量已抽查核对。

### 4.1 AE2 的规则

来源：`me/service/TickManagerService.java`、`me/service/helpers/TickTracker.java`、
`api/networking/ticking/TickingRequest.java`、`core/settings/TickRates.java`、`parts/automation/IOBusPart.java`。

| 项 | AE2 的值 |
|---|---|
| 干了活（FASTER） | 间隔 −2 |
| 能干但没活（SLOWER） | 间隔 +1 |
| 无法工作（IDLE，如目标区块未加载） | 间隔直接设为最大 |
| 紧急（URGENT） | 间隔直接设为最小 |
| 被唤醒（alert） | 间隔设为 1，下一 tick 执行，不受最小值限制 |
| 结果钳制 | 夹到 [最小, 最大] |
| 初始间隔 | (最小 + 最大) / 2 |
| 输入总线 ImportBus | 最小 5，最大 40（初始 22） |
| 输出总线 ExportBus | 最小 5，最大 60（初始 32） |
| 接口 Interface | 最小 5，最大 120；干完活 URGENT，有计划没干成 SLOWER，无计划睡眠 |
| 存储总线 StorageBus | 最小 5，最大 60 |

### 4.2 和现有 P5 退避的差别

现有实现的 −2 / +1 其实已经是 AE2 的调节量，差别在上下限：

| | 现有 P5 | AE2 |
|---|---|---|
| 最小间隔 | 1 | 5 |
| 最大间隔 | 20 | 抽入 40，弹出 60 |
| 初始间隔 | 1（首 tick 必触发） | (最小 + 最大) / 2 |

最小间隔 1 正是现在满负荷时每 tick 全量扫描的来源。换成 5 之后，热态成本直接降到 1/5。

### 4.3 照搬到本模组的映射

- **抽入升级** = ImportBus：最小 5，最大 40，初始 22。
- **弹出升级** = ExportBus：最小 5，最大 60，初始 32。
- **调节**：本次触发移动了物品 → −2；能工作但没移动 → +1；没有任何可用邻居容器 → 设为最大。
- **唤醒**：自身或对端的变更戳变化（第二节）时，间隔设为 1，下一 tick 执行。
  这对应 AE2 的 alert，也是冷态下保证响应速度的手段——冷态 40/60 tick 的间隔只是兜底，正常靠戳唤醒。

### 4.4 一处必须偏离 AE2 的地方：吞吐补偿

AE2 的总线**不按间隔补偿吞吐**：`ticksSinceLastCall` 参数传进去了但没用，单次传输量只由加速卡决定
（0/1/2/3/4 张 → 1/8/32/64/96 次操作）。它的速率语义是"每次操作"，间隔变长吞吐就真的下降。

本模组的速率语义是对齐原版 IC2 的"每 tick `4^(n-1)` 个"，所以必须保留现有的"配额 = 速率 × 流逝 tick"。
否则最小间隔从 1 改成 5 会让吞吐掉到 1/5。这一点已有实现，保持不变即可。

## 五、相位分散：非每 tick 负载不允许挤在一起

AE2 自己**没有做错峰**：新设备首次触发在 `当前 tick + 初始间隔`，同一 tick 到期的设备按上次触发时间排序，没有随机打散。
同时加载的一批设备会一直同步。这部分需要自己设计。

### 5.1 按机器个体，不按区块

- 负载集中在少数区块（200 台机器的工厂只占 3~5 个区块），区块共享偏移只有 3~5 个相位，在最需要分散的地方失效。
- 机器仍然各自被 BE ticker 逐个调用，相位相同并不能减少每台每 tick 的那次比较，没有可摊销的公共工作。
- 个体方案的成本只是每台机器一个整数字段。

### 5.2 相位来源

对 `BlockPos.asLong()` 做一次混合哈希（如 fastutil `HashCommon.mix`）后取模 20。

- 重载后相位不变，不用存盘，问题可复现。
- 必须用带混合的哈希，直接取模会让成排摆放的机器落在规律的相位上。

### 5.3 冷态对齐到相位槽

只给初始偏移不够：一批机器被同一个事件唤醒（区块加载、恢复供电、共用的箱子被补货）后，
会沿完全相同的升温降温轨迹走，在同一个 tick 进入冷态并一直保持同步。

规则：

- **间隔 < 20（热态和过渡态）**：`下次触发 = 现在 + 间隔`，不对齐。热态本来就接近连续触发，不需要分散。
- **间隔 ≥ 20（冷态）**：下次触发取"不早于 `现在 + 间隔` 且 `tick % 20 == 相位`"的那一 tick。
  最多多等 19 tick，冷态机器无论之前经历过什么都均匀分布在 20 个槽里。
- **首次触发**：`加载 tick + 初始间隔` 之后同样对齐到相位槽，解决区块加载时的同步。

### 5.4 共用相位字段

相位放在 `MachineBlockEntity` 上，所有周期性逻辑共用：

- 弹出、抽入的冷态对齐；
- 标脏限流（现有的 `initialDirtyDelayTicks` 并进来）；
- 第二节里无戳目标的定时重试、100 tick 兜底校验；
- 其他"每 N tick 做一次"的逻辑（需要排查各 BE 里的 `world.time % N == 0` 写法，这种写法全服同相位）。

### 5.5 现有取模写法排查结果

全库按 tick 取模共 30 处。需要处理的是 9 处无偏移的服务端周期逻辑，另有 2 处全局处理器值得评估。

**需要改：无偏移，全服同相位（9 处）**

| 位置 | 周期 | 说明 |
|---|---|---|
| `CropBlock.kt:297` | 256 tick | 每个作物 BE 的主逻辑，所有作物同一 tick 一起跑，最重的一处 |
| `CropBlock.kt:316` | 1024 tick | 同上，嵌套在 297 里 |
| `CropStickBlock.kt:319` | 256 tick | 每个作物架 BE 的杂交逻辑 |
| `UvLampBlockEntity.kt:165` | 256 tick | 跟随作物周期 |
| `AnimalSlaughtererBlockEntity.kt:188` | 20 tick | 实体扫描。同类的 Animalmatron、Cropmatron、CropHarvester 都有 `workOffset`，只有它漏了 |
| `CropBlock.kt:1084` | 20 tick | 毒莲的实体范围查询 |
| `NuclearReactorBlockEntity.kt:1011` | 100 tick | 堆叠异常扫描。同文件主逻辑已有 `tickOffset`，这里没用上 |
| `RedstoneEngineBlockEntity.kt:136`（buildcraft 附属） | 16 tick | |
| `ModFluids.kt:596` | 20 tick 中的前 5 tick | 蒸汽伤害的实体查询，所有蒸汽方块同相位 |

作物三处（`CropBlock`、`CropStickBlock`、`UvLamp`）周期长但完全同步，大农场每 256 tick 出现一次集中尖峰。
它们之间可能有先后依赖（紫外灯跟作物周期对齐），改相位时按作物个体哈希，并确认灯的逻辑不依赖"同一 tick"。
作物和作物架不是 `MachineBlockEntity`，相位计算要做成不依赖基类的工具函数。

**值得评估：全局处理器（2 处）**

- `RadiationHandler.kt:27`、`UUFluidEffectHandler.kt:47`：每 20 tick 一次性遍历所有玩家或实体。
- 单个处理器无法靠机器相位分散；玩家多时可按实体 ID 取模分摊到 20 tick。

**已有偏移，但实现各不相同（5 套）**

| 位置 | 做法 |
|---|---|
| `NuclearReactorBlockEntity.kt:1050,1264` | 随机 0~19，存 NBT |
| `WaterGeneratorBlockEntity.kt:388` | 随机 0~19，存 NBT |
| Animalmatron / Cropmatron / CropHarvester 的 `workOffset` | 随机，存 NBT |
| `WindKineticGenerator` / `WaterKineticGenerator` 的 `scanOffset` | 坐标直接取模，不存盘 |
| `EnergyNetwork.kt:277,357,444` 的 `damageTickOffset` | 按电网分配 |

功能上没问题。统一成公共相位后可以删掉 4 个 NBT 键和重复代码（旧存档里的键直接忽略即可）。
两个动能发电机的 `scanOffset` 没有混合哈希，成排摆放时相位有规律，统一时顺带修掉。

**不用动**

- 一天内时间换算：`dayTime % 24000`，4 处太阳能相关。
- 客户端渲染和界面：2 个渲染器、`MachineLoopSoundController`、`ChunkLoaderScreen`。
- 按实体或物品自身计数：`BuildingBlocks.kt:142`、`Tools.kt:1333`、`LaserProjectileEntity.kt:159`、`TeleporterBlockEntity.kt:466`。
- `NuclearReactorBlockEntity.kt:1117`：每秒一条 debug 日志。
- `BandwidthStatsService.kt:36`：每秒一次的统计汇总。

自增计数器式的周期（`xxxTicks++`）只有 `LeashKineticGeneratorBlockEntity.kt:191` 的 `syncCounter` 一处，
从各自放置时开始计数，不存在同相位问题。

## 六、实施顺序

每一步都可以单独合入、单独验证。

1. **只动两个组件文件，不碰 BE**（半天量级，风险最低）
   - 输出空 / 输入满的检查提到 NBT 解析之前；
   - NBT 只读访问；
   - 去掉 `isValidInputItem` 和 `canPlaceItem` 的 `ItemStack` 分配；
   - 抽入改成单事务，输入装满后立即跳出剩余候选；
   - 输入槽已有物品时走直接 `extract`，不枚举。
2. **变更戳**：mixin + `MachineBlockEntity.changeStamp`，先只用于"两端戳都没变就跳过"，仍保留现有退避作为无戳兜底。
3. **`ItemIoController` 进 BE**：替换两张 `WeakHashMap`，升级状态按戳缓存，三个 `apply` 一并收进来。需要改 28 + 23 个调用点。
4. **冷热变速 + 相位分散**：P5 退避的上下限换成 AE2 数值（第四节），加入相位字段和冷态对齐（第五节），戳变化接入唤醒。
   其中"上下限换成 5/40、5/60"和"首次触发加相位"只改两个组件文件，可以提前到第 1 步一起做。

第 1 步做完就应该能在 spark 里看到明显下降；第 2 步是把成本从"按 tick"变成"按变更"的关键。
