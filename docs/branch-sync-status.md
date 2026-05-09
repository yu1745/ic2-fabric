# 分支同步状态：main ↔ 1.21.1

最后更新：2026-05-09

分叉点：`c5e247c`

验证方法：对 main 上每个 commit 的代码改动在 1.21.1 worktree 中逐文件比对。

## 总览

从分叉点 `c5e247c` 到 main HEAD 共 **67 个 commit**，**67 个已同步，0 个待同步**。
（归档 1-37 → `branch-sync-archive.md`）

## 逐 commit 状态

| # | Commit | 说明 | 在 1.21.1？ | 备注 |
|---|--------|------|:-----------:|------|
| 47 | `1656996e` | fix: 修正量子/纳米护甲减伤值为IC2经典值，工具提示改为可翻译文本 | ✅ | cherry-pick + 适配 1.21.1 appendTooltip 签名 |
| 48 | `4b692f98` | feat: 添加 explodeWhenNoEnergyFlow 配置项，无能量时不触发超压爆炸 | ✅ | 无冲突直接 cherry-pick |
| 49 | `b708df3e` | feat: 半流体发电机燃料颜色网络同步 + 流体颜色注册表 | ✅ | cherry-pick + 适配 1.21.1 Payload 网络 |
| 50 | `0d23511f` | feat: 物质生成机移除超频支持，消耗速度按可用能量比例无上限 | ✅ | cherry-pick + 1.21.1 API 适配（ExtendedScreenHandlerFactory 泛型）|
| 51 | `cedc65d1` | feat: 传送卷轴+传送机玩家文档+NBT动态模型切换 | ✅ | cherry-pick + 适配 1.21.1（DataComponent NBT、TooltipType、Identifier.of、RecipeExporter）|
| 52 | `8f064e2d` | fix: 泡沫枪独立键位，不与 M 键及其他装备冲突 | ✅ | cherry-pick + 适配 1.21.1 Payload 网络 |
| 53 | `e303132b` | refactor: 提取公共 config sync 分包组包/发送逻辑到 core，消除附属重复代码 | ✅ | cherry-pick + 适配 1.21.1 CustomPayload API（保留 AddonConfigSyncPacket），main 的 ConfigSyncHelper 不适用于 1.21.1 已移除 |
| 54 | `dc4425d2` | fix: 采矿镭射枪散射模式改为平行散布，修复弹体/BER渲染距离限制 | ✅ | cherry-pick + 适配 1.21.1 API（`canUsePortals(allowVehicles)`），BER 自动合并 |
| 55 | `f9f399fc` | feat: 合成表调整 & 量子太阳能发电机添加4充电槽 | ✅ | cherry-pick + 适配 1.21.1 API（getScreenOpeningData、RegistryWrapper、validateTicker、RecipeExporter）+ datagen 刷新 |
| 56 | `54beaf9f` | feat: 采矿镭射枪配置化+扳手支持变压器六面朝向 | ✅ | |
| 57 | `48bbc126` | feat: 扫描仪GUI范围控制+能耗按体积动态计算+4列网格结果展示 | ✅ | |
| 58 | `10a8b66d` | feat: 核反应堆布局锁定与漏斗堆叠绕过修复 | ✅ | |
| 59 | `3508fb3b` | feat: 扳手支持拆解储物箱——左键瞬间拆解保留物品 | ✅ | 无冲突直接 cherry-pick |
| 60 | `20d2b0c7` | fix: 采矿镭射枪贴脸使用时发射弹射体而非执行实体交互 | ✅ | 无冲突直接 cherry-pick |
| 61 | `964989b` | fix: FTB Chunks 领地保护检查始终失效——Protection 是接口不是枚举 | ✅ | 无冲突直接 cherry-pick |
| 62 | `9673112a` | feat: ModMenu + Cloth Config GUI 配置编辑界面 | ✅ | cherry-pick + 适配 1.21.1 版本号（Cloth Config 15.0.140, ModMenu 11.0.4）|
| 63 | `84e84c79` | fix: 变压器升压/降压文本互换+ScreenHandler六面朝向修复 | ✅ | 无冲突直接 cherry-pick |
| 64 | `207f0c3d` | feat: 新增 AdjacentEnergyTransferComponent 统一贴脸能量传输 | ✅ | 无冲突直接 cherry-pick |
| 65 | `9cfc66eb` | feat: 全量迁移剩余 26 个机器到 AdjacentEnergyTransferComponent | ✅ | 无冲突直接 cherry-pick |
| 66 | `7bcc2a27` | feat: 所有发电机添加 AdjacentEnergyTransferComponent 支持贴脸能量传输 | ✅ | 无冲突直接 cherry-pick |
| 67 | `e5be3a10` | feat: 飞行改为双击空格触发，移除独立的 Alt+F / Alt+M 按键开关 | ✅ | cherry-pick + 适配 1.21.1 API（Payload 网络、DataComponent NBT）+ datagen 刷新 |

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
- 2026-05-05：同步 commits 39-43——配方删除、铁炉/焦窑防刷、储罐防刷流体、量子护腿神行+量子靴子大跳+胸甲飞行修复、JEI 缺失保护，适配 1.21.1 Payload 网络/DataComponent NBT/appendTooltip 签名
- 2026-05-05：同步 commit 44——datagen 改用 `fabric-api.datagen.modid` 过滤，附属运行 datagen 不再触发本体 entrypoint；同时运行附属 datagen 首次生成高级太阳能合成表 JSON；移除本体已废弃的 `skip.ic2_120.datagen` 代码
- 2026-05-06：批量同步 commits 47-52——护甲减伤/explodeWhenNoEnergyFlow/半流体燃料颜色同步/物质生成机移除超频/传送卷轴/泡沫枪独立键位，适配 1.21.1 API（DataComponent NBT、CustomPayload 网络、TooltipType、Identifier.of、RecipeExporter）+ datagen 刷新
- 2026-05-06：同步 commit 53——提取公共 config sync 分包组包逻辑到 core（ChunkedConfigReceiver），附属删除 AddonConfigSyncReceiver，保留 AddonConfigSyncPacket（1.21.1 需要 CustomPayload.Id），主要变化为 refactor 无 API 适配
- 2026-05-08：同步 commit 59——扳手支持拆解储物箱，无冲突直接 cherry-pick |
- 2026-05-09：同步 commit 67——飞行改为双击空格触发，cherry-pick + 适配 1.21.1 API（Payload 网络、DataComponent NBT）+ datagen 刷新 |
