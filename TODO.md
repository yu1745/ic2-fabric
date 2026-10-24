## UI / 配置

- [ ] Cloth Config + ModMenu GUI 配置界面（集成现有 Ic2Config JSON 体系）
- [ ] 更新 Compose UI 异步渲染 + slot 合并 + ScreenHandler/Screen 封装
- [ ] `uiscope.kt` 改成扩展函数拆分
- [ ] 机器 slot 按面处理（顶面输入底面输出，AE2 终端直接对着面操作）

## 机器 / 功能

- [ ] 戴森球计划的增产机制
- [ ] IC2 特有船 + 各种电动工具逻辑实现
- [ ] 核电添加锁定布局按钮（防止 AE 抽走散热片后重新放入炸反应堆）
- [ ] 电动钓鱼机
- [ ] 特斯拉线圈电鱼（对范围内生物持续电击，对水中鱼类加倍伤害）
- [ ] 传送机 + 回城卷轴（贴图复用遥控器，传送时召唤雷霆/临时改变天气）
- [ ] 磁化机加按钮切换音效（"我要把你崩飞！！！"）
- [ ] 电动船功能实现
- [ ] 钓鱼可获得低阶机器或更复杂玩法（需讨论）
- [ ] 敌对生物监控机——复用牲畜监管机材质和围笼逻辑，不繁殖，定期从敌对生物采集掉落物（蜘蛛眼、线等）。需论证：是扩展屠宰机支持敌对生物更好，还是单独写一个机器。关键分歧：屠宰机侧重杀死拿掉落，监控机侧重活体定时采集。

## 装备 / 防具

- [ ] 纳米/量子靴子特殊处理摔落伤害
- [ ] 防化服实现真实功能 + 辐射效果（每个子防具单独效果）

## 动能 / 电力

- [ ] 动能系统做成类似机械动力的传动轴系统
- [ ] 动能发电机动态电压等级（当前 tier=3 HV 固定，手摇接入后低压线烧毁）
- [ ] 日光灯只有背面可通电（避免侧面吸附导线），顶部放置发光方向反了
- [ ] 风力动能发生机叶片碰撞对实体造成伤害

## 世界 / 物品

- [ ] 铝矿、冶炼、合金体系（精准配比）

## 跨 Mod / 其他

- [ ] 钓鱼失败全服公告（写在别的 mod 里）

## 存储 / 自动化

- [ ] 将 `RoutedItemStorage` 改为真正的 `SlottedStorage<ItemVariant>`（暂缓，当前不实现）
  - 背景：当前 `StorageView.extract()` 委托给整个 `RoutedItemStorage.extract()`；多输出槽含相同物品时，请求指定槽位可能实际从更靠前的槽位提取。
  - 兼容问题：Kilt 会把非 `SlottedStorage` 的普通 Fabric Storage 动态包装为 Forge `IItemHandler`，其槽位数量和索引随非空视图变化，空槽也无法稳定暴露给 Forge 自动化。
  - 建议方案：基于固定的 `visibleSlots` 创建稳定 `SingleSlotStorage` 列表，包含空槽；单槽插入继续执行现有 route、validator 和 `maxPerSlot`，单槽提取只能操作对应的 `extractSlots` 槽位；保留现有全局插入/提取和事务快照逻辑。
  - 规模：约 60–100 行核心改动，预计无需修改 53 个构造调用点；影响 51 个 core 实现和 2 个 advanced-solar-addon 实现。
  - 验收：打粉机/电炉、感应炉双输出、离心机或洗矿机多输出、电池/升级槽路由、空槽 Forge 插入、Fabric/Forge 管道、模拟事务回滚、NBT 保留。
  - 注意：Kilt 中“提取最后一件返回空栈”的修复仍需保留，两项修复位于不同层级。

## 已知问题：电压等级（tier）体系在高等级截断 ✅ 已解决（2025）

影响范围：工业升级附属（industrial-upgrade-addon）的高等级太阳能发电机（tier 6–11）。

根因：
- `ic2_120.content.energy.EnergyTier.euPerTickFromTier(tier: Int)` 内部原为 `repeat((tier - 1).coerceAtMost(8))`，tier 被 clamp 到最多 9；tier 10/11 实际按 tier 9 处理（输出 = 32 × 4^8 = 2,097,152 EU/t）。

解决方案（已实施）：
- 将 `coerceAtMost(8)` 放开为 `coerceAtMost(11)`，覆盖工业升级最高 tier 11。
- tier 6–11 面板输出上限（32K / 131K / 524K / 2M / 8M / 33M EU/t）均 > 各自 dayPower，能量可正常输出。
- Int 不会溢出：`euPerTickFromTier` 返回 Long；tier 11 输出 33,554,432，远小于 Int.MAX；仅 tier≥14 才超 Int，但那些值均存入 Long 字段。
- core 现有机器不受影响（最高 tier=5，核反应堆）。

残留（非阻塞）：
- `IBatteryItem.transferSpeed: Int` 仍用 `toInt()`，但电池 tier 不会到 10+，无实际影响。
- 工业升级 tier 11 面板 maxStorage=10T 在 GUI 同步 `toInt()` 处会溢出，仅影响进度条显示，不影响能量传输。
