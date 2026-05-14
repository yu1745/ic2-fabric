# 分支同步状态：main ↔ 1.21.1

最后更新：2026-05-14

分叉点：`c5e247c`

验证方法：对 main 上每个 commit 的代码改动在 1.21.1 worktree 中逐文件比对。

## 总览

从分叉点 `c5e247c` 到 main HEAD 共 **87 个 commit**，**86 个已同步，1 个待同步**。
（归档 1-49, 50-62 → `branch-sync-archive.md`）

## 逐 commit 状态

| # | Commit | 说明 | 在 1.21.1？ | 备注 |
|---|--------|------|:-----------:|------|
| 50 | `207f0c3d` | feat: 新增 AdjacentEnergyTransferComponent 统一贴脸能量传输 | ✅ | 无冲突直接 cherry-pick |
| 51 | `9cfc66eb` | feat: 全量迁移剩余 26 个机器到 AdjacentEnergyTransferComponent | ✅ | 无冲突直接 cherry-pick |
| 52 | `7bcc2a27` | feat: 所有发电机添加 AdjacentEnergyTransferComponent 支持贴脸能量传输 | ✅ | 无冲突直接 cherry-pick |
| 53 | `e5be3a10` | feat: 飞行改为双击空格触发，移除独立的 Alt+F / Alt+M 按键开关 | ✅ | cherry-pick + 适配 1.21.1 API（Payload 网络、DataComponent NBT）+ datagen 刷新 |
| 54 | `c7a3ae42` | fix: 固体装罐机食物无法放入 + JEI 显示与实际消耗数量不匹配 | ✅ | cherry-pick + 适配 1.21.1 API（DataComponentTypes.FOOD、nutrition）|
| 55 | `3534ffc5` | fix: 流体热力发电机无限免费燃料——tryInsertFuel 在空间不足 1B 时仍注入 | ✅ | 无冲突直接 cherry-pick |
| 56 | `10ca9f12` | feat: 高级太阳能附属机器 Screen 左侧添加发电/输出/耗能状态渲染 | ✅ | 已在 1.21.1 存在（`182ac446`） |
| 57 | `953ba56e` | feat: 添加 enableOvervoltageExplosion 配置项，支持完全关闭超压爆炸 | ✅ | 无冲突直接 cherry-pick |
| 58 | `dd0b29d9` | fix: 修复平面照明器东西方向碰撞箱互换的问题 | ✅ | 无冲突直接 cherry-pick |
| 59 | `66edf7da` | feat: 紫外线灯（UV Lamp）实现——加速作物生长+Jade剩余时间修正 | ✅ | cherry-pick + 适配 1.21.1 API（validateTicker、RecipeExporter、getCodec、getScreenOpeningData、NBT registryLookup、onUse/onUseWithItem 拆分）|
| 60 | `c4379ed2` | fix: 修复紫外线灯模型形状——EAST/WEST 碰撞箱互换 + blockstates 上下旋转角度修正 | ✅ | cherry-pick + 适配 1.21.1 data 目录单复数变化 |
| 61 | `07dda56e` | fix: 拦截 attachLeash 中的 stopRiding，防止区块重载时被拴生物从矿车掉出 | ✅ | cherry-pick + 适配 1.21.1（getHoldingEntity → getLeashHolder）|
| 62 | `c735f845` | fix: 修复紫外线灯上下方向渲染反置——blockstates up/down x-rotation 对齐 ic2_120 日光灯 | ✅ | 无冲突直接 cherry-pick |
| 63 | `354c59e5` | feat: 剪刀机制 + 监管机额外产物 + 耐久条渲染修复 | ✅ | cherry-pick + 解决冲突（import、tooltip 参数适配、TODO.md）|
| 64 | `c127a41b` | fix: 修复所有机器 GUI 耐久条被 SlotAnchor 背景覆盖的渲染顺序问题——ui.render 在 super.render 之前执行 | ✅ | 无冲突直接 cherry-pick |
| 65 | `b246fc9c` | feat: 简化动能发电机配方——不再需要机器外壳 | ✅ | 无冲突直接 cherry-pick |
| 66 | `bd94c51d` | feat: 牲畜监管机机制重做 + 屠宰机掉落补充 | ✅ | 无冲突直接 cherry-pick |
| 67 | `5f1ffaa3` | chore: 更新TODO + 日光灯缓存改为100EU防频闪 | ✅ | 无冲突直接 cherry-pick |
| 68 | `da2bc1b1` | fix: 量子护腿神行移动检测改用 blockPos 坐标对比 + 量子胸甲入水不取消飞行 | ✅ | cherry-pick + 解决冲突（TODO.md release350 保留、QuantumLeggings.kt velocity→blockPos）|
| 69 | `ae483fd2` | fix: 打粉机 inputCount 未生效——一律按1个消耗，改为按配方数量消耗 | ✅ | cherry-pick + 适配 1.21.1 API（codec/packetCodec、RecipeInput、datagen inputCount 写入）+ datagen 刷新 |
| 70 | `717ebaae` | feat: 焦炉——窑口添加朝向、screen显示剩余时间、blockstate旋转修正 | ✅ | cherry-pick + 解决 CokeKilnBlocks.kt import 冲突 + datagen 刷新 |
| 71 | `0c935b69` | fix: 发酵机UI溢出容器修复，半流质发电机UI高度调整 | ✅ | 无冲突直接 cherry-pick + datagen 刷新 |
| 72 | `9186d55c` | feat: 添加泥炭矿及其处理配方、tooltip、世界生成与着色器 | ✅ | cherry-pick + 适配 1.21.1 API（Identifier.of、ItemTooltipCallback 4参数、PeatOreBlock::class.item()）+ datagen 刷新 |
| 87 | `223a745e` | fix: ModMenu 配置界面添加泥炭矿世界生成配置项 | ❌ | 补上遗漏的 peatOreConfig UI，cherry-pick 无冲突 |

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
- 2026-05-10：同步 commit 68——固体装罐机食物无法放入修复，cherry-pick + 适配 1.21.1 API（DataComponentTypes.FOOD、nutrition）+ datagen 刷新 |
- 2026-05-12：同步 commit 69——流体热力发电机无限免费燃料修复，无冲突直接 cherry-pick |
- 2026-05-12：同步 commits 72-73——平面照明器碰撞箱修复（无冲突）+ 紫外线灯（cherry-pick + 适配 1.21.1 API + datagen 刷新）|
- 2026-05-12：同步 commit 74——紫外线灯模型形状修复（cherry-pick + 适配 1.21.1 data 目录单复数）|
- 2026-05-13：同步 commits 77-78——剪刀机制 + 监管机额外产物 + 耐久条渲染修复（cherry-pick + 冲突解决）+ 全机器 GUI 耐久条渲染顺序修复 |
- 2026-05-14：同步 commits 80-82——牲畜监管机重做 + TODO/日光灯缓存 + 量子护腿 blockPos 移动检测（cherry-pick + 解决 QuantumLeggings.kt 冲突）|
- 2026-05-14：同步 commit 83——打粉机 inputCount 修复（cherry-pick + 适配 1.21.1 API + datagen 刷新）|
- 2026-05-14：同步 commits 84-86（70-72）——焦炉窑口 + 发酵机UI + 泥炭矿（cherry-pick + 适配 1.21.1 API Identifier.of/ItemTooltipCallback + datagen 刷新）|
