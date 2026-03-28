# 导线与电网系统（与当前实现一致）

本文档对应以下实现：
- `src/main/kotlin/ic2_120/content/block/energy/EnergyNetworkManager.kt`
- `src/main/kotlin/ic2_120/content/block/energy/EnergyNetwork.kt`

目标：说明当前代码的真实行为，而不是理想设计。

## 1. 数据模型

`EnergyNetwork` 表示一个连通导线集合共享的能量池：
- `cables: MutableSet<Long>`：成员导线坐标（`BlockPos.asLong`）。
- `energy: Long`：共享池能量。
- `capacity: Long`：所有成员导线 `transferRate` 之和。
- `outputLevel: Int`：电网输出等级，初始为 `1`，由拓扑扫描时”可向电网该面输出能量（`supportsExtraction()`）”的邻接机器在该方向的电压等级最大值决定。对于支持分面电压等级的设备（如变压器），使用该方向的有效电压等级（[ITieredMachine.effectiveVoltageTierForSide](../../src/main/kotlin/ic2_120/content/block/ITieredMachine.kt)）；否则使用整体电压等级（[ITieredMachine.tier](../../src/main/kotlin/ic2_120/content/block/ITieredMachine.kt)）。
- `cableLossMilliEu`：每根导线的路径损耗权重（milliEU）。

`EnergyNetworkManager` 维护按维度分组的索引：
- `RegistryKey<World> -> (posLong -> EnergyNetwork)`。

## 2. 生命周期

### 2.1 惰性构建

`getOrCreateNetwork(world, pos)`：
1. 若 `pos` 已在索引中，直接返回已有网络。
2. 否则从 `startPos` 做 BFS，仅沿导线连接属性为 `true` 的方向扩展。
3. 对每根访问到的导线：
   - `addCable()` 累加容量并记录损耗。
   - 将该位置映射到新网络。
   - 把 `CableBlockEntity.localEnergy` 并入网络并清零。
   - 把 `be.network` 指向新网络。
4. 若 BFS 过程中碰到旧网络成员，会把旧网络能量吸收到新网络。
5. 新网络 `damageTickOffset` 取 `world.random.nextBetween(0, 199)`。
6. 构建结束后执行 `energy = min(energy, capacity)`。

### 2.2 失效/拆分

`invalidateAt(world, pos)`：
1. 找到 `pos` 所在网络。
2. 将网络总能量按成员 BE 数量均分回 `localEnergy`（余数从前几个 BE +1）。
3. 清理该网络所有成员位置索引。
4. 清空成员 `be.network`。

之后剩余导线会在下一次 `getOrCreateNetwork` 时惰性重建。

### 2.3 连接缓存失效

`invalidateConnectionCachesAt(world, pos)` 只做：
- `network.invalidateConnectionCaches()`，即清空拓扑/路径缓存。
- 不会拆网，不会修改 `energy`。

### 2.4 世界卸载

`onWorldUnload(world)` 仅移除该维度缓存，不影响其他维度。

## 3. 每 tick 执行流程

`tickIfNeeded(world)` 保证同一网络每个游戏 tick 只执行一次：
1. `pushToConsumers(world)`：执行能量传输。
2. `tryUninsulatedCableDamage(world)`：执行漏电伤害判定。

## 4. 传输规则（pushToConsumers）

### 4.1 边界端点识别

基于拓扑缓存的 `boundaries`，查找边界相邻方块的 `EnergyStorage.SIDED`：
- `supportsInsertion()` => 记为消费者。
- `supportsExtraction()` => 记为供电者。
- 同一端点可能同时是消费者和供电者。

### 4.2 每 tick 临时线缆容量

`remainingCableCapacity[cable] = transferRate`。后续每次成功传输都会扣减，避免同 tick 叠加超载。

### 4.3 执行顺序

1. 先让消费者从网络缓冲池 `energy` 取电（`pullFromBufferedEnergyByPath`）。
2. 再让消费者从供电者取电（`pullFromProvidersByPath`）。

### 4.4 路径与损耗

