# TLM × IC2 动能扩展计划

为 Touhou Little Maid: Orihime 提供与 IC2 Fabric 动能发生机的协作能力，并补充 FTB Chunks 软依赖的权限校验。

## 总体目标

1. IC2 手摇动能发生机支持"外部实体驱动"（不再强制 PlayerEntity）
2. 新增 TLM 工作模式"右键手摇机"——女仆周期性驱动附近的手摇机
3. 女仆所有可能破坏方块或右键交互的工作模式统一加 FTB Chunks 权限校验
4. 女仆跨 chunk 移动加 FTB Chunks 进入校验，禁区块内 TP 回 owner 或静止

## 架构总览

```
ic2-fabric (自有, 分支 feat/external-kinetic-drive)
  └─ ManualKineticGeneratorBlockEntity 放宽驱动判定 + driveByExternal 公开 API

tlm-ic2-addon (新独立仓库)
  ├─ TlmIc2Extension       (little_maid_extension entrypoint)
  │   └─ addMaidTask(new CrankManualGenTask())
  ├─ TlmIc2Addon           (main entrypoint)
  │   └─ 软依赖检测 + PermissionManager 初始化
  ├─ task/CrankManualGenTask (IMaidTask 实现)
  ├─ permission/            (软依赖门面 + NoopChecker + FtbChunksCheckerImpl)
  └─ mixin/                 (8 个 mixin)
      ├─ EntityMaidMoveMixin           (customServerAiStep, 进入校验)
      ├─ EntityMaidDestroyBlockMixin   (destroyBlock 3 重载, 破坏校验)
      ├─ MaidShearTaskMixin            (start, 右键校验)
      ├─ MaidMilkTaskMixin
      ├─ MaidFeedAnimalTaskMixin
      ├─ MaidCollectHoneyTaskMixin
      └─ MaidStealEdibleUseTaskMixin
```

## 依赖关系

| 依赖 | 来源 | 类型 |
|------|------|------|
| ic2-fabric core | `com.github.yu1745:ic2-fabric:<tag>` (jitpack) | 硬依赖 (compile + runtime) |
| TLM-Orihime | `cn.sh1rocu:touhoulittlemaid-fabric-1.20.1:0.6.4-forge1.5.0` (Sh1roCu MCRepo) | 硬依赖 |
| FTB Chunks | modrinth maven | 软依赖 (compile, runtime 可选) |

### 关键 maven 坐标

**TLM-Orihime 官方 maven**:
- 仓库: `https://raw.githubusercontent.com/Sh1roCu/Sh1roCu_MCRepo/master/repository`
- artifactId: `touhoulittlemaid-fabric-1.20.1`
- 提供 `-shadow.jar` (dev) 和 `-sources.jar`

**ic2-fabric** (待配置):
- jitpack 仓库
- 需要在 ic2-fabric 加 `jitpack.yml` + 让 core 版本号支持环境变量注入

## JDK 版本

- **构建 JDK**: 21 (Fabric Loom 1.15+ 强制要求 JVM 21)
- **字节码 target**: 17 (`release = 17`, 与 MC 1.20.1 运行时一致)
- **jitpack.yml**: `openjdk21`
- **addon build.gradle**: 同 ic2-fabric (`release = 17`, `jvmTarget = 17`)

## mappings 选择

- ic2-fabric: yarn (`yarn_mappings=1.20.1+build.10`)
- TLM-Orihime: Mojang + parchment (其 dev jar 是 Mojang names)
- **addon 统一用 yarn** — Loom 会把 TLM 的 dev jar 自动 remap 到 addon 的 mappings

## ic2-fabric 侧改动 (本仓库)

### 1. ManualKineticGeneratorBlockEntity 放宽驱动判定

**文件**: `core/src/main/kotlin/ic2_120/content/block/machines/ManualKineticGeneratorBlockEntity.kt`

**当前状态**:
```kotlin
private val playerLastUseTick = mutableMapOf<PlayerEntity, Long>()

fun onUse(player: PlayerEntity, ...): ActionResult {
    // 空手 + 有摇把 → playerLastUseTick[player] = world.time
}

private fun isPlayerActivelyUsing(player: PlayerEntity): Boolean { ... }

fun tick(...) {
    val isTurning = playerLastUseTick.isNotEmpty() && hasCrank()
}
```

