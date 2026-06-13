# mcdebug 测试编写指南

`mcdebug` 是一个 localhost JSON-RPC 服务（绑定 127.0.0.1:25580），让我们用 Kotlin/TypeScript 远程驱动一个跑着的 Fabric 1.20.1 客户端，给 `ic2_120` 的方块实体做端到端断言。本指南是踩过 59 个测试后归纳的实战经验，不是 API reference——查具体方法看 `mcdebug` 项目自身。

## 1. 跑测试

```bash
./gradlew :core:mcdebugTest
```

会做这些事：
1. 启 `gradle :core:runServer` 子进程，dev-launcher 把 `core/build/classes/kotlin/main` 直接注入 classpath（**不要**把 jar 拷到 `run/mods/`，会和注入冲突）
2. 等 `<core>/run/mcdebug/port` 文件出现（最多 90s）
3. 调 `mcdebug.runAllTests` RPC，并行执行所有 `McDebugTest` 实现
4. 强制 tear down server

每个 test 在自己的 world region 跑（有 force-load chunk 保护）。

## 2. Test 生命周期与基础设施

### 2.1 注册测试

3 步：

1. 写 `object XxxTest : McDebugTest { override val name; override fun run() }` —— 必须是 Kotlin `object`（无参 ctor）
2. 在对应方块类上贴 `@McDebugTestPackages("ic2_120.tests.<machine>")` 注解
3. 放 `tests/<machine>/XxxTests.kt` 下，**包名必须跟注解一致**

### 2.2 共享工具（看 `tests/TestBase.kt`）

```kotlin
val TEST_POS: Pos get() = McDebugTestApi.testOrigin
fun testPos(dx, dy, dz): Pos = McDebugTestApi.pos(...)
fun invItemEquals(pos, slot, itemId): String   // wait.until predicate
fun invCountLessThan(pos, slot, count): String
fun beFieldGreaterThan(pos, path, value): String
```

### 2.3 McDebugTestApi 关键方法

| 方法 | 用途 | 备注 |
|------|------|------|
| `place(pos, blockId)` | 直接 set block | 同步产生 BE |
| `setBlocks(ops: List<SetBlockOp>)` | 批量 set | 一次 RPC，多块 |
| `setBeField(pos, path, value)` | 写 BE 字段 | `setBeField(pos, "EnergyStored", 40000)` |
| `setBeField(pos, "Mode", 0/1/2)` | 切 mode | NBT int |
| `insertItem(pos, id, count, slot)` | 路由到第一个匹配 slot | RoutedItemStorage 决定 slot |
| `setSlot(pos, slot, id, count, nbt?)` | 强制写 slot | 跳过 routing |
| `getSlot(pos, slot)` / `getBeNbt` | 读 | |
| `assertBlockId` / `assertSlotHas` / `assertBeField` | 断言，失败抛 AssertionError | |
| `waitUntil(predicate, timeoutTicks)` | 被动等 | 走 `ServerTickEvents.END_SERVER_TICK` |
| `fluidInfo/get/insert/extract(pos, ...)` | FluidStorage API | 81 000 droplets = 1 bucket |

predicate 是 DSL 表达式：
```
inv[x,y,z].<slot>.item == "minecraft:stone"
inv[x,y,z].<slot>.count < 10
be[x,y,z].EnergyStored > 0
block[x,y,z].id == "minecraft:air"
tick > 200
```

### 2.4 给机器放电源的 3 步

```kotlin
McDebugTestApi.setBlocks(listOf(
    SetBlockOp(BATBOX_POS, "ic2_120:batbox", stateProps = mapOf("facing" to "west"))
))
McDebugTestApi.setBeField(BATBOX_POS, "EnergyStored", 40000)
McDebugTestApi.place(TEST_POS, "ic2_120:macerator")
```

`facing=west` 是储电盒的输出面，朝向 machine 才能用 AdjacentEnergyTransferComponent 传电。不设 facing 默认 north，机器放错方向时能量过不来。

## 3. 标准测试矩阵（7 个测最常见）

