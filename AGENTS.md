# AGENTS.md - ic2_120 协作规范（重排版）

本文件是代理/协作者的最小执行规范。详细实现细节统一放在 `docs/`，避免重复维护。

## 1. 先看这里

- 文档总入口：`docs/README.md`
- 新机器实现：`docs/guides/machine-implementation-guide.md`
- 机器组合复用：`docs/guides/machine-composition-reuse.md`（减少 container/slot/sync 重复代码）
- 新物品实现：`docs/guides/item-implemented.md`
- 注解注册系统：`docs/registry/CLASS_BASED_REGISTRY.md`

## 2. 硬性约束

- 资源文件只能新增/修改在 `src/main/resources/assets/ic2_120/**`。
- `assets/ic2/**` 仅作为被引用上游资源，不可直接修改。
- 机器类改动后必须同时验证服务端与客户端编译，不接受只跑一侧。
- 涉及 Screen/ScreenHandler/SyncedData 的改动，必须检查属性顺序与同步链路一致。

## 3. 注册约定（摘要）

使用类级注解进行注册，不手写分散注册表逻辑：

- `@ModBlock`
- `@ModItem`
- `@ModBlockEntity`
- `@ModScreenHandler`
- `@ModScreen`
- `@ModCreativeTab`

主入口通过 `ClassScanner.scanAndRegister(...)` 扫描包。
注册与参数细节以 `docs/registry/CLASS_BASED_REGISTRY.md` 为准。

合成表同样遵循”基于约定的扫描”：

- 所有 `data/**/recipes/*.json` 合成表均由 Kotlin 代码导出（datagen），不手写、不直接维护 JSON。
- `ClassScanner.scanAndRegister(...)` 会收集 Block/Item companion 中签名为 `generateRecipes(Consumer<RecipeJsonProvider>)` 的方法。
- 统一由 `ClassScanner.generateAllRecipes(...)` 执行，不新增分散的手写配方注册入口。
- 约定与实现细节以 `docs/registry/CLASS_BASED_REGISTRY.md` 为准。
- **每个物品的合成配方必须写在对应物品类的 companion object 中，不得写在其他类里。**

## 4. 机器实现最小清单

1. Block + BlockEntity + Sync + ScreenHandler + Screen 成套落地。
2. 能量容器使用现有可升级容器与组件，不重复造轮子。
3. 升级支持至少覆盖：超频、变压、储能（按机器需求可裁剪）。
4. Shift 快速移动遵循 `SlotSpec + SlotMoveHelper` 规则。
5. 语言、模型、blockstates 在 `ic2_120` 命名空间补齐。

完整模板见 `docs/guides/machine-implementation-guide.md`。

## 5. 子系统文档位置

- 电网与能量流速：`docs/systems/energy-network.md`、`docs/systems/energy-flow-sync.md`
- 流体：`docs/systems/fluid-system.md`
- 热能：`docs/systems/heat-system.md`
- 动能传输：`docs/systems/kinetic-transmission.md`
- 核电：`docs/systems/nuclear-power.md`
- 升级：`docs/systems/upgrade-system.md`
- 同步：`docs/systems/sync-system.md`
- 声音：`docs/systems/sound-system.md`
- 配置系统：`docs/systems/config-system.md`
- 作物系统：`docs/systems/crop-growth-requirements.md`、`docs/systems/crop-hybrid-system.md`
- 橡胶树世界生成：`docs/systems/rubber-tree-worldgen.md`
- JEI 集成：`docs/systems/jei-integration.md`

## 6. UI 文档位置

- Compose UI 总览：`docs/ui/compose-ui.md`
- 槽位规则：`docs/ui/slot-spec-system.md`
- DrawContext 参考：`docs/ui/drawcontext-methods.md`
- 坐标换算：`docs/ui/canner-ui-coordinates.md`
- Compose 子文档：`docs/compose-ui/*.md`
- 踩坑记录：`docs/pitfalls/common-pitfalls.md`

## 7. 提交前验证

推荐最小命令：

```bash
./gradlew build
```

- 运行 Gradle 时**不要**使用 `--no-daemon`（保持 daemon 以复用 JVM、加快增量构建）。
- `markDirty()` 仅标记区块需要落盘保存，**不会**触发网络包同步到客户端。同步到客户端需要调用 `world.updateListeners(pos, state, state, NOTIFY_LISTENERS)` + `chunkManager.markForUpdate(pos)`（参考 `markDirtyAndSync()` 模式）。

若只改文档，可跳过编译；若改 Kotlin/资源/注册链路，不可跳过。

- 若遇到 Gradle lock（如 `gradle-*.zip.lck`）导致构建或 datagen 失败，直接删除对应 `.lck` 文件后重试：
  - Linux: `rm -f ~/.gradle/wrapper/dists/gradle-*.zip.lck`
  - Windows: `Remove-Item -Force "$env:USERPROFILE\.gradle\wrapper\dists\...\gradle-*.zip.lck" -ErrorAction SilentlyContinue`

## 8. 变更策略

- 优先复用已有组件与模式，避免引入第二套实现。
- 优先使用机器组合模式复用（见 `docs/guides/machine-composition-reuse.md`）以避免重复的 container/slot/sync 代码。
- 发现规则冲突时：以 `docs/README.md` 导航到对应主文档修正，而不是在 AGENTS.md 堆细节。
- 新增规范时，优先写入对应 `docs/{guides|systems|ui|registry}`，AGENTS.md 只保留摘要与入口。

