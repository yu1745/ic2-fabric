# 分支同步状态：main ↔ 1.21.1

最后更新：2026-06-11

分叉点：`c5e247c`

验证方法：对 main 上每个 commit 的代码改动在 1.21.1 worktree 中逐文件比对。

> 迁移参考：`docs/migration-1.20.1-to-1.21.1.md` — 汇总了截至早期同步批次中出现的 API 变化及对应代码示例。

## 总览

同步跟踪表共 **194 个条目**，**192 个已同步，2 个跳过，0 个待同步**。
已同步/跳过条目 `#1-#174` 归档在 `branch-sync-archive.md`；主表保留最近 20 条 `#175-#194`。

## 逐 commit 状态

| # | Commit | 说明 | 在 1.21.1？ | 备注 |
|---|--------|------|:-----------:|------|
| 175 | `847596a5` | feat: add tag-based recipe and fluid compat | ✅ | cherry-pick 到 1.21.1：`2608e788` |
| 176 | `4949b6a8` | feat: 采矿机自动回收管道 + 缺管时回收旁支复用 + 流体阻塞提示 + 游标坐标显示 | ✅ | cherry-pick 到 1.21.1：`6b23697b` |
| 177 | `3baafd6b` | fix: 采矿机到底判定改为检测基岩（不可破坏方块）而非硬编码 Y 值 | ✅ | cherry-pick 到 1.21.1：`3b47dbd7` |
| 178 | `1d480e3f` | feat: 添加 BuildCraftAddon 模块 — 引擎/液泵/流体/世界生成 | ✅ | cherry-pick 到 1.21.1：`054a3175` + 适配 1.21.1 API/Remapper/Guidebook 依赖排除 |
| 179 | `c5453125` | @ feat: 石油世界生成噪声斑块 + 流体桶注册 + Spring 方块修复 | ✅ | cherry-pick 到 1.21.1：`b0489025` |
| 180 | `c16943b9` | build: 添加 Jade 依赖 + Guidebook 校验/preview 任务 | ✅ | cherry-pick 到 1.21.1：`63cb92cc`；保留 Guidebook 校验任务但在 1.21.1 暂禁用 |
| 181 | `80db3a36` | feat(buildcraft-addon): 引擎接入 IC2 动能系统(IKineticMachinePort) + 泵 A*/BFS 重写 + 油井 Spring/Tube + Jade 集成 | ✅ | cherry-pick 到 1.21.1：`71407517` + 适配 BlockEntity NBT/ScreenOpeningData/renderer API |
| 182 | `cc4d210b` | fix(core): Jade 管道流量改 mB/s + 太阳能/水能/风能 MIN/MAX 对齐 + JEI 流体绑定单元与桶 | ✅ | cherry-pick 到 1.21.1：`f18bc4a3` + 解决 JEI 冲突 |
| 183 | `03aa4708` | feat(core): 添加 Guidebook 校验工具(供 validateGuidebook 任务调用) | ✅ | cherry-pick 到 1.21.1：`903d3ef2`；校验工具源码保留但 client Java 临时排除编译 |
| 184 | `6d199717` | docs: 迁移玩家文档至游戏内 Guidebook + 删除旧 player-docs/ + 更新 README 指向 | ✅ | cherry-pick 到 1.21.1：`1160e6d7` |
| 185 | `8bdfada5` | docs(guidebook): 将装备/工具/物品等引用从 reference/ 拆分到独立 items/ 页面 + 添加 Build-Time 校验规则 | ✅ | cherry-pick 到 1.21.1：`182542b0` |
| 186 | `9855d47d` | feat(core): 升级GUI按钮7px文字 + 高级采矿机过滤槽锁定 | ✅ | cherry-pick 到 1.21.1：`d9903f12` + 适配 `ItemStack.areItemsAndComponentsEqual` |
| 187 | `9f3aa882` | refactor(core): 喷气背包/量子胸甲飞行改用原版创造模式机制 | ✅ | cherry-pick 到 1.21.1：`8bfe8206` + 移除旧飞行 payload/双击处理器 |
| 188 | `6c90259b` | fix(core): 橡胶树苗生长空间检查避免被既有木材挤掉 | ✅ | cherry-pick 到 1.21.1：`bf02a424` |
| 189 | `ab6773ed` | build: 升级 fabric_guidebook 至 0.1.4 + AGENTS.md / docs/README.md 文档入口 | ✅ | cherry-pick 到 1.21.1：`bc72bdfd`；不重新引入 Guidebook 运行依赖 |
| 190 | `afc9168a` | chore(deploy): mod-sync-fabric 后台同步 mod 索引与重启并发 | ✅ | cherry-pick 到 1.21.1：`fa054d03` |
| 191 | `8d982aa6` | fix(core): 橡胶树叶块放置前检查 TreeFeature.canReplace 避免与既有方块冲突 | ✅ | cherry-pick 到 1.21.1：`fdf637b2` |
| 192 | `9e78e92a` | docs(guidebook): 批量文档润色 + 新增 scrap_box 物品条目 + lang 微调 | ✅ | cherry-pick 到 1.21.1：`e28901fb` |
| 193 | `5e86c0d0` | docs(pitfalls): 新增 Kotlin LSP 工具链踩坑记录 | ✅ | cherry-pick 到 1.21.1：`be00bf76` |
| 194 | `e04f4bcc` | docs: add branch cherry-pick guide | ✅ | cherry-pick 到 1.21.1：`5d1c1dbf` |

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
- 2026-06-10：更新同步状态表——添加 commits 166-193（28 个 main 上 0f483fa6 之后、1.21.1 上均未同步的 commit：包括 1d6ade59 docs: pending container screen sync、c66c6a3d 高炉 1280 HU 缓存、BuildCraftAddon 模块（1d480e3f/c5453125/80db3a36/c16943b9）、Jade 集成（cc4d210b/03aa4708）、采矿机基岩判定+自动回收（3baafd6b/4949b6a8）、高炉 HU 机制（e8b64aa3/c66c6a3d）、贴图迁移（6d768271）、编译警告消除（96034c3f/42420fcd/7eb01c05）、能量条校准（649ae57f）、Guidebook 迁移（6d199717/8bdfada5/9855d47d）、飞行改原版创造模式（9f3aa882）、橡胶树修复（6c90259b/8d982aa6）、fabric_guidebook 升级（ab6773ed）、mod-sync-fabric 后台同步（afc9168a）、Guidebook 文档润色+Kotlin LSP 踩坑（9e78e92a/5e86c0d0）等），全部待同步至 1.21.1 |
- 2026-06-11：同步 commits 126-155 到 1.21.1，并归档到 `branch-sync-archive.md`。其中 `91740f2e docs: update branch sync status` 为纯同步状态提交，迁移过程中跳过；当前下一条待迁移为 `560e5cff` |
- 2026-06-11：对比 main ↔ 1.21.1 后批量更新——commits #156-#168（除 #166 文档）实际已通过 cherry-pick 存在于 1.21.1（带 API 适配），已迁移到 `branch-sync-archive.md` 并附等效 1.21.1 SHA（`0557afcc`/`a615d36a`/`93c84cb9`/`7bb3d163`/`95cb2bb7`/`c11c1d39`/`c9eee5fb`/`5aef4298`/`123d2b10`/`dee9a00f`/`83e4380b`/`4219800b`）；同步状态表从 155/193 收敛为 167/193，剩余 26 条 #166 + #169-#193 待 cherry-pick。1.21.1 端额外提交 `b1591091 chore: 注释化 build.yml,与 main 保持一致`（重新注释化 1.21.1 的 CI workflow 以与 main 状态对齐）
- 2026-06-11：完成剩余迁移——#169-#193 全部 cherry-pick 到 1.21.1，#194 `e04f4bcc` 迁移指南同步到 1.21.1；#166 纯同步状态记录、#172 merge commit 跳过。`./gradlew build` 在 1.21.1 通过；同步状态收敛为 192/194 已同步、2/194 跳过、0 待同步。