| # | 名字 | 测什么 | 关键 API |
|---|------|--------|----------|
| 1 | `place` | 方块能放、id 对 | `assertBlockId` |
| 2 | `<机器>:<典型 recipe>` | 主流程：放原料 → 等 output | `insertItem` + `waitUntil` + `assertSlotHas` |
| 3 | `<机器>:N×→1×:tag_ingredient` | tag 配方 | 同上，用 tag 涵盖的输入 |
| 4 | `<机器>:1×→N×:<...>` | 增产配方 | 用 9-stack 输入 |
| 5 | `<机器>:no_power:idle` | 没电源时不消耗 | `place` 后不设 BatBox，wait 200 ticks |
| 6 | `<机器>:invalid_input:<dirt>` | 无配方输入不触发 | 用 minecraft:dirt |
| 7 | `<机器>:output_full:blocks_next` | output 满时不消耗下一个 | `setSlot(slot, 64)` 预填 |

- **不要每个 recipe 都测**。同类（同输入数、同模式）测一个代表。
- **有 bug 的 recipe 必测**（比如 macerator N→1 配方 `copyWithCount(1)` 的 bug 必须在 test 里盯住，否则会被静默回归）。
- **没 special edge case** 的机器（已经测过 place + 1 个 recipe）就这 5-7 个够了。

## 4. 配方时间 vs 15s 硬约束

`waitUntil` 默认 timeout=60s，但**项目规范**是 ≤ 15s（parallel load 下时间会涨，30s+ timeout 经常误判通过）。计算公式：

```
recipe_ticks = PROGRESS_MAX / speed_multiplier
recipe_wall_s = recipe_ticks / 20 + setup_overhead (~3-5s)
```

`PROGRESS_MAX` 在 `<Machine>Sync.kt` 里的常量。`speed_multiplier` 默认 1，每个 overclocker × (1/0.7) ≈ 1.43×。

| PROGRESS_MAX | base time | 1 overclocker | 2 overclocker |
|--------------|-----------|---------------|---------------|
| 200 (metal_former) | 10s | 7s | 5s |
| 400 (macerator) | 20s | 14s | 10s |
| 450 (block_cutter) | 22.5s | 16s | 11s |
| 500 (ore_washer) | 25s | 18s | 12.5s |

实测 1 overclocker 不够的（实测墙钟 = recipe + 5s setup overhead = 接近 20s，会超 15s）。**PROGRESS_MAX ≥ 400 一律 2 overclocker**。`< 400` 不需要。

### 4.1 Overclocker 的隐藏限制：machine tier

每个 overclocker 同时把 `energyMultiplier` 抬 `1.6×`（不是 1.43×）。机器的能量消耗 = `ENERGY_PER_TICK * energyMultiplier`，不能超过机器 tier 的 `maxInsert`。

| tier | name | max EU/t |
|------|------|----------|
| 1 | batbox | 32 |
| 2 | cesu | 128 |
| 3 | mfe | 512 |
| 4 | mfsu | 2048 |

**例子**（踩过）：OreWashingPlant tier 1, ENERGY_PER_TICK=16, 2 overclocker = 16 × 1.6² = **41 EU/t > 32 EU/t** → BatBox 限速 → recipe 不前进 → wait.until timeout。

**修法**：加 1 个 transformer upgrade（slot 任意 4-7，**注意 OreWasher slot 7 = SLOT_UPGRADE_0**），把 `voltageTierBonus += 1`，把 max 抬到 128 EU/t。**电源也必须换**成 CESU/MFE/MFSU。

**完整 setup**：
```kotlin
McDebugTestApi.setBlocks(listOf(
    SetBlockOp(CESU_POS, "ic2_120:cesu", stateProps = mapOf("facing" to "west"))
))
McDebugTestApi.setBeField(CESU_POS, "EnergyStored", 40000)
McDebugTestApi.place(TEST_POS, "ic2_120:ore_washing_plant")
McDebugTestApi.insertItem(TEST_POS, "ic2_120:transformer_upgrade", count = 1, slot = 7)
McDebugTestApi.insertItem(TEST_POS, "ic2_120:overclocker_upgrade", count = 2, slot = 8)
```

