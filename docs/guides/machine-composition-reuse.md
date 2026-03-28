# 机器能力组合复用（发电机/槽位）

本文总结 `ic2_120` 中“组合优于继承”的机器复用方案，目标是让后续新增发电机或机器时，只做能力拼装而不是复制代码。

## 1. 设计目标

- 把“可充电物品充电逻辑”从具体机器 `BlockEntity` 中抽离（电池与电动工具均可充电）。
- 把“ScreenHandler 槽位规则”从匿名 `Slot` 子类中抽离。
- 让规则在多个机器间共享，减少重复实现和行为漂移。

## 2. 已落地组件

### 2.1 可充电物品

**可充电**的物品分为两类，行为不同：

- **电池**（`IBatteryItem`）：如 Re-Battery、Energy Crystal 等。既可被充电，也可放电给机器供电。
- **电动工具**（`IElectricTool`）：如链锯、钻头、电动扳手等，实现 `IElectricTool` 接口。**仅可被充电**，不可放电——电动工具不是电池，不能给机器供电。

两者均遵循 `ITiered` 能量等级规则，充电时需满足 `item.tier <= machineTier`。

### 2.2 充电组件

- 文件：`src/main/kotlin/ic2_120/content/energy/charge/BatteryChargerComponent.kt`
- 作用：统一执行“机器 -> 可充电物品”充电流程（电池与电动工具均可放入充电槽充电）。
- 通过注入回调实现组合：
  - `machineTierProvider`: 机器等级
  - `machineEnergyProvider`: 当前可用能量
  - `extractEnergy`: 扣除机器能量
  - `canChargeNow`: 充电时机（例如发电机仅燃烧时）

核心规则由组件统一保证：

- 仅允许 `item.tier <= machineTier`
- 充电量受三者最小值限制：
  - 物品传输速率（电池）或充电上限（电动工具）
  - 机器当前可用能量
  - 物品剩余容量

**充电速度**：电池充电速度设置为 **16倍** 于基础传输速度（`transferSpeed * 4 * 4`），电动工具充电速度为 `ELECTRIC_TOOL_CHARGE_SPEED_BASE * tool.tier * 16`。

### 2.3 放电组件

- 文件：`src/main/kotlin/ic2_120/content/energy/charge/BatteryDischargerComponent.kt`
- 作用：统一执行“**电池** -> 机器”放电流程。**仅支持电池**（`IBatteryItem`），电动工具不可放入放电槽。
- 通过注入回调实现组合：
  - `machineTierProvider`: 机器等级（用于放电兼容检查）
  - `canDischargeNow`: 放电时机（例如机器未满电时）

核心规则由组件统一保证：

- 仅允许 `item.tier >= machineTier`
- 放电量受三者最小值限制：
  - 机器本 tick 需求
  - 电池传输速率
  - 电池当前电量

### 2.4 槽位规则系统

- 文件：
  - `src/main/kotlin/ic2_120/content/screen/slot/SlotSpec.kt`
  - `src/main/kotlin/ic2_120/content/screen/slot/PredicateSlot.kt`
  - `src/main/kotlin/ic2_120/content/screen/slot/SlotMoveHelper.kt`
- 作用：
  - `SlotSpec` 声明规则（`canInsert` / `canTake` / `maxItemCount`）
  - `PredicateSlot` 依据 `SlotSpec` 实现通用槽位
  - `SlotMoveHelper` 复用 `quickMove` 的目标槽位插入顺序

### 2.5 升级槽位布局

- 文件：`src/main/kotlin/ic2_120/content/screen/slot/UpgradeSlotLayout.kt`
- 作用：可复用的升级槽位布局与规则。每个机器有 4 个升级槽位，紧贴原版 UI（176 宽）右侧纵向排列。
- 规则：仅允许放入 `IUpgradeItem` 实现类的物品，最大堆叠 1。
- 接入：在 `ScreenHandler` 中循环添加 4 个 `PredicateSlot`，使用 `UpgradeSlotLayout.SLOT_SPEC` 与 `slotY(i)`；在 `BlockEntity` 中预留 4 个库存槽位。
- 各升级的实际效果由机器在 tick 中自行读取并应用，`UpgradeSlotLayout` 不定义效果逻辑。

### 2.6 升级物品接口

- **文件**：`src/main/kotlin/ic2_120/content/item/Upgrades.kt`
- **接口**：`IUpgradeItem`，所有升级类实现此接口。
- **升级注册**：`UpgradeItemRegistry` 维护升级物品到机器接口的映射。

