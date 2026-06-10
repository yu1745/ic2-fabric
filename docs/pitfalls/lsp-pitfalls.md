# LSP 调用踩坑记录

opencode 通过 JetBrains 官方 Kotlin LSP（`com.jetbrains.ls.kotlinLsp.KotlinLspServerKt`，版本 `2.4.0-dev-3554`，K2 分析器）提供代码智能。调用 `lsp` 工具时遇到的实际坑点如下。

## 1. `goToImplementation` 光标必须落在标识符**首字符之后**

- 现象：
  - 光标停在标识符第 1 个字符（`char == 起始偏移`）时，**全部返回空**。
  - 移到第 2-3 个字符（`char >= 起始偏移 + 2`）立刻返回完整实现列表。
- 示例：
  - `interface ITiered {` 的 `I` 在 `char=10`。`char=10` → 空；`char=12` → 14 个实现。
  - `interface ITieredMachine {` 的 `I` 在 `char=10`。`char=10` → 空；`char=17` → 100+ 个实现。
  - `sealed class Position` 的 `P` 在 `char=12`。`char=12` → 空；`char=14` → 2 个子类。
- 推测根因：
  - K2 分析器对 `CharPosition.ON_KEYWORD` 与 `CharPosition.ON_REFERENCE` 的判定边界刚好把首字符划到 keyword 区。
  - 不是光标精度问题，是 LSP 内部对位置的离散化。
- 修复方式：
  - 调 `lsp` 工具时**手动把 `character` 偏移 +2 到 +5** 即可，不要用"看上去对"的第一个字符。
  - 不要因为一次空返回就判定"该 LSP 不支持该能力"，先换偏移再测。

## 2. `prepareCallHierarchy` / `incomingCalls` / `outgoingCalls` 在本环境**未实现**

- 现象：
  - 多个文件、多种符号类型、多种字符偏移（共 8+ 次）测试，**全部返回空**。
  - 即使接口/方法明确存在多个调用方/被调用方，也拿不到 `CallHierarchyItem`。
- 根因（已翻 jar 确认）：
  - LSP 协议层 jar `fleet.lsp.protocol.jar` 中：
    - ✅ `CallHierarchyClientCapabilities`（客户端能力类）
    - ❌ `CallHierarchyItem` / `CallHierarchyPrepareParams` / `CallHierarchyIncomingCallsParams` / `CallHierarchyOutgoingCallsParams` / `CallHierarchyRequests`（服务端必需，**全部缺失**）
  - 对比 `TypeHierarchy*` 系列**全部齐全**（`TypeHierarchyItem` / `TypeHierarchyPrepareParams` / `TypeHierarchyRequests` 等），说明是选择性实现。
  - IDE 内部 `intellij.kotlin.codeInsight.jar` 里有 `KotlinCallHierarchyProvider` / `KotlinCallHierarchyBrowser`，但**未挂到 LSP 协议处理器**。
- 排查路径（已验证可用）：
  - 进程确认：`ps aux | grep kotlin-ls` 看到 `com.jetbrains.ls.kotlinLsp.KotlinLspServerKt --stdio`，是 JB 官方 LSP。
  - 协议能力确认：`unzip -l ~/.cache/opencode/bin/kotlin-ls/lib/fleet.lsp.protocol.jar | grep -iE "CallHierarchy"` 仅返回 client capabilities。
- 结论：
  - 这是 **JetBrains 官方 Kotlin LSP 当前快照的能力缺口**，不是位置/配置问题。
  - 不要花时间换字符偏移，浪费时间。
  - 如必须用调用层级分析（call hierarchy），需要等 JB 后续版本或回退到 `fwcd/kotlin-language-server`（同样不完全支持）。
- 替代方案：
  - 改用 `findReferences` 反向查找"被哪些位置引用"，再人工筛选调用点。
  - 改用 `grep` / `ripgrep` 在源码里搜方法名。

## 3. 9 种 LSP 操作的实测可用性

| 操作 | 状态 | 备注 |
|------|------|------|
| `documentSymbol` | ✅ | 列出文件内所有符号（含嵌套类、属性、构造函数） |
| `workspaceSymbol` | ✅ | 空查询返回全工作区；精确查询能力有限（Kotlin LSP 模糊匹配较弱） |
| `goToDefinition` | ✅ | 正常工作 |
| `findReferences` | ✅ | 跨模块、跨子项目都能找到（实测找到 6+ 处跨 addon 引用） |
| `hover` | ✅ | 返回 KDoc + 完整函数签名（markdown 格式） |
| `goToImplementation` | ✅ | **需 `char >= 标识符起始偏移 + 2`**（见第 1 条） |
| `prepareCallHierarchy` | ❌ | 协议层未实现（见第 2 条） |
| `incomingCalls` | ❌ | 依赖 `prepareCallHierarchy` |
| `outgoingCalls` | ❌ | 依赖 `prepareCallHierarchy` |

## 4. 调 `lsp` 工具的通用建议

1. **先看 `documentSymbol` 摸清结构**，再决定用哪个 navigation 操作。
2. **`goToDefinition` / `hover` / `findReferences`** 是最稳的三个，先用它们。
3. **`goToImplementation` 必加偏移补偿**（参考第 1 条）。
4. **遇到空返回时先换偏移**，连续 3 个不同偏移都空再判定为能力缺失。
5. **判定为能力缺失前先确认 LSP 进程身份**（`ps aux | grep kotlin-ls`），避免冤枉官方 LSP。
6. **判定为协议层能力缺失时翻 jar 验证**（`unzip -l <jar> | grep <Keyword>`），不要靠经验猜。
