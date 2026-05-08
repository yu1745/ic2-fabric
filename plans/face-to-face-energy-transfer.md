# 贴脸能量传输支持方案

> 场景：拥有能量能力的方块相邻放置（无导线）时，IC2 内部机器按 IC2 规则传输并保留过压保护；非 IC2 Energy API 方块作为兼容端点按实际 sided storage 能力交互。

## 设计结论

贴脸传输不并入导线电网。

- `EnergyNetwork` 继续只负责导线网络、线损、线缆容量、漏电、导线烧毁与导线边界传输。
- 贴脸传输由机器能力组件处理，不参与导线 BFS、Dijkstra、线损、线缆容量与导线负载统计。
- 首批代表机器移除旧 `pullEnergyFromNeighbors()` 调用；全局删除旧函数待其余机器完成迁移后执行。
- 新增统一组件，例如 `AdjacentEnergyTransferComponent`，由需要贴脸能量交互的机器组合复用。

## 规则边界

- 贴脸传输只使用具体 side lookup，不使用 `side == null` fallback。
- IC2 ↔ IC2：双方方块实体都是 `ITieredMachine`，采用 consumer 拉 provider，并执行 IC2 过压检测。
- IC2 ↔ 外部 Energy API：邻居不是 `ITieredMachine` 但暴露 `EnergyStorage.SIDED` 时，只按双方 `EnergyStorage` 实际能力 pull/push，不做 IC2 过压。
- IC2 机器的每个具体 side 必须输入/输出互斥；外部兼容层以 IC2 side 能力决定方向。
- 外部方块同一 side 即使同时支持输入和输出，也不额外做双向尝试。
- 传输上限不手算固定 tier 速率，优先由 `EnergyStorage.insert/extract` 根据具体面、tick budget、容量、升级和机器模式裁剪。

## 新组件

建议新增组件：

```kotlin
class AdjacentEnergyTransferComponent(
    private val owner: BlockEntity,
    private val energy: TickLimitedSidedEnergyContainer
) {
    fun tick(): Long {
        // 遍历 6 个具体 side
        // 获取本机 side storage
        // 查找相邻方块 opposite side storage
        // 根据 neighbor 是否 ITieredMachine 分派到 IC2 内部规则或外部兼容规则
    }
}
```

组件职责：

- 遍历本机 6 个具体 side。
- 获取本机该面的 `EnergyStorage`。
- 查找相邻方块对应面的 `EnergyStorage.SIDED`。
- 判断邻居是否 `ITieredMachine`。
- IC2 内部只执行 consumer 拉 provider。
- 外部兼容按 IC2 side 能力选择 pull 或 push，单个 side 单 tick 最多执行一个方向。
- 所有传输都先模拟目标可接收量，再执行事务，避免吞能。

## IC2 内部

IC2 ↔ IC2 贴脸传输采用拉模型：

```kotlin
if (selfBe is ITieredMachine && neighborBe is ITieredMachine) {
    if (selfStorage.supportsInsertion() && neighborStorage.supportsExtraction()) {
        if (wouldOvervoltage(neighborBe, neighborSide, selfBe, selfSide)) {
            explodeConsumer(selfPos)
            return
        }
        pullProviderIntoConsumer(neighborStorage, selfStorage)
    }
}
```

过压按实际供电方向判断：

- provider 面支持 `supportsExtraction()`。
- consumer 面支持 `supportsInsertion()`。
- `providerVoltage = provider.effectiveVoltageTierForSide(providerSide)`。
- `consumerVoltage = consumer.effectiveVoltageTierForSide(consumerSide)`。
- 若 `providerVoltage > consumerVoltage`，consumer 爆炸，并跳过本次传输。

变压器仍按当前规则排除在“被炸目标”之外；但它作为 provider 时，其输出面的分面电压仍参与 provider 电压计算。

## 外部兼容

邻居不是 `ITieredMachine` 但对应面暴露 `EnergyStorage.SIDED` 时，不做 IC2 过压。

外部兼容层以 IC2 侧能力决定方向。因为 IC2 机器保证具体 side 输入/输出互斥，外部 side 即使双向也不会导致同 tick 来回搬运：

```kotlin
if (selfStorage.supportsInsertion() && neighborStorage.supportsExtraction()) {
    pullProviderIntoConsumer(neighborStorage, selfStorage)
} else if (selfStorage.supportsExtraction() && neighborStorage.supportsInsertion()) {
    pushProviderIntoConsumer(selfStorage, neighborStorage)
}
```

