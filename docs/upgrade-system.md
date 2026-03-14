# 机器升级系统

机器升级系统允许玩家通过放入升级物品来增强机器性能。升级槽位位于机器 GUI 右侧，每个机器有 4 个升级槽位。**只有实现了对应接口的机器才能接受特定类型的升级**。

**源码位置**：
- 升级物品：`src/main/kotlin/ic2_120/content/item/Upgrades.kt`
- 升级组件与接口：`src/main/kotlin/ic2_120/content/upgrade/`
- 升级槽布局：`src/main/kotlin/ic2_120/content/screen/slot/UpgradeSlotLayout.kt`

---

## 架构概览

```
┌─────────────────────────────────────────────────────────────────────────┐
│  升级物品 (IUpgradeItem)                                                  │
│  OverclockerUpgrade, TransformerUpgrade, EnergyStorageUpgrade, ...       │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  UpgradeItemRegistry                                                     │
│  物品 → 机器接口 映射表，决定「该升级可放入哪些机器」                          │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  机器接口 (I*UpgradeSupport)                                              │
│  IOverclockerUpgradeSupport, ITransformerUpgradeSupport, ...              │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  升级组件 (*UpgradeComponent)                                             │
│  从升级槽统计数量，计算倍率/加成，写入机器属性                                │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 核心类型

### 1. 升级物品（IUpgradeItem）

实现 `IUpgradeItem` 接口的物品可放入机器的升级槽。各升级的实际效果由机器在 tick 中读取并应用。

| 升级物品 | 对应接口 | 效果 |
|----------|----------|------|
| `OverclockerUpgrade` | `IOverclockerUpgradeSupport` | 加速加工，耗能增加 |
| `TransformerUpgrade` | `ITransformerUpgradeSupport` | 提高电压等级，增加输入速度 |
| `EnergyStorageUpgrade` | `IEnergyStorageUpgradeSupport` | 增加能量缓冲容量 |
| `RedstoneInverterUpgrade` | `IRedstoneInverterUpgradeSupport` | 红石反转（待实现） |
| `EjectorUpgrade` / `AdvancedEjectorUpgrade` / `PullingUpgrade` / `AdvancedPullingUpgrade` / `FluidEjectorUpgrade` / `FluidPullingUpgrade` | `IEjectorUpgradeSupport` | 弹出/抽入（待实现） |

### 2. UpgradeItemRegistry

升级物品与机器接口的映射表。**机器未实现对应接口时，该升级不允许放入升级槽**；未在映射表中的升级物品不允许放入任何机器。

```kotlin
// 检查机器是否支持某升级
UpgradeItemRegistry.canAccept(machine, item)

