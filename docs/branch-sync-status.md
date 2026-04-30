# 分支同步状态：main ↔ 1.21.1

最后更新：2026-04-30

分叉点：`c5e247c`

验证方法：对 main 上每个 commit 的代码改动在 1.21.1 worktree 中逐文件比对。

## 总览

从分叉点 `c5e247c` 到 main HEAD 共 **15 个 commit**，其中 **12 个已实际同步**（cherry-pick + 适配），**3 个未同步**。

## 逐 commit 状态

| # | Commit | 说明 | 在 1.21.1？ | 备注 |
|---|--------|------|:-----------:|------|
| 1 | `b80fd0d` | feat: add mc1.20.1 suffix to release jar | ✅ | 适配为 `mc1.21.1` |
| 2 | `ab1d698` | fix: FluidStorage insert 拒绝 blank variant | ✅ | cherry-pick 确认 |
| 3 | `34448f9` | refactor: 电网直通模型，取消池缓冲 | ✅ | cherry-pick 确认 |
| 4 | `455f77f` | fix: 回滚导线容量，放开多入口注入 | ✅ | cherry-pick 确认 |
| 5 | `8b5df13` | fix: Transaction.openNested 避免嵌套异常 | ✅ | |
| 6 | `921e2d2` | refactor: advanced-solar-addon 改为子项目 | ✅ | |
| 7 | `c705674` | refactor: 根项目改为纯父项目，core 为子项目 | ✅ | |
| 8 | `ee91d69` | fix: 多子项目 CI 产物筛选与收集 | ✅ | |
| 9 | `549eeb8` | refactor: 固体装罐机 foodComponent 运行时兜底 | ❌ | **未同步** |
| 10 | `9c7dd2d` | feat: CfPack 建筑泡沫背包 | ✅ | |
| 11 | `48934bc` | docs: add 26.1 migration plan | ❌ | 纯文档，未同步 |
| 12 | `19c16ee` | perf: Jar-in-Jar 内嵌 fabric-language-kotlin | ✅ | |
| 13 | `775100f` | chore: 删除 libs/ 目录 | ✅ | |
| 14 | `bf99891` | feat: advanced-weapons-addon 子项目 | ❌ | **未同步** |
| 15 | `799fda9` | revert: 取消 Jar-in-Jar，恢复 libs/ | ✅ | 已 cherry-pick |

## 同步历史

- 2026-04-30：首次逐 commit 比对并补齐 3 个缺失 commit（`549eeb8` 原已存在、`48934bc` 和 `bf99891` 和 `799fda9` 手动 cherry-pick）
- 2026-04-30：修复 `bf99891` cherry-pick 后的 1.21.1 API 兼容问题（`QuantumSaber.kt` + `Identifier` 构造函数 + `build.gradle` remapper 扩展）并 push
