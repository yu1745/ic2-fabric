# 分支同步状态：main ↔ 1.21.1

最后更新：2026-05-31

分叉点：`c5e247c`

验证方法：对 main 上每个 commit 的代码改动在 1.21.1 worktree 中逐文件比对。

> 迁移参考：`docs/migration-1.20.1-to-1.21.1.md` — 汇总了截至早期同步批次中出现的 API 变化及对应代码示例。

## 总览

同步跟踪表共 **165 个条目**，**125 个已同步，40 个待同步**。
已同步条目 `#1-#125` 归档在 `branch-sync-archive.md`；未同步条目不归档，即使主表超过 20 行。

## 逐 commit 状态

| # | Commit | 说明 | 在 1.21.1？ | 备注 |
|---|--------|------|:-----------:|------|
| 126 | `4ee9b03e` | feat: 核爆炸对齐 IC2 原版——自定义射线投射+分tick方块摧毁 | ❌ | 待同步 |
| 127 | `88fe5847` | feat: 核爆炸全分tick+严格数值对齐+全服公告+冷却 | ❌ | 待同步 |
| 128 | `f9841c26` | docs: TODO 添加特斯拉线圈电鱼 | ❌ | 待同步 |
| 129 | `6e412bcf` | feat: 高炉GUI改为传统纹理渲染 + 温度/HU/压缩空气机制重做 | ❌ | 待同步 |
| 130 | `b0ae678f` | fix: 高炉缺失压缩空气纹理源 | ❌ | 待同步 |
| 131 | `e159a492` | 部分GUI更新 + 高炉更改（PR #2） | ❌ | 待同步 |
| 132 | `e0d5114e` | fix: 恢复 PR 中误删的 steam 流体常量和类 + 修复 BlastFurnace typo | ❌ | 待同步 |
| 133 | `90605282` | feat: 流体系统重构——Map 查表替代 when、颜色单一数据源、JEI 排除自动同步 | ❌ | 待同步 |
| 134 | `7845b2fa` | feat: 钻头同时支持镐+铲双工具 + @ModItem 新增 tags 参数 | ❌ | 待同步 |
| 135 | `59c2b646` | GUI全面更新：铁炉/电炉/感应炉/储电箱/太阳能蒸馏机/方块切割机改用PNG纹理渲染 | ❌ | 待同步 |
| 136 | `e860b5d4` | GUI 版本更新：纹理修复 + 变压器/能量存储/机器 GUI 更新 | ❌ | 待同步 |
| 137 | `3377e9e6` | fix: 补上冲突解决时误删的 Ic2Fluid 结束大括号 | ❌ | 待同步 |
| 138 | `06235b00` | 新增压缩机/提取机/回收机/固体装罐机PNG GUI + 多机器电量条/流体槽悬停提示 | ❌ | 待同步 |
| 139 | `8d619cab` | 压缩机/提取机/收集机/固体装罐机等多机器储能/流体槽新增GUI及提示文本 | ❌ | 待同步 |
| 140 | `961dcf2e` | docs: 新增 19 份机器玩家文档，重写流体管道系统文档 | ❌ | 待同步 |
| 141 | `7299140f` | 更新了部分GUI 优化了高炉程序（PR #12） | ❌ | 待同步 |
| 142 | `ebd14a81` | 更新了部分GUI 以及优化了高炉程序 | ❌ | 待同步 |
| 143 | `ea30898b` | feat: 全面GUI重构 + 流体/气体系统区分 + 储罐系统 + 离心机配方修正 | ❌ | 待同步 |
| 144 | `19bf0da8` | feat: 采矿机流体处理 + 高级采矿机逐层回收管道 + 核电日志降级 | ❌ | 待同步 |
| 145 | `e58e819d` | docs: 更新同步状态表——添加 commits 129-144，归档 105-124 | ❌ | 待同步 |
| 146 | `7e78eeda` | 高级太阳能相关机器重构、GUI更新 | ❌ | 待同步 |
| 147 | `ab7a2cfd` | Merge pull request #13 from yu1745/feat/macerator-gui-texture | ❌ | 待同步 |
| 148 | `4ea41f63` | fix: 流体管道输入水被拒——isFluid 排除 WATER + canInsert/canExtract 误改 + simulateInsertion 精度 | ❌ | 待同步 |
| 149 | `801f453f` | 部分GUI交互更新 | ❌ | 待同步 |
| 150 | `336efb20` | GUI交互更新 | ❌ | 待同步 |
| 151 | `019e3ea6` | fix: 泵不吸流体——canInsert=false 阻断内部insert + 迁移ic2翻译到ic2_120 | ❌ | 待同步 |
| 152 | `ca0c1a89` | ci: 启用 GitHub Actions 构建与发布工作流 | ❌ | 待同步 |
| 153 | `ad08a51f` | refactor: 传送机GUI重构 + OD/OV扫描仪GUI重构 + build.yml注释化 | ❌ | 待同步 |
| 154 | `fcb99f05` | fix: 流体槽屏幕同步 + 压缩空气相关更新 | ❌ | 待同步 |
| 155 | `ba742b49` | refactor: 高炉空气单位mB→droplets + 风力计客户端化 + 流体着色修复 | ❌ | 待同步 |
| 156 | `560e5cff` | refactor: 全机器流体单位mB→Fabric droplet/bucket原生重构 | ❌ | 待同步 |
| 157 | `75b07ee0` | refactor: ComposeUI→HandledScreen迁移 + 通用装罐机/磁化机/焦炉功能增强 + 流体着色与显示修复 | ❌ | 待同步 |
| 158 | `5613b0fa` | fix: OD/OV扫描仪tooltip中扫描半径修正为扫描范围 | ❌ | 待同步 |
| 159 | `1dd7edc4` | fix: 消除Kotlin编译警告（废弃覆盖/冗余转换/不必要安全调用） | ❌ | 待同步 |
| 160 | `8e613c1a` | feat: 压缩空气单元统一为 air_cell + 高炉支持任意 FluidStorage 容器 | ❌ | 待同步 |
| 161 | `cd1f176b` | feat: 蒸汽系统修复 + UU模板安全 + 发电机tooltip + 复制机取消/状态 | ❌ | 待同步 |
| 162 | `43f60963` | feat: 集成 Fabric Guidebook + 修复 guidebook 页面链接与 Recipe ID | ❌ | 待同步 |
| 163 | `742b5414` | feat: 启用 CI 发版 + 更新斜齿轮 guidebook 文档与 3D 预览 | ❌ | 待同步 |
| 164 | `4c7063f4` | feat: JEI 配方背景使用机器纹理 + Canner 流体渲染 + 新增流体单元 | ❌ | 待同步 |
| 165 | `0f483fa6` | fix: 加固容器 GUI 交互防止刷物品 | ❌ | 待同步 |

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
