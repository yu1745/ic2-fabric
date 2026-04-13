# 机器文档索引

本文档目录包含所有机器的详细文档。

---

## 加工机器

| 机器 | 文件 | 电压等级 | 能耗 | 升级槽 |
|------|------|----------|------|--------|
| [粉碎机](macerator.md) | macerator.md | Tier 1 (LV) | 2 EU/t | 4 |
| [压缩机](compressor.md) | compressor.md | Tier 1 (LV) | 2 EU/t | 4 |
| [提取机](extractor.md) | extractor.md | Tier 1 (LV) | 2 EU/t | 4 |
| [金属成型机](metal-former.md) | metal-former.md | Tier 1 (LV) | 10 EU/t | 4 |
| [电炉](electric-furnace.md) | electric-furnace.md | Tier 1 (LV) | 3 EU/t | 0 |
| [感应炉](induction-furnace.md) | induction-furnace.md | Tier 2 (MV) | 可变 | 0 |
| [流体装罐机](fluid-bottler.md) | fluid-bottler.md | Tier 1 (LV) | 2 EU/t | - |
| [固体装罐机](solid-canner.md) | solid-canner.md | Tier 1 (LV) | 2 EU/t | - |
| [装罐机](canner.md) | canner.md | Tier 2 (MV) | 4 EU/t | - |
| [方块切割机](block-cutter.md) | block-cutter.md | Tier 1 (LV) | 4 EU/t | - |
| [回收机](recycler.md) | recycler.md | Tier 1 (LV) | 1 EU/t | - |
| [洗矿机](ore-washing-plant.md) | ore-washing-plant.md | Tier 1 (LV) | 16 EU/t | 4 |
| [热能离心机](centrifuge.md) | centrifuge.md | Tier 2 (MV) | 48+1 EU/t | 4 |

## 发电机

| 机器 | 文件 | 电压等级 | 输出 | 燃料/能量来源 |
|------|------|----------|------|---------------|
| [火力发电机](generator.md) | generator.md | Tier 1 (LV) | 10 EU/t | 煤/木炭 |
| [斯特林发电机](stirling-generator.md) | stirling-generator.md | Tier 2 (MV) | 50 EU/t | 热量（HU） |
| [地热发电机](geo-generator.md) | geo-generator.md | Tier 1 (LV) | 20 EU/t | 岩浆 |
| [太阳能发电机](solar-generator.md) | solar-generator.md | Tier 1 (LV) | 1 EU/t | 日光 |
| [风力发电机](wind-generator.md) | wind-generator.md | Tier 1 (LV) | 可变 | 高度/天气 |
| [动能发电机](kinetic-generator.md) | kinetic-generator.md | Tier 3 (HV) | 512 EU/t | 动能（KU） |

## 动能机器

| 机器 | 文件 | 输出 | 能量来源 |
|------|------|------|----------|
| [手动动能发电机](manual-kinetic-generator.md) | manual-kinetic-generator.md | 4-16 KU/t | 手摇曲柄 |
| [水力动能发电机](water-kinetic-generator.md) | water-kinetic-generator.md | 64-384 KU/t | 水流 + 转子 |
| [风力动能发电机](wind-kinetic-generator.md) | wind-kinetic-generator.md | 128-768 KU/t | 风力 + 转子 |

## 资源机器

| 机器 | 文件 | 电压等级 | 功能 |
|------|------|----------|------|
| [泵](pump.md) | pump.md | Tier 1 (LV) | 流体提取 |
| [采矿机](miner.md) | miner.md | Tier 3 (HV) | 自动采矿 |
| [作物收获机](crop-harvester.md) | crop-harvester.md | Tier 1 (LV) | 自动收获 |
| [作物监管机](cropmatron.md) | cropmatron.md | Tier 1 (LV) | 作物管理 |
| [牲畜监管机](animalmatron.md) | animalmatron.md | Tier 1 (LV) | 动物养殖 |
| [磁化机](magnetizer.md) | magnetizer.md | Tier 1 (LV) | 磁化铁块 |

## 储电与变压器

| 机器 | 文件 | 容量 | 输入/输出 |
|------|------|------|-----------|
| [储电箱](storage.md) | storage.md#batbox | 40,000 EU | 32 EU/t |
| [CESU](storage.md) | storage.md#cesu | 300,000 EU | 128 EU/t |
| [MFE](storage.md) | storage.md#mfe | 4,000,000 EU | 512 EU/t |
| [MFSU](storage.md) | storage.md#mfsu | 40,000,000 EU | 2048 EU/t |
| [变压器](transformer.md) | transformer.md | - | 电压转换 |

---

## 相关文档

- [EU 能量系统](../systems/eu-energy.md) - 导线、过压爆炸
- [发电与储电](../guides/power-generation.md) - 发电机指南
- [升级系统](../guides/upgrades.md) - 升级槽说明
- [动能系统](../systems/kinetic-transmission.md) - KU 传动详情
- [热能系统](../systems/heat-system.md) - HU 热量
- [核反应堆](../systems/nuclear-reactor.md) - 核电系统
