# 同步系统（C/S 属性同步）

基于 Minecraft `ScreenHandler` 的 `PropertyDelegate`，用 Kotlin 委派和共享“属性定义类”实现服务端→客户端的整型同步。**属性只定义一次，两端复用，index 从结构上保证一致。**

**源码位置**：`src/main/kotlin/ic2_120/content/syncs/SyncedData.kt`、各机器的 `*Sync.kt`（位于 `src/main/kotlin/ic2_120/content/sync/` 目录，如 `ElectricFurnaceSync.kt`）。

---

## 概述

- **服务端**：BlockEntity 持有 `SyncedData`（实现 `PropertyDelegate`），通过 `addProperties(syncedData)` 交给 ScreenHandler，由游戏自动同步到客户端。
- **客户端**：ScreenHandler 用 `ArrayPropertyDelegate(propertyCount)` 接收同步；通过 `SyncedDataView(delegate)` 包装后，与服务端共用**同一个属性定义类**，按声明顺序自动对齐 index。
- **NBT**：`SyncedData` 提供 `readNbt`/`writeNbt`，按属性名（`int("Name")` 的 `"Name"`）读写。

---

## 核心类型

| 类型 | 作用 |
|------|------|
| `SyncSchema` | 接口：提供 `int(name, default)`，返回可读写的委派。 |
| `SyncedData` | 服务端：实现 `PropertyDelegate` + `SyncSchema`，持有数据，供 NBT 与 ScreenHandler。 |
| `SyncedDataView` | 客户端：包装 `PropertyDelegate`，实现 `SyncSchema`，按调用顺序分配 index。 |

两端都通过 **同一个“属性定义类”** 接收 `SyncSchema`，在构造函数里按固定顺序调用 `schema.int(...)`，因此 index 必然一致。

---

## 快速开始

### 1. 定义同步属性（只写一次）

新建一个类，构造函数接收 `SyncSchema`，用 `by schema.int("NbtKey", default)` 声明属性。**属性顺序即 index 顺序，两端共用此类，故顺序自动一致。**

```kotlin
// 简单示例：content/sync/IronFurnaceSync.kt
class IronFurnaceSync(schema: SyncSchema) {
    var burnTime     by schema.int("BurnTime")
    var totalBurnTime by schema.int("TotalBurnTime")
    var cookTime     by schema.int("CookTime")
}

// 能量机器示例：通常继承 TickLimitedSidedEnergyContainer
class ElectricFurnaceSync(
    schema: SyncSchema,
    currentTickProvider: () -> Long? = { null }
) : TickLimitedSidedEnergyContainer(
    ENERGY_CAPACITY,
    { 0L },  // capacityBonusProvider
    MAX_INSERT,
    MAX_EXTRACT,
    currentTickProvider
) {
    var energy by schema.int("Energy")
    var progress by schema.int("Progress")
}
```

- `"SyncCounter"`、`"Energy"` 等为 NBT 持久化的键名（仅服务端 `SyncedData` 使用）。
- 新增属性时只在此类中加一行即可，无需改 index 或 buf 逻辑。

### 2. 服务端：BlockEntity

- 创建 `SyncedData(this)`（传入当前 BlockEntity），并用它构造你的属性定义类；传入 BlockEntity 后，任意同步属性写入时会自动调用 `markDirty()`，无需再传回调。
- 把 `syncedData` 传给 ScreenHandler；在 `writeScreenOpeningData` 里写入 `syncedData.size()`。
- NBT 中调用 `syncedData.readNbt(nbt)` / `syncedData.writeNbt(nbt)`。

```kotlin
// BlockEntity 内
val syncedData = SyncedData(this)
val sync = ElectricFurnaceSync(syncedData)

override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
    buf.writeBlockPos(pos)
    buf.writeVarInt(syncedData.size())
}

override fun createMenu(...): ScreenHandler =
    MyScreenHandler(..., syncedData)

override fun readNbt(nbt: NbtCompound) {
    super.readNbt(nbt)
    syncedData.readNbt(nbt)
}

override fun writeNbt(nbt: NbtCompound) {
    super.writeNbt(nbt)
    syncedData.writeNbt(nbt)
}
```

读写属性一律通过 `sync.xxx`，例如 `sync.syncCounter++`、`sync.energy = 1000`。