## 9. 分支同步管理

本项目维护 `main`（1.20.1）和 `1.21.1` 两个分支，commit 必须双向同步。

### 9.1 提交 → 同步的完整流程（严格按顺序执行）

1. 在 main 上完成功能开发，`git commit`（一次性干净提交，禁止修修补补的 fixup commit）
2. 立即记录同步状态：在 `docs/branch-sync-status.md` 末尾添加新条目，需标注 ❌ （未同步）
3. `git commit` 同步状态更新（**单独提交，不与功能 commit 混在一起**）
4. 切换到 1.21.1：`git checkout 1.21.1`
5. Cherry-pick：`git cherry-pick <功能 commit 的 SHA>`
6. 解决冲突后，**必须跑 datagen（全项目）**：`./gradlew runDatagen`
7. `git add -A && git commit`（提交 datagen 刷新结果）
8. 切回 main：`git checkout main`
9. 更新同步状态：将之前标记 ❌ 的条目改为 ✅，确保总览计数正确
10. `git commit` 同步状态更新（**单独提交**）

### 9.2 同步状态表

- 文件：`docs/branch-sync-status.md`
- 记录格式：commit SHA、说明、是否已在 1.21.1（✅/❌）、备注
- **总览计数必须与逐 commit 清单保持一致**（示例：共 25 个 commit，24 个已同步，1 个待同步 → 不要写成"全部已同步"）

### 9.3 Cherry-pick 注意事项

1. 1.21.1 的 data 目录使用单数（`advancement/`、`recipe/`、`loot_table/`），main 使用复数（`advancements/`、`recipes/`、`loot_tables/`）
2. 1.21.1 API 签名差异：`BlockEntityTicker` 用 `validateTicker`（main 用 `checkType`），`RecipeExporter`（main 用 `Consumer<RecipeJsonProvider>`）
3. `assets/ic2/**` 为上游引用不可修改；`assets/ic2_120/**` 可修改

## 10. 命名约定

- 中文名 → registry key：在 `zh_cn.json` 中搜索中文名，找到 `"block.ic2_120.<key>"` 或 `"item.ic2_120.<key>"`，key 即 registry name
- 类名 = registry name 的 PascalCase + 类型后缀（`Block`/`Item`/`BlockEntity`/`ScreenHandler`/`Screen`/`Sync`）
- 同一功能的 Block、BlockEntity、ScreenHandler、Screen、Sync 类名必须使用一致的命名前缀

## 11. 常见操作检查清单

### 修改合成配方后
1. 修改 companion object 中 `@RecipeProvider` 方法
2. 运行 `./gradlew :core:runDatagen`
3. 构建 `./gradlew build`

### 重命名（注册名 + 类名）
1. `grep -r` 完整搜索旧名（区分大小写：`FluidBottler` / `fluid_bottler` / `FLUID_BOTTLER`）
2. `git mv` 移动所有源文件（保持 rename 跟踪）
3. 全局替换内容（可用 `sed -i`）
4. 更新 lang 文件（zh_cn.json / en_us.json 中的 key）
5. 运行 datagen 重新生成 data 文件
6. 构建验证

### 提交前确认
- 只有一个功能 commit + 一个同步状态 commit，没有 fixup/amend

## 12. 物品与方块清单

所有使用 `@ModBlock` / `@ModItem` 注解注册的类及其中文翻译：

- 自动生成文档：`docs/item-block-list.md`（154 方块 + 293 物品）
- 生成脚本：`scripts/generate_item_block_list.py`
- 重新生成：运行 `python scripts/generate_item_block_list.py`

## 13. 源码参考（MC 源码 & Fabric API）

### MC 源码

使用 Gradle 任务生成脱编译源码（只需在 core 执行，附属与 core 同 MC 版本）：

```bash
./gradlew :core:genSources
```

- 产物在项目根目录的 `.gradle/loom-cache/minecraftMaven/net/minecraft/` 下，`{hash}` 为自动生成的随机字符串，如：
  `minecraft-clientOnly-{hash}/{minecraft_version}-net.fabricmc.yarn.{yarn_mappings}-v2/`
- `../mc_source_{minecraft_version}/` 是手动解压的副本（项目根目录父级，`{minecraft_version}` 替换为 `gradle.properties` 中的 `minecraft_version`）。
- loom-cache 中路径包含版本后缀（如 `1.20.1-net.fabricmc.yarn.1_20_1.1.20.1+build.10-v2`），搜索时需要用通配符或占位符匹配版本号。
- 如果源码已生成过可直接参考，不需要重新生成。

### Fabric API 源码

Fabric API 项目通过 `git clone` 获取，按 MC 版本后缀区分目录：

```bash
# 1.20.1
git clone -b 1.20.1 https://github.com/FabricMC/fabric-api.git fabric-api_1.20.1

# 1.21.1
git clone -b 1.21.1 https://github.com/FabricMC/fabric-api.git fabric-api_1.21.1
```

- 如果已经存在可直接参考，不存在则重新 clone 并签出对应分支。
- 如果被删除或需要特定版本，重新 clone 并签出对应分支即可。
- Gradle dependency 版本（如 `fabric_api_version=0.92.7+1.20.1`）用于协调 API 模块版本，源码参考不受限制。