对单个消费者，以其 `entryCables` 作为 Dijkstra 多源起点：
- 邻接图来自当前拓扑缓存。
- 边权使用“到达下一根导线的损耗” `cableLossMilliEu[next]`。
- 起点初始距离为该起点导线自身损耗。

候选路径按 `pathLossMilliEu` 升序尝试。

### 4.5 单路径可传输上限

对一条候选路径：
- `pathCapacity = min(路径上各导线 remainingCableCapacity)`
- `pathLossEu = (pathLossMilliEu + 999) / 1000`（向上取整，确保"可以多扣不能少扣"）
- `maxDeliverable = max(pathCapacity - pathLossEu, 0)`

实际成功量还受消费者插入量、供电者提取量、网络池余量限制。

### 4.6 扣减规则

一次成功传输后，对路径上每根导线统一扣减 `moved`（包含损耗）：
- `remainingCableCapacity[cable] -= moved`

缓冲池路径会额外：
- `energy -= moved`

供电者路径不改 `energy`，能量从 provider 到 consumer 直接完成事务。

## 5. 拓扑缓存与路径缓存

`EnergyNetwork` 有三类缓存：
1. `topologyCache`：
   - `cableRates`
   - `neighbors`
   - `boundaries`
   - `cablesThatLeak`（绝缘等级 &lt; outputLevel，用于漏电伤害）
   - `cablesThatCanBurn`（导线电压等级 &lt; outputLevel，用于低耐压烧毁）
2. `dijkstraCacheByEntries`：按消费者入口集合缓存最短路结果。
3. `bufferedCandidatesCacheByEntries`：按消费者入口集合缓存“从缓冲池出发可用的导线路径候选”。

失效时机：
- `addCable()` -> 清空所有缓存。
- `invalidateConnectionCaches()` -> 清空所有缓存。

缓存上限保护：
- 任一路径缓存 map 条目数 `> 512` 时整表清空。

## 6. 漏电伤害

触发条件（全部满足才执行）：
- 服务端世界。
- `(world.time + damageTickOffset) % 200 == 0`。
- `outputLevel >= 1`。
- 存在漏电导线：`block.insulationLevel < outputLevel`。

伤害规则：
- 目标：漏电导线附近 `Chebyshev` 距离 `<= outputLevel` 的 `LivingEntity`（排除 spectator/dead）。
- 伤害源：`ic2_120:cable_shock`，若不存在回退 `minecraft:lightning`。
- 伤害值：`outputLevel * 2f`（即 `outputLevel` 颗心）。

## 7. 低耐压导线烧毁

当电网中存在**输出等级高于部分导线电压等级**的情况（例如锡线接入含 MFSU 的玻璃纤维电网）时，低耐压导线会随机烧毁，而非一次性全部消失。

触发条件（全部满足才执行）：
- 服务端世界。
- 与漏电伤害同一周期：`(world.time + damageTickOffset) % 200 == 0`。
- `outputLevel >= 1`。
- 存在“可烧毁”导线：导线实现 [ITiered](../../src/main/kotlin/ic2_120/content/item/energy/ITiered.kt)，且 `block.tier < outputLevel`（**只烧电压等级低于电网输出等级的导线**，同等级或更高不烧，例如玻璃纤维不烧）。

烧毁规则：
- **损坏比例**：常量 `underTierCableBurnRatio`（默认 1，即 100%）。每次检查时，在所有”可烧毁”导线中**随机**选出该比例的导线进行烧毁。当前设置意味着所有可烧毁的导线都会被烧毁。
- **示例**：10 根锡线接入一个全为玻璃纤维且存在 MFSU（4 级输出）的电网，电网 `outputLevel` 为 4，锡线 tier=1，属于可烧毁；每次检查随机烧毁约 10% 的锡线，玻璃纤维（tier=5）不会被烧。
- **效果**：被选中的导线方块被破坏（`breakBlock(pos, false)`，不掉落），并在该位置生成烟雾与火焰粒子（`LARGE_SMOKE`、`FLAME`）表示烧毁。
- 烧毁后电网会因导线消失而触发 `invalidateAt`，拓扑在下次 tick 重建。

