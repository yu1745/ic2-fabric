# 分支同步状态：main ↔ 1.21.1

最后更新：2026-05-16

分叉点：`c5e247c`

验证方法：对 main 上每个 commit 的代码改动在 1.21.1 worktree 中逐文件比对。

> 迁移参考：`docs/migration-1.20.1-to-1.21.1.md` — 汇总了全部 125 个 commit 中出现的 API 变化及对应代码示例。

## 总览

从分叉点 `c5e247c` 到 main HEAD 共 **128 个 commit**，**127 个已同步，1 个待同步**。
（归档 1-104 → `branch-sync-archive.md`）

## 逐 commit 状态

| # | Commit | 说明 | 在 1.21.1？ | 备注 |
|---|--------|------|:-----------:|------|

| 105 | `260a09d2` | merge: refactor: 特殊机器B迁移到 RoutedItemStorage 驱动 quickMove | ✅ | cherry-pick + 解决冲突（PatternStorage/Pump/Replicator/SolarDistiller）|
| 106 | `da4bb903` | merge: Unit 8 BE — 为缺少 RoutedItemStorage 的 BlockEntity 新增存储 | ✅ | cherry-pick + 解决冲突 + 1.21.1 API 适配 |
| 107 | `c136631b` | refactor: 完成所有机器的 ScreenHandler 迁移到 RoutedItemStorage 驱动 | ✅ | cherry-pick + 解决冲突 + 1.21.1 API 适配 |
| 108 | `20b714a5` | refactor: 清除旧 API，迁移附属机器到 RoutedItemStorage | ✅ | cherry-pick + SlotMoveHelper 冲突 + ItemStack.areEqual 适配 |
| 109 | `cd5e7cdc` | merge: 全机器 RoutedItemStorage 驱动 quickMove 迁移完成 | ✅ | cherry-pick -m 1 + 1.21.1 API 适配（Identifier.of/ExtendedScreenHandlerFactory/Inventories/RecipeInput/DataComponentTypes.FOOD）+ datagen 刷新 |
| 110 | `3bbfa45b` | fix: 洗矿机输入槽改用配方表查询，修复沙砾无法放入 | ✅ | cherry-pick 无冲突 |
| 111 | `44865f50` | feat: 流体热交换机 UI 三栏布局优化，增加 MetalFormer ScreenFactory | ✅ | cherry-pick 无冲突 |
| 112 | `83305a9c` | fix: 橡胶树叶加入 minecraft:leaves tag | ✅ | cherry-pick + 适配 1.21.1 tags/block/ 路径 |
| 113 | `1c4cddfc` | fix: 修复压缩机/离心机/切石机输入槽无法放入需要多个原料的配方物品 | ✅ | cherry-pick + 适配 1.21.1 API（RecipeInput→SingleStackRecipeInput）|
| 114 | `ec799f6e` | fix: 复制机/模板存储模板选择按钮因 ButtonClickC2SPacket byte 溢出失效 | ✅ | cherry-pick + 适配 1.21.1 CustomPayload C2S（SelectTemplatePayload）|
| 115 | `c35cb357` | fix: 修正能量水晶粉合成配方为4钻石粉+5红石粉 | ✅ | cherry-pick 无冲突 |
| 116 | `f7b604d9` | feat: 绝缘导线递进合成/剪刀剥离配方，添加耐压等级 tooltip | ✅ | cherry-pick + 适配 1.21.1 API（RecipeExporter、Identifier.of、CustomPayload C2S）+ datagen 刷新 |
| 117 | `d80abc0d` | feat: 电路板合成改用铁板，粘性树脂+活塞合成粘性活塞 | ✅ | cherry-pick + 适配 1.21.1 路径（recipe/ 单数）+ datagen 刷新 |
| 118 | `f82870b4` | feat: EU分流导线（红石开关）+ EU限流导线（GUI限流） | ✅ | cherry-pick + 适配 1.21.1 API（onUse 无 Hand、ExtendedScreenHandlerFactory<PacketByteBuf>/getScreenOpeningData、RecipeExporter）+ data 目录单数 + datagen 刷新 |
| 119 | `06f74979` | feat: 蒸汽系统——蒸汽/过热蒸汽流体、蒸汽发生器/蒸汽动能发生机/冷凝器 | ✅ | cherry-pick + 解决多处冲突（FluidVariant CODEC、ExtendedScreenHandlerFactory、OnUse 无 Hand、validateTicker）+ 适配 1.21.1 API + datagen 刷新 |
| 120 | `37af24e4` | feat: 压缩机配方补全+容器返还支持 | ✅ | cherry-pick + 解决冲突（containerReturn 适配 codec/packetCodec）+ 适配 1.21.1 API（ItemStack.fromNbt/encode）+ datagen 刷新 |
| 121 | `5f6df96f` | feat: 流体管道系统重构——simulateInsertion + 升级解耦 + 泵附件作唯一 provider | ✅ | cherry-pick + 解决冲突（FluidPipeUpgradeComponent DataComponent 适配）|
| 122 | `6f2633f3` | fix: BER 光照硬编码 MAX_LIGHT_COORDINATE，改用环境光照 | ✅ | cherry-pick 无冲突 |
| 123 | `152ca7ab` | feat: 多台机器GUI从ComposeUI改为传统纹理渲染 | ✅ | cherry-pick + Screen conflict 解决（取 HEAD 版本）|
| 124 | `33a52f58` | chore: 移除 .deploy-enabled 引用（文件已删除） | ✅ | cherry-pick 无冲突 |
| 125 | `7f74eecc` | feat: 风力计配方+粗矿块反向分解+发电机燃料过滤修复 | ✅ | cherry-pick + 适配 1.21.1 RecipeExporter/CraftingRecipeCategory（tools→equipment）+ datagen 刷新 |
| 126 | `4ee9b03e` | feat: 核爆炸对齐 IC2 原版——自定义射线投射+分tick方块摧毁 | ❌ | 待同步 |

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
- 2026-05-14：同步 commit 87——ModMenu 泥炭矿配置界面（cherry-pick 无冲突）|
- 2026-05-14：同步 commits 90-92——流体/物品升级 GUI 配置 + Canner 储罐分离 + 配方检测性能优化（cherry-pick + 适配 1.21.1 API + datagen 刷新）|
- 2026-05-14：同步 commits 93-96——发酵机肥料 + 蜘蛛眼配方 + 监管机 SingleVariantStorage 重构 + 鸡下蛋 Mixin（cherry-pick + 适配 1.21.1 API + Mixin 改用 @Inject + datagen 全量重跑）|
- 2026-05-14：修复 datagen 串 JSON 问题——core 和 weapons-addon 添加 modid 过滤，全部 generated 删除后逐子项目重跑（main 和 1.21.1 同步修复）|
- 2026-05-15：同步 commits 98-112——全机器 RoutedItemStorage 驱动 quickMove 迁移 + 洗矿机修复 + 流体热交换机 UI + 橡胶树叶 tag。批量 cherry-pick + 解决多处冲突 + 1.21.1 API 适配（Identifier.of、ExtendedScreenHandlerFactory\<PacketByteBuf\>、getScreenOpeningData、readNbt/writeNbt WrapperLookup、Inventories、ItemStack.areEqual、RecipeInput、DataComponentTypes.FOOD）+ datagen 刷新 |
- 2026-05-16：同步 commits 113-117——压缩机输入槽修复 + 复制机 CustomPayload 迁移 + 能量水晶粉 + 绝缘导线/耐压 tooltip + 电路板/粘性活塞。cherry-pick + 适配 1.21.1 API（SelectTemplatePayload、SingleStackRecipeInput、RecipeExporter、Identifier.of）+ datagen 刷新 |
- 2026-05-18：批量同步 commits 119-124——蒸汽系统 + 压缩机配方容器返还 + 流体管道重构 + BER 光照 + GUI 传统纹理 + 移除 deploy-enabled。cherry-pick + 解决多处冲突 + 适配 1.21.1 API（FluidVariant CODEC、ExtendedScreenHandlerFactory getScreenOpeningData、validateTicker、ItemStack.fromNbt/encode、onUse 无 Hand、appendTooltip TooltipType）+ datagen 刷新 |
