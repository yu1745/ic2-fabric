---
navigation:
  title: 量子护甲
  parent: index.md
  position: 303
  icon: ic2_120:quantum_helmet
item_ids:
  - ic2_120:quantum_helmet
  - ic2_120:quantum_chestplate
  - ic2_120:quantum_leggings
  - ic2_120:quantum_boots
---

# 量子护甲

<ItemImage id="ic2_120:quantum_helmet" scale="4" />

**状态：计划中，本版本尚未实现。** 量子护甲的四件物品已注册且可合成，但下文描述的完整能量驱动战斗机制、飞行、超级跳跃以及状态免疫行为为**即将发布版本的计划机制**，在当前构建中尚未生效。目前，量子护甲仅作为本模组的顶级原版风格护甲存在：最高防护、25 倍耐久、最高的韧性与击退抗性。

量子护甲是 IC2 的毕业护甲。它旨在让玩家成为自给自足的全身动力装甲单位：每件 1,000 万 EU 缓冲、受击伤害吸收、环境抗性以及移动增强。

## 方块视图

| 量子头盔 | 量子胸甲 | 量子护腿 | 量子靴子 |
|:---:|:---:|:---:|:---:|
| <ItemImage id="ic2_120:quantum_helmet" scale="2" /> | <ItemImage id="ic2_120:quantum_chestplate" scale="2" /> | <ItemImage id="ic2_120:quantum_leggings" scale="2" /> | <ItemImage id="ic2_120:quantum_boots" scale="2" /> |

## 属性

| 部位 | 物品 | 护甲值 | 耐久倍率 | 韧性 | 击退抗性 | 修复材料 |
|------|------|:---:|:---:|:---:|:---:|---|
| 头盔 | quantum_helmet | 4 | 25x | 3.0 | 0.4 | <ItemLink id="ic2_120:iridium_ingot" /> |
| 胸甲 | quantum_chestplate | 9 | 25x | 3.0 | 0.4 | <ItemLink id="ic2_120:iridium_ingot" /> |
| 护腿 | quantum_leggings | 6 | 25x | 3.0 | 0.4 | <ItemLink id="ic2_120:iridium_ingot" /> |
| 靴子 | quantum_boots | 4 | 25x | 3.0 | 0.4 | <ItemLink id="ic2_120:iridium_ingot" /> |

3.0 韧性与 0.4 击退抗性可与来自其他来源的原版修正项叠加。

## 计划机制（暂未生效）

下列行为是量子护甲的设计内容，**将在未来版本中实现**：

- **能量缓冲**——每件护甲可储存 `10,000,000 EU`（`10 MEU`）。可由任何为已穿戴护甲充电的 IC2 电源补充。
- **穿刺伤害减免**——单件伤害吸收比例：头盔 15%、胸甲 44%、护腿 30%、靴子 15%。只要缓冲有电，套装几乎可吸收全部来袭伤害。
- **受击 EU 消耗**——参与吸收本次伤害的每件护甲至少消耗 `900 EU`，每吸收 1 点伤害再额外消耗 `30 EU`。
- **全套状态免疫**——穿戴全套时，消除中毒、凋零与辐射伤害。
- **量子头盔**——水下呼吸（穿戴时自动补满氧气）、夜视（`Alt+M` 切换）、被动的饥饿饱和度恢复。
- **量子胸甲**——飞行（缓冲有电时进入创造式飞行）以及完全防火。
- **量子护腿**——`3x` 疾跑速度，冰面移动速度 `9x`。
- **量子靴子**——摔落伤害减免（与纳米靴子类似但更强）以及 **Ctrl+Space 超级跳跃**（同时按住两键蓄力，最远 9 格跳跃，每次消耗 `1,000 EU`）。

在机制实装之前，这套护甲最好被视为本模组中顶级的基线护甲——25 倍耐久与 3.0 韧性使它成为最强的非下界合金风格套装。

## 使用方式（当前构建）

- 穿戴全部四件以最大化防护。0.4 击退抗性让你在 PvE 中几乎不被推走。
- 在铁砧或工作台中使用 <ItemLink id="ic2_120:iridium_ingot" /> 修复各部件。
- 物品的 NBT 数据接受 EU 充能；战斗、飞行与超级跳跃功能尚未接入。

## 配方

<Recipe id="ic2_120:quantum_helmet" />
<Recipe id="ic2_120:quantum_chestplate" />
<Recipe id="ic2_120:quantum_leggings" />
<Recipe id="ic2_120:quantum_boots" />

## 相关

- [工具与防护装备](../reference/tools_armor.md)
- [电池与移动供电](../reference/energy_items.md)
- [纳米护甲](nano_armor.md)——量子护甲的下一档
- [喷气背包与电力喷气背包](../reference/energy_items.md)——可选的替代飞行方案
