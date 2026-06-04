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

**状态：计划中，本版本尚未实现。** 纳米护甲的四件物品已注册且可合成，但下文描述的能量驱动战斗机制为**即将发布版本的计划机制**，在当前构建中尚未生效。目前，纳米护甲仅作为一套高耐久、高防护的护甲存在，受到伤害时不会消耗 EU。

纳米护甲是 IC2 第一档能量增强护甲。设计目标是利用内部 EU 缓冲吸收伤害，在基础防护值之上叠加受击时的能量消耗。套装在科技等级上位于[青铜护甲](bronze_armor.md)与[量子护甲](quantum_armor.md)之间。

## 方块视图

| 纳米头盔 | 纳米胸甲 | 纳米护腿 | 纳米靴子 |
|:---:|:---:|:---:|:---:|
| <ItemImage id="ic2_120:nano_helmet" scale="2" /> | <ItemImage id="ic2_120:nano_chestplate" scale="2" /> | <ItemImage id="ic2_120:nano_leggings" scale="2" /> | <ItemImage id="ic2_120:nano_boots" scale="2" /> |

## 属性

| 部位 | 物品 | 护甲值 | 耐久倍率 | 韧性 | 修复材料 |
|------|------|:---:|:---:|:---:|---|
| 头盔 | nano_helmet | 3 | 15x | 2.0 | <ItemLink id="ic2_120:carbon_fibre" /> |
| 胸甲 | nano_chestplate | 8 | 15x | 2.0 | <ItemLink id="ic2_120:carbon_fibre" /> |
| 护腿 | nano_leggings | 6 | 15x | 2.0 | <ItemLink id="ic2_120:carbon_fibre" /> |
| 靴子 | nano_boots | 3 | 15x | 2.0 | <ItemLink id="ic2_120:carbon_fibre" /> |

韧性 2.0 与原版下界合金等级持平，即便在叠加任何 EU 机制之前，这套护甲也具备可观的防护。

## 计划机制（暂未生效）

下列行为是纳米护甲的设计内容，**将在未来版本中实现**：

- **能量缓冲**——每件护甲可储存 `1,000,000 EU`（`1 MEU`）。可由任何为已穿戴护甲充电的 IC2 电源补充，或将物品放在充电台上充能。
- **伤害吸收**——玩家受击时，护甲按每点吸收伤害消耗 `5,000 EU`（按覆盖该次伤害的护甲件数计算）。缓冲耗尽后，护甲回退为原版护甲行为。
- **纳米靴子摔落减免**——4 格或以内的摔落不造成伤害、不消耗 EU；5–12 格的摔落消耗 EU 仍不造成伤害；超过 12 格的摔落则按原版摔落伤害结算。
- **纳米头盔夜视**——按 `Alt+M` 切换，激活时持续消耗 EU（典型速率：数 EU/tick）。

在机制实装之前，这套护甲最好被视为拥有下界合金同级韧性、15 倍原版耐久的高阶护甲。

## 使用方式（当前构建）

- 穿戴全部四件以最大化防护。2.0 韧性已能有效抵抗高伤害攻击。
- 在铁砧或工作台中使用 <ItemLink id="ic2_120:carbon_fibre" /> 修复各部件。
- 物品的 NBT 数据接受 EU 充能，但受击消耗与夜视功能尚未接入。

## 配方

<Recipe id="ic2_120:nano_helmet" />
<Recipe id="ic2_120:nano_chestplate" />
<Recipe id="ic2_120:nano_leggings" />
<Recipe id="ic2_120:nano_boots" />

## 相关

- [工具与防护装备](../reference/tools_armor.md)
- [电池与移动供电](../reference/energy_items.md)
- [量子护甲](quantum_armor.md)——纳米护甲的下一档
- [青铜护甲](bronze_armor.md)