## 5. Slot 布局速查

| 机器 | input | output | discharging | upgrades |
|------|-------|--------|-------------|----------|
| macerator | 0 | 1 | 2 | 3-6 |
| compressor | 0 | 1 | 2 | 3-6 |
| extractor | 0 | 1 | 2 | 3-6 |
| electric_furnace | 0 | 1 | 2 | 3-6 |
| recycler | 0 | 1 | 2 | 3-6 |
| metal_former | 0 | 1 | 2 | 3-6 |
| block_cutter | 1 (slot 0 = blade) | 3 | 2 | 4-7 |
| ore_washing_plant | 0/1 (ore/water) | 2/3/4 (multi) | 6 | 7-10 |
| blast_furnace | 0 (input) | 1 (output) + heat-input | 4 | 5-8 |

`SLOT_INPUT_N` / `SLOT_OUTPUT_N` / `SLOT_UPGRADE_0` 等常量在每个 BlockEntity 的 `companion object`。

**OreWasher multi-output 顺序 = recipe 声明顺序**，不是 SLOT_OUTPUT_N 名字顺序（datagen 写 `purified, small_dust, stone_dust` → slot 2/3/4 = 同序）。

## 6. 流体机器的 assert 模式

mcdebug 通过 Fabric Transfer API 的 FluidStorage 注入流体：

```kotlin
val r = McDebugTestApi.fluidInsert(TEST_POS, "minecraft:water", 81_000L)  // 1 bucket
require(r.transferred == 81_000L) { "tank full?" }
```

注意：
- 不需要准备 water_cell/water_bucket item，也不需要处理 slot 5 的空桶
- FluidStorage 是 side-filtered（OreWasher 的 `getFluidStorageForSide` 排除了"前面"），但 `fluid.insert(..., side=null)` 走 CombinedStorage 仍能通
- `fluid.extract(pos, amount)` 可以 drain 测 no_water 边缘

## 7. 经典坑点（按踩过顺序）

### 7.1 BatBox 默认 facing 让能量过不来

不显式设 `facing="west"`，放出的 BatBox 默认朝 north。如果 machine 放它 north 边，AdjacentEnergyTransferComponent 走的是 machine 自己看其他方块的 `getStrongRedstonePower` / `getEnergy`，**实际走 AdjacentEnergyTransferComponent.tick 的 per-side**。BatBox 没设 facing 时输出面错。**总写 `mapOf("facing" to "west")`** 强制朝 west，靠东边放 machine。

### 7.2 N→1 配方需要 stack.count 检查

`SimpleInventory(input)` 在 stack.count > 1 时，`matches()` 里 `input.count < inputCount` 会失败。Macerator 早期版本用 `copyWithCount(1)` 强制只匹配 1 个，导致 N→1 配方（melon_slice→bio_chaff 等）永不触发。**写 test 时用 N→1 配方**（`8×→1×`）盯这个 bug，否则回归悄无声息。

### 7.3 `waitUntil` 的 timeout 实际是 ticks

`timeoutTicks = 15 * 20` = 15 秒，**不是 15 ticks**。`tick > 200` 这种 200 也是 ticks。**别把 ticks 写错数量级**。

### 7.4 `assertSlotHas` 立即 fail 当 chunk unload

机器方块在 force-load chunk 里，理论上不会 unload。但**跨测并行**时前一个测试的 `cleanupArea` 调 `setBlocks(air)` 撞到相邻 chunk 边界会刷掉**部分方块**。`setupOreWasher` 这种"先 place machine，wait 200 ticks"的测试如果 BE 在 wait 期间丢了，**assert 会抛 "no block entity at BlockPos"**。修法：要么缩短 wait（200 ticks 是 10s 已经够测 idle），要么把 idle 测改用 `tick > 100` 这种更短。

### 7.5 `mode: 0/1/2` 是 NBT int 不是 enum

