# 分支同步归档（main ↔ 1.21.1）

## 逐 commit 状态（已归档）

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
| 37 | `d41976e` | feat: 核反应堆无红石信号时仅停止燃料棒，散热片/热交换器等器件继续工作 | ✅ | 无冲突直接 cherry-pick `8c9ce6d` |
| 38 | `8863ece` | fix: 拴绳动能发生机拴住的生物坐上矿车时不再断开拴绳 | ✅ | cherry-pick `3a05b8c` + 1.21.1 适配 `getLeashHolder()` (`ef63e23`) + datagen 刷新 (`1146b57`) |
| 39 | `df4c5ed` | fix: 删除压缩机中 4 青金石→1 青金石块的刷物品配方 | ✅ | cherry-pick + 适配 1.21.1 单数 recipe/ 路径 |
| 40 | `d4de777` | fix: 铁炉和焦窑无扳手拆卸不再掉落机器外壳（防刷物品） | ✅ | 无冲突直接 cherry-pick |
| 41 | `95e6339` | fix: 徒手/镐子挖储罐不再掉落流体（防止刷流体） | ✅ | cherry-pick + 适配 1.21.1 `onBreak` 返回 `BlockState` |
| 42 | `5286bae` | feat: 量子护腿神行+量子靴子大跳，修复量子胸甲飞行条件 | ✅ | cherry-pick + 适配 1.21.1 Payload 网络/DataComponent NBT/appendTooltip 签名 |
| 43 | `9964f3b` | fix: JEI 缺失时 ConfigSyncReceiver 不再崩溃 | ✅ | 无冲突直接 cherry-pick |
| 44 | `2595b3f0` | fix: datagen 使用 modid 过滤避免附属触发本体缓存冲突 | ✅ | cherry-pick + datagen 刷新 |
| 45 | `cc03769b` | fix: 传送机移除内部缓存改为直接抽取相邻MFE，新增Jade诊断面板 | ✅ | cherry-pick + 适配 1.21.1 API（冲突解决）+ datagen 刷新 |
| 46 | `1be61024` | fix: 传送机移除能量条，绕过MFE单tick上限直接抽电，能量不足不渲染BER | ✅ | cherry-pick + 移除旧 cherry-pick（冲突 branc-sync-status.md）+ datagen 刷新 |
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
| 64 | `354c59e5` | feat: 剪刀机制 + 监管机额外产物 + 耐久条渲染修复 | ✅ | cherry-pick + 解决冲突（import、tooltip 参数适配、TODO.md）|
| 65 | `c127a41b` | fix: 修复所有机器 GUI 耐久条被 SlotAnchor 背景覆盖的渲染顺序问题——ui.render 在 super.render 之前执行 | ✅ | 无冲突直接 cherry-pick |
| 66 | `b246fc9c` | feat: 简化动能发电机配方——不再需要机器外壳 | ✅ | 无冲突直接 cherry-pick |
| 67 | `bd94c51d` | feat: 牲畜监管机机制重做 + 屠宰机掉落补充 | ✅ | 无冲突直接 cherry-pick |
| 68 | `5f1ffaa3` | chore: 更新TODO + 日光灯缓存改为100EU防频闪 | ✅ | 无冲突直接 cherry-pick |
| 69 | `da2bc1b1` | fix: 量子护腿神行移动检测改用 blockPos 坐标对比 + 量子胸甲入水不取消飞行 | ✅ | cherry-pick + 解决冲突（TODO.md release350 保留、QuantumLeggings.kt velocity→blockPos）|
| 70 | `ae483fd2` | fix: 打粉机 inputCount 未生效——一律按1个消耗，改为按配方数量消耗 | ✅ | cherry-pick + 适配 1.21.1 API（codec/packetCodec、RecipeInput、datagen inputCount 写入）+ datagen 刷新 |
| 71 | `717ebaae` | feat: 焦炉——窑口添加朝向、screen显示剩余时间、blockstate旋转修正 | ✅ | cherry-pick + 解决 CokeKilnBlocks.kt import 冲突 + datagen 刷新 |
| 72 | `0c935b69` | fix: 发酵机UI溢出容器修复，半流质发电机UI高度调整 | ✅ | 无冲突直接 cherry-pick + datagen 刷新 |
| 73 | `9186d55c` | feat: 添加泥炭矿及其处理配方、tooltip、世界生成与着色器 | ✅ | cherry-pick + 适配 1.21.1 API（Identifier.of、ItemTooltipCallback 4参数、PeatOreBlock::class.item()）+ datagen 刷新 |
| 87 | `223a745e` | fix: ModMenu 配置界面添加泥炭矿世界生成配置项 | ✅ | 补上遗漏的 peatOreConfig UI，cherry-pick 无冲突 |
| 88 | `9380770c` | fix: 移除 getTicker 中不必要的 `as XxxBlockEntity` 强转 | ✅ | 65 文件 66 处，直接文本替换到 1.21.1（`validateTicker` 版本）|
| 89 | `cc0571c4` | fix: 修正 FluidHeatGenerator HOTBAR_END 越界错误（43→41） | ✅ | 无冲突直接 cherry-pick |
| 90 | `a38fcc62` | feat: 为物品/流体抽入弹出升级添加 GUI 配置界面 | ✅ | cherry-pick + 适配 1.21.1 API（ExtendedScreenHandlerFactory<PacketByteBuf>、getScreenOpeningData）+ datagen 刷新 |
| 91 | `6a15c3c3` | feat: CannerBlockEntity 左右储罐对外暴露分离为只输入/只输出 | ✅ | 无冲突直接 cherry-pick |
| 92 | `3f20436e` | feat: 优化固体装罐机配方检测性能 + 管道 Jade 停滞显示冲突流体 | ✅ | cherry-pick + 适配 1.21.1 API（RecipeInput 签名）+ 解决冲突 |
| 93 | `2a9e3207` | feat: 发酵机添加肥料产出——每消耗 1000 mB 生物质产出 1 个肥料 | ✅ | cherry-pick + 适配 1.21.1 API（Identifier.of）+ datagen 刷新 |
| 94 | `3c7d41c7` | fix: 添加蜘蛛眼→蛤蛤粉打粉机配方 | ✅ | cherry-pick + 适配 1.21.1 路径（recipe/ 单数）+ datagen 刷新 |
| 95 | `da72f80d` | feat: 作物/牲畜监管机 Int mB → SingleVariantStorage 重构 + FluidStorage 暴露 + 流体抽入升级 | ✅ | cherry-pick + 适配 1.21.1 API（ExtendedScreenHandlerFactory<PacketByteBuf>）+ datagen 刷新 |
| 96 | `32f04999` | feat: 阻止牲畜监管机范围内的鸡自然下蛋 | ✅ | cherry-pick + 改用 @Inject 替代 @Redirect（1.21.1 无 refMap）+ datagen 刷新 |
| 97 | `38f1af75` | fix: 贴脸传输超压爆炸也受 enableOvervoltageExplosion 配置开关控制 | ✅ | cherry-pick 无冲突 |
| 98 | `deacc818` | refactor: POC — Macerator ScreenHandler 使用 RoutedItemStorage 驱动 quickMove | ✅ | cherry-pick 无冲突 |
| 99 | `59b1c863` | merge: Unit 1 — 基础加工机A (Compressor, Extractor, Centrifuge, Recycler, MetalFormer) | ✅ | cherry-pick 无冲突 |
| 100 | `a6f98f6d` | merge: refactor: 熔炉家族迁移到 RoutedItemStorage 驱动 quickMove | ✅ | cherry-pick 无冲突 |
| 101 | `69f48a00` | merge: refactor: 装罐机家族迁移到 RoutedItemStorage 驱动 quickMove | ✅ | cherry-pick + 解决冲突（Canner/FluidCanner/SolidCanner import + 旧 SlotSpec）|
| 102 | `64611492` | merge: refactor: 发电机A迁移到 RoutedItemStorage 驱动 quickMove | ✅ | cherry-pick 无冲突 |
| 103 | `c161afb2` | merge: refactor: 发电机B+热能迁移到 RoutedItemStorage 驱动 quickMove | ✅ | cherry-pick + 解决冲突 + Identifier.of 适配 |
| 104 | `8a9d1142` | merge: refactor: 特殊机器A迁移到 RoutedItemStorage 驱动 quickMove | ✅ | cherry-pick + 解决冲突（MatterGenerator/OreWashingPlant）|
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
| 126 | `4ee9b03e` | feat: 核爆炸对齐 IC2 原版——自定义射线投射+分tick方块摧毁 | ✅ | cherry-pick 到 1.21.1：`6a7ca573` |
| 127 | `88fe5847` | feat: 核爆炸全分tick+严格数值对齐+全服公告+冷却 | ✅ | cherry-pick 到 1.21.1：`9308ae38` |
| 128 | `f9841c26` | docs: TODO 添加特斯拉线圈电鱼 | ✅ | cherry-pick 到 1.21.1：`d1b8ba04` |
| 129 | `6e412bcf` | feat: 高炉GUI改为传统纹理渲染 + 温度/HU/压缩空气机制重做 | ✅ | cherry-pick 到 1.21.1：`bca45109` |
| 130 | `b0ae678f` | fix: 高炉缺失压缩空气纹理源 | ✅ | cherry-pick 到 1.21.1：`4e2c2426` |
| 131 | `e159a492` | 部分GUI更新 + 高炉更改（PR #2） | ✅ | 已迁移到 1.21.1，含 GUI/高炉相关冲突适配 |
| 132 | `e0d5114e` | fix: 恢复 PR 中误删的 steam 流体常量和类 + 修复 BlastFurnace typo | ✅ | cherry-pick 到 1.21.1：`fb1b8f13` |
| 133 | `90605282` | feat: 流体系统重构——Map 查表替代 when、颜色单一数据源、JEI 排除自动同步 | ✅ | cherry-pick 到 1.21.1：`b06d5b99` |
| 134 | `7845b2fa` | feat: 钻头同时支持镐+铲双工具 + @ModItem 新增 tags 参数 | ✅ | cherry-pick 到 1.21.1：`d37c4f70` |
| 135 | `59c2b646` | GUI全面更新：铁炉/电炉/感应炉/储电箱/太阳能蒸馏机/方块切割机改用PNG纹理渲染 | ✅ | cherry-pick 到 1.21.1：`c3808d61` |
| 136 | `e860b5d4` | GUI 版本更新：纹理修复 + 变压器/能量存储/机器 GUI 更新 | ✅ | 已迁移到 1.21.1，含上游 GUI 资源补齐 |
| 137 | `3377e9e6` | fix: 补上冲突解决时误删的 Ic2Fluid 结束大括号 | ✅ | 已迁移到 1.21.1 |
| 138 | `06235b00` | 新增压缩机/提取机/回收机/固体装罐机PNG GUI + 多机器电量条/流体槽悬停提示 | ✅ | cherry-pick 到 1.21.1：`c337a608` |
| 139 | `8d619cab` | 压缩机/提取机/收集机/固体装罐机等多机器储能/流体槽新增GUI及提示文本 | ✅ | 已迁移到 1.21.1 |
| 140 | `961dcf2e` | docs: 新增 19 份机器玩家文档，重写流体管道系统文档 | ✅ | cherry-pick 到 1.21.1：`fc567b65` |
| 141 | `7299140f` | 更新了部分GUI 优化了高炉程序（PR #12） | ✅ | 已迁移到 1.21.1，含 GUI/高炉相关冲突适配 |
| 142 | `ebd14a81` | 更新了部分GUI 以及优化了高炉程序 | ✅ | cherry-pick 到 1.21.1：`67eca288` |
| 143 | `ea30898b` | feat: 全面GUI重构 + 流体/气体系统区分 + 储罐系统 + 离心机配方修正 | ✅ | cherry-pick 到 1.21.1：`7af80f85` |
| 144 | `19bf0da8` | feat: 采矿机流体处理 + 高级采矿机逐层回收管道 + 核电日志降级 | ✅ | cherry-pick 到 1.21.1：`7cb43d83` |
| 145 | `e58e819d` | docs: 更新同步状态表——添加 commits 129-144，归档 105-124 | ✅ | 同步状态记录，本次按已处理归档 |
| 146 | `7e78eeda` | 高级太阳能相关机器重构、GUI更新 | ✅ | cherry-pick 到 1.21.1：`e6a82d3a` |
| 147 | `ab7a2cfd` | Merge pull request #13 from yu1745/feat/macerator-gui-texture | ✅ | merge 内容已由前后功能提交覆盖并迁移 |
| 148 | `4ea41f63` | fix: 流体管道输入水被拒——isFluid 排除 WATER + canInsert/canExtract 误改 + simulateInsertion 精度 | ✅ | cherry-pick 到 1.21.1：`1f57ee5c` |
| 149 | `801f453f` | 部分GUI交互更新 | ✅ | cherry-pick 到 1.21.1：`9178a4b9` |
| 150 | `336efb20` | GUI交互更新 | ✅ | cherry-pick 到 1.21.1：`9bd9e2ac` |
| 151 | `019e3ea6` | fix: 泵不吸流体——canInsert=false 阻断内部insert + 迁移ic2翻译到ic2_120 | ✅ | cherry-pick 到 1.21.1：`0ba1e500` |
| 152 | `ca0c1a89` | ci: 启用 GitHub Actions 构建与发布工作流 | ✅ | cherry-pick 到 1.21.1：`f5bcff58` |
| 153 | `ad08a51f` | refactor: 传送机GUI重构 + OD/OV扫描仪GUI重构 + build.yml注释化 | ✅ | cherry-pick 到 1.21.1：`df3b69c7` |
| 154 | `fcb99f05` | fix: 流体槽屏幕同步 + 压缩空气相关更新 | ✅ | cherry-pick 到 1.21.1：`92be2cbc` + 适配 1.21.1 FluidVariant NBT/ContainerItemContext API |
| 155 | `ba742b49` | refactor: 高炉空气单位mB→droplets + 风力计客户端化 + 流体着色修复 | ✅ | cherry-pick 到 1.21.1：`9ca5a277` + 解决 ChunkLoader/Cropnalyzer/WindMeter/GUI PNG 冲突 |
| 156 | `560e5cff` | refactor: 全机器流体单位mB→Fabric droplet/bucket原生重构 | ✅ | cherry-pick 到 1.21.1：`0557afcc` |
| 157 | `75b07ee0` | refactor: ComposeUI→HandledScreen迁移 + 通用装罐机/磁化机/焦炉功能增强 + 流体着色与显示修复 | ✅ | cherry-pick 到 1.21.1：`a615d36a` + 适配 1.21.1 `appendTooltip` 签名 |
| 158 | `5613b0fa` | fix: OD/OV扫描仪tooltip中扫描半径修正为扫描范围 | ✅ | cherry-pick 到 1.21.1：`93c84cb9`（exact patch match） |
| 159 | `1dd7edc4` | fix: 消除Kotlin编译警告（废弃覆盖/冗余转换/不必要安全调用） | ✅ | cherry-pick 到 1.21.1：`7bb3d163` + 适配 1.21.1 注解/属性接口 |
| 160 | `8e613c1a` | feat: 压缩空气单元统一为 air_cell + 高炉支持任意 FluidStorage 容器 | ✅ | cherry-pick 到 1.21.1：`95cb2bb7` + 适配 1.21.1 `FluidStorage` 抽象 |
| 161 | `cd1f176b` | feat: 蒸汽系统修复 + UU模板安全 + 发电机tooltip + 复制机取消/状态 | ✅ | cherry-pick 到 1.21.1：`c11c1d39` |
| 162 | `43f60963` | feat: 集成 Fabric Guidebook + 修复 guidebook 页面链接与 Recipe ID | ✅ | cherry-pick 到 1.21.1：`c9eee5fb` + 适配 1.21.1 RecipeExporter 签名 |
| 163 | `742b5414` | feat: 启用 CI 发版 + 更新斜齿轮 guidebook 文档与 3D 预览 | ✅ | cherry-pick 到 1.21.1：`5aef4298`（后续 1.21.1 提交 `b1591091` 重新注释化 CI） |
| 164 | `4c7063f4` | feat: JEI 配方背景使用机器纹理 + Canner 流体渲染 + 新增流体单元 | ✅ | cherry-pick 到 1.21.1：`123d2b10` + JEI 背景纹理与流体单元 1.21.1 适配 |
| 165 | `0f483fa6` | fix: 加固容器 GUI 交互防止刷物品 | ✅ | cherry-pick 到 1.21.1：`dee9a00f`（exact patch match） |
| 166 | `1d6ade59` | docs: record pending container screen sync | ⏭️ | 纯同步状态记录，后续状态表已覆盖，未单独迁移 |
| 167 | `c66c6a3d` | feat: 高炉增加 1280 HU 缓存机制，消除 1401 温度边界工作抽搐 | ✅ | cherry-pick 到 1.21.1：`83e4380b` + 适配 1.21.1 BlastFurnaceBlockEntity 接口泛型 |
| 168 | `96034c3f` | fix: 消除 compileKotlin 编译警告（废弃覆盖/API调用/多余转换） | ✅ | cherry-pick 到 1.21.1：`4219800b` + 适配 1.21.1 注解/API 调用 |
| 169 | `7eb01c05` | fix: 泵无流体预检 + 手摇动能发生机物品贴图对齐 | ✅ | cherry-pick 到 1.21.1：`b2328985` |
| 170 | `42420fcd` | fix: 补充 Block 类废弃覆盖警告抑制 | ✅ | cherry-pick 到 1.21.1：`0d0a0349` |
| 171 | `6d768271` | feat: 动能发生机贴图迁移到 ic2_120 命名空间 + 新增贴图 | ✅ | cherry-pick 到 1.21.1：`fb2b9550` |
| 172 | `b20656f8` | Merge branch main of https://github.com/yu1745/ic2-fabric | ⏭️ | merge commit，无需单独迁移；内容已由相邻功能提交覆盖 |
| 173 | `649ae57f` | fix: 校准机器 GUI 能量条/进度条纹理坐标与像素偏移，BlockCutter 增加升级提示图标 | ✅ | cherry-pick 到 1.21.1：`36a49259` |
| 174 | `e8b64aa3` | feat: 高炉 HU 指示灯条件改为升温条件满足时渲染 + 达温上限维持 HU 消耗 | ✅ | cherry-pick 到 1.21.1：`68b35985` |
| 175 | `847596a5` | feat: add tag-based recipe and fluid compat | ✅ | cherry-pick 到 1.21.1：`2608e788` |
| 176 | `4949b6a8` | feat: 采矿机自动回收管道 + 缺管时回收旁支复用 + 流体阻塞提示 + 游标坐标显示 | ✅ | cherry-pick 到 1.21.1：`6b23697b` |
| 177 | `3baafd6b` | fix: 采矿机到底判定改为检测基岩（不可破坏方块）而非硬编码 Y 值 | ✅ | cherry-pick 到 1.21.1：`3b47dbd7` |
| 178 | `1d480e3f` | feat: 添加 BuildCraftAddon 模块 — 引擎/液泵/流体/世界生成 | ✅ | cherry-pick 到 1.21.1：`054a3175` + 适配 1.21.1 API/Remapper/Guidebook 依赖排除 |
| 179 | `c5453125` | @ feat: 石油世界生成噪声斑块 + 流体桶注册 + Spring 方块修复 | ✅ | cherry-pick 到 1.21.1：`b0489025` |
| 180 | `c16943b9` | build: 添加 Jade 依赖 + Guidebook 校验/preview 任务 | ✅ | cherry-pick 到 1.21.1：`63cb92cc`；保留 Guidebook 校验任务但在 1.21.1 暂禁用 |
| 181 | `80db3a36` | feat(buildcraft-addon): 引擎接入 IC2 动能系统(IKineticMachinePort) + 泵 A*/BFS 重写 + 油井 Spring/Tube + Jade 集成 | ✅ | cherry-pick 到 1.21.1：`71407517` + 适配 BlockEntity NBT/ScreenOpeningData/renderer API |