// 获取升级对应的接口
UpgradeItemRegistry.getRequiredInterface(item)
```

### 3. 机器接口（I*UpgradeSupport）

机器实现这些接口后，才能接受对应类型的升级，并接收升级组件写入的属性。

| 接口 | 属性 | 说明 |
|------|------|------|
| `IOverclockerUpgradeSupport` | `speedMultiplier`, `energyMultiplier` | 速度与耗能倍率 |
| `ITransformerUpgradeSupport` | `voltageTierBonus` | 电压等级加成 |
| `IEnergyStorageUpgradeSupport` | `capacityBonus` | 储能容量加成 |
| `IRedstoneInverterUpgradeSupport` | `redstoneInverted`（继承自 `IRedstoneControlSupport`） | 红石反转（按需接入） |
| `IEjectorUpgradeSupport` | — | 弹出/抽入（待实现） |

### 4. 升级组件（*UpgradeComponent）

单例对象，负责从升级槽统计升级数量，计算效果，并写入实现对应接口的机器。

### 5. 红石控制组件（RedstoneControlComponent）

- 通用组件：`RedstoneControlComponent.canRun(world, pos, machine)`
- 机器只要实现 `IRedstoneControlSupport` 即可被红石控制启停
- 逻辑：
  - 默认（`redstoneInverted = false`）：有红石信号运行，无信号停机
  - 反转（`redstoneInverted = true`）：有红石信号停机，无信号运行

---

## 已实现的升级效果

### 加速升级（OverclockerUpgrade）

- **公式**（可叠加，指数增长）：
  - 每个超频：工作时间缩短到 70%（速度倍率 = 1/0.7）
  - 每个超频：额外消耗 60% 电力（耗能倍率 = 1.6）
  - n 个：速度 = (1/0.7)^n，耗能 = 1.6^n
- **上限**：最多按 16 个计算，避免溢出
- **使用**：机器在 tick 中读取 `speedMultiplier` 和 `energyMultiplier`，分别放大进度增量和能量消耗

### 高压升级（TransformerUpgrade）

- **效果**：每个高压升级提高电压等级 1，从而增加 `maxInsertPerTick`
- **等级对应**：等级 1 = 32 EU/t，等级 2 = 128 EU/t，等级 3 = 512 EU/t（32 × 4^(tier-1)）
- **配合**：超频升级放多后耗能增加，等级 1 的输入速度跟不上，需配合高压升级

### 储能升级（EnergyStorageUpgrade）

- **效果**：每个储能升级增加 1 万 EU 电量缓冲
- **常量**：`EnergyStorageUpgradeComponent.CAPACITY_PER_UPGRADE = 10_000L`

### 红石反转升级（RedstoneInverterUpgrade）

- **当前状态**：项目里暂时没有机器实现 `IRedstoneInverterUpgradeSupport`，因此该升级暂不可用
- **设计目标**：仅对“明确需要红石控制”的少数机器接入，和“是否有升级槽”无直接关系

接入示例（按需给某台机器启用）：

```kotlin
class MyMachineBlockEntity(...) : BlockEntity(...),
    IRedstoneInverterUpgradeSupport {

    override var redstoneInverted: Boolean = false

    fun tick(world: World, pos: BlockPos) {
        // 1) 从升级槽检测是否安装了红石反转升级
        RedstoneInverterUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)

        // 2) 统一红石门控
        if (!RedstoneControlComponent.canRun(world, pos, this)) {
            return
        }

        // 3) 正常机器逻辑...
    }
}
```

---

## 为机器添加升级支持

### 1. 实现机器接口

在 BlockEntity 上实现需要的接口，并声明对应属性：

```kotlin
class MyMachineBlockEntity(...) : BlockEntity(...),
    IOverclockerUpgradeSupport,
    IEnergyStorageUpgradeSupport,
    ITransformerUpgradeSupport {

    override var speedMultiplier: Float = 1f
    override var energyMultiplier: Float = 1f
    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0

    companion object {
        val SLOT_UPGRADE_INDICES = intArrayOf(4, 5, 6, 7)  // 升级槽索引
    }
}
```

### 2. 在 tick 中应用升级

每 tick 调用各升级组件的 `apply` 方法：

```kotlin
fun tick(world: World, pos: BlockPos, state: BlockState) {
    if (world.isClient) return

    // 应用升级效果
    OverclockerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
    EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
    TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)

    // 使用 speedMultiplier、energyMultiplier、capacityBonus、voltageTierBonus
    val progressIncrement = speedMultiplier.toInt().coerceAtLeast(1)
    val need = (ENERGY_PER_TICK * energyMultiplier).toLong().coerceAtLeast(1L)
    // ...
}
```

### 3. 能量存储与高压升级

若机器使用 `UpgradeableTickLimitedEnergyStorage`，需传入 `capacityBonusProvider` 和 `maxInsertPerTickProvider`：

```kotlin
val sync = MetalFormerSync(
    syncedData,
    { world?.time },
    { capacityBonus },  // 储能升级
    { TransformerUpgradeComponent.maxInsertForTier(BASE_TIER + voltageTierBonus) }  // 高压升级
)
```

### 4. 添加升级槽到 ScreenHandler

使用 `UpgradeSlotLayout` 创建升级槽，并传入机器获取器：

```kotlin
private val upgradeSlotSpec: SlotSpec by lazy {
    UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
}

// 添加 4 个升级槽
for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
    addSlot(
        PredicateSlot(
            blockInventory,
            SLOT_UPGRADE_INDICES[i],
            UpgradeSlotLayout.SLOT_X,
            UpgradeSlotLayout.slotY(i),
            upgradeSlotSpec
        )
    )
}
```

### 5. 槽位规则

`UpgradeSlotLayout.slotSpec(machineProvider)` 会：
- 仅允许放入 `IUpgradeItem`
- 仅允许放入在 `UpgradeItemRegistry` 中注册的升级
- 仅当机器实现了该升级对应的接口时才允许放入

---

## 升级槽布局

- **数量**：4 个
- **位置**：紧贴原版 UI（176 宽）右侧纵向排列
- **坐标**：`SLOT_X = 176`，`SLOT_Y_FIRST = 17`，间距 18

---

## 添加新升级类型

1. **定义升级物品**：在 `Upgrades.kt` 中创建实现 `IUpgradeItem` 的物品类
2. **定义机器接口**：在 `content/upgrade/` 中创建 `I*UpgradeSupport` 接口
3. **注册映射**：在 `UpgradeItemRegistry.init` 中调用 `register(ItemClass, InterfaceClass)`
4. **实现升级组件**（可选）：若需要统计数量并写入机器，创建 `*UpgradeComponent` 单例
5. **机器集成**：在支持该升级的机器上实现接口，在 tick 中调用组件的 `apply`

---

## 参考实现

- **金属成型机**（`MetalFormerBlockEntity`）：完整支持加速、储能、高压三种升级
- **MetalFormerScreenHandler**：升级槽布局与 `UpgradeSlotLayout` 集成示例
