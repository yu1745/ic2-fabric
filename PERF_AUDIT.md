# 性能审查：ClassScanner 注解注册 + SyncedData 委托同步

日期：2026-11-10　分支：`1.21.1`

> 结论来自静态审查，**没有做 profiling**，排序按调用频率推断，不是实测数据。
> 唯一实际验证过的是用 `javap` 确认委托属性的装箱字节码。
> 动手前建议先用 spark 在机器密集的存档里采样一次，确认各项实际占比。

## 一、SyncedData 委托

规模：334 个委托属性，66 处 `SyncedData()` 实例化，111 处每 tick 调用 `syncCurrentTickFlow()`。

涉及文件：`core/src/main/kotlin/ic2_120/content/syncs/SyncedData.kt`、`content/sync/EnergyFlowSync.kt`、`content/sync/HeatFlowSync.kt`、`content/TickLimitedSidedEnergyContainer.kt`。

### 1.1 没人看 GUI 时也在每 tick 计算显示数据（最值得修）

- `EnergyFlowSync.syncCurrentTickFlow()` 每台机器每 tick 做 3 次滑动窗口写入，每次含一次 64 位除法和两次 entry 写回。
- `sync.energy = ...` 在一个 tick 内被写 2~4 次（见 `MaceratorBlockEntity.kt:224,282,305`）。
- 这些值只在菜单打开时才被 `broadcastChanges` 读取。

**方案**：在 `SyncedData.get(index)` 里记录 `lastPolledTick`（原版只在菜单打开时每 tick 轮询它），不被观看时 `syncCurrentTickFlow()` 直接返回，重新被观看时清空窗口。76 个 Handler 都不用改。

**需先确认**：有没有服务端代码（如 Jade 类 tooltip）在无菜单时读 `getSyncedXxxAmount()`。

### 1.2 每次读写都走泛型接口并装箱

- `MaceratorSync.getEnergy()` 的字节码是 `invokeinterface ReadWriteProperty.getValue → Object → checkcast Number → intValue`；`setEnergy` 先 `Integer.valueOf`。
- `setValue` 内部再调 `getValue`，一次写入约 6 次指针追逐（`SyncedData.kt:63-67`）。
- 热代码里 C2 可能通过内联消掉装箱，但不保证。

**方案**：`SyncSchema.int()` 改为返回非泛型的具体类，带 `operator fun getValue(...): Int`。Kotlin 会生成直接调用，没有装箱；各 Sync 类的 `by schema.int(...)` 写法不变。

```kotlin
abstract class SyncIntProp {
    abstract operator fun getValue(thisRef: Any?, property: KProperty<*>): Int
    abstract operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int)
}
interface SyncSchema {
    fun int(name: String, default: Int = 0): SyncIntProp
    fun intAveraged(name: String, default: Int = 0, windowSize: Int = 20): SyncIntProp
}
```

### 1.3 每个 BE 实例的构造和内存开销偏大

- 每个 `int` 字段产生 1 个匿名委托对象、2 个 `Entry`、2 个运行时拼接的字符串（`"${name}_High"`）。
- `intAveraged` 另加一个 `IntArray(20)` 和 3 个闭包捕获的 `var`（编译成堆上的 `IntRef`/`LongRef`，每次写多一层间接）。
- 一台打粉机约 40 多个小对象，客户端加载区块时也同样构造一份。

**方案**：存储改成一个 `IntArray`，每字段一个完整 int；`get(index)` 按奇偶现场拆高低 16 位，线上协议不变。窗口状态放进委托类的普通字段。

```kotlin
override fun get(index: Int): Int {
    val v = values[index ushr 1]
    return if (index and 1 == 0) v ushr 16 else v and 0xFFFF
}
```

### 1.4 NBT 体积翻倍且存了显示数据

- `writeNbt` 每字段写 `_High`/`_Low` 两个 key，连滑动平均值也落盘。

**方案**：每字段写一个 key，读取时回退兼容旧的 `_High/_Low`；平均值字段不落盘。

### 1.5 高低位拆分导致数据槽翻倍（低优先级）

每个 int 占 2 个 `DataSlot`，变化时最多发 2 个包。只在有人开着 GUI 时发生，不建议现在为它换自定义包。

### 1.6 同类问题：`() -> Long?` 注入

- `TickLimitedSidedEnergyContainer.kt:25` 的 `currentTickProvider: () -> Long?` 每次调用都装箱一个 `Long`。
- 它在 `getMaxInsert`、`getMaxExtract`、`onFinalCommit` 和每次内部增减能量时都被调，`trackInternalMutation` 里还调了两遍。这是电网热路径。
- 48 个机器各传一个不同的 lambda 类，调用点是多态的，JIT 基本无法内联消除。`capacityBonusProvider: () -> Long` 同理。

