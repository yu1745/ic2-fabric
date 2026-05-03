# 分支同步状态：main ↔ 1.21.1

最后更新：2026-05-03

分叉点：`c5e247c`

验证方法：对 main 上每个 commit 的代码改动在 1.21.1 worktree 中逐文件比对。

## 总览

从分叉点 `c5e247c` 到 main HEAD 共 **36 个 commit**，**36 个已同步，0 个待同步**。

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
| 9 | `549eeb8` | refactor: 固体装罐机 foodComponent 运行时兜底 | ✅ | 之前已存在（1.21.1 API 版） |
| 10 | `9c7dd2d` | feat: CfPack 建筑泡沫背包 | ✅ | |
| 11 | `48934bc` | docs: add 26.1 migration plan | ✅ | 已 cherry-pick |
| 12 | `19c16ee` | perf: Jar-in-Jar 内嵌 fabric-language-kotlin | ✅ | |
| 13 | `775100f` | chore: 删除 libs/ 目录 | ✅ | |
| 14 | `bf99891` | feat: advanced-weapons-addon 子项目 | ✅ | cherry-pick + API 适配 |
| 15 | `799fda9` | revert: 取消 Jar-in-Jar，恢复 libs/ | ✅ | 已 cherry-pick |
| 16 | `fe72f4e6` | feat: 高炉添加抽入/弹出升级，压缩机修复放电槽UI，电力加热机添加电池槽 | ✅ | cherry-pick，保留 1.21.1 `ExtendedScreenHandlerFactory<PacketByteBuf>` 泛型 |
| 17 | `6ae93daa` | feat: 能量水晶和蓝波顿水晶添加右键一键充电功能 | ✅ | cherry-pick（跳过 docs 文件） |
| 18 | `27f47004` | feat: 所有管道和泵附件tooltip显示流量（按秒计算） | ✅ | cherry-pick + 适配 1.21.1 `appendTooltip` 签名（移除 `world`、`@Environment`，改用 `TooltipType` + `Item.TooltipContext`） |
| 19 | `99e13e06` | fix: 管道tooltip使用国际化字符串 | ✅ | cherry-pick，在 1.21.1 适配基础上应用 `Text.translatable` |
| 20 | `e33e55fe` | feat: 区块加载器改为GUI控制区块加载范围 | ✅ | cherry-pick（跳过 docs/runClient.bat） |
| 21 | `211e8c08` | feat: 扳手右键改为直接设置方块朝向 | ✅ | cherry-pick（跳过 docs 文件） |
| 22 | `8bd5d4c` | fix: 量子剑创造栏图标改用 iconResource 避免显示耐久条 | ✅ | cherry-pick + API 适配修复（1.21.1 额外提交 `6d3c446`） |
| 23 | `165faaf` | perf: RadiationHandler 改用 world.players 遍历避免全量实体扫描导致的 CPU 满载 | ✅ | cherry-pick 确认 |
| 24 | `41fbef2` | fix: 修复牲畜监管和收割机合成配方 | ✅ | cherry-pick，1.21.1 额外排除 datagen 缓存文件 |
| 25 | `f984ec2` | fix: 流体装罐机配方改为空单元+电路板+机器外壳，FluidBottler 统一更名为 FluidCanner | ✅ | cherry-pick + 适配 1.21.1 API（`validateTicker`、`RecipeExporter`）+ datagen 刷新 |
| 26 | `2d14ffe` | feat: 更新分子重组仪默认配方——移除工业钻石/AE/铂/钛/镍/铬，新增萤石→阳光化合物、煤炭→钻石等配方 | ✅ | 无冲突直接 cherry-pick |
| 27 | `e394908` | feat: 添加 shockWhenNoEnergyFlow 配置项，修复空电网仍电人问题 | ✅ | 无冲突直接 cherry-pick |
| 28 | `f3fbc77` | fix: solid canner slot 1 左移，感应炉升级支持，命令显示物品 | ✅ | cherry-pick + 适配 1.21.1 `ExtendedScreenHandlerFactory<PacketByteBuf>` 泛型 |
| 29 | `3e9f9d0` | docs: 更新 AGENTS.md 源码参考章节——添加 MC 源码与 Fabric API 获取及参考方式 | ✅ | 无冲突直接 cherry-pick |
| 30 | `59e897e` | feat: JEI 配方显示(分子重组仪/复制机) + 配置全量分包同步 | ✅ | cherry-pick + 适配 1.21.1 API（Identifier.of、CustomPayload、getCustomData） |
| 31 | `1bc03c2` | refactor: 统一 Tooltip 格式(状态国际化+剩余时间) + 重构飞行消耗逻辑 | ✅ | cherry-pick + 适配同上 |
| 32 | `714524c` | feat: 扩展可拴绳实体类型（怪物/村民） | ✅ | 代码已存在于 1.21.1 |
| 33 | `8f4078e` | feat: 添加拴绳动能发生机 | ✅ | 代码已存在于 1.21.1，额外 API 适配提交 `6abb6e4` |
| 34 | `408c5a3` | chore: 更新 AGENTS.md 与 deploy 脚本 | ✅ | 代码已存在于 1.21.1 |
| 35 | `4f0aaad` | refactor: 注释核反应堆热模式检测的粒子效果代码 | ✅ | 无冲突直接 cherry-pick `d7cdc5c` |
| 36 | `f03e79f` | feat: 拴绳动能发生机合成配方改为中间机器外壳上方拴绳的3x3配方 | ✅ | cherry-pick `127fa4c` + 适配 1.21.1 recipe JSON 格式 + datagen 刷新 |
## 同步历史

