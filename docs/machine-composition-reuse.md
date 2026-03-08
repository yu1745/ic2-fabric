# 机器能力组合复用（发电机/槽位）

本文总结 `ic2_120` 中“组合优于继承”的机器复用方案，目标是让后续新增发电机或机器时，只做能力拼装而不是复制代码。

## 1. 设计目标

- 把“电池充电逻辑”从具体机器 `BlockEntity` 中抽离。
- 把“ScreenHandler 槽位规则”从匿名 `Slot` 子类中抽离。
- 让规则在多个机器间共享，减少重复实现和行为漂移。

## 2. 已落地组件

### 2.1 电池充电组件

- 文件：`src/main/kotlin/ic2_120/content/energy/charge/BatteryChargerComponent.kt`
- 作用：统一执行“机器 -> 电池”充电流程。
- 通过注入回调实现组合：
  - `machineTierProvider`: 机器等级
  - `machineEnergyProvider`: 当前可用能量
  - `extractEnergy`: 扣除机器能量
  - `canChargeNow`: 充电时机（例如发电机仅燃烧时）

核心规则由组件统一保证：

- 仅允许 `battery.tier <= machineTier`
- 充电量受三者最小值限制：
  - 电池传输速率
  - 机器当前可用能量
  - 电池剩余容量

### 2.2 电池放电组件

- 文件：`src/main/kotlin/ic2_120/content/energy/charge/BatteryDischargerComponent.kt`
- 作用：统一执行“电池 -> 机器”放电流程。
- 通过注入回调实现组合：
  - `machineTierProvider`: 机器等级（用于放电兼容检查）
  - `canDischargeNow`: 放电时机（例如机器未满电时）

核心规则由组件统一保证：

- 仅允许 `battery.tier >= machineTier`
- 放电量受三者最小值限制：
  - 机器本 tick 需求
  - 电池传输速率
  - 电池当前电量

### 2.3 槽位规则系统

- 文件：
  - `src/main/kotlin/ic2_120/content/screen/slot/SlotSpec.kt`
  - `src/main/kotlin/ic2_120/content/screen/slot/PredicateSlot.kt`
  - `src/main/kotlin/ic2_120/content/screen/slot/SlotMoveHelper.kt`
- 作用：
  - `SlotSpec` 声明规则（`canInsert` / `canTake` / `maxItemCount`）
  - `PredicateSlot` 依据 `SlotSpec` 实现通用槽位
  - `SlotMoveHelper` 复用 `quickMove` 的目标槽位插入顺序

## 3. 当前接入点

- `GeneratorBlockEntity`：
  - 使用 `BatteryChargerComponent` 代替内联充电代码。
- `GeneratorScreenHandler`：
  - 使用 `SlotSpec + PredicateSlot` 定义燃料槽和电池槽。
  - 使用 `SlotMoveHelper` 处理玩家背包到机器槽位的路由。
- `MetalFormerScreenHandler`：
  - 放电槽改为 `PredicateSlot`，规则为“仅允许 `IBatteryItem` + 最大堆叠 1”。
  - 输入槽改为 `SlotSpec` 规则，避免电池误入输入槽。
  - 玩家物品栏 `quickMove` 改为 `SlotMoveHelper` 路由（放电槽 -> 输入槽）。
- `MetalFormerBlockEntity`：
  - 放电槽接入 `BatteryDischargerComponent`，并限制单堆叠。
- `GeneratorBlockEntity`：
  - 电池槽在 `setStack` 中限制单堆叠。

## 4. 新机器接入建议

新增机器（尤其发电机）时，优先按以下步骤接入：

1. 在 `BlockEntity` 里组合 `BatteryChargerComponent`，只提供本机参数与回调。
2. 在 `ScreenHandler` 用 `SlotSpec` 声明各槽位规则，避免临时匿名 `Slot` 子类。
3. `quickMove` 优先复用 `SlotMoveHelper`，把“路由顺序”作为配置表达。
4. 机器特有逻辑（发电、配方、模式）保留在自身类中，不下沉到通用组件。

## 5. 后续可扩展方向

- 增加 `BatteryDischargerComponent`，统一“电池 -> 机器”放电逻辑。
- 引入 `MachineSlotLayout`（槽位布局 + 规则 + 路由配置）减少 `ScreenHandler` 样板代码。
- 为组件补单元测试，固定 tier/速率/容量边界行为。