**重要**：修改了 `syncedData` 里的属性后，必须调用 BlockEntity 的 `markDirty()`，否则：
- **持久化**：区块/世界保存时，该 BlockEntity 可能不会写回 NBT，重载世界后能量、进度等会丢失或恢复为旧值。
- 界面同步（PropertyDelegate）不依赖 markDirty，但若希望“改了就存盘”，必须在改完后 markDirty。

推荐在创建 `SyncedData` 时传入 BlockEntity：`SyncedData(this)`，则属性一经写入会自动 `markDirty()`，无需在属性定义类或业务代码里再传回调或手动调用。

### 3. 客户端：ScreenHandler

- 构造函数接收 `PropertyDelegate`（服务端传 `syncedData`，客户端在 `fromBuffer` 里用 `ArrayPropertyDelegate(propertyCount)`）。
- 用 `SyncedDataView(propertyDelegate)` 构造**同一个**属性定义类，得到 `sync`。
- `init` 里照常 `addProperties(propertyDelegate)`。

```kotlin
// ScreenHandler 内
class MyScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(...) {

    val sync = ElectricFurnaceSync(SyncedDataView(propertyDelegate))

    init {
        addProperties(propertyDelegate)
        // ... 槽位等
    }
}

companion object {
    fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): MyScreenHandler {
        val pos = buf.readBlockPos()
        val propertyCount = buf.readVarInt()
        // ...
        return MyScreenHandler(..., ArrayPropertyDelegate(propertyCount))
    }
}
```

### 4. 客户端：Screen 显示

通过 handler 的 `sync` 只读访问即可，例如：

```kotlin
Text("计数: ${handler.sync.syncCounter}", color = 0xAAAAAA)
Text("能量: ${handler.sync.energy}", color = 0xAAAAAA)
```

---

## 数据流小结

```
服务端 BlockEntity                   客户端 ScreenHandler / Screen
─────────────────────                ─────────────────────────────
syncedData = SyncedData()            propertyDelegate = ArrayPropertyDelegate(count)
sync = ElectricFurnaceSync(syncedData)   sync = ElectricFurnaceSync(SyncedDataView(propertyDelegate))
sync.syncCounter = 1  (写)            handler.sync.syncCounter  (读，由游戏同步写入 delegate)
syncedData → createMenu → addProperties   fromBuffer 读 count → addProperties(Array...)
writeScreenOpeningData: writeVarInt(size)   fromBuffer: readVarInt() → count
readNbt/writeNbt(syncedData)          （无需 NBT）
```

---

## 为新机器接一套同步

1. 在 `content/` 下新建 `XxxSync.kt`，定义 `class XxxSync(schema: SyncSchema) { var a by schema.int("A"); ... }`。
2. 在 BlockEntity 中：`val syncedData = SyncedData(this)`；`val sync = XxxSync(syncedData)`，菜单传 `syncedData`，buf 写 `syncedData.size()`，NBT 用 `syncedData.readNbt/writeNbt`。
3. 在 ScreenHandler 中：构造参数收 `PropertyDelegate`，`val sync = XxxSync(SyncedDataView(propertyDelegate))`，`addProperties(propertyDelegate)`；`fromBuffer` 里 `readVarInt()` 得到 count，`ArrayPropertyDelegate(count)`。
4. 在 Screen 中：用 `handler.sync.xxx` 显示。

---

## 为何顺序一定一致

- 服务端：`ElectricFurnaceSync(syncedData)` 构造时按类中声明顺序依次调用 `syncedData.int("SyncCounter")`、`syncedData.int("Energy", ...)`，即 index 0、1、2…
- 客户端：`ElectricFurnaceSync(SyncedDataView(propertyDelegate))` 构造时按**同一类**的同一顺序调用 `view.int(...)`，`SyncedDataView` 内部按调用顺序递增 index。
- 两处唯一引用的是同一个 `XxxSync` 类，所以顺序在结构上一致，无需手写 index。

---

## 参考

- 实现：`SyncedData.kt`、`ElectricFurnaceSync.kt`
- 使用：`ElectricFurnaceBlockEntity`、`ElectricFurnaceScreenHandler`、`ElectricFurnaceScreen`
