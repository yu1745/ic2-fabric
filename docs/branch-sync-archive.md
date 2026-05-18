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
