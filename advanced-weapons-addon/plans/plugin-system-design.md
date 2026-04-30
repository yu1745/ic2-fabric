# 插件系统设计计划书

> 注意：本文档为计划书，待审阅通过后实施。

## 1. 概述

为 IC2 高级武器附属（Advanced Weapons Addon）设计一套插件系统，使武器的威力、电量容量、攻击速度等属性可通过安装不同的升级插件来提升，而非依赖原版附魔。**该附属的所有武器禁止附魔**，一切属性增强都通过插件系统实现。

## 2. 核心概念

### 2.1 武器插槽（Weapon Socket）

- 每把武器拥有固定数量的插槽（基础 1-3 个，可通过特定插件扩展）
- 插槽类型分为：
  - **通用插槽**：可安装任意插件
  - **专属插槽**：仅限特定类型插件

### 2.2 插件（Plugin）物品

- 插件本身为独立物品，使用 `@ModItem` 注册
- 插件安装到武器上后不可移除（或需要消耗材料拆卸）
- 同一类效果插件不可叠加（取最高值），不同类型可共存

## 3. 插件分类

### 3.1 威力插件（Damage Plugin）
- 基础型：+X 基础伤害
- 进阶型：+X% 伤害倍率
- 元素型：附加火焰/雷电/穿甲等特殊效果

### 3.2 能量插件（Energy Plugin）
- 扩容型：增加武器 EU 容量
- 节能型：降低每次攻击的能耗
- 充能型：缓慢自动恢复能量

### 3.3 攻速插件（Speed Plugin）
- 轻型：增加攻击速度
- 平衡型：微幅增加攻击速度，微幅降低能耗

### 3.4 特殊插件（Special Plugin）
- 范围攻击：攻击时对周围敌人造成溅射伤害
- 吸血：伤害的部分转化为生命回复
- 击退增强：增加击退距离

## 4. 原版附魔 → 插件映射表

原版附魔**全部禁用**，每种效果由对应的能量插件替代。插件无附魔冲突限制、无等级上限、消耗 EU 驱动。

| 原版附魔 | 对应插件 | 插件行为差异 | 优点 |
|---|---|---|---|
| 锋利 V | 威力插件 I~III | 统一 +X 伤害，不分目标类型 | 不再需要亡灵/节肢分支 |
| 亡灵杀手 V | → 合并入威力插件 | 无 | 统一为通用伤害加成 |
| 节肢杀手 V | → 合并入威力插件 | 无 | 同上 |
| 火焰附加 II | 火焰插件 I~III | 每次命中消耗额外 EU 触发火焰 | 可调节能耗控制强度 |
| 击退 II | 击退插件 I~III | 增加击退距离，每级 +X 格 | 无附魔等级硬上限 |
| 抢夺 III | 掠夺插件 I~III | 消耗 EU 触发额外掉落判定 | 防止无成本刷物品 |
| 横扫之刃 III | 横扫插件 I~III | 范围伤害，消耗 EU，范围可控 | 范围+能耗双参数可调 |
| 经验修补 | 充能插件（吸血型） | 击杀生物回复 EU 而非耐久 | 与电动工具的能量体系天然契合 |
| 耐久 III | 强化插件 I~III | 降低能量消耗，非直接加耐久 | 电动工具无耐久，改为节能 |
| 消失诅咒 | **忽略** | 不在插件系统中实现 | 对玩家不友好，无意义 |
| 绑定诅咒 | **忽略** | 不在插件系统中实现 | 同上 |

### 插件较附魔的优势

1. **无冲突限制** — 附魔不能同时拥有锋利+亡灵杀手，插件可以同时安装威力+火焰+击退
2. **无等级上限** — 插件可设计到 V、X 甚至更高，成本指数增长即可
3. **能量驱动** — 每种效果消耗 EU，避免无成本堆叠，增加策略深度
4. **自由拆卸/升级** — 不需要铁砧反复重铸，插件管理界面即可操作
5. **专属联动** — 插件可与武器特性互动（如量子剑激活时插件效果翻倍）

## 5. 技术实现方案

### 5.1 数据存储

```kotlin
// 武器 NBT 结构
{
  "Plugins": [
    { "id": "more_weapons:damage_boost_i", "level": 1 },
    { "id": "more_weapons:energy_expand_i", "level": 1 }
  ]
}
```

- 每个插件条目记录 `id` 和 `level`
- 武器运行时根据 NBT 计算实际属性总值

### 5.2 核心接口

```kotlin
interface WeaponPlugin {
    val id: Identifier
    val maxLevel: Int
    val type: PluginType
    fun modifyDamage(base: Double, stack: ItemStack, level: Int): Double
    fun modifyCapacity(base: Long, stack: ItemStack, level: Int): Long
    fun modifyEnergyCost(base: Long, stack: ItemStack, level: Int): Long
    fun modifyAttackSpeed(base: Double, stack: ItemStack, level: Int): Double
    fun onHit(target: LivingEntity, attacker: LivingEntity, stack: ItemStack, level: Int)
}
```

### 5.3 注册机制

```kotlin
// 使用 @Plugin 注解注册插件
@Plugin
class DamageBoostPlugin : WeaponPlugin { ... }
```

- 在 `ClassScanner` 中增加 `@Plugin` 扫描支持
- 插件注册到全局 `PluginRegistry`

### 5.4 武器基类

```kotlin
abstract class PluginSwordItem : SwordItem, IElectricTool {
    // 从 NBT 读取插件列表并计算最终属性
    abstract val maxSockets: Int
    fun getPlugins(stack: ItemStack): List<PluginInstance>
    fun installPlugin(stack: ItemStack, plugin: WeaponPlugin, level: Int): Boolean
    fun getCalculatedDamage(stack: ItemStack): Double
    fun getCalculatedCapacity(stack: ItemStack): Long
}
```

- 所有附属武器继承 `PluginSwordItem` 获取插件能力
- `QuantumSaber` 改为继承 `PluginSwordItem`
- 基础攻击力设为较低值（如 12），通过插件提升至 31+，后续插件可进一步提升

### 5.5 GUI

- 增加武器插件管理界面（Screen + ScreenHandler）
- 右键武器打开界面
- 左侧显示武器信息（当前属性），右侧显示插槽与背包插件列表

## 6. 安装与拆卸

### 6.1 安装
- 在插件管理界面中将插件拖入插槽
- 成功安装后插件物品消耗

### 6.2 拆卸
- 使用特定工具（如纳米扳手）拆卸
- 拆卸有一定概率损坏插件

## 7. 平衡性设计

| 插件等级 | 伤害加成 | 能量加成 | 合成材料 |
|---------|---------|---------|---------|
| I       | +3     | +50k    | 基础材料 |
| II      | +6     | +100k   | 中级材料 |
| III     | +10    | +200k   | 高级材料 |

- 武器基础伤害保持较低，插件叠加达到高伤害
- 高级插件需要稀有材料，鼓励玩家探索

## 8. 待决策项

- [ ] 插件是否可拆卸？（当前设计：不可拆卸，或消耗材料拆卸）
- [ ] 插槽数量如何扩展？
- [ ] 是否需要插件工作台（专用机器）？
- [ ] 飞行/特殊能力是否也通过插件实现？

---

*以上为初版设计方案，请审阅后确认是否继续实施。*
