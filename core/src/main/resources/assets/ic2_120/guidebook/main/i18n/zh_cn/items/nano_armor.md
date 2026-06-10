---
navigation:
  title: 纳米护甲
  parent: index.md
  position: 302
  icon: ic2_120:nano_helmet
item_ids:
  - ic2_120:nano_helmet
  - ic2_120:nano_chestplate
  - ic2_120:nano_leggings
  - ic2_120:nano_boots
---

# 纳米护甲

<ItemImage id="ic2_120:nano_helmet" scale="4" />

纳米护甲是 IC2 第一档能量增强护甲。每件部件都能储存 EU，并在有电时提供额外的动力减伤。套装在科技等级上位于[青铜护甲](bronze_armor.md)与[量子护甲](quantum_armor.md)之间。

## 物品视图

| 纳米头盔 | 纳米胸甲 | 纳米护腿 | 纳米靴子 |
|:---:|:---:|:---:|:---:|
| <ItemImage id="ic2_120:nano_helmet" scale="2" /> | <ItemImage id="ic2_120:nano_chestplate" scale="2" /> | <ItemImage id="ic2_120:nano_leggings" scale="2" /> | <ItemImage id="ic2_120:nano_boots" scale="2" /> |

## 属性

| 部位 | 物品 | 护甲值 | 韧性 | EU 容量 | 等级 | 动力减伤 |
|------|------|:---:|:---:|:---:|:---:|:---:|
| 头盔 | nano_helmet | 3 | 2.0 | 1,000,000 EU | 3 | 12% |
| 胸甲 | nano_chestplate | 8 | 2.0 | 1,000,000 EU | 3 | 36% |
| 护腿 | nano_leggings | 6 | 2.0 | 1,000,000 EU | 3 | 24% |
| 靴子 | nano_boots | 3 | 2.0 | 1,000,000 EU | 3 | 8% |

韧性 2.0 与原版下界合金等级持平。四件全部有电时，纳米护甲最多提供 80% 动力减伤，是升级到量子护甲前的主力防护套装。

## 已生效机制

- **能量缓冲**——每件护甲可储存 `1,000,000 EU`（`1 MEU`），充电等级为 3。
- **动力减伤**——只有有电的纳米部件会贡献减伤。空电部件仍保留基础护甲值，但不参与动力减伤。
- **受击 EU 消耗**——被减免的伤害按每点 `5,000 EU` 消耗，费用由本次参与减伤的有电电力护甲分摊。
- **纳米头盔夜视**——穿戴头盔时按 `Alt+N` 切换。开启后，头盔会消耗 EU，并在黑暗区域提供夜视。

## 纳米头盔夜视

<ItemLink id="ic2_120:nano_helmet" /> 包含来自夜视镜配方的夜视系统。

- **默认缓冲：** 1,000,000 EU。
- **默认时长：** 满电可提供 3,571 秒主动夜视。
- **控制：** 穿戴头盔时按 `Alt+N`。
- **明亮区域：** 光照等级 8 或更高时会移除夜视并短暂施加失明，与夜视镜行为一致。
- **Tooltip：** 显示夜视开关状态和预计剩余时间。

## 使用方式

- 穿戴全部四件以获得完整 80% 动力减伤。
- 保持各部件充电；纳米护甲靠 EU 充能维护，不靠原版耐久修复。空电部件仍有基础护甲值，但不提供动力减伤。
- 进入黑暗区域前使用 `Alt+N` 开启纳米头盔夜视。

## 配方

| 纳米头盔 | 纳米胸甲 |
|:---:|:---:|
| <Recipe id="ic2_120:nano_helmet" /> | <Recipe id="ic2_120:nano_chestplate" /> |
| 纳米护腿 | 纳米靴子 |
| <Recipe id="ic2_120:nano_leggings" /> | <Recipe id="ic2_120:nano_boots" /> |

## 相关

- [工具与防护装备](../reference/tools_armor.md)
- [电池与移动供电](../reference/energy_items.md)
- [量子护甲](quantum_armor.md)——纳米护甲的下一档
- [青铜护甲](bronze_armor.md)
- [纳米剑](nano_saber.md)——同档能量近战武器
- [夜视镜](night_vision_goggles.md)——独立夜视物品；纳米头盔内置相同的夜视系统