`pullProviderIntoConsumer()` / `pushProviderIntoConsumer()` 必须先模拟目标可接收量，再按可接收量从 provider 提取并插入。提取量和插入量必须一致，否则会吞能量。

## 接入方式

- 在需要贴脸取电或对外供电兼容的机器中组合 `AdjacentEnergyTransferComponent`。
- 在机器 tick 中调用组件 `tick()`。
- 首批四类代表机器不再调用 `EnergyUtils.pullEnergyFromNeighbors()`，不保留旧路径和新组件并行运行。
- 旧函数暂时保留给未迁移机器；后续普通机器与核反应堆迁移完成后再全局删除。

首批改造只覆盖四类代表机器：

| 机器 | 代表类型 | 接入目标 |
|------|----------|----------|
| 储电箱（BatBox/CESU/MFE/MFSU 及 chargepad 变体） | 特殊分面储能方块 | 正面输出、其余面输入；可从 IC2/外部 provider 拉电，也可向外部 consumer 推电 |
| 变压器 | 特殊分面转换方块 | 按当前模式和朝向决定输入/输出面；IC2 内部参与过压判断；可向外部 consumer 推电 |
| 打粉机（Macerator） | 大多数纯耗电机器代表 | 只作为 consumer，从 IC2 provider 或外部 provider 拉电 |
| 火力发电机（Generator） | 大多数纯发电机器代表 | 只作为 provider，向外部 consumer 推电；IC2 内部由 consumer 拉取 |

这四类机器改造完成后，视为验证了本 mod 的主要能量机器形态：

- 特殊分面输入/输出：储电箱、变压器。
- 纯耗电机器：打粉机。
- 纯发电机器：火力发电机。

多方块核反应堆作为第二批改造：中心反应堆和反应仓都接入 `AdjacentEnergyTransferComponent`。反应仓不持有独立能量，而是代理中心反应堆的 `NuclearReactorSync`；IC2 consumer 仍由 consumer 拉取，反应堆/反应仓只负责对外部 Energy API consumer 的兼容推送。

本轮必须移除上述机器及其代表路径中的分散 `pullEnergyFromNeighbors()` 调用；这些代表机器不保留旧路径和新组件并行运行。

## 不采用的方案

| 方案 | 原因 |
|------|------|
| 扩展导线 `EnergyNetwork` BFS 到机器 | 会把导线路径语义、机器贴脸语义、全局电压语义混在一起 |
| 新建 `MachineNetwork` 图网络 | 贴脸传输本质是相邻两方块 transfer，不需要图网络 |
| 新增推/拉接口 | 已有 `EnergyStorage.SIDED` 足够表达具体面能力 |
| 合并到 `ITieredMachine` | `ITieredMachine` 是等级/电压语义，不应混入传输行为 |
| IC2 内部同时推和拉 | IC2 内部统一采用拉模型，避免重复调度 |
| 每台机器各自手写传输 | 行为分散，难以统一过压、事务和兼容策略 |

## 改动量估算

| 文件 | 改动 | 行数 |
|------|------|------|
| 新组件文件 | `AdjacentEnergyTransferComponent`：IC2 内部拉模型、外部兼容 pull/push、过压、事务 | ~120 |
| `EnergyUtils.kt` | 本轮保留旧函数供未迁移机器使用；后续全量迁移后删除 | 0 |
| 储电箱/变压器/打粉机/火力发电机 BE | 组合组件并替换旧 `pullEnergyFromNeighbors()` 调用；覆盖特殊分面、纯耗电、纯发电三类代表 | ~80 |
| **总计** | | **~220** |

## 验证方式

1. `./gradlew build` 编译通过
2. Generator + 耗电机器相邻无导线 → 能量传输正常
3. Generator + BatBox 相邻无导线 → BatBox 充电
4. HV 源 + 低压机器相邻 → 低压机器过压爆炸
5. 变压器不同模式/朝向下，只有当前输入面拉电、当前输出面供电
6. 外部 Energy API provider + 打粉机相邻 → 打粉机按自身输入面实际可接收量拉电，不爆炸
7. 火力发电机或储电箱输出面 + 外部 Energy API consumer 相邻 → IC2 provider 按双方实际能力推电，不爆炸
8. 纯导线电网回归测试：功能不变
9. 贴脸传输不使用 `side == null` fallback
10. 核反应堆中心方块 + 反应仓相邻外部 Energy API consumer → 可按反应堆输出缓存推电