- 2026-04-30：首次逐 commit 比对，确认 3 个缺失 commit：`48934bc`（docs）、`bf99891`（weapons-addon）、`799fda9`（revert）；`549eeb8` 代码已在 1.21.1 中存在
- 2026-04-30：补齐 3 个缺失 commit（cherry-pick），其中 `bf99891` 需适配 1.21.1 API（`QuantumSaber.kt`、`Identifier` 构造函数、`build.gradle` remapper 扩展）
- 2026-04-30：全部 15 个 commit 同步完成
- 2026-05-01：新增能量水晶/蓝波顿水晶右键一键充电功能
- 2026-05-01：新增管道tooltip显示流量功能（按秒计算，mB/s格式）
- 2026-05-01：修复管道tooltip使用国际化字符串
- 2026-05-01：区块加载器改为GUI控制区块加载范围（bitmask替代固定5×5）
- 2026-05-01：扳手右键改为直接设置方块朝向（替代循环旋转）
- 2026-05-01：量子剑创造栏图标改用 iconResource 避免显示耐久条
- 2026-05-01：修复量子剑在1.21.1的API适配编译错误（Identifier构造器、FabricItemSettings、NBT组件、AttributeModifiers等）
- 2026-05-01：逐 cherry-pick 同步 commits 16-21，适配 PipeBlocks.kt appendTooltip、BlastFurnaceBlockEntity 接口泛型到 1.21.1 API
- 2026-05-01：同步 commit 23——RadiationHandler 改用 world.players 遍历，修复 CPU 满载
- 2026-05-01：同步 commit 24——牲畜监管和收割机合成配方修复，cherry-pick 到 1.21.1 并排除 datagen 缓存文件
- 2026-05-02：同步 commit 25——流体装罐机配方 + FluidBottler→FluidCanner 更名，cherry-pick 到 1.21.1 + 适配 API + datagen 刷新
- 2026-05-02：提交 commit 26 并同步到 1.21.1——更新分子重组仪默认配方
- 2026-05-02：同步 commits 27-29 到 1.21.1——shockWhenNoEnergyFlow、感应炉升级支持、AGENTS.md 源码参考
- 2026-05-03：同步 commits 30-31 到 1.21.1——JEI 配方显示+配置全量分包同步、Tooltip 统一+飞行消耗重构，适配 1.21.1 API
- 2026-05-03：提交 commits 32-34（拴绳动能发生机相关）到 main，待同步到 1.21.1
- 2026-05-03：同步 commits 32-35 到 1.21.1——拴绳动能发生机 API 适配（RecipeExporter、validateTicker、canBeLeashed→canBeLeashed()、updateLeash→beforeLeashTick、Identifier.ofVanilla、RegistryWrapper.WrapperLookup、onBreak 返回 BlockState）+ datagen 刷新，1.21.1 额外提交 `6abb6e4`
- 2026-05-03：提交 commit 36 到 main——拴绳动能发生机合成配方从 1×2 改为 3×3（中间机械外壳+上方拴绳），cherry-pick 到 1.21.1 + datagen 刷新，同步完成
