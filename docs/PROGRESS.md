# PROGRESS — 1.21.1 重建微观进度

## 已完成

- worktree `../ic2-fabric-1.21.1`，分支 `1.21.1-v2`（自 main c1e9a966 重建）。
- 三方合并 backup/1.21.1（cb351059）：gradle/fabric.mod.json/mixins 取 1.21.1，settings.gradle 补回 industrial-upgrade-addon / addon-template 模块。
- 冲突标记事故修复（0a941dea）：1149 文件恢复 main 侧内容；assets 全取 main（1.20.1→1.21.1 资产格式不变，main 是更新的 3D 模型内容）。
- 机械 sed 第一轮：`Identifier(` → `Identifier.of(`、`FabricItemSettings()` → `Item.Settings()`、`checkType(` → `validateTicker(world, `、`RecipeJsonProvider` → `RecipeExporter`、`RecipeCategory.TOOLS` → `EQUIPMENT`、`.success(x, true)` → `.success(x)`。
- loom 切 `loom.officialMojangMappings()`（1.21.1-v2），gradle 配置解析通过。
- fabric-api 源码已 clone：`../fabric-api_1.20.1/`、`../fabric-api_1.21.1/`。
- PLAN.md、本文件、`tools/gen-yarn-mojang-table.py` 建立。

## 进行中

- 转换表生成脚本调试（yarn tiny v2 解析 bug：namespace 列偏移）。

## 待办（微观）

- [ ] 生成 `docs/yarn-to-mojang-1.21.1.tsv`（类/方法/字段三段）。
- [ ] 全码库 yarn→mojang 类名替换（import 驱动）+ 方法名编译器驱动修正。
- [ ] datagen 自定义 `RecipeJsonProvider` 包装类 → 1.21.1 `exporter.accept(id, recipe, null)`（9 个 Datagen 文件 + industrial-upgrade 1 个，范本 backup 分支同路径文件可直接对照）。
- [ ] readNbt/writeNbt 加 `RegistryWrapper.WrapperLookup`（编译器找点）。
- [ ] CustomPayload/PacketCodec 全部网络包重写（§3.3 模板）。
- [ ] onUse 拆 onUseWithItem、appendTooltip 签名、getCodec/MapCodec、ItemStack.fromNbt lookup、FluidVariant CODEC。
- [ ] 耐久度等 Item.Settings 侧检查（maxDamage 走 Settings，1.20.5+ 组件化已由 Settings 封装）。
- [ ] `./gradlew build` 全模块通过。
- [ ] datagen 重生成 + 删 plural 旧目录（`recipes/`, `advancements/`, `loot_tables/`, `tags/items|blocks/`）。
- [ ] 自检清单（cherry-pick 指南 §8：冲突标记、旧 API 残留 grep）。
- [ ] main（1.20.1）worktree 同样切 mojmap + 转换 + genSources → `../mc_source_1.20.1/`。
- [ ] 1.21.1 genSources → `../mc_source_1.21.1/`。
- [ ] AGENTS.md 重写（大幅精简）。

## 踩坑记录（本轮新增）

- 三方合并 `git checkout --ours/--theirs` 对 **rename/rename 冲突**会报 "does not have our version"，且部分文件 checkout 失败后 `git add` 会把**带冲突标记的工作区内容**直接staged——事后必须全仓 `grep '<<<<<<<'` 复查。
- main 已重构为多子项目（core/ 前缀），backup/1.21.1 还是根项目 src/ 布局；rename 检测把大部分路径对上了，generated 目录的 rename/rename 需删除解决。
- 1.21.1 资产 JSON 格式与 1.20.1 兼容（item model 大改是 1.21.4），资产冲突一律取 main。
- loom 1.15 `officialMojangMappings()` 与 yarn 缓存可共存，切换后 `dependencies` 任务即可触发 mojmap 下载。
- yarn mappings.tiny v2 列布局：`c <official> <intermediary> <named>`、`f/m <desc> <official> <intermediary> <named>`。
