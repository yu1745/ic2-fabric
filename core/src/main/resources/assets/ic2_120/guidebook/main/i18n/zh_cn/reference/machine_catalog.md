---
navigation:
  title: 机器文档索引
  parent: index.md
  position: 210
  icon: minecraft:book
---

# 机器文档索引

本文档目录包含所有机器的详细文档。

---

## 加工机器

| 机器 | 文件 | 电压等级 | 能耗 | 升级槽 |
|------|------|----------|------|--------|
| [铁炉](../machines/iron_furnace.md) | iron-furnace.md | 无（烧燃料） | 燃料 | 0 |
| [粉碎机](../machines/macerator.md) | macerator.md | Tier 1 (LV) | 2 EU/t | 4 |
| [压缩机](../machines/compressor.md) | compressor.md | Tier 1 (LV) | 2 EU/t | 4 |
| [提取机](../machines/extractor.md) | extractor.md | Tier 1 (LV) | 2 EU/t | 4 |
| [金属成型机](../machines/metal_former.md) | metal-former.md | Tier 1 (LV) | 10 EU/t | 4 |
| [电炉](../machines/electric_furnace.md) | electric-furnace.md | Tier 1 (LV) | 3 EU/t | 0 |
| [感应炉](../machines/induction_furnace.md) | induction-furnace.md | Tier 2 (MV) | 可变 | 0 |
| [流体装罐机](../machines/fluid_canner.md) | fluid-bottler.md | Tier 1 (LV) | 2 EU/t | - |
| [固体装罐机](../machines/solid_canner.md) | solid-canner.md | Tier 1 (LV) | 2 EU/t | - |
| [装罐机](../machines/canner.md) | canner.md | Tier 2 (MV) | 4 EU/t | - |
| [方块切割机](../machines/block_cutter.md) | block-cutter.md | Tier 1 (LV) | 4 EU/t | - |
| [回收机](../machines/recycler.md) | recycler.md | Tier 1 (LV) | 1 EU/t | - |
| [洗矿机](../machines/ore_washing_plant.md) | ore-washing-plant.md | Tier 1 (LV) | 16 EU/t | 4 |
| [热能离心机](../machines/centrifuge.md) | centrifuge.md | Tier 2 (MV) | 48+1 EU/t | 4 |
| [高炉](../machines/blast_furnace.md) | blast-furnace.md | Tier 1 | 热量（HU）+ 压缩空气 | 2（仅弹出/抽取） |

## 发电机

| 机器 | 文件 | 电压等级 | 输出 | 燃料/能量来源 |
|------|------|----------|------|---------------|
| [火力发电机](../generator.md) | generator.md | Tier 1 (LV) | 10 EU/t | 煤/木炭 |
| [斯特林发电机](../machines/stirling_generator.md) | stirling-generator.md | Tier 2 (MV) | 50 EU/t | 热量（HU） |
| [蒸汽发电机](../machines/steam_generator.md) | steam-generator.md | Tier 2 (MV) | 蒸汽 | 水 + 热量（HU） |
| [地热发电机](../machines/geo_generator.md) | geo-generator.md | Tier 1 (LV) | 20 EU/t | 岩浆 |
| [太阳能发电机](../machines/solar_generator.md) | solar-generator.md | Tier 1 (LV) | 1 EU/t | 日光 |
| [风力发电机](../machines/wind_generator.md) | wind-generator.md | Tier 1 (LV) | 可变 | 高度/天气 |
| [动能发电机](../machines/kinetic_generator.md) | kinetic-generator.md | Tier 3 (HV) | 512 EU/t | 动能（KU） |
| [创造模式发电机](../machines/creative_generator.md) | creative-generator.md | Tier 1 (LV) | 32 EU/t | 无限（创造模式） |

## 动能机器

| 机器 | 文件 | 输出 | 能量来源 |
|------|------|------|----------|
| [手动动能发电机](../machines/manual_kinetic_generator.md) | manual-kinetic-generator.md | 4-16 KU/t | 手摇曲柄 |
| [水力动能发电机](../machines/water_kinetic_generator.md) | water-kinetic-generator.md | 64-384 KU/t | 水流 + 转子 |
| [风力动能发电机](../machines/wind_kinetic_generator.md) | wind-kinetic-generator.md | 128-768 KU/t | 风力 + 转子 |
| [拴绳动能发电机](../machines/leash_kinetic_generator.md) | leash-kinetic-generator.md | ≤512 KU/t | 动物绕圈 |

## 热量相关机器

