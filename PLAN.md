# PLAN.MD — 1.21.1 分支重建（宏观路线）

> 微观进度见 `docs/PROGRESS.md`。本文件只放路线与决策，不放细节。

## 背景决策

- **1.20.1（main）归档为内容源；1.21.1 重新成为主线。**
- 旧 `backup/1.21.1` 分支**只作为 API 迁移范本**（yarn 时代的适配手法参考），不逐条 cherry-pick。
- **切换 Mojang mappings**（放弃 yarn），为 26.x 做准备；**仅 1.21.1 切**，1.20.1 已归档不动（只出 genSources 源码供查阅）。
- yarn↔mojang 转换表维护在 `docs/yarn-to-mojang-{version}.tsv`（生成脚本 `tools/gen-yarn-mojang-table.py`）。
- API 问题的查询入口：文件系统中的 `../mc_source_{version}/`（genSources 产物）与 `../fabric-api_{version}/`。

## 阶段

1. **三方合并**：main ⊕ backup/1.21.1（base=分叉点 c5e247c5），内容取 main、构建配置取 1.21.1、generated 丢弃待重生成。
2. **1.20.1→1.21.1 API 迁移**（yarn 名义下先完成机械部分：Identifier.of、Item.Settings、validateTicker 等）。
3. **切 Mojang mappings**：loom 配置 + 转换表全码库替换（类名 import 批量，方法/字段编译器驱动）。
4. **编译通过**：core + 全部 addon。
5. **datagen 重生成** + 清理 plural 目录 + 自检清单（cherry-pick 指南 §8）。
6. **源码基础设施**：两版本 `genSources` → `../mc_source_{version}/`；`../fabric-api_{version}/` 源码。
7. **AGENTS.md 重写**：大幅精简，唯一硬规则——API 问题直接看 `mc_source_{version}` / `fabric-api_{version}`。

## 完成标准

- `./gradlew build` 在 1.21.1-v2 全模块通过（Mojang mappings）。
- datagen 重跑通过，data 目录单数化，无 plural 残留。
- PLAN/PROGRESS/AGENTS/转换表就位。