## 8. 机器耐压检测与过压爆炸

与电网相连的机器若**在连接方向的有效耐压等级**低于电网 `outputLevel`，会在同一检查周期内**直接爆炸**（不按比例随机，电压等级不对就炸）。

**有效耐压等级**：
- 基础为机器的 [ITieredMachine.tier](../../src/main/kotlin/ic2_120/content/block/ITieredMachine.kt)。
- 对于支持分面电压等级的设备（如变压器），使用该方向的电压等级（[ITieredMachine.getVoltageTierForSide](../../src/main/kotlin/ic2_120/content/block/ITieredMachine.kt)）。
- 若机器实现了 [ITransformerUpgradeSupport](../../src/main/kotlin/ic2_120/content/upgrade/ITransformerUpgradeSupport.kt)（高压升级），则有效耐压 = 基础等级 + `voltageTierBonus`（每插入一个高压升级 +1）。
- 由 [ITieredMachine.effectiveVoltageTierForSide](../../src/main/kotlin/ic2_120/content/block/ITieredMachine.kt) 统一计算，它会：
  1. 优先使用分面电压等级（如果机器实现了 [getVoltageTierForSide](../../src/main/kotlin/ic2_120/content/block/ITieredMachine.kt)）
  2. 加上高压升级带来的等级加成（如果有）
  3. 如果没有分面电压等级，则使用整体等级

**分面电压等级示例（变压器）**：
- 变压器的正面（输出面）为低级电压（tier），其他面为高级电压（tier+1）。
- 当电网通过变压器的正面连接时，电网看到的是低级电压等级。
- 当电网通过变压器的其他面连接时，电网看到的是高级电压等级。
- 这样可以正确识别变压器的输入输出电压，避免误判过压。

触发条件（全部满足才执行）：
- 服务端世界。
- 与漏电/导线烧毁同一周期：`(world.time + damageTickOffset) % 200 == 0`。
- `outputLevel >= 1`。
- 边界上存在 [ITieredMachine](../../src/main/kotlin/ic2_120/content/block/ITieredMachine.kt)，且 `outputLevel > effectiveVoltageTierForSide(边界方向)`。

爆炸规则：
- 实现 [IGenerator](../../src/main/kotlin/ic2_120/content/block/IGenerator.kt) 的发电机不参与过压检测，**不会爆炸**。
- 对每个与电网相邻的非发电机机器，若 `outputLevel > effectiveVoltageTierForSide(边界方向)`，先清空该方块的物品栏（不生成掉落物），再移除方块（不掉落机器本身），最后在该位置创建爆炸，不生成火焰。
- **不掉落任何物品**：机器本身和槽位内所有物品均不掉落，全部消失。
- **爆炸伤害**按电网电压等级：等级 4 对应 10 颗心伤害（爆炸威力 2），电压每提高 1 级伤害翻倍（威力 = 2 × 2^(outputLevel-4)）。

## 9. 与旧文档不一致处（已修正）

- 删除了重复章节，避免同一内容出现两版表述。
- 明确 `damageTickOffset` 实际范围是 `0..199`，不是 `0..200`。
- 明确 `outputLevel` 在代码里没有硬性上限截断，来源是”该面可提取（`supportsExtraction()`）”的边界机器的**该方向有效耐压等级**（[ITieredMachine.effectiveVoltageTierForSide](../../src/main/kotlin/ic2_120/content/block/ITieredMachine.kt)）最大值（初始 1）。
  - 支持分面电压等级：对于变压器等设备，不同方向可能有不同的电压等级。
  - 电网会查询机器在该方向的有效电压等级，而不是使用整体等级。
  - 这样可以正确识别变压器的输入输出电压，避免误判过压或错误计算电网输出等级。
- 明确”连接缓存失效”只清缓存，不拆网。
- 明确 Dijkstra 起点距离包含起点导线自身损耗。
- 明确缓存清理阈值条件是 `> 512`。



