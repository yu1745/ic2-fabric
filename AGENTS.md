# AGENTS.md / CLAUDE.md — ic2_120 协作规范

本文件通过符号链接同时作为 `AGENTS.md` 和 `CLAUDE.md` 使用。

## 1. 唯一硬规则：API 问题看本地源码

遇到任何 Minecraft / Fabric API 的问题（类名、方法签名、继承链、事件、网络包），**直接查文件系统中的本地源码，不要猜、不要靠记忆**：

- MC 反编译源码（按版本）：`../mc_source_{version}/`
  - 当前开发线：`../mc_source_1.21.1/`（Mojang 官方名）
  - 1.20.1 归档参考：`../mc_source_1.20.1/`（yarn 名）
- Fabric API 源码：`../fabric-api_{version}/`
  - `../fabric-api_1.21.1/`、`../fabric-api_1.20.1/`（注意：fabric-api 仓库源码是 yarn 名，经 loom remap 编译，类名行为一致但方法名要换算）
- 需要重新生成 MC 源码时：`sh gradlew :core:genSources`，产物在 `.gradle/loom-cache/minecraftMaven/` 下提取。

## 2. 项目状态

- **当前主线：1.21.1（分支 `1.21.1-v2`），Mojang mappings。** 宏观路线见 `PLAN.md`，微观进度见 `docs/PROGRESS.md`。
- 1.20.1（`main` 分支）已归档，只读参考，不再开发。
- 旧 `backup/1.21.1` 分支只是当年 yarn 时代的迁移范本，已无内容价值。
- yarn↔mojang 名字对照表：`docs/yarn-to-mojang-1.21.1.tsv`（生成：`tools/gen-yarn-mojang-table.py`；全库替换工具：`tools/yarn2mojang.py`）。

## 3. 硬性约束

- 资源文件只能动 `assets/ic2_120/**`；`assets/ic2/**` 是上游引用，不可修改。
- 机器类（Block/BlockEntity/ScreenHandler/Screen/Sync）改动必须服务端+客户端都编译过。
- Screen/ScreenHandler/SyncedData 改动必须检查属性顺序与同步链路一致。
- `mc_source_1.21.1` 里查不到的东西（fabric 专属 API）才去 `fabric-api_1.21.1` 查。

## 4. 构建与验证

```bash
sh gradlew build              # 全模块编译（gradle 用 sh 调用或加执行权限）
sh gradlew :core:runDatagen   # 配方/数据变更后必须重跑，data json 是生成物不手写
sh gradlew :core:validateGuidebook  # guidebook 文档校验
```

- 不要用 `--no-daemon`。
- `data/**/recipes/*.json` 全部由 datagen 生成；配方写在对应物品类 companion 的 `generateRecipes(exporter)`（1.21.1 是 `RecipeOutput`），`@RecipeProvider` 注解，`ClassScanner` 统一收集。
- `markDirty()` 不触发网络同步；同步用 `markDirtyAndSync()` 模式。

## 5. 注册与结构约定

- 注解注册：`@ModBlock` / `@ModItem` / `@ModBlockEntity` / `@ModScreenHandler` / `@ModScreen` / `@ModCreativeTab`，入口 `ClassScanner.scanAndRegister(...)`。
- 类名 = registry key 的 PascalCase + 类型后缀；同一机器的 Block/BE/Handler/Screen/Sync 用一致前缀。
- 中文名查 key：`zh_cn.json` 里搜中文名得 `"block.ic2_120.<key>"`。
- 模块：`core` + `advanced-solar-addon` + `advanced-weapons-addon` + `buildcraft-addon` + `industrial-upgrade-addon` + `addon-template`（新附属复制模板）。

## 6. 已知坑（1.21.1 mojmap 特有）

- Kotlin 调用 mojmap 的 `is(...)` 方法要反引号：``stack.`is`(item)``。
- `ExtendedScreenHandlerFactory<FriendlyByteBuf>` 必须带泛型，方法是 `getScreenOpeningData(player)` 返回 buf（不是 write）。
- C2S/S2C 网络包：`CustomPacketPayload` + `Type<>` + `PacketCodec`/`StreamCodec`，`override fun type()`；接收 lambda 单参 `(payload, context)`，不要包 `server.execute {}`。
- BE 序列化：`saveAdditional(tag, lookup)` / `loadAdditional(tag, lookup)`，`Inventories`→`ContainerHelper.saveAllItems/loadAllItems`（都带 lookup）。
- Item 耐久/堆叠走 `Item.Properties().maxDamage()/stacksTo()`；`maxCountPerStack`→`Item.ABSOLUTE_MAX_STACK_SIZE`。
- 高频 yarn 风格属性靠 `ic2_120/compat/YarnStyleCompat.kt` 垫片，新代码请直接用 mojmap 名。
- gradle lock 报错删 `~/.gradle/wrapper/dists/gradle-*.zip.lck`。

## 7. 详细文档

实现细节文档在 `docs/`（系统、UI、registry、pitfalls 子目录），`docs/PROGRESS.md` 记录迁移进度，`PLAN.md` 是宏观路线。文档主索引以本文件为唯一入口。
