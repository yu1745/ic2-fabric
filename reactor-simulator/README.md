# IC2 核反应堆模拟器（Preact）

把 `core/src/main/kotlin/ic2_120/content/block/nuclear/NuclearReactorBlockEntity.kt` 及其元件类的核电逻辑忠实移植为浏览器可用的反应堆布局模拟器。

## 运行

```bash
cd reactor-simulator
pnpm install          # 注意 .npmrc 里 ignore-workspace=true，绕开上层 ic2_1.20 的 pnpm workspace
pnpm copy-textures    # 从 assets 复制元件贴图到 public/textures/（一次性，已执行过）
pnpm dev              # 本地开发服务器
pnpm build            # 生产构建（dist/）
pnpm test             # 跑 vitest 单测
```

## 功能

- **拖放布局**：从元件栏拖元件到 9 行 × (3..9) 列的网格，右键移除。
- **电力 / 流体模式切换**：电力模式看 EU/t；流体模式看热冷却液产量（按 `HU_PER_BUCKET=20000` 换算）。
- **可调腔室数**：0..6，网格从 3×9 到 9×9（列优先索引 `slot = x*9+y`，与游戏一致）。
- **稳态 + 时间运行**：仪表盘实时显示当前布局的产热/散热/EU/t 净收支；点「运行」逐 cycle 推进，观察元件耐久衰减、堆温变化、最终熔毁或燃料烧完。
- **热力图叠层**：网格槽边框红=产热、蓝=散热，耐久条按游戏 `getItemBarColor` 阈值（绿/黄/橙/红）。

## 移植正确性验证（重要）

模拟逻辑通过两层测试与游戏本体对拍，全部 21 个 vitest 用例 green：

### A. 快照对拍（稳态元件）— `test/sim/parity.test.ts`
golden values 来自游戏内 mcdebug 测试 `core/test/mcdebug/reactor_golden.test.ts`：
放反应堆 + 相邻红石块（反应堆默认 `redstoneInverted=false`，**需红石信号才运行**）→ 填槽 → `waitUntil(EnergyStored>0)` + `waitTicks(20)` → 读 `HeatStored` / `EnergyStored` / 各槽 NBT `use`。

稳态元件（燃料棒 / 散热片 / 冷却单元 / 反射板）在 2 cycle 内已进入周期性循环，golden 可信。simulator 2-cycle 后的堆温与各槽 `use` 与游戏**逐项精确一致**。

golden 提取流程（重新生成时）：
```bash
# 1. 临时禁用其它 mcdebug 测试（加速），只留 reactor_golden.test.ts
cd core/test/mcdebug
for f in block_cutter centrifuge ...; do mv ${f}.test.ts ${f}.test.ts.disabled; done
# 2. 跑 mcdebug
cd ../../.. && ./gradlew :core:mcdebugTest
# 3. 从输出抓 "GOLDEN" 行，更新 test/sim/golden-values.ts
# 4. 恢复：mv *.disabled 回 *.test.ts
```

### B. 热交换器白盒单周期 — `test/sim/exchanger.test.ts`
**热交换器是唯一「自派生状态」的元件**：它的热量分布根据四邻温度演化，长周期才收敛（甚至可能进入长周期振荡），2 cycle 快照对拍不可行。改用代数手算单周期 Δ 验证：
- 构造 `switchSide=0` 的 `reactor_heat_exchanger`，把问题降为一维（只与反应堆本体交换），独立实现一份手算函数（含分层节流 `avg<1.0/0.75/0.5/0.25` 的链式覆盖），与 simulator 单周期结果逐项比对。
- 另测 `component_heat_exchanger(switchSide=36)` 的四邻元件交换通路。

⚠️ 分层节流逻辑里源码注释把档位说成「高温/中温」，但 `avgTemp = othermed + mymed/2` 量纲是 0-150，与字面常量 `1.0/0.75/0.5/0.25` 比较时绝大多数情况会落入 `<1.0` 档。移植**照抄源码逻辑而非注释**。

## 贴图来源

全部 16×16 PNG 来自 `core/src/main/resources/assets/ic2/textures/item/`（上游 `ic2` 命名空间，不可修改），由 `scripts/copy-textures.mjs` 按 registry name 复制到 `public/textures/`。映射关系见该脚本与 `src/sim/components/registry.ts` 的 `texture` 字段。

## 元件名称

所有元件显示名直接取自 `core/src/main/resources/assets/ic2_120/lang/zh_cn.json`（`item.ic2_120.<id>`），不自行翻译。

## 范围外（明确不做）

- **5×5×5 压力壳 / 红石口 / 流体口的 3D 摆放与判定**：用户明确「外壳判定是游戏本体的事」。流体模式仅算内部产热/冷却液转换，不含外壳结构验证。
- **锂燃料棒 / 枯竭同位素棒**：源码里是无 reactor 行为的 stub（仅辐射源），不进模拟器。
- **密封反应堆隔热板（containment_reactor_plating）**：源码是纯 Item，无 reactor 行为。
- **向游戏导入/导出布局**：纯独立模拟器。

## 架构

```
src/sim/           纯逻辑层（无 DOM，可单测）
  types.ts         IReactorComponent / IReactor 接口、Slot、Grid、CycleStats
  constants.ts     所有魔法数（逐个标注 Kotlin 来源）
  reactor.ts       CycleSimulator：单 cycle 推进（pass0 发电/耐久 + pass1 热量结算/爆炸）
  components/      元件 behavior，类层级与 Kotlin 严格对应
    registry.ts    id → {i18n 名、贴图、behavior 工厂}；启动时注入 resolver
    FuelRods.ts    铀/MOX 燃料棒（脉冲传播、三角产热、耐久衰减）
    HeatVents.ts   散热片 + 元件散热片
    HeatExchangers.ts  热交换器（含分层节流）
    CoolantCells.ts    冷却单元 / 冷凝器 / 隔板
    Reflectors.ts      中子反射板（有限 + 铱）
src/ui/            Preact 视图层
src/hooks/         useReactor 状态 hook
test/sim/          vitest（smoke + parity A + exchanger B）
```

## 已知差异

- **EU 缓冲瞬时值（EnergyStored）不严格对拍**：受游戏内 tickOffset（随机 0..19）与放出节奏（`pendingEnergyOutput/20` 每 tick）影响，瞬时 EU 波动较大，仅作量级参考。稳态 EU/t（= `outputAccumulator * 100 / 20`）是确定性的，仪表盘展示的是这个值。
- **流体模式假设冷却液充足**：`hasCoolant()` 默认 true，便于纯产热计算；游戏里冷却液供给由流体口决定（本模拟器不含）。
