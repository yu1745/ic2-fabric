# 方块变体系统（创造模式专用）

在不新建方块类型的前提下，为已有方块增加”变体”：同一方块、同一 BlockEntity 类型，仅通过**物品 NBT** 区分，放置时根据 NBT 初始化不同状态，且变体**仅在创造模式物品栏**中提供。

当前实现：**储能方块满电变体**（BatBox、CESU、MFE、MFSU）

---

## 思路

- **不新建 Block / BlockEntity**：复用现有方块与方块实体。
- **继承基类 BlockItem**：使用 `EnergyStorageBlockItem` 抽象基类，处理放置时的 NBT 初始化和名称显示。
- **使用 restoreEnergy()**：安全地从 NBT 恢复能量到能量存储系统。
- **创造模式物品栏**：用 `ItemGroupEvents.modifyEntriesEvent(...)` 向对应物品组追加带 NBT 的 ItemStack。

---

## 步骤

### 1. 方块类使用基类模式

当前实现使用 `EnergyStorageBlock` 抽象基类，包含：
- `NBT_FULL` 常量定义
- 抽象内部类 `EnergyStorageBlockItem`
- 配置对象 `EnergyStorageConfig`

**实际文件位置**：
- 基类：`src/main/kotlin/ic2_120/content/block/storage/EnergyStorageBlock.kt`
- 方块定义：`src/main/kotlin/ic2_120/content/block/EnergyStorageBlocks.kt`（BatBox、CESU、MFE、MFSU）

### 2. BlockItem 放置逻辑

`EnergyStorageBlockItem` 的 `place()` 方法实现：

```kotlin
override fun place(context: ItemPlacementContext): ActionResult {
    val result = super.place(context)
    if (result.isAccepted && !context.world.isClient) {
        val nbt = context.stack.nbt ?: return result
        if (nbt.getBoolean(NBT_FULL)) {
            val be = context.world.getBlockEntity(context.blockPos) as? EnergyStorageBlockEntity ?: return result
            be.sync.restoreEnergy(config.capacity)  // 使用 restoreEnergy 安全恢复
            be.markDirty()
        }
    }
    return result
}
```

**关键改进**：
- 使用 `restoreEnergy(config.capacity)` 而非直接设置 `amount`
- 使用抽象 `config.capacity` 而非硬编码值
- 统一基类处理，无需为每个方块重复代码

### 2. 在主类中覆盖物品注册并添加创造模式入口

在 `ModInitializer.onInitialize()` 中（例如在“添加特殊物品”等注释下）：

1. **用自定义 BlockItem 覆盖该方块的物品**  
   - 取方块 ID：`Identifier(MOD_ID, "mfsu")`。  
   - 取方块：`Registries.BLOCK.get(mfsuId)`。  
   - 用 **`.set()`** 覆盖同一 ID 下已注册的物品（不能用 `Registry.register()`，否则会报 “different raw IDs”）。需保留原条目的 raw ID 与 key，并传入 `Lifecycle.stable()`：  
     - 取 `mfsuKey = RegistryKey.of(RegistryKeys.ITEM, mfsuId)`，`rawId = Registries.ITEM.getRawId(Registries.ITEM.get(mfsuId))`；  
     - 调用 `(Registries.ITEM as SimpleRegistry<Item>).set(rawId, mfsuKey, customBlockItem, Lifecycle.stable())`（`Lifecycle` 为 `com.mojang.serialization.Lifecycle`）。  
   这样会覆盖 ClassScanner 之前注册的默认 BlockItem。

2. **向创造模式物品栏追加变体物品堆**  
   - 取物品组 Key：`RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier(MOD_ID, CreativeTab.IC2_MACHINES.id))`（按你用的物品组改）。  
   - `ItemGroupEvents.modifyEntriesEvent(ic2MachinesKey).register { entries -> ... }`。  
   - 在回调里：`ItemStack(Registries.ITEM.get(mfsuId))`，`stack.orCreateNbt.putBoolean(EnergyStorageBlock.NBT_FULL, true)`，`entries.add(fullStack)`。

示例（节选）：

```kotlin
val mfsuId = Identifier(MOD_ID, "mfsu")
val mfsuBlock = Registries.BLOCK.get(mfsuId)
val mfsuKey = RegistryKey.of(RegistryKeys.ITEM, mfsuId)
val rawId = Registries.ITEM.getRawId(Registries.ITEM.get(mfsuId))
val customMfsuItem = MfsuBlock.MfsuBlockItem(mfsuBlock, FabricItemSettings())
(Registries.ITEM as SimpleRegistry<Item>).set(rawId, mfsuKey, customMfsuItem, Lifecycle.stable())
val ic2MachinesKey = RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier(MOD_ID, CreativeTab.IC2_MACHINES.id))
ItemGroupEvents.modifyEntriesEvent(ic2MachinesKey).register { entries ->
    val fullStack = ItemStack(Registries.ITEM.get(mfsuId))
    fullStack.orCreateNbt.putBoolean(EnergyStorageBlock.NBT_FULL, true)
    entries.add(fullStack)
}
```

### 3. 语言文件

在 `assets/<modid>/lang/` 中为变体名称增加条目，例如：

- `block.ic2_120.mfsu_full`: `"MFSU (Full)"` / `"MFSU储电箱（满电）"`

---

## 要点小结

| 项目 | 说明 |
|------|------|
| 不新建方块 | 仅复用已有 Block + BlockEntity，用物品 NBT 区分变体。 |
| 自定义 BlockItem | 必须覆盖物品注册（主类里再 `Registry.register(ITEM, id, customItem)`），否则创造栏里拿到的仍是默认 BlockItem。 |
| 放置时写 BE | 在 `place()` 里服务端、`result.isAccepted` 后根据 `context.stack.nbt` 写 BlockEntity 并 `markDirty()`。 |
| 创造栏入口 | 只通过 `ItemGroupEvents.modifyEntriesEvent(...).add(带 NBT 的 ItemStack)` 添加，生存无法合成该 NBT，即“仅创造可拿”。 |

按上述步骤即可为任意已有方块增加仅创造模式可用的变体，而不新增方块类型。