**目标状态**:
```kotlin
private val driverTick = mutableMapOf<Entity, Long>()  // Key 放宽为 Entity

// 玩家路径 (向后兼容, 不变)
fun onUse(player: PlayerEntity, ...): ActionResult {
    // 空手 + 有摇把 → driverTick[player] = world.time
}

// 新增: 供女仆等非玩家实体调用的公开 API
fun driveByExternal(driver: Entity) {
    val world = world ?: return
    driverTick[driver] = world.time
}

private fun isEntityActivelyUsing(entity: Entity): Boolean {
    val lastTick = driverTick[entity] ?: return false
    val world = world ?: return false
    if (world.time - lastTick > 6) return false
    val distance = entity.pos.distanceTo(Vec3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5))
    if (distance > MAX_USE_DISTANCE) return false
    return true
}

fun tick(...) {
    driverTick.entries.removeIf { (e, _) ->
        !e.isAlive || !isEntityActivelyUsing(e)
    }
    val isTurning = driverTick.isNotEmpty() && hasCrank()
    // 其余逻辑不变, sync 字段保持兼容
}
```

**影响范围**:
- 仅 `ManualKineticGeneratorBlockEntity.kt` 一个文件
- 玩家玩法 100% 向后兼容 (player 实例同时是 Entity, 自动走新 map)
- 摇把、距离、6-tick 超时等所有逻辑保留

## tlm-ic2-addon 侧设计 (新仓库)

### 工作模式: CrankManualGenTask

**ID**: `tlm_ic2:crank_manual_generator`
**图标**: 手摇机物品
**启用条件**: 女仆附近 6 格内存在已装摇把的手摇机
**Brain 任务组合**:
1. `MaidMoveToPredicateBlockTask`: 搜索附近手摇机 BlockEntity, 走过去
2. 自定义 Brain 节点 `DriveExternalTask`: 到达后每 tick 调用 `be.driveByExternal(maid)`
   - 每 4 tick 调一次 (避免刷 map), 满足 6-tick 超时窗口

### Mixin: 移动校验 (EntityMaidMoveMixin)

**目标**: `EntityMaid.customServerAiStep(ServerLevel)` 方法
**注入**: `@Inject(method = "customServerAiStep", at = @At("HEAD"), cancellable = true)`
**逻辑**:
```java
if (!this.level.isClientSide
        && !PermissionManager.canEnterChunk(self, self.blockPosition())) {
    LivingEntity owner = self.getOwner();
    if (owner != null && owner.isAlive() && !owner.level().equals(self.level())) {
        // owner 在同维度: TP 到 owner
        self.teleportToOwner(owner);
    }
    // owner 离线或跨维度: 完全静止
    ci.cancel();
}
```

**覆盖场景**:
- 女仆自主走入禁区块 → Brain tick 取消, 走不过去
- 被活塞/水流/末影珍珠推进禁区块 → 同上, owner 在线就 TP 走, 离线就静止
- 被跨维度传送 (硫磺草莓等) → owner 跨维度, 不 TP, 完全静止等救援

### Mixin: 破坏校验 (EntityMaidDestroyBlockMixin)

**目标**: `EntityMaid.destroyBlock` 的 3 个重载
**注入**: 各自 `@Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)`
**逻辑**:
```java
if (!PermissionManager.canBreakBlock(self, pos)) {
    ci.cancel();  // 或返回 false (取决于签名)
}
```

**覆盖内置任务**:
- TaskCocoa / TaskGrass / TaskMelon / TaskNormalFarm / TaskSnow / TaskSugarCane
- NetherWartCropHandler

### Mixin: 右键类任务校验 (5 个 TaskMixin)

**目标**: 5 个内置任务的 `start(ServerLevel, EntityMaid, long)` 方法
- `MaidShearTask.start`
- `MaidMilkTask.start`
- `MaidFeedAnimalTask.start`
- `MaidCollectHoneyTask.start`
- `MaidStealEdibleUseTask.start`

**注入**: 各自 `@Inject(method = "start", at = @At("HEAD"), cancellable = true)`
**逻辑**:
```java
if (!PermissionManager.canInteract(self, self.blockPosition())) {
    ci.cancel();
}
```

**注**: 用女仆当前位置作为校验位置 (因为所有内置任务都要求女仆贴近目标 ≤ 2 格, 与目标几乎同 chunk)。

### 软依赖架构

**门面**: `PermissionManager` (静态)
- `static IPermissionChecker checker = new NoopChecker();`
- 三个方法: `canEnterChunk / canBreakBlock / canInteract`

