# ClassScanner 启动性能分析（2026-07-23）

## 背景

通过带耗时统计的 Fabric Loader 本地 fork 发现，`ic2_120` 服务端 entrypoint 启动耗时是其他 mod 的 7-30 倍。冷启动 13438ms，热启动 7381ms。怀疑 ClassScanner 的反射式包扫描存在性能问题，因此展开本次调查。

这是一次独立的性能探索，结论对后续优化方向有参考价值，但**当前未落地任何代码变更**（所有实验已还原）。

## 扫描机制概览

`ClassScanner`（[ClassScanner.kt](/home/wangyu/server/develop/ic2-fabric/core/src/main/kotlin/ic2_120/registry/ClassScanner.kt)）有三个扫描入口：

1. `scanAndRegister`：扫描 6 个包（`tab`/`entity`/`effect`/`block`/`screen`/`item`），发现 `@ModBlock`/`@ModItem` 等注解类，收集后批量注册。
2. `collectMachineRecipeRegistrations`：扫描 `ic2_120.content.recipes` 包，找 `@ModMachineRecipe`。
3. `collectMachineRecipeBindings`：扫描 `ic2_120.content.block.machines` 包，找 `@ModMachineRecipeBinding`。

每个入口通过 `forEachClassInPackage` 枚举目录/jar/union 中的所有 `.class` 文件，对每个类执行 `Class.forName(className).kotlin`，然后 7 次 Kotlin `findAnnotation` 检查注解。

## 数据基线

### 扫描量

| 包 | `.class` 文件数 |
---|---|
| `tab` + `entity` + `effect` + `block` + `screen` + `item` | 1575 |
| `recipes` | 102 |
| `block.machines`（block 子包） | 233 |
| **合计** | **~1910** |

其中 963 个 class 文件是内部类（文件名含 `$`），但 `EnergyStorageBlockEntity.kt` 等文件中有**嵌套类各自带 `@ModBlockEntity` 注解**的情况，因此内部类不能简单跳过。

实际有 `@Mod*` 注解的类约 233 个，注册了 186 个方块、297 个物品、85 个方块实体、69 个 ScreenHandler、323 个配方生成器。

### 基线耗时（`./gradlew :core:runServer` 单次热启动）

| 入口 | scan | register | 合计 |
---|---|---|---|
| `scanAndRegister` | 1255ms | 2875ms | 4131ms |
| `collectMachineRecipeRegistrations` | — | — | 87ms |
| `collectMachineRecipeBindings` | — | — | 3ms |
| **总计** | | | **4221ms** |

## 初步分析（代码静态审查）

识别出 7 个潜在问题：

1. **`processClass` 使用 `Class.forName(className)`（initialize=true）**：1575 个类全部执行 `<clinit>`。
2. **每个类 7 次 Kotlin `findAnnotation`**：约 11,025 次注解查找，85% 返回 null。
3. **内部类没有短路**：963 个内部类被加载和反射，但后来发现有嵌套类带注解的情况。
4. **`block.machines` 子包被重复扫描**：主扫描和 binding 扫描覆盖同一批类。
5. **`collectMachineRecipeBindingFromClass` 也用 initialize=true**。
6. **`findAllMemberProperties` 调两次**：对每个 BlockEntity 分别找 `@RegisterEnergy` 和 `@RegisterItemStorage`。
7. **`collectRecipeGenerators` 对 323 个类做 companion 反射**。

## 实验

### 实验设置

在三个扫描方法中加临时 `System.nanoTime()` 计时探针，`./gradlew :core:build -x test` 后 `./gradlew :core:runServer` 跑完整启动流程，从日志提取 `[BENCH]` 行。每轮跑完发 `stop` 停服。

### 实验 v1：`initialize=false` + Java 注解 + 内部类短路

改动：
- `processClass`：`Class.forName(className, false, loader)` + `jClass.getAnnotation(...)` + `className.contains('$') return`
- `collectMachineRecipeBindingFromClass`：同样三改
- `collectMachineRecipeRegistrationsFromClass`：加内部类短路 + Java 注解（已有 initialize=false）
- `registerBlockEntities`：合并 `findAllMemberProperties` 为一遍遍历

结果：**服务器崩溃**。`MfsuBlockEntity`（`EnergyStorageBlockEntity.kt` 中的嵌套类，JVM 名含 `$`）被 `$` 短路跳过，BlockEntity 从 85 个降到 77 个。移除 `$` 短路后重新测试：

| 入口 | scan | register | 合计 |
---|---|---|---|
| `scanAndRegister` | 578ms (-54%) | 3787ms (+32%) | 4366ms |
| `collectMachineRecipeRegistrations` | — | — | 112ms |
| `collectMachineRecipeBindings` | — | — | 4ms |
| **总计** | | | **4482ms (+6%)** |