| 机器 | 文件 | 说明 |
|------|------|------|
| [固体加热机](../machines/solid_heat_generator.md) | solid-heat-generator.md | 烧煤炭/木炭/焦煤 → 20 HU/t |
| [流体加热机](../machines/fluid_heat_generator.md) | fluid-heat-generator.md | 烧生物燃料 → 32 HU/t |
| [电力加热机](../machines/electric_heat_generator.md) | electric-heat-generator.md | 1:1 EU→HU，线圈最高 100 HU/t |
| [放射性同位素温差加热机](../machines/rt_heat_generator.md) | rt-heat-generator.md | RTG 靶丸永久运行，最高 64 HU/t |
| [流体热交换机](../machines/liquid_heat_exchanger.md) | liquid-heat-exchanger.md | 热冷却液/岩浆 → HU + 冷却液 |
| [太阳能蒸馏机](../machines/solar_distiller.md) | solar-distiller.md | 太阳能免费蒸馏水 |
| [冷凝器](../machines/condenser.md) | condenser.md | 蒸汽 + EU → 蒸馏水 |
| [发酵机](../machines/fermenter.md) | fermenter.md | 耗热 + 生物质 → 生物燃料 + 肥料 |

## 资源机器

| 机器 | 文件 | 电压等级 | 功能 |
|------|------|----------|------|
| [泵](../machines/pump.md) | pump.md | Tier 1 (LV) | 流体提取 |
| [采矿机](../machines/miner.md) | miner.md | Tier 2 (MV) | 自动采矿 |
| [高级采矿机](../machines/advanced_miner.md) | advanced-miner.md | Tier 3 (HV) | 高级自动采矿（过滤/精准采集/大范围） |
| [区块加载器](../machines/chunk_loader.md) | chunk-loader.md | Tier 1 (LV) | 强制加载区块 |
| [动物屠宰机](../machines/animal_slaughterer.md) | animal-slaughterer.md | Tier 1 (LV) | 自动屠宰动物 |
| [作物收获机](../machines/crop_harvester.md) | crop-harvester.md | Tier 1 (LV) | 自动收获 |
| [作物监管机](../machines/cropmatron.md) | cropmatron.md | Tier 1 (LV) | 作物管理 |
| [紫外线灯](../machines/uv_lamp.md) | uv-lamp.md | Tier 1-5（可变） | 作物生长加速 |
| [牲畜监管机](../machines/animalmatron.md) | animalmatron.md | Tier 1 (LV) | 动物养殖 |
| [磁化机](../machines/magnetizer.md) | magnetizer.md | Tier 1 (LV) | 磁化铁块 |

## 储电与变压器

| 机器 | 文件 | 容量 | 输入/输出 |
|------|------|------|-----------|
| [储电箱](../machines/energy_storage.md) | storage.md#batbox | 40,000 EU | 32 EU/t |
| [CESU](../machines/energy_storage.md) | storage.md#cesu | 300,000 EU | 128 EU/t |
| [MFE](../machines/energy_storage.md) | storage.md#mfe | 4,000,000 EU | 512 EU/t |
| [MFSU](../machines/energy_storage.md) | storage.md#mfsu | 40,000,000 EU | 2048 EU/t |
| [变压器](../machines/transformer.md) | transformer.md | - | 电压转换 |

## 储存容器

| 机器 | 文件 | 容量 |
|------|------|------|
| [储物箱](../machines/storage_box.md) | storage-box.md | 27-126 格（5 种材质） |
| [流体储罐](../machines/tank.md) | fluid-tanks.md | 32-1024 桶（4 种材质） |

## 核电

| 机器 | 文件 | 说明 |
|------|------|------|
| [核反应堆](../machines/nuclear_reactor.md) | nuclear-reactor.md | 反应堆 + 反应仓（EU 模式） |
| [流体冷却反应堆](../machines/nuclear_reactor_fluid_mode.md) | nuclear-reactor-fluid-mode.md | 压力容器、流体接口、访问接口、红石接口（热模式） |

## UU 物质

| 机器 | 文件 | 说明 |
|------|------|------|
| [UU 物质系统](../machines/matter_generator.md) | uu-matter.md | 物质生成机 + UU 扫描仪 + 样板存储器 + 复制机 |

## 焦炉

| 机器 | 文件 | 说明 |
|------|------|------|
| [焦炭窑](../machines/coke_kiln.md) | coke-kiln.md | 多方块结构，木炭/焦煤 + 杂酚油 |

## 传送

| 机器 | 文件 | 功能 |
|------|------|------|
| [传送机](../machines/teleporter.md) | teleporter.md | 实体传送 |

## 其他

| 机器 | 文件 | 说明 |
|------|------|------|
| [特斯拉线圈](../machines/tesla_coil.md) | tesla-coil.md | 红石触发电击怪物 |
| [日光灯](../machines/luminator_flat.md) | luminator.md | 6 面贴装照明 |

---

## 相关文档

- [EU 能量系统](../systems/eu_energy.md) - 导线、过压爆炸
- [发电与储电](../guides/power_generation.md) - 发电机指南
- [升级系统](../guides/upgrades.md) - 升级槽说明
- [动能系统](../systems/kinetic_transmission.md) - KU 传动详情
- [热能系统](../systems/heat_system.md) - HU 热量

