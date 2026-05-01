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
./gradlew clean compileKotlin compileClientKotlin
```

- 运行 Gradle 时**不要**使用 `--no-daemon`（保持 daemon 以复用 JVM、加快增量构建）。

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

本项目维护 `main` 和 `1.21.1` 两个分支，通过 cherry-pick 保持同步。

- 同步状态表：`docs/branch-sync-status.md`
- **每次向 main 提交后，必须更新该文件**，记录新 commit 是否已同步到 1.21.1
- 记录格式：commit SHA、说明、是否已在 1.21.1、备注
- **分支同步跟踪文件的更新必须单独提交，不得 amend 到功能 commit 中**。amend 会改变 commit hash，导致跟踪表中的 SHA 失效

## 10. 物品与方块清单

所有使用 `@ModBlock` / `@ModItem` 注解注册的类及其中文翻译：

- 自动生成文档：`docs/item-block-list.md`（154 方块 + 293 物品）
- 生成脚本：`scripts/generate_item_block_list.py`
- 重新生成：运行 `python scripts/generate_item_block_list.py`
