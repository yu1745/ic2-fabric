---
navigation:
  title: 流体、单元与桶
  parent: index.md
  position: 215
  icon: ic2_120:empty_cell
item_ids:
  - ic2_120:empty_cell
  - ic2_120:water_cell
  - ic2_120:lava_cell
  - ic2_120:coolant_cell
  - ic2_120:hot_coolant_cell
  - ic2_120:steam_cell
  - ic2_120:superheated_steam_cell
  - ic2_120:biomass_cell
  - ic2_120:biofuel_cell
  - ic2_120:uu_matter_cell
  - ic2_120:weed_ex_cell
  - ic2_120:construction_foam_cell
  - ic2_120:air_cell
  - ic2_120:distilled_water_cell
  - ic2_120:pahoehoe_lava_cell
  - ic2_120:coolant_bucket
  - ic2_120:hot_coolant_bucket
  - ic2_120:biomass_bucket
  - ic2_120:biofuel_bucket
  - ic2_120:uu_matter_bucket
  - ic2_120:weed_ex_bucket
  - ic2_120:construction_foam_bucket
  - ic2_120:distilled_water_bucket
  - ic2_120:pahoehoe_lava_bucket
  - ic2_120:creosote_bucket
  - ic2_120:compressed_air_bucket
---

# 流体、单元与桶

IC2 使用桶、单元、储罐和管道移动流体。单元适合配方和背包携带，桶适合与原版交互，储罐和管道适合自动化。

所有已注册物质都使用同一套 Fabric 流体表示进行储存和运输。“蒸汽”“压缩空气”“液体燃料”等名称描述的是具体玩法行为，并不是管道系统中的不同类别。

## 常用流体

| 流体 | 常见用途 |
|------|----------|
| 冷却液 / 热冷却液 | 核反应堆流体冷却、热交换 |
| 蒸汽 / 过热蒸汽 | 蒸汽发生器、蒸汽动能发生机 |
| 生物质 / 生物燃油 | 发酵与半流质发电 |
| UU 物质 | 复制系统 |
| 蒸馏水 | 高级热力链路 |
| 建筑泡沫 | 建筑泡沫喷枪、泡沫方块 |
| 除草剂 | 作物护理 |
| 杂酚油、压缩空气、熔岩岩浆 | 焦炭窑、高炉与热力工具链中的辅助流体 |

## 单元

- 空单元可承载水、岩浆、冷却液、热冷却液、蒸汽、过热蒸汽、生物质、生物燃油、UU 物质、除草剂、建筑泡沫等。
- 压缩空气单元可被防化头盔在水下消耗，用于恢复氧气。
- 反应堆冷却单元不是普通流体单元，它们是反应堆内部热缓冲组件。

## 储罐与管道

- 青铜、铁、钢、铱储罐用于固定储液。
- 青铜管道与碳纤维管道用于流体网络。
- 泵附件可让管道从相邻容器或世界方块抽取流体。

相关系统：[流体管道系统](../systems/fluid_pipes.md)
