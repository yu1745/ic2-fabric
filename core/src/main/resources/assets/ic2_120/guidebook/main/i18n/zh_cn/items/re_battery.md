---
navigation:
  title: 可充电电池系列
  parent: index.md
  position: 320
  icon: ic2_120:re_battery
item_ids:
  - ic2_120:re_battery
  - ic2_120:advanced_re_battery
  - ic2_120:single_use_battery
  - ic2_120:re_battery_wireless
  - ic2_120:advanced_re_battery_wireless
---

# 可充电电池系列

<ItemImage id="ic2_120:re_battery" scale="4" />

可充电电池系列是 IC2 入门级的可充电 EU 储能。其中包含两个可充电型号——<ItemLink id="ic2_120:re_battery" /> 与 <ItemLink id="ic2_120:advanced_re_battery" />——以及一个放电后即消耗的 <ItemLink id="ic2_120:single_use_battery" />。三者都放入玩家物品栏，可在任意兼容的储电设备中充电，并能为电动工具与护甲供电。

完整阶梯表（含更高级的能量水晶与兰波顿水晶）见 [电池与移动供电](../reference/energy_items.md)。

## 属性

可充电电池系列是获取移动 EU 成本最低的方式。可充电型号可反复放电与回充；一次性型号是放完即消失的消耗品。

| 物品 | 等级 | 最大 EU | 说明 |
|------|:---:|---:|-------|
| <ItemLink id="ic2_120:single_use_battery" /> | 1 | 10,000 EU | 一次性电池，在任何普通电池槽放电，耗尽后销毁。 |
| <ItemLink id="ic2_120:re_battery" /> | 1 | 10,000 EU | 基础可充电电池，是早期工具最可靠的移动 EU 来源。 |
| <ItemLink id="ic2_120:advanced_re_battery" /> | 2 | 100,000 EU | 高容量可充电电池，能让中期电动工具免去频繁充电。 |

## 物品视图

| 一次性电池 | 充电电池 | 高级充电电池 |
|:---:|:---:|:---:|
| <ItemImage id="ic2_120:single_use_battery" scale="2" /> | <ItemImage id="ic2_120:re_battery" scale="2" /> | <ItemImage id="ic2_120:advanced_re_battery" scale="2" /> |

## 使用方式

### 可充电型号行为

充电电池与高级充电电池可在以下位置充放电：

- **储电方块**——<ItemLink id="ic2_120:batbox" />、<ItemLink id="ic2_120:cesu" />、<ItemLink id="ic2_120:mfe" /> 与 <ItemLink id="ic2_120:mfsu" />。将电池放入储电方块的电池槽即可充放电。
- **充电座**——站在充电座变种上，物品栏内所有电池（以及兼容装备）会被充电。详见 [充电座](../machines/chargepad.md)。
- **电池背包**——<ItemLink id="ic2_120:batpack" /> 与 <ItemLink id="ic2_120:advanced_batpack" /> 会自动为物品栏内等级不高于背包等级的充电电池补电。

### 一次性型号行为

<ItemLink id="ic2_120:single_use_battery" /> 在为物品供电方面与充电电池一致，但耗尽后即被消耗。它专为没有充电器时使用，是低成本的起步电池。

### 无线型号

充电电池系列存在两种无线型号。只要它们在玩家物品栏中任意位置，就会自动从网络中抽取 EU 并推送给穿戴或手持的电动工具。本质上属于个人化的被动补电来源。

| 无线电池 | 等级 | 说明 |
|------|:---:|-------|
| <ItemLink id="ic2_120:re_battery_wireless" /> | 1 | 与基础充电电池同等级，具备被动补电行为。 |
| <ItemLink id="ic2_120:advanced_re_battery_wireless" /> | 2 | 更高等级，可为不高于高级充电电池等级的工具供电。 |

更高级的无线版本（无线能量水晶与无线兰波顿水晶）参见 [能量水晶与兰波顿水晶](energy_crystal.md) 页面。

## 配方

可充电电池系列的配方定义在模组的数据驱动配方系统中。具体材料可使用 JEI 查看，或打开下方代码片段引用的配方文件。

<Recipe id="ic2_120:re_battery" />
<Recipe id="ic2_120:advanced_re_battery" />
<Recipe id="ic2_120:single_use_battery" />

## 相关

- [工具与防护装备](../reference/tools_armor.md)
- [电池与移动供电](../reference/energy_items.md)
- [储电设备](../machines/energy_storage.md)——为电池充电的固定方块
- [充电座](../machines/chargepad.md)——玩家站在上面时为装备充电的方块
- [能量水晶与兰波顿水晶](energy_crystal.md)——再高一级的两种电池
