---
navigation:
  title: 防化服
  parent: index.md
  position: 301
  icon: ic2_120:hazmat_helmet
item_ids:
  - ic2_120:hazmat_helmet
  - ic2_120:hazmat_chestplate
  - ic2_120:hazmat_leggings
---

# 防化服

<ItemImage id="ic2_120:hazmat_helmet" scale="4" />

防化服是三件套（头盔、胸甲、护腿），第四个部位由[橡胶靴](rubber_boots.md)补齐。它不是为战斗而设计的——其用途是让你在危险环境中存活下来：核反应堆以及水下。

## 物品视图

| 防化头盔 | 防化胸甲 | 防化护腿 | 橡胶靴 |
|:---:|:---:|:---:|:---:|
| <ItemImage id="ic2_120:hazmat_helmet" scale="2" /> | <ItemImage id="ic2_120:hazmat_chestplate" scale="2" /> | <ItemImage id="ic2_120:hazmat_leggings" scale="2" /> | <ItemImage id="ic2_120:rubber_boots" scale="2" /> |

## 属性

| 部位 | 物品 | 护甲值 | 耐久倍率 | 修复材料 |
|------|------|:---:|:---:|---|
| 头盔 | hazmat_helmet | 1 | 5x | <ItemLink id="ic2_120:rubber" /> |
| 胸甲 | hazmat_chestplate | 3 | 5x | <ItemLink id="ic2_120:rubber" /> |
| 护腿 | hazmat_leggings | 2 | 5x | <ItemLink id="ic2_120:rubber" /> |
| 靴子 | rubber_boots（共用） | 1 | 5x | <ItemLink id="ic2_120:rubber" /> |

靴子部位与[橡胶靴](rubber_boots.md)页面共用——在靴子栏穿同一件物品可同时获得两套套装加成。

## 套装效果

穿戴全部四件（头盔 + 胸甲 + 护腿 + 橡胶靴）即可获得：

- **辐射免疫**——在或靠近活跃核反应堆、辐射区时，完全免受辐射伤害。
- **防化头盔：紧急水下呼吸**——当玩家氧气值降至 **60 ticks（3 秒）或更少**时，头盔会自动从物品栏消耗一个 <ItemLink id="ic2_120:air_cell" />（压缩空气单元）来补满氧气。物品栏中没有空气单元时，玩家仍按正常方式溺水。

套装加成**不**要求下方穿全套金属护甲；橡胶即可提供全部防护。视觉上你可以在防化服*外面*再穿其他护甲，但加成仅检查四个防化部位。

## 使用方式

- 始终将三件防化部件与[橡胶靴](rubber_boots.md)搭配穿在靴子栏——缺少橡胶靴时，辐射免疫不会生效。
- 探索被淹没的反应堆舱室或深水建筑时，随身携带一组 <ItemLink id="ic2_120:air_cell" />。
- 在铁砧或工作台中使用 <ItemLink id="ic2_120:rubber" /> 修复。
- 这套护甲的作战护甲值很低（1/3/2/1）——离开危险区域后请换回战斗套。

## 配方

| 部件 | 配方 | 材料 |
|-------|--------|-------------|
| 防化头盔 | <Recipe id="ic2_120:hazmat_helmet" /> | 橙色染料、橡胶、玻璃、铁栏杆 |
| 防化胸甲 | <Recipe id="ic2_120:hazmat_chestplate" /> | 橡胶、橙色染料 |
| 防化护腿 | <Recipe id="ic2_120:hazmat_leggings" /> | 橡胶、橙色染料 |
| 橡胶靴 | <Recipe id="ic2_120:rubber_boots" /> | 橡胶、白色羊毛 |

## 相关

- [工具与防护装备](../reference/tools_armor.md)
- [电池与移动供电](../reference/energy_items.md)
- [橡胶靴](rubber_boots.md)
- [橡胶树与世界资源](../reference/rubber_and_worldgen.md)