扫描阶段砍半，但 register 阶段反向增长，净效果反而更差。

### 实验 v2：`initialize=true` + Java 注解

改动：保留 Java `getAnnotation` 替代 Kotlin `findAnnotation`，但 `Class.forName` 改回 initialize=true。

| 入口 | scan | register | 合计 |
---|---|---|---|
| `scanAndRegister` | 717ms (-43%) | 3467ms (+21%) | 4185ms |
| `collectMachineRecipeRegistrations` | — | — | 99ms |
| `collectMachineRecipeBindings` | — | — | 3ms |
| **总计** | | | **4287ms (+1.6%)** |

仍在噪声范围内。

## 结论

**扫描阶段的优化有效（-43% 到 -54%），但 register 阶段会完全吃掉收益。** 三种变体的总计都在 4200-4500ms 区间内，差异在单次运行噪声范围内。

### 根因：kotlin-reflect JIT 预热效应

原始代码对 1575 个类全部调用 `.kotlin`（解析 Kotlin `@Metadata`、构建反射数据结构），虽然看起来浪费，但这给 JVM 的 JIT 编译器和 kotlin-reflect 内部缓存做了充分预热。后续 `createInstance()` 调用受益于这些热缓存。

跳过无注解类的 `.kotlin` 调用后，预热不足，233 个有注解类的反射操作（构造器查找、实例化）反而变慢，正好抵消了扫描阶段省下的时间。

### 时间分布拆解

ClassScanner 总计 ~4200ms 大致分配：

- **类加载 + 验证 + 链接**（`Class.forName`）：~700ms，对 1575 个类
- **Kotlin 元数据解析**（`.kotlin`）：~400ms
- **类初始化**（`<clinit>`）：~150ms（大部分类无重静态初始化）
- **7x findAnnotation**：~150ms
- **实际注册工作**（`createInstance` + `Registry.register` + creative tab + recipe generators）：~2800ms

实际注册工作占 67%，与 IC2 的内容量成正比（648 个注册类、323 个配方生成器），这部分是真正的成本中心，且不可被扫描优化削减。

### 对 entrypoint 的定位

Fabric Loader fork 数据显示 ic2_120 entrypoint 冷启动 13438ms、热启动 7381ms。ClassScanner 占 4200ms（57% 热 / 31% 冷）。即使完全消除扫描阶段，entrypoint 也只降到 ~7000ms（热）。真正的"7-30x"差距主要来自 IC2 的内容量（648 个注册类 vs 其他 mod 通常几十个）和后续的实际注册工作，而非扫描效率。

## 未落地原因

所有实验代码已 `git checkout` 还原。三种优化策略均无法突破 ~4200ms 的瓶颈，在反射式扫描架构下：

- `initialize=false`：把 `<clinit>` 成本从 scan 转移到 register，净效果更差。
- Java 注解替代 Kotlin `findAnnotation`：scan 省了 ~500ms，register 赔了 ~600ms。
- 内部类短路：正确性风险（嵌套类带注解），已放弃。

唯一保留的改动是 `findAllMemberProperties` 合并遍历（代码质量改进，性能影响可忽略），但为保持实验隔离也已一并还原。

## 如果后续要真正优化

在当前反射式扫描架构下，改 `initialize` 策略或注解查找方式都无法突破 ~4200ms 的下限。真正有效的路径：

1. **编译期生成注册索引**（KSP/annotation processor）：构建时扫描 `@ModBlock` 等注解，生成一个 `GeneratedRegistryIndex.kt`，列出所有需要注册的类。运行时直接遍历预生成列表，跳过 `Class.forName` + `.kotlin` + `findAnnotation` 整条扫描链。预计省掉整个 scan 阶段（~1250ms），register 不变。
2. **ASM 字节码扫描**：不加载类，直接用 ASM 读 `.class` 文件检查注解。比 `Class.forName` 快约 10 倍，但只省扫描阶段，register 不变。
3. 两者结合效果最好，但 register 阶段的 ~2800ms 是硬成本（与内容量成正比），无法通过扫描优化解决。
4. 更激进的方向是减少 `createInstance()` 的反射开销——例如 KSP 生成工厂函数替代 `KClass.createInstance()`，但这需要重构所有注册类。

**底线**：ClassScanner 不是 entrypoint 慢的主因。IC2 有 648 个注册类和 323 个配方生成器，内容量本身就是其他 mod 的数倍。要降低 entrypoint 时间，需要减少注册总量（拆分子模块/懒加载）或用编译期生成替代运行时反射，而不是优化扫描循环本身。