**接口**: `IPermissionChecker`
- `NoopChecker` 永远 true
- `FtbChunksCheckerImpl` (在 `compat.ftbchunks` 包内, 唯一引用 ftbchunks 类的位置)

**初始化**: `TlmIc2Addon.onInitialize()`
```java
if (FabricLoader.getInstance().isModLoaded("ftbchunks")) {
    PermissionManager.checker = new FtbChunksCheckerImpl();
}
```

**类隔离保证**: 所有 `dev.ftb.mods.ftbchunks.*` 引用严格限制在 `compat.ftbchunks.FtbChunksCheckerImpl` 一个文件。Mixin 代码只调 `PermissionManager.canXxx`, 永远不直接 import ftbchunks 类。ftbchunks 不在场时, `FtbChunksCheckerImpl` 永远不会被类加载, 不触发 NoClassDefFoundError。

### FTB Chunks 权限映射

| addon 接口 | FTB Chunks API | 用谁的身份 |
|-----------|----------------|-----------|
| `canEnterChunk(maid, pos)` | `BLOCK_ENTRY_MODE` (PrivacyMode) | owner UUID |
| `canBreakBlock(maid, pos)` | `BLOCK_EDIT_MODE` + `Protection.EDIT_BLOCK` | owner UUID |
| `canInteract(maid, pos)` | `BLOCK_INTERACT_MODE` + `Protection.INTERACT_BLOCK` | owner UUID |

owner 通过 `maid.getOwnerUUID()` 拿 (`EntityMaid extends TamableAnimal`)。

## 实施顺序

### 阶段 1: ic2-fabric 改动 (本分支 feat/external-kinetic-drive)
1. ✅ 写计划文档 (本文件)
2. ⏳ 改 `ManualKineticGeneratorBlockEntity.kt`
3. ⏳ 加 `jitpack.yml` (openjdk21)
4. ⏳ 让 core 版本支持 `JITPACK_VERSION` 环境变量注入
5. ⏳ 推送到 GitHub, 等 jitpack 第一次构建 green
6. ⏳ 部署到 fabric1.20.1-1 验证手摇机玩家玩法零回归

### 阶段 2: tlm-ic2-addon 项目搭建 (新仓库)
1. ⏳ 初始化 Gradle 项目 (build.gradle / settings.gradle / gradle.properties)
2. ⏳ 配置三个 maven 仓库 + 三个依赖
3. ⏳ fabric.mod.json (main + little_maid_extension 两个 entrypoint)
4. ⏳ tlm-ic2-addon.mixins.json (8 个 mixin 注册)

### 阶段 3: addon 功能实现
1. ⏳ `TlmIc2Extension` (ILittleMaid + addMaidTask)
2. ⏳ `CrankManualGenTask` (IMaidTask)
3. ⏳ `PermissionManager` + `IPermissionChecker` + `NoopChecker`
4. ⏳ `FtbChunksCheckerImpl` (软依赖)
5. ⏳ 8 个 mixin 类

### 阶段 4: 验证部署
1. ⏳ 构建出 addon jar
2. ⏳ 部署到 fabric1.20.1-1 (已有 TLM + 依赖齐全)
3. ⏳ 验证手摇机右键任务工作
4. ⏳ 验证禁区块内女仆被 TP / 静止
5. ⏳ 验证右键类任务在 claim 区被拦
6. ⏳ ftbchunks 不在场时 addon 正常工作 (NoopChecker)

## 设计风险

1. **mappings 不一致** — addon 用 yarn, TLM 用 Mojang. Loom 自动 remap, 但首次构建可能需要拉取 parchment metadata. 如果出问题, fallback 方案是 addon 也改用 Mojang mappings.
2. **mixins.json target 字符串** — 5 个内置任务 mixin 用完整类名字符串声明 target, 避免 compile-time 引用. 但运行期仍需要类存在.
3. **TLM 更新时 mixin 失效** — 5 个任务的 `start` 方法签名相对稳定 (基类 `Behavior.start` 定义), 但仍可能被重命名/重构. 加 try-catch 容错.
4. **owner 跨维度** — 用 `owner.level().equals(self.level())` 判断, 不同维度不 TP. 避免跨维度传送的女仆被强行拉回.

## 不在本计划内

- 畜力动能发生机的女仆绕圈走模式 (用户决定不做, 用真实拴绳 + 普通生物绕圈即可)
- 全局 mixin MC `Level.destroyBlock` (误伤其他 mod)
- 修改 FTB Chunks 源码 (用户决定 ftbchunks 保持原样, addon 单边适配)