**方案**：换成返回原生 `long` 的 `fun interface`，用 `Long.MIN_VALUE` 表示"无世界"。

```kotlin
fun interface TickSource { fun now(): Long }   // 无世界时返回 Long.MIN_VALUE
fun interface LongSource { fun get(): Long }
```

## 二、ClassScanner：运行期

涉及文件：`core/src/main/kotlin/ic2_120/registry/ClassScanner.kt`、`core/src/client/kotlin/ic2_120/client/ClientScreenRegistrar.kt`。

### 2.1 ItemStorage 查找每次走 kotlin-reflect

- `ClassScanner.kt:942` 用 `KProperty1.get(be)`，涉及 54 个 BE；管道、漏斗、弹出升级每次查询都经过它。
- 隔壁 `@RegisterEnergy` 已经用 `createPropertyAccessor` 修过，这里漏了。

**方案**：把 `:942` 换成同一个访问器。

### 2.2 BE 和实体工厂用 `KFunction.call`

- `:901`（125 个 BE 类型）在区块加载和放置方块时双端触发。
- `:1508` 在每次生成实体（如激光弹）时触发。

**方案**：注册时取 `ctor.javaConstructor`，用 `LambdaMetafactory` 生成真正的工厂对象。

### 2.3 菜单和 Screen 工厂用 `callBy(mapOf(...))`

`:815,856,864` 和 `ClientScreenRegistrar.kt:171`，只在开 GUI 时触发，频率低，可与 2.2 一起顺手改。

### 2.4 `Foo::class.instance()` 查表

- 全库 2568 处，绝大多数在配方生成里，无所谓。
- 运行期的十几处值得改：
  - `CokeKilnBlocks.kt:261-301` 的结构检测循环
  - `MinerBlockEntity.kt:217-291`
  - `CropmatronBlockEntity.kt:611,619`
  - `MiningPipeHelper.kt:13`

**方案**：提到循环外，或缓存成 `private val`。

## 三、ClassScanner：启动期

core 主源码集有 2232 个类，其中 1103 个是 `$` 内部类；实际带注解的只有约 770 个。

| 问题 | 位置 |
|---|---|
| 对扫描包下每个类做带初始化的 `Class.forName`，再用 kotlin-reflect 调 7 次 `findAnnotation` | `:521-528` |
| 重复扫描：`content.block` 扫完后 `block.machines` 为配方绑定再扫一遍，`recipes` 和客户端各一遍；jar 模式下每个包都重新打开 `JarFile` 遍历全部条目，core 一次启动约 9 遍 | `:277`、`:388`、`:416`、`:475` |
| 对 MC 的 Java 类做 kotlin-reflect：沿继承链一路爬到 `BlockEntity`/`Block` | `findAllMemberProperties` `:918,937`（每个 BE 调两次）、`createBlockItemForClass` `:1100` |
| 非 datagen 也收集配方生成器，对约 560 个 companion 做 `memberFunctions` | `collectRecipeGenerators` `:1306` |
| `camelToSnake` 每次调用编译一次 `Regex` | `:1161` |
| `resolveHandlerName` 对每个 Screen 遍历整个方块注册表，而 `blockClassToName` 里已有现成映射 | `ClientScreenRegistrar.kt` |

### 方案分两步

**第一步：不改构建的快修**

- `Class.forName(name, false, loader)` 不初始化，先用 Java 的 `isAnnotationPresent` 过滤，命中才取 `.kotlin`。
- 三个收集器共用一次遍历，jar 只开一次。
- 配方生成器只在 datagen 时收集。
- 属性查找到 `BlockEntity::class` 就停。

**第二步：彻底方案**

在已有的 `buildSrc` 里加一个 Gradle 任务，编译后用 ASM 扫注解，生成 `META-INF/ic2_120/registry-index.json`。运行时只加载索引里的类，不再遍历 jar。注解写法和开发体验不变，union 协议的兼容分支也可以删掉。

## 建议顺序

1. `ItemStorage` 访问器（2.1）和 `currentTickProvider` 装箱（1.6）：改动最小，都在物流和电网热路径上。
2. SyncedData 的观看门控（1.1），加上非泛型委托（1.2）和 `IntArray` 存储（1.3、1.4）：只改 `SyncedData.kt` 和 `EnergyFlowSync`/`HeatFlowSync`。按项目约束，需验证双端属性顺序和旧存档 NBT 兼容。
3. BE、实体、菜单工厂换成 `LambdaMetafactory`（2.2、2.3）。
4. 启动期快修（三·第一步），之后再考虑构建期索引（三·第二步）。