**当前已实现的升级类型**：
- `OverclockerUpgrade` - 加速升级
- `TransformerUpgrade` - 高压升级
- `EnergyStorageUpgrade` - 储能升级
- `RedstoneInverterUpgrade` - 红石反转升级
- `EjectorUpgrade` - 物品弹出升级
- `AdvancedEjectorUpgrade` - 高级物品弹出升级
- `PullingUpgrade` - 流体抽取升级
- `AdvancedPullingUpgrade` - 高级流体抽取升级
- `FluidEjectorUpgrade` - 流体弹出升级
- `FluidPullingUpgrade` - 流体抽取升级

**升级接口映射**（`UpgradeItemRegistry`）：
- 每个升级物品映射到其要求的机器接口
- `UpgradeSlotLayout` 检查机器是否实现对应接口
- 未实现接口的升级无法放入该机器的升级槽

### 2.7 升级支持接口与处理组件

- **机器接口**（`src/main/kotlin/ic2_120/content/upgrade/`）：每个升级对应一个接口，机器选择性实现。
  - `IOverclockerUpgradeSupport`：暴露 `speedMultiplier`、`energyMultiplier`，供加速升级写入。
  - `IEnergyStorageUpgradeSupport`：暴露 `capacityBonus`，供储能升级写入。✅ **已实现**
  - `ITransformerUpgradeSupport`：暴露 `voltageTierBonus`，供高压升级写入。✅ **已实现**
  - `IRedstoneInverterUpgradeSupport`：红石控制反转。✅ **基础设施完整，机器可按需接入**
  - `IEjectorUpgradeSupport`：物品/流体弹出槽支持。⚠️ **仅接口定义，机器需自行实现**
  - `IFluidPipeUpgradeSupport`：继承自 `IEjectorUpgradeSupport`，流体管道升级支持。✅ **已实现**
  - `IRedstoneControlSupport`：红石控制组件支持。
- **升级处理组件**：
  - `OverclockerUpgradeComponent`：统计 OverclockerUpgrade 数量，按 2^n 计算速度与耗能倍率。
  - `EnergyStorageUpgradeComponent`：每个储能升级增加 10,000 EU 容量。✅ **已实现**
  - `TransformerUpgradeComponent`：每个高压升级提升 1 个电压等级，提高 maxInsertPerTick。✅ **已实现**
  - `RedstoneInverterUpgradeComponent`：处理红石反转升级。
  - `FluidPipeUpgradeComponent`：处理流体管道弹出/抽取升级。✅ **已实现**

## 3. 当前接入点

- `GeneratorBlockEntity`：
  - 使用 `BatteryChargerComponent` 代替内联充电代码。
- `GeneratorScreenHandler`：
  - 使用 `SlotSpec + PredicateSlot` 定义燃料槽和充电槽（电池、电动工具均可充电）。
  - 使用 `SlotMoveHelper` 处理玩家背包到机器槽位的路由。
- `MetalFormerScreenHandler`：
  - 放电槽改为 `PredicateSlot`，规则为“仅允许电池（`IBatteryItem`）+ 最大堆叠 1”（电动工具不可放电）。
  - 输入槽改为 `SlotSpec` 规则，避免电池误入输入槽。
  - 玩家物品栏 `quickMove` 改为 `SlotMoveHelper` 路由（放电槽 -> 输入槽 -> 次级输入槽 -> 4 个升级槽）。
  - 使用 `UpgradeSlotLayout` 添加 4 个升级槽，紧贴原版 UI 右侧纵向排列。
- `MetalFormerBlockEntity`：
  - 放电槽接入 `BatteryDischargerComponent`，并限制单堆叠。
- `GeneratorBlockEntity`：
  - 可充电物品槽在 `setStack` 中限制单堆叠。

## 4. 新机器接入建议

新增机器（尤其发电机）时，优先按以下步骤接入：

1. 在 `BlockEntity` 里组合 `BatteryChargerComponent`（支持电池与电动工具），只提供本机参数与回调。
2. 在 `ScreenHandler` 用 `SlotSpec` 声明各槽位规则，避免临时匿名 `Slot` 子类。
3. `quickMove` 优先复用 `SlotMoveHelper`，把“路由顺序”作为配置表达。
4. 机器特有逻辑（发电、配方、模式）保留在自身类中，不下沉到通用组件。

## 5. 后续可扩展方向

- 扩展充电组件，使 `IElectricTool` 电动工具可在机器充电槽中充电（放电组件仅支持电池）。
- 引入 `MachineSlotLayout`（槽位布局 + 规则 + 路由配置）减少 `ScreenHandler` 样板代码。
- 为组件补单元测试，固定 tier/速率/容量边界行为。