MetalFormer 的 mode 存 NBT `Mode` int。`setBeField("Mode", 0)` 改成 ROLLING。**不要写 enum.toString()**，会序列化失败。

### 7.6 `place` 同步 setBlock，但 setBlockState 触发 `setBlock` callback 在 tick 0

setup 里 `place → insertItem` 立即连续调，BE 已经在了 insertItem 能正确路由 slot。**但 `assertBlockId` 在 place 之后立即调会过**，因为 `world.getBlockState` 直接读 chunk state。如果想测 place 的副作用（比如 generate 旁边方块），等至少 1 tick。

### 7.7 `insertItem` 走 RoutedItemStorage 自动选 slot

`insertItem(pos, item, count, slot=null)` 不指定 slot 时根据 `ItemInsertRoute` 路由。overclocker 找 `SLOT_UPGRADE_INDICES` 第一个空位。**指定 slot 时强制放那个 slot**——如果 slot 已有不兼容物品（比如有 iron_block_cutting_blade 在 slot 0，强行 insertItem item=overclocker slot=0）会 throw "stack in slot 0 is not a valid IUpgradeItem"。所以 setup helper 里**显式指定 slot 是好习惯**，避免 routing 变位。

### 7.8 Recipe 输入 N 是 count 不是 type

1× clay_ball → clay（compressor 4×→1×）实际是 `IngredientInput.item(Items.CLAY_BALL)` + `inputCount=4`。**insertItem count=4 + 1 个 recipe cycle**。但 macerator 的 tag_ingredient 是 1×→N×。**别用 `count=2` 跑 tag_ingredient 单测**——它会先抽 1 个跑完，再抽下一个 1 个，count 永远不归零。

### 7.9 `tick > 200` 的 200 ticks 在 200ms 内不返回

`wait.until` 注册 `ServerTickEvents.END_SERVER_TICK` 回调，**回调是异步的**。200 ticks 需要 ~10 秒实际墙钟。**别用 `tick > 10` 当 idle 测 timeout**——跑完了但 tick 还没到 10。**用 `tick > 200` (10s 墙钟) 或更长**。

## 8. CI 集成

mcdebugTest 在 Gradle 里是 `mcdebugTest` task（JVM unit test 旁路）。`./gradlew :core:mcdebugTest` 单跑这一项。

PR CI 应该跑：
```bash
./gradlew :core:build :core:mcdebugTest
```

不跑 `:core:test`（testClasses 是 NO-SOURCE——我们没用 JUnit）。

## 9. 调试技巧

- **server 启动失败**：`run/logs/latest.log` 看 mcdebug port 有没有出现。loom 注入失败 → `core/build/classes/kotlin/main` 路径错了。
- **测试卡 setup**：在 `setupXxx()` 末尾加 `McDebugTestApi.assertBeField(TEST_POS, "EnergyStored", 40000)` 之类 sanity check 验证配置生效。
- **recipe 没触发**：看 `tick.progress` 字段（如果 sync 暴露了）。macerator 暴露了 `sync.progress` NBT path，可以 `assertBeFieldGreaterThan("progress", 0)` 验证 tick 在跑。
- **overvoltage 没炸**：macerator 的 `enableOvervoltageExplosion` 默认 true，但需要 MFSU `facing=west` 直接相邻，BatBox/CESU 能量级别不够触发超压。

## 10. 提交前检查

- [ ] 新机器 test 文件里 import `com.mcdebug.test.{McDebugTest, McDebugTestApi, SetBlockOp, Pos}` 4 个
- [ ] 在 Block 类加 `@McDebugTestPackages("ic2_120.tests.<machine>")`
- [ ] 跑一次 `./gradlew :core:mcdebugTest`，新测试全过，总数 +N
- [ ] 没有任何 `waitUntil` 超 15s timeout
- [ ] 实际 recipe 时间 < 12.5s（留 2.5s 缓冲给 setup overhead）
- [ ] 不改 `core/src/main/resources/assets/ic2_120/` 之外的资源文件
