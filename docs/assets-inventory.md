# Assets 清单

本档列出了 `assets` 目录中所有的方块和物品资源。

**复选框说明**：表格首列为实现状态，`- [x]` 表示已在 ic2_120 模组中实现，`- [ ]` 表示尚未实现。

**材质路径约定**：ic2_120 模组内所有模型 JSON 引用纹理时，命名空间路径**不带复数 s**——方块纹理用 `ic2:block/...`（不用 `blocks`），物品纹理用 `ic2:item/...`（不用 `items`）。例如：`ic2:block/resource/copper_block`、`ic2:item/resource/ingot/copper`。

## 统计信息

- **方块总数**: 244 个（来自 blockstates 目录）
- **物品模型总数**: 518 个（来自 models/item 目录）
- **方块模型数**: 30 个（来自 models/block 目录，包含多开门等变体共 41 个文件）
- **纹理文件总数**: 1537 个（来自 textures 目录）

---

## 一、方块列表 (Block States)

### 矿物与矿石类
|  | ID | 名称 |
|--|----|------|
| - [x] | block.ic2.lead_ore | 铅矿石 |
| - [x] | block.ic2.tin_ore | 锡矿石 |
| - [x] | block.ic2.uranium_ore | 铀矿石 |
| - [x] | block.ic2.deepslate_lead_ore | 深层铅矿石 |
| - [x] | block.ic2.deepslate_tin_ore | 深层锡矿石 |
| - [x] | block.ic2.deepslate_uranium_ore | 深层铀矿石 |
| - [x] | block.ic2.iridium_ore | 铱矿石 |
| - [x] | block.ic2.copper_ore | 铜矿石 |

### 块状材料类
|  | ID | 名称 |
|--|----|------|
| - [x] | block.ic2.raw_lead_block | 粗铅块 |
| - [x] | block.ic2.raw_tin_block | 粗锡块 |
| - [x] | block.ic2.raw_uranium_block | 粗铀块 |
| - [x] | block.ic2.bronze_block | 青铜块 |
| - [x] | block.ic2.lead_block | 铅块 |
| - [x] | block.ic2.steel_block | 钢块 |
| - [x] | block.ic2.tin_block | 锡块 |
| - [x] | block.ic2.uranium_block | 铀块 |
| - [x] | block.ic2.silver_block | 银块 |
| - [x] | block.ic2.coal_block | 压缩煤块 |
| - [x] | block.ic2.reinforced_stone | 防爆石 |
| - [x] | block.ic2.basalt | 玄武石 |

### 木材相关类
|  | ID | 名称 |
|--|----|------|
| - [x] | block.ic2.rubber_log | 橡胶原木 |
| - [x] | block.ic2.stripped_rubber_log | 去皮橡胶原木 |
| - [x] | block.ic2.rubber_wood | 橡胶木 |
| - [x] | block.ic2.stripped_rubber_wood | 去皮橡胶木 |
| - [x] | block.ic2.rubber_planks | 橡胶木板 |
| - [x] | block.ic2.rubber_sapling | 橡胶树苗 |
| - [x] | block.ic2.rubber_button | 橡胶木按钮 |
| - [x] | block.ic2.rubber_door | 橡胶木门 |
| - [x] | block.ic2.rubber_fence | 橡胶木栅栏 |
| - [x] | block.ic2.rubber_fence_gate | 橡胶木栅栏门 |
| - [x] | block.ic2.rubber_pressure_plate | 橡胶木压力板 |
| - [x] | block.ic2.rubber_sign | 橡胶木告示牌 |
| - [x] | block.ic2.rubber_slab | 橡胶木台阶 |
| - [x] | block.ic2.rubber_stairs | 橡胶木楼梯 |
| - [x] | block.ic2.rubber_trapdoor | 橡胶木活板门 |
| - [x] | block.ic2.rubber_leaves | 橡胶树叶 |
| - [x] | block.ic2.wooden_scaffold | 脚手架 |
| - [x] | block.ic2.reinforced_wooden_scaffold | 强化脚手架 |
| - [x] | block.ic2.iron_scaffold | 铁质脚手架 |
| - [x] | block.ic2.reinforced_iron_scaffold | 强化铁质脚手架 |

### 建筑与装饰类
|  | ID | 名称 |
|--|----|------|
| - [x] | block.ic2.iron_fence | 铁栅栏 |
| - [x] | block.ic2.reinforced_glass | 防爆玻璃 |
| - [x] | block.ic2.foam | 建筑泡沫 |
| - [x] | block.ic2.white_wall | 建筑泡沫墙 (白色) |
| - [x] | block.ic2.orange_wall | 建筑泡沫墙 (橙色) |
| - [x] | block.ic2.magenta_wall | 建筑泡沫墙 (品红色) |
| - [x] | block.ic2.light_blue_wall | 建筑泡沫墙 (浅蓝色) |
| - [x] | block.ic2.yellow_wall | 建筑泡沫墙 (黄色) |
| - [x] | block.ic2.lime_wall | 建筑泡沫墙 (柠檬色) |
| - [x] | block.ic2.pink_wall | 建筑泡沫墙 (粉色) |
| - [x] | block.ic2.gray_wall | 建筑泡沫墙 (灰色) |
| - [x] | block.ic2.light_gray_wall | 建筑泡沫墙 (浅灰色) |
| - [x] | block.ic2.cyan_wall | 建筑泡沫墙 (青色) |
| - [x] | block.ic2.purple_wall | 建筑泡沫墙 (紫色) |
| - [x] | block.ic2.blue_wall | 建筑泡沫墙 (蓝色) |
| - [x] | block.ic2.brown_wall | 建筑泡沫墙 (棕色) |
| - [x] | block.ic2.green_wall | 建筑泡沫墙 (绿色) |
| - [x] | block.ic2.red_wall | 建筑泡沫墙 (红色) |
| - [x] | block.ic2.black_wall | 建筑泡沫墙 (黑色) |
| - [x] | block.ic2.resin_sheet | 树脂垫 |
| - [x] | block.ic2.rubber_sheet | 橡胶垫 |
| - [x] | block.ic2.wool_sheet | 羊毛垫 |
| - [x] | block.ic2.mining_pipe | 采矿管道 |
| - [x] | block.ic2.itnt | 工业 TNT |

### 机器 - 发电类
|  | ID | 名称 |
|--|----|------|
| - [x] | block.ic2.generator | 火力发电机 |
| - [x] | block.ic2.geo_generator | 地热发电机 |
| - [ ] | block.ic2.kinetic_generator | 动能发电机 |
| - [ ] | block.ic2.rt_generator | 放射性同位素温差发电机 |
| - [ ] | block.ic2.semifluid_generator | 半流质发电机 |
| - [ ] | block.ic2.solar_generator | 太阳能发电机 |
| - [ ] | block.ic2.stirling_generator | 斯特林发电机 |
| - [ ] | block.ic2.water_generator | 水力发电机 |
| - [ ] | block.ic2.wind_generator | 风力发电机 |
| - [ ] | block.ic2.electric_heat_generator | 电力加热机 |
| - [ ] | block.ic2.fluid_heat_generator | 流体加热机 |
| - [ ] | block.ic2.rt_heat_generator | 放射性同位素温差加热机 |
| - [ ] | block.ic2.solid_heat_generator | 固体加热机 |
| - [ ] | block.ic2.electric_kinetic_generator | 电力动能发生机 |
| - [ ] | block.ic2.manual_kinetic_generator | 手摇动能发生机 |
| - [ ] | block.ic2.steam_kinetic_generator | 蒸汽动能发生机 |
| - [ ] | block.ic2.stirling_kinetic_generator | 斯特林动能发生器 |
| - [ ] | block.ic2.water_kinetic_generator | 水力动能发生机 |
| - [ ] | block.ic2.wind_kinetic_generator | 风力动能发生机 |
| - [ ] | block.ic2.creative_generator | 创造模式发电器 |

### 机器 - 核反应堆类
|  | ID | 名称 |
|--|----|------|
| - [ ] | block.ic2.nuclear_reactor | 核反应堆 |
| - [ ] | block.ic2.reactor_access_hatch | 反应堆访问接口 |
| - [ ] | block.ic2.reactor_chamber | 核反应仓 |
| - [ ] | block.ic2.reactor_fluid_port | 反应堆流体接口 |
| - [ ] | block.ic2.reactor_redstone_port | 反应堆红石接口 |

### 机器 - 基础加工类
|  | ID | 名称 |
|--|----|------|
| - [x] | block.ic2.compressor | 压缩机 |
| - [x] | block.ic2.electric_furnace | 电炉 |
| - [ ] | block.ic2.extractor | 提取机 |
| - [ ] | block.ic2.iron_furnace | 铁炉 |
| - [x] | block.ic2.macerator | 打粉机 |
| - [ ] | block.ic2.recycler | 回收机 |
| - [ ] | block.ic2.blast_furnace | 高炉 |
| - [ ] | block.ic2.block_cutter | 方块切割机 |
| - [ ] | block.ic2.centrifuge | 热能离心机 |
| - [ ] | block.ic2.fermenter | 发酵机 |
| - [ ] | block.ic2.induction_furnace | 感应炉 |
| - [x] | block.ic2.metal_former | 金属成型机 |
| - [ ] | block.ic2.ore_washing_plant | 洗矿机 |

### 机器 - 高级类
|  | ID | 名称 |
|--|----|------|
| - [ ] | block.ic2.advanced_miner | 高级采矿机 |
| - [ ] | block.ic2.crop_harvester | 作物收割机 |
| - [ ] | block.ic2.miner | 采矿机 |
| - [ ] | block.ic2.mass_fabricator | 物质生成机 |
| - [ ] | block.ic2.uu_assembly_bench | UU 组装台 |
| - [ ] | block.ic2.matter_generator | 物质生成机 |
| - [ ] | block.ic2.pattern_storage | 模式存储机 |
| - [ ] | block.ic2.replicator | 复制机 |
| - [ ] | block.ic2.uu_scanner | 模式扫描机 |

### 机器 - 流体处理类
|  | ID | 名称 |
|--|----|------|
| - [ ] | block.ic2.condenser | 冷凝机 |
| - [ ] | block.ic2.fluid_bottler | 流体装罐机 |
| - [ ] | block.ic2.fluid_distributor | 流体分配机 |
| - [ ] | block.ic2.fluid_regulator | 流体流量调节机 |
| - [ ] | block.ic2.liquid_heat_exchanger | 流体热交换机 |
| - [ ] | block.ic2.pump | 泵 |
| - [ ] | block.ic2.solar_distiller | 太阳能蒸馏机 |
| - [ ] | block.ic2.steam_generator | 蒸汽机 |
| - [ ] | block.ic2.steam_repressurizer | 蒸汽 Re-Pressurizer |

### 机器 - 其他功能类
|  | ID | 名称 |
|--|----|------|
| - [ ] | block.ic2.item_buffer | 物品缓冲机 |
| - [ ] | block.ic2.magnetizer | 磁化机 |
| - [ ] | block.ic2.sorting_machine | 电动分拣机 |
| - [ ] | block.ic2.teleporter | 传送机 |
| - [ ] | block.ic2.terraformer | 地形转换机 |
| - [ ] | block.ic2.tesla_coil | 特斯拉线圈 |
| - [ ] | block.ic2.canner | 流体/固体装罐机 |
| - [ ] | block.ic2.solid_canner | 固体装罐机 |
| - [ ] | block.ic2.energy_o_mat | 能源交易机 |
| - [ ] | block.ic2.trade_o_mat | 贸易箱 |
| - [ ] | block.ic2.personal_chest | 保险箱 |
| - [ ] | block.ic2.chunk_loader | 区块加载器 |
| - [ ] | block.ic2.weighted_fluid_distributor | 高级流体分配机 |
| - [ ] | block.ic2.weighted_item_distributor | 高级物品分配机 |

### 机器 - 机械外壳类
|  | ID | 名称 |
|--|----|------|
| - [x] | block.ic2.machine | 基础机械外壳 |
| - [x] | block.ic2.advanced_machine | 高级机械外壳 |
| - [ ] | block.ic2.reactor_vessel | 核反应堆压力容器 |

### 机器 - 储能设备类
|  | ID | 名称 |
|--|----|------|
| - [ ] | block.ic2.batbox | 储电箱 |
| - [ ] | block.ic2.cesu | CESU 储电箱 |
| - [ ] | block.ic2.mfe | MFE 储电箱 |
| - [x] | block.ic2.mfsu | MFSU 储电箱 |
| - [ ] | block.ic2.batbox_chargepad | 充电座 (BatBox) |
| - [ ] | block.ic2.cesu_chargepad | 充电座 (CESU) |
| - [ ] | block.ic2.mfe_chargepad | 充电座 (MFE) |
| - [ ] | block.ic2.mfsu_chargepad | 充电座 (MFSU) |

### 机器 - 电缆与变压器类
|  | ID | 名称 |
|--|----|------|
| - [x] | block.ic2.copper_cable | 铜质导线 |
| - [ ] | block.ic2.insulated_copper_cable | 绝缘铜质导线 |
| - [x] | block.ic2.glass_fibre_cable | 玻璃纤维导线 |
| - [x] | block.ic2.gold_cable | 金质导线 |
| - [ ] | block.ic2.insulated_gold_cable | 绝缘金质导线 |
| - [ ] | block.ic2.double_insulated_gold_cable | 2x绝缘金质导线 |
| - [x] | block.ic2.iron_cable | 高压导线 |
| - [ ] | block.ic2.insulated_iron_cable | 绝缘高压导线 |
| - [ ] | block.ic2.double_insulated_iron_cable | 2x绝缘高压导线 |
| - [ ] | block.ic2.triple_insulated_iron_cable | 3x绝缘高压导线 |
| - [x] | block.ic2.tin_cable | 锡质导线 |
| - [ ] | block.ic2.insulated_tin_cable | 绝缘锡质导线 |
| - [ ] | block.ic2.detector_cable | EU 检测导线 |
| - [ ] | block.ic2.splitter_cable | EU 分流导线 |
| - [ ] | block.ic2.lv_transformer | 低压变压器 |
| - [ ] | block.ic2.mv_transformer | 中压变压器 |
| - [ ] | block.ic2.hv_transformer | 高压变压器 |
| - [ ] | block.ic2.ev_transformer | 超高压变压器 |
| - [ ] | block.ic2.tank | 流体储存器 |

### 机器 - 特殊类
|  | ID | 名称 |
|--|----|------|
| - [ ] | block.ic2.nuke | 核弹 |

### 机器 - 工作台类
|  | ID | 名称 |
|--|----|------|
| - [ ] | block.ic2.industrial_workbench | 工业工作台 |
| - [ ] | block.ic2.batch_crafter | 批量工作台 |

### 机器 - 储物类
|  | ID | 名称 |
|--|----|------|
| - [x] | block.ic2.wooden_storage_box | 木质储物箱 |
| - [x] | block.ic2.iron_storage_box | 铁质储物箱 |
| - [x] | block.ic2.bronze_storage_box | 青铜储物箱 |
| - [x] | block.ic2.steel_storage_box | 钢制储物箱 |
| - [x] | block.ic2.iridium_storage_box | 铱储物箱 |
| - [ ] | block.ic2.bronze_tank | 青铜储罐 |
| - [ ] | block.ic2.iridium_tank | 铱储罐 |

### 机器 - 输入输出类
|  | ID | 名称 |
|--|----|------|
| - [ ] | block.ic2.rci_rsh | 反应堆冷却液注入器 (RSH) |
| - [ ] | block.ic2.rci_lzh | 反应堆冷却液注入器 (LZH) |

### 作物类
|  | ID | 名称 |
|--|----|------|
| - [ ] | block.ic2.crop | 作物 |
| - [ ] | block.ic2.acacia_sapling_crop | 金合欢树苗作物 |
| - [ ] | block.ic2.aurelia_crop | Aurelia 作物 |
| - [ ] | block.ic2.beetroots_crop | 甜菜作物 |
| - [ ] | block.ic2.birch_sapling_crop | 白桦树苗作物 |
| - [ ] | block.ic2.blackthorn_crop | Blackthorn 作物 |
| - [ ] | block.ic2.brown_mushroom_crop | 棕色蘑菇作物 |
| - [ ] | block.ic2.carrots_crop | 胡萝卜作物 |
| - [ ] | block.ic2.cocoa_crop | 可可作物 |
| - [ ] | block.ic2.coffee_crop | 咖啡作物 |
| - [ ] | block.ic2.cyazint_crop | Cyazint 作物 |
| - [ ] | block.ic2.cyprium_crop | Cyprium 作物 |
| - [ ] | block.ic2.dandelion_crop | 蒲公英作物 |
| - [ ] | block.ic2.dark_oak_sapling_crop | 深色橡树树苗作物 |
| - [ ] | block.ic2.eating_plant_crop | 食用植物作物 |
| - [ ] | block.ic2.ferru_crop | Ferru 作物 |
| - [ ] | block.ic2.flax_crop | 亚麻作物 |
| - [ ] | block.ic2.hops_crop | 啤酒花作物 |
| - [ ] | block.ic2.crop_stick | 作物棍 |

### 线缆泡沫类
|  | ID | 名称 |
|--|----|------|
| - [ ] | block.ic2.copper_foam_cable | 铜质泡沫电缆 |
| - [ ] | block.ic2.detector_foam_cable | 检测泡沫电缆 |
| - [ ] | block.ic2.double_insulated_gold_foam_cable | 2x绝缘金质泡沫电缆 |
| - [ ] | block.ic2.double_insulated_iron_foam_cable | 2x绝缘铁质泡沫电缆 |
| - [ ] | block.ic2.glass_fibre_foam_cable | 玻璃纤维泡沫电缆 |
| - [ ] | block.ic2.insulated_copper_foam_cable | 绝缘铜质泡沫电缆 |
| - [ ] | block.ic2.insulated_gold_foam_cable | 绝缘金质泡沫电缆 |
| - [ ] | block.ic2.insulated_iron_foam_cable | 绝缘铁质泡沫电缆 |
| - [ ] | block.ic2.insulated_tin_foam_cable | 绝缘锡质泡沫电缆 |

### 特殊门类
|  | ID | 名称 |
|--|----|------|
| - [ ] | block.ic2.reinforced_door | 防爆门 |


---

## 三、方块模型 (models/block)

以下为独立的方块模型文件（30 个基础模型，包含多开门等变体共 41 个文件）：

|  | 模型文件 | 对应方块类型 |
|--|---------|-------------|
| - [ ] | `bronze_storage_box.json` | 青铜储物箱 |
| - [ ] | `bronze_tank.json` | 青铜储罐 |
| - [ ] | `iridium_storage_box.json` | 铱储物箱 |
| - [ ] | `iridium_tank.json` | 铱储罐 |
| - [ ] | `iron_storage_box.json` | 铁质储物箱 |
| - [ ] | `iron_tank.json` | 铁储罐 |
| - [ ] | `reinforced_door_bottom_left.json` | 防爆门下半左 |
| - [ ] | `reinforced_door_bottom_left_open.json` | 防爆门下半左（打开） |
| - [ ] | `reinforced_door_bottom_right.json` | 防爆门下半右 |
| - [ ] | `reinforced_door_bottom_right_open.json` | 防爆门下半右（打开） |
| - [ ] | `reinforced_door_top_left.json` | 防爆门上半左 |
| - [ ] | `reinforced_door_top_left_open.json` | 防爆门上半左（打开） |
| - [ ] | `reinforced_door_top_right.json` | 防爆门上半右 |
| - [ ] | `reinforced_door_top_right_open.json` | 防爆门上半右（打开） |
| - [ ] | `reinforced_glass.json` | 防爆玻璃 |
| - [ ] | `reinforced_stone.json` | 防爆石 |
| - [ ] | `resin_sheet.json` | 树脂垫 |
| - [ ] | `rubber_sheet.json` | 橡胶垫 |
| - [ ] | `sheet_base.json` | 垫子基座（用于羊毛垫等） |
| - [ ] | `steel_storage_box.json` | 钢制储物箱 |
| - [ ] | `steel_tank.json` | 钢储罐 |
| - [ ] | `storage_box.json` | 储物箱基座 |
| - [ ] | `tank.json` | 储罐基座 |
| - [ ] | `wooden_storage_box.json` | 木质储物箱 |
| - [ ] | `wool_sheet.json` | 羊毛垫 |

---

## 四、纹理清单 (Textures)

总计：**1537 个纹理文件**

### 4.1 bcTrigger（能量显示触发器）- 20 个文件
用于 UI 上的能量/热量/工作状态显示图标：

| 文件名 | 用途 |
|-------|------|
| `capacitor_empty.png` | 电容器空 |
| `capacitor_full.png` | 电容器满 |
| `capacitor_has_energy.png` | 电容器有能量 |
| `capacitor_has_room.png` | 电容器有空位 |
| `charge_empty.png` | 充电空 |
| `charge_full.png` | 充电满 |
| `charge_partial.png` | 充电中 |
| `discharge_empty.png` | 放电空 |
| `discharge_full.png` | 放电满 |
| `discharge_partial.png` | 放电中 |
| `energy_flowing.png` | 能量流动中 |
| `energy_not_flowing.png` | 能量未流动 |
| `full_heat.png` | 热量满 |
| `has_fuel.png` | 有燃料 |
| `has_scrap.png` | 有废料 |
| `not_working.png` | 不工作 |
| `no_fuel.png` | 无燃料 |
| `no_full_heat.png` | 未满热量 |
| `no_scrap.png` | 无废料 |
| `working.png` | 工作中 |

### 4.2 blocks（方块纹理）- 940 个文件
各种方块的纹理，包括：

#### 储罐类
| 文件名 | 对应方块 |
|-------|---------|
| `barrel_bottomtop.png` | 储罐上下盖 |
| `barrel_sides.png` | 储罐侧面 |
| `barrel_tap.png` | 储罐龙头 |
| `bronze_tank_cap.png` | 青铜储罐盖 |
| `bronze_tank_side.png` | 青铜储罐侧面 |

#### 建筑泡沫墙类 (cf/) - 34 个文件
| 纹理前缀 | 类型 |
|---------|-----|
| `cf/foam.png` | 建筑泡沫基础 |
| `cf/reinforced_foam.png` | 强化建筑泡沫 |
| `cf/wall_{color}.png` | 彩色墙 (16 色) |
| `cf/wall_{color}_smooth.png` | 平滑彩色墙 (16 色) |

#### 作物生长类 (blocks/crop/) - 约 300+ 个文件
各种农作物的不同生长阶段纹理，例如：
- `acacia_sapling_0~4.png` - 金合欢树苗 (5 个阶段)
- `aurelia_0~4.png` - Aurelia 作物 (5 个阶段)
- `beetroots_0~3.png` - 甜菜 (4 个阶段)
- `birch_sapling_0~3.png` - 白桦树苗 (4 个阶段)
- ... 更多作物

#### 矿物矿石类
- 铅矿石、锡矿石、铀矿石等原版和深层变体
- 铜矿石、银矿石、金矿石等

#### 木材类
- 橡胶原木（侧/顶）
- 橡胶木板材质
- 其他木材变体

#### 线缆类
- 铜导线、锡导线、铁导线、金导线等
- 绝缘导线系列
- 检测导线
- 泡沫电缆

#### 机器外壳与面板
- 机械外壳正面/侧面/顶部
- 高级机械外壳
- 反应堆压力容器

#### 玻璃与透明方块
- 防爆玻璃
- 玻璃纤维

#### 其他方块纹理
- 脚手架系列
- 铁栅栏
- 防爆门
- 工业 TNT
- 核弹
- 等等...

### 4.3 gui（用户界面）- 77 个文件
各种机器的 GUI 界面纹理：

| 类别 | 示例 |
|-----|------|
| 发电机界面 | generator_*.png |
| 反应堆界面 | reactor_*.png |
| 变压器界面 | transformer_*.png |
| 机器界面 | compressor.png, electric_furnace.png, metal_former.png 等 |
| 槽位图标 | 物品槽、流体槽、升级槽等 |
| 进度条 | 加工进度、发酵进度等 |
| 按钮和滑块 | 交互元件 |

### 4.4 items（物品纹理）- 493 个文件
几乎所有物品的独立纹理，包括：

#### 工具和武器
- 锻造锤、扳手、链锯等工具
- 青铜剑、钻石钻头等武器
- 采矿镭射枪、电动工具等特殊工具

#### 护甲套装
- 青铜盔甲（头盔、胸甲、护腿、靴子）
- 纳米盔甲套装
- 量子盔甲套装
- 防化服

#### 材料和部件
- 各种金属锭、板、粉尘
- 电路板、线圈、马达
- 燃料棒、反应堆组件

#### 电池和能源
- 充电电池、能量水晶
- 兰波顿水晶
- 加热元件

#### 农作物和产品
- 咖啡豆、啤酒花、杂草等种子
- 咖啡杯（热/冷/黑咖啡）
- 肥料、植物球等

#### 特殊物品
- UU 物质
- 工业货币
- 地形转换模板
- 刷子（16 种颜色）
- 喷气背包
- 等等...

### 4.5 entity（实体纹理）- 3 个文件
模组添加的实体纹理：
- 可能包含机器人、特殊生物等

### 4.6 mob_effect（状态效果）- 1 个文件
- 状态效果图标（如辐射效果等）

### 4.7 models（自定义模型纹理）- 3 个文件
- 可能用于某些复杂模型的单独纹理


---

## 五、物品列表 (Item Models)

### 原材料 - 矿石与粉末类
|  | ID | 名称 |
|--|----|------|
| - [x] | item.ic2.raw_lead | 粗铅 |
| - [x] | item.ic2.raw_tin | 粗锡 |
| - [x] | item.ic2.raw_uranium | 粗铀 |
| - [x] | item.ic2.crushed_copper | 粉碎铜矿石 |
| - [x] | item.ic2.crushed_gold | 粉碎金矿石 |
| - [x] | item.ic2.crushed_iron | 粉碎铁矿石 |
| - [x] | item.ic2.crushed_lead | 粉碎铅矿石 |
| - [x] | item.ic2.crushed_silver | 粉碎银矿石 |
| - [x] | item.ic2.crushed_tin | 粉碎锡矿石 |
| - [x] | item.ic2.crushed_uranium | 粉碎铀矿石 |
| - [x] | item.ic2.purified_copper | 纯净的粉碎铜矿石 |
| - [x] | item.ic2.purified_gold | 纯净的粉碎金矿石 |
| - [x] | item.ic2.purified_iron | 纯净的粉碎铁矿石 |
| - [x] | item.ic2.purified_lead | 纯净的粉碎铅矿石 |
| - [x] | item.ic2.purified_silver | 纯净的粉碎银矿石 |
| - [x] | item.ic2.purified_tin | 纯净的粉碎锡矿石 |
| - [x] | item.ic2.purified_uranium | 纯净的粉碎铀矿石 |

### 原材料 - 粉尘类
|  | ID | 名称 |
|--|----|------|
| - [x] | item.ic2.bronze_dust | 青铜粉 |
| - [x] | item.ic2.clay_dust | 粘土粉 |
| - [x] | item.ic2.coal_dust | 煤粉 |
| - [x] | item.ic2.coal_fuel_dust | 湿煤粉 |
| - [x] | item.ic2.copper_dust | 铜粉 |
| - [x] | item.ic2.diamond_dust | 钻石粉 |
| - [x] | item.ic2.energium_dust | 能量水晶粉 |
| - [x] | item.ic2.gold_dust | 金粉 |
| - [x] | item.ic2.iron_dust | 铁粉 |
| - [x] | item.ic2.lapis_dust | 青金石粉 |
| - [x] | item.ic2.lead_dust | 铅粉 |
| - [x] | item.ic2.lithium_dust | 锂粉 |
| - [x] | item.ic2.obsidian_dust | 黑曜石粉 |
| - [x] | item.ic2.silicon_dioxide_dust | 二氧化硅粉 |
| - [x] | item.ic2.silver_dust | 银粉 |
| - [x] | item.ic2.stone_dust | 石粉 |
| - [x] | item.ic2.sulfur_dust | 硫粉 |
| - [x] | item.ic2.tin_dust | 锡粉 |
| - [x] | item.ic2.hydrated_tin_dust | 氢氧化锡粉 |

### 原材料 - 小撮粉尘类
|  | ID | 名称 |
|--|----|------|
| - [x] | item.ic2.small_bronze_dust | 小撮青铜粉 |
| - [x] | item.ic2.small_copper_dust | 小撮铜粉 |
| - [x] | item.ic2.small_gold_dust | 小撮金粉 |
| - [x] | item.ic2.small_iron_dust | 小撮铁粉 |
| - [x] | item.ic2.small_lapis_dust | 小撮青金石粉 |
| - [x] | item.ic2.small_lead_dust | 小撮铅粉 |
| - [x] | item.ic2.small_lithium_dust | 小撮锂粉 |
| - [x] | item.ic2.small_obsidian_dust | 小撮黑曜石粉 |
| - [x] | item.ic2.small_silver_dust | 小撮银粉 |
| - [x] | item.ic2.small_sulfur_dust | 小撮硫粉 |
| - [x] | item.ic2.small_tin_dust | 小撮锡粉 |

### 原材料 - 锭与块类
|  | ID | 名称 |
|--|----|------|
| - [x] | item.ic2.mixed_metal_ingot | 合金锭 |
| - [x] | item.ic2.bronze_ingot | 青铜锭 |
| - [x] | item.ic2.copper_ingot | 铜锭 |
| - [x] | item.ic2.lead_ingot | 铅锭 |
| - [x] | item.ic2.silver_ingot | 银锭 |
| - [x] | item.ic2.steel_ingot | 钢锭 |
| - [x] | item.ic2.tin_ingot | 锡锭 |
| - [x] | item.ic2.refined_iron_ingot | 精炼铁锭 |
| - [x] | item.ic2.uranium_ingot | 铀锭 |

铜锭在 ic2_120 模组中已实现为可获取物品，使用本清单中的材质路径 `ic2:items/resource/ingot/copper`（纹理文件：`textures/items/resource/ingot/copper.png`）。

### 原材料 - 板类
|  | ID | 名称 |
|--|----|------|
| - [x] | item.ic2.bronze_plate | 青铜板 |
| - [x] | item.ic2.copper_plate | 铜板 |
| - [x] | item.ic2.gold_plate | 金板 |
| - [x] | item.ic2.iron_plate | 铁板 |
| - [x] | item.ic2.lapis_plate | 青金石板 |
| - [x] | item.ic2.lead_plate | 铅板 |
| - [x] | item.ic2.obsidian_plate | 黑曜石板 |
| - [x] | item.ic2.steel_plate | 钢板 |
| - [x] | item.ic2.tin_plate | 锡板 |
| - [x] | item.ic2.dense_bronze_plate | 致密青铜板 |
| - [x] | item.ic2.dense_copper_plate | 致密铜板 |
| - [x] | item.ic2.dense_gold_plate | 致密金板 |
| - [x] | item.ic2.dense_iron_plate | 致密铁板 |
| - [x] | item.ic2.dense_lapis_plate | 致密青金石板 |
| - [x] | item.ic2.dense_lead_plate | 致密铅板 |
| - [x] | item.ic2.dense_obsidian_plate | 致密黑曜石板 |
| - [x] | item.ic2.dense_steel_plate | 致密钢板 |
| - [x] | item.ic2.dense_tin_plate | 致密锡板 |

### 原材料 - 外壳类
|  | ID | 名称 |
|--|----|------|
| - [x] | item.ic2.bronze_casing | 青铜外壳 |
| - [x] | item.ic2.copper_casing | 铜质外壳 |
| - [x] | item.ic2.gold_casing | 黄金外壳 |
| - [x] | item.ic2.iron_casing | 铁质外壳 |
| - [x] | item.ic2.lead_casing | 铅质外壳 |
| - [x] | item.ic2.steel_casing | 钢质外壳 |
| - [x] | item.ic2.tin_casing | 锡质外壳 |

### 核能相关材料
|  | ID | 名称 |
|--|----|------|
| - [x] | item.ic2.uranium | 浓缩铀核燃料 |
| - [x] | item.ic2.uranium_235 | 铀 -235 |
| - [x] | item.ic2.uranium_238 | 铀 -238 |
| - [x] | item.ic2.plutonium | 钚 |
| - [x] | item.ic2.mox | 钚铀混合氧化物核燃料 (MOX) |
| - [x] | item.ic2.small_uranium_235 | 小撮铀 -235 |
| - [x] | item.ic2.small_uranium_238 | 小撮铀 -238 |
| - [x] | item.ic2.small_plutonium | 小撮钚 |
| - [x] | item.ic2.uranium_pellet | 浓缩铀核燃料靶丸 |
| - [x] | item.ic2.mox_pellet | 钚铀混合氧化物核燃料靶丸 (MOX) |
| - [x] | item.ic2.rtg_pellet | 放射性同位素燃料靶丸 |

### 核反应堆组件
|  | ID | 名称 |
|--|----|------|
| - [ ] | item.ic2.depleted_uranium_fuel_rod | 燃料棒 (枯竭铀) |
| - [ ] | item.ic2.depleted_dual_uranium_fuel_rod | 双联燃料棒 (枯竭铀) |
| - [ ] | item.ic2.depleted_quad_uranium_fuel_rod | 四联燃料棒 (枯竭铀) |
| - [ ] | item.ic2.depleted_mox_fuel_rod | 燃料棒 (枯竭 MOX) |
| - [ ] | item.ic2.depleted_dual_mox_fuel_rod | 双联燃料棒 (枯竭 MOX) |
| - [ ] | item.ic2.depleted_quad_mox_fuel_rod | 四联燃料棒 (枯竭 MOX) |
| - [ ] | item.ic2.near_depleted_uranium | 接近枯竭的铀 |
| - [ ] | item.ic2.re_enriched_uranium | 重新浓缩的铀 |
| - [ ] | item.ic2.ashes | 灰烬 |
| - [ ] | item.ic2.iridium_shard | 铱碎片 |
| - [ ] | item.ic2.uu_matter | UU 物质 |

### 反应堆核心部件
|  | ID | 名称 |
|--|----|------|
| - [ ] | item.ic2.reactor_coolant_cell | 10k 冷却单元 |
| - [ ] | item.ic2.triple_reactor_coolant_cell | 30k 冷却单元 |
| - [ ] | item.ic2.sextuple_reactor_coolant_cell | 60k 冷却单元 |
| - [ ] | item.ic2.reactor_plating | 反应堆隔板 |
| - [ ] | item.ic2.reactor_heat_plating | 高热容反应堆隔板 |
| - [ ] | item.ic2.containment_reactor_plating | 密封反应堆隔热板 |
| - [ ] | item.ic2.heat_exchanger | 热交换器 |
| - [ ] | item.ic2.reactor_heat_exchanger | 反应堆热交换器 |
| - [ ] | item.ic2.component_heat_exchanger | 元件热交换器 |
| - [ ] | item.ic2.advanced_heat_exchanger | 高级热交换器 |
| - [ ] | item.ic2.heat_vent | 散热片 |
| - [ ] | item.ic2.reactor_heat_vent | 反应堆散热片 |
| - [ ] | item.ic2.overclocked_heat_vent | 超频散热片 |
| - [ ] | item.ic2.component_heat_vent | 元件散热片 |
| - [ ] | item.ic2.advanced_heat_vent | 高级散热片 |
| - [ ] | item.ic2.neutron_reflector | 中子反射板 |
| - [ ] | item.ic2.thick_neutron_reflector | 加厚中子反射板 |
| - [ ] | item.ic2.iridium_neutron_reflector | 铱中子反射板 |
| - [ ] | item.ic2.rsh_condensator | 红石冷凝模块 |
| - [ ] | item.ic2.lzh_condensator | 青金石冷凝模块 |
| - [x] | item.ic2.fuel_rod | 燃料棒 (空) |
| - [ ] | item.ic2.uranium_fuel_rod | 燃料棒 (铀) |
| - [ ] | item.ic2.dual_uranium_fuel_rod | 双联燃料棒 (铀) |
| - [ ] | item.ic2.quad_uranium_fuel_rod | 四联燃料棒 (铀) |
| - [ ] | item.ic2.mox_fuel_rod | 燃料棒 (MOX) |
| - [ ] | item.ic2.dual_mox_fuel_rod | 双联燃料棒 (MOX) |
| - [ ] | item.ic2.quad_mox_fuel_rod | 四联燃料棒 (MOX) |
| - [ ] | item.ic2.lithium_fuel_rod | 燃料棒 (锂) |
| - [ ] | item.ic2.depleted_isotope_fuel_rod | 近衰变铀棒 |
| - [ ] | item.ic2.heatpack | 加热元件 |

### 电路与机械部件
|  | ID | 名称 |
|--|----|------|
| - [x] | item.ic2.circuit | 电路板 |
| - [x] | item.ic2.advanced_circuit | 高级电路板 |
| - [x] | item.ic2.alloy | 高级合金 |
| - [x] | item.ic2.iridium | 强化铱板 |
| - [x] | item.ic2.coil | 线圈 |
| - [x] | item.ic2.electric_motor | 电动马达 |
| - [x] | item.ic2.heat_conductor | 热传导器 |
| - [x] | item.ic2.copper_boiler | 铜锅炉 |

### 驱动与动力类
|  | ID | 名称 |
|--|----|------|
| - [x] | item.ic2.small_power_unit | 小型驱动把手 |
| - [x] | item.ic2.power_unit | 驱动把手 |
| - [x] | item.ic2.tin_can | 锡罐 (空) |
| - [x] | item.ic2.filled_tin_can | 锡罐 (满) |

### 碳材料类
|  | ID | 名称 |
|--|----|------|
| - [x] | item.ic2.carbon_fibre | 粗制碳网 |
| - [x] | item.ic2.carbon_mesh | 粗制碳板 |
| - [x] | item.ic2.carbon_plate | 碳板 |
| - [x] | item.ic2.rotor_blade | 扇叶 |

### 能源球类
|  | ID | 名称 |
|--|----|------|
| - [x] | item.ic2.coal_ball | 煤球 |
| - [x] | item.ic2.coal_block | 压缩煤球 |
| - [x] | item.ic2.coal_chunk | 煤块 |
| - [x] | item.ic2.industrial_diamond | 工业钻石 |
| - [x] | item.ic2.plant_ball | 植物球 |
| - [x] | item.ic2.compressed_plants | 压缩植物 |
| - [x] | item.ic2.bio_chaff | 糠 |
| - [x] | item.ic2.compressed_hydrated_coal | 压缩煤 |
| - [x] | item.ic2.scrap | 废料 |
| - [x] | item.ic2.scrap_box | 废料盒 |

### 建筑泡沫类
|  | ID | 名称 |
|--|----|------|
| - [x] | item.ic2.cf_powder | 建筑泡沫粉 |
| - [x] | item.ic2.pellet | 建筑泡沫颗粒 |

### 模式存储类
|  | ID | 名称 |
|--|----|------|
| - [ ] | item.ic2.raw_crystal_memory | (粗制) 模式存储水晶 |
| - [ ] | item.ic2.crystal_memory | 模式存储水晶 |

### 柄与转子类
|  | ID | 名称 |
|--|----|------|
| - [x] | item.ic2.iron_shaft | 铁柄 (铁) |
| - [x] | item.ic2.steel_shaft | 铁柄 (钢) |
| - [x] | item.ic2.wooden_rotor_blade | 木扇叶 |
| - [x] | item.ic2.iron_rotor_blade | 铁扇叶 |
| - [x] | item.ic2.steel_rotor_blade | 钢扇叶 |
| - [x] | item.ic2.carbon_rotor_blade | 碳扇叶 |
| - [x] | item.ic2.steam_turbine_blade | 蒸汽涡轮扇叶 |
| - [x] | item.ic2.steam_turbine | 蒸汽涡轮 |
| - [x] | item.ic2.jetpack_attachment_plate | 喷射背包连接部件 |
| - [x] | item.ic2.wooden_rotor | 转子 (木) |
| - [x] | item.ic2.iron_rotor | 转子 (铁) |
| - [x] | item.ic2.steel_rotor | 转子 (钢) |
| - [x] | item.ic2.carbon_rotor | 转子 (碳) |

### 货币与特殊物品
|  | ID | 名称 |
|--|----|------|
| - [x] | item.ic2.coin | 工业货币 |
| - [x] | item.ic2.resin | 粘性树脂 |
| - [x] | item.ic2.slag | 渣渣 |
| - [x] | item.ic2.iodine | 碘 |

### 容器类
|  | ID | 名称 |
|--|----|------|
| - [x] | item.ic2.water_cell | 水单元 |
| - [x] | item.ic2.lava_cell | 岩浆单元 |
| - [x] | item.ic2.air_cell | 压缩空气单元 |
| - [x] | item.ic2.biofuel_cell | 植物能量单元 |
| - [x] | item.ic2.bio_cell | 压缩植物球单元 |
| - [x] | item.ic2.weed_ex_cell | 除草剂 |
| - [x] | item.ic2.construction_foam_bucket | 建筑泡沫桶 |
| - [x] | item.ic2.biofuel_bucket | 生物燃料桶 |
| - [x] | item.ic2.biofuel_cell | 生物质桶 |
| - [x] | item.ic2.biomass_bucket | 生物质桶 |
| - [x] | item.ic2.construct_foam_bucket | 建筑泡沫桶 |
| - [x] | item.ic2.coolant_bucket | 冷却液桶 |

### 工具升级类
|  | ID | 名称 |
|--|----|------|
| - [x] | item.ic2.overclocker_upgrade | 超频升级 |
| - [x] | item.ic2.transformer_upgrade | 高压升级 |
| - [x] | item.ic2.energy_storage_upgrade | 储能升级 |
| - [x] | item.ic2.redstone_inverter_upgrade | 红石信号反转升级 |
| - [x] | item.ic2.ejector_upgrade | 弹出升级 |
| - [x] | item.ic2.advanced_ejector_upgrade | 自动弹出升级 |
| - [x] | item.ic2.pulling_upgrade | 抽入升级 |
| - [x] | item.ic2.advanced_pulling_upgrade | 自动抽入升级 |
| - [x] | item.ic2.fluid_ejector_upgrade | 流体弹出升级 |
| - [x] | item.ic2.fluid_pulling_upgrade | 流体抽入升级 |

### 地形转换模板类
|  | ID | 名称 |
|--|----|------|
| - [x] | item.ic2.blank_tfbp | 地形转换模板 - 空白 |
| - [x] | item.ic2.chilling_tfbp | 地形转换模板 - 冰原 |
| - [x] | item.ic2.cultivation_tfbp | 地形转换模板 - 耕地 |
| - [x] | item.ic2.desertification_tfbp | 地形转换模板 - 沙漠 |
| - [x] | item.ic2.flatification_tfbp | 地形转换模板 - 平地 |
| - [x] | item.ic2.irrigation_tfbp | 地形转换模板 - 灌溉 |
| - [x] | item.ic2.mushroom_tfbp | 地形转换模板 - 蘑菇 |

### 服装与护甲类
|  | ID | 名称 |
|--|----|------|
| - [ ] | item.ic2.alloy_chestplate | 复合胸甲 |
| - [ ] | item.ic2.bronze_boots | 青铜靴子 |
| - [ ] | item.ic2.bronze_chestplate | 青铜胸甲 |
| - [ ] | item.ic2.bronze_helmet | 青铜头盔 |
| - [ ] | item.ic2.bronze_leggings | 青铜护腿 |
| - [ ] | item.ic2.hazmat_chestplate | 防化服 |
| - [ ] | item.ic2.hazmat_helmet | 防化头盔 |
| - [ ] | item.ic2.hazmat_leggings | 防化裤 |
| - [ ] | item.ic2.nano_boots | 纳米靴子 |
| - [ ] | item.ic2.nano_chestplate | 纳米胸甲 |
| - [ ] | item.ic2.nano_helmet | 纳米头盔 |
| - [ ] | item.ic2.nano_leggings | 纳米护腿 |
| - [ ] | item.ic2.quantum_boots | 量子靴子 |
| - [ ] | item.ic2.quantum_chestplate | 量子护甲 |
| - [ ] | item.ic2.quantum_helmet | 量子头盔 |
| - [ ] | item.ic2.quantum_leggings | 量子护腿 |
| - [ ] | item.ic2.rubber_boots | 橡胶靴 |
| - [ ] | item.ic2.cf_pack | 建筑泡沫背包 |

### 电池类
|  | ID | 名称 |
|--|----|------|
| - [x] | item.ic2.re_battery | 充电电池 |
| - [x] | item.ic2.charging_re_battery | 无线充电电池 |
| - [x] | item.ic2.advanced_re_battery | 高级充电电池 |
| - [x] | item.ic2.advanced_charging_re_battery | 高级无线充电电池 |
| - [x] | item.ic2.energy_crystal | 能量水晶 |
| - [x] | item.ic2.lapotron_crystal | 兰波顿水晶 |
| - [x] | item.ic2.single_use_battery | 一次性电池 |

### 植物种子类
|  | ID | 名称 |
|--|----|------|
| - [x] | item.ic2.fertilizer | 肥料 |
| - [x] | item.ic2.grin_powder | 蛤蛤粉 |
| - [x] | item.ic2.hops | 啤酒花 |
| - [x] | item.ic2.weed | 杂草 |
| - [x] | item.ic2.terra_wart | 大地疣 |
| - [x] | item.ic2.coffee_beans | 咖啡豆 |
| - [x] | item.ic2.coffee_powder | 咖啡粉 |

### 饮料杯类
|  | ID | 名称 |
|--|----|------|
| - [x] | item.ic2.empty_mug | 石杯 |
| - [x] | item.ic2.coffee_mug | 咖啡 |
| - [x] | item.ic2.cold_coffee_mug | 冷咖啡 |
| - [x] | item.ic2.dark_coffee_mug | 黑咖啡 |

### 工具类
|  | ID | 名称 |
|--|----|------|
| - [x] | item.ic2.cutter | 板材切割剪刀 |
| - [x] | item.ic2.debug_item | 测试工具 |
| - [x] | item.ic2.forge_hammer | 锻造锤 |
| - [x] | item.ic2.tool_box | 工具箱 |
| - [x] | item.ic2.meter | EU 电表 |
| - [x] | item.ic2.treetap | 木龙头 |
| - [x] | item.ic2.wrench | 扳手 |
| - [x] | item.ic2.frequency_transmitter | 遥控器 |
| - [x] | item.ic2.chainsaw | 链锯 |
| - [x] | item.ic2.diamond_drill | 钻石钻头 |
| - [x] | item.ic2.drill | 采矿钻头 |
| - [x] | item.ic2.mining_laser | 采矿镭射枪 |
| - [x] | item.ic2.electric_treetap | 电动树脂提取器 |
| - [x] | item.ic2.electric_wrench | 电动扳手 |
| - [x] | item.ic2.iridium_drill | 铱钻头 |
| - [x] | item.ic2.obscurator | 拟态板 |
| - [x] | item.ic2.scanner | OD 扫描器 |
| - [x] | item.ic2.advanced_scanner | OV 扫描器 |
| - [x] | item.ic2.wind_meter | 风力计 |

### 刷子类
|  | ID | 名称 |
|--|----|------|
| - [ ] | item.ic2.painter | 刷子 |
| - [ ] | item.ic2.black_painter | 黑色刷子 |
| - [ ] | item.ic2.blue_painter | 蓝色刷子 |
| - [ ] | item.ic2.brown_painter | 棕色刷子 |
| - [ ] | item.ic2.cyan_painter | 青色刷子 |
| - [ ] | item.ic2.gray_painter | 灰色刷子 |
| - [ ] | item.ic2.green_painter | 绿色刷子 |
| - [ ] | item.ic2.light_blue_painter | 浅蓝色刷子 |
| - [ ] | item.ic2.light_gray_painter | 浅灰色刷子 |
| - [ ] | item.ic2.lime_painter | 柠檬色刷子 |
| - [ ] | item.ic2.magenta_painter | 品红色刷子 |
| - [ ] | item.ic2.orange_painter | 橙色刷子 |
| - [ ] | item.ic2.pink_painter | 粉色刷子 |
| - [ ] | item.ic2.purple_painter | 紫色刷子 |
| - [ ] | item.ic2.red_painter | 红色刷子 |
| - [ ] | item.ic2.white_painter | 白色刷子 |
| - [ ] | item.ic2.yellow_painter | 黄色刷子 |

### 农具类
|  | ID | 名称 |
|--|----|------|
| - [x] | item.ic2.bronze_axe | 青铜斧 |
| - [x] | item.ic2.bronze_hoe | 青铜锄 |
| - [x] | item.ic2.bronze_sword | 青铜剑 |
| - [x] | item.ic2.bronze_shovel | 青铜铲 |
| - [x] | item.ic2.bronze_pickaxe | 青铜镐 |

### 载具类
|  | ID | 名称 |
|--|----|------|
| - [x] | item.ic2.broken_rubber_boat | 破损的橡胶船 |
| - [x] | item.ic2.carbon_boat | 碳纤维船 |

### 机器物品模型（作为物品展示）
|  | ID | 名称 |
|--|----|------|
| - [x] | item.ic2.advanced_machine | 高级机械外壳 |
| - [ ] | item.ic2.advanced_miner | 高级采矿机 |
| - [ ] | item.ic2.batbox | 储电箱 |
| - [ ] | item.ic2.batbox_chargepad | 充电座 (BatBox) |
| - [ ] | item.ic2.batch_crafter | 批量工作台 |
| - [ ] | item.ic2.blast_furnace | 高炉 |
| - [ ] | item.ic2.block_cutter | 方块切割机 |
| - [x] | item.ic2.bronze_block | 青铜块 |
| - [ ] | item.ic2.bronze_storage_box | 青铜储物箱 |
| - [ ] | item.ic2.bronze_tank | 青铜储罐 |
| - [ ] | item.ic2.canner | 流体/固体装罐机 |
| - [ ] | item.ic2.cesu | CESU 储电箱 |
| - [ ] | item.ic2.cesu_chargepad | 充电座 (CESU) |
| - [ ] | item.ic2.centrifuge | 热能离心机 |
| - [ ] | item.ic2.cesu | CESU 储电箱 |
| - [ ] | item.ic2.chunk_loader | 区块加载器 |
| - [ ] | item.ic2.compressor | 压缩机 |
| - [ ] | item.ic2.condenser | 冷凝机 |
| - [x] | item.ic2.copper_block | 铜块 |
| - [ ] | item.ic2.copper_cable | 铜质导线 |
| - [ ] | item.ic2.crop_harvester | 作物收割机 |
| - [ ] | item.ic2.crop_stick | 作物棍 |
| - [ ] | item.ic2.ces u | CESU 储电箱 |
| - [x] | item.ic2.electric_furnace | 电炉 |
| - [ ] | item.ic2.electrolyzer | 电解机 |
| - [ ] | item.ic2.ev_transformer | 超高压变压器 |
| - [ ] | item.ic2.extractor | 提取机 |
| - [ ] | item.ic2.hv_transformer | 高压变压器 |
| - [ ] | item.ic2.iron_cable | 高压导线 |
| - [x] | item.ic2.iron_fence | 铁栅栏 |
| - [ ] | item.ic2.lv_transformer | 低压变压器 |
| - [ ] | item.ic2.mfe | MFE 储电箱 |
| - [ ] | item.ic2.mfe_chargepad | 充电座 (MFE) |
| - [ ] | item.ic2.macerator | 打粉机 |
| - [ ] | item.ic2.metal_former | 金属成型机 |
| - [ ] | item.ic2.mfsu | MFSU 储电箱 |
| - [ ] | item.ic2.mfsu_chargepad | 充电座 (MFSU) |
| - [ ] | item.ic2.miner | 采矿机 |
| - [ ] | item.ic2.mv_transformer | 中压变压器 |
| - [ ] | item.ic2.induction_furnace | 感应炉 |


---

## 六、总结

### 方块分类统计

| 分类 | 数量 |
|------|------|
| 矿物与矿石 | 9 |
| 块状材料 | 14 |
| 木材相关 | 21 |
| 建筑与装饰 | 25 |
| 发电类机器 | 20 |
| 核反应堆类 | 5 |
| 基础加工类 | 12 |
| 高级机器 | 12 |
| 流体处理类 | 9 |
| 其他功能类 | 17 |
| 机械外壳类 | 3 |
| 储能设备类 | 10 |
| 电缆与变压器类 | 18 |
| 特殊类 | 7 |
| 工作台类 | 2 |
| 储物类 | 6 |
| 输入输出类 | 2 |
| 作物类 | 18 |
| 线缆泡沫类 | 9 |
| 特殊门类 | 1 |

**总计：244 个方块**

### 物品主要分类

| 分类 | 示例 |
|------|------|
| 原材料 | 矿石、粉尘、锭、板 |
| 核能材料 | 铀、钚、燃料棒 |
| 反应堆组件 | 冷却单元、热交换器 |
| 电路部件 | 电路板、线圈、马达 |
| 工具 | 镐、斧、链锯等 |
| 护甲 | 青铜、纳米、量子套装 |
| 电池与能源 | 充电电池、能量水晶 |
| 农作物 | 各种种子和作物产品 |
| 升级插件 | 超频、储能、弹出等 |
| 地形模板 | TF 模板系列 |

---

## 备注

- 本清单基于 `assets/ic2/` 目录下的以下子目录生成：
  - `blockstates/` - 方块状态定义文件（244 个）
  - `models/item/` - 物品模型 JSON（518 个）
  - `models/block/` - 方块模型 JSON（30 个基础模型，含变体共 41 个文件）
  - `textures/` - 纹理图片（1537 个 PNG 文件，分为 7 个子类）
- 部分机器同时具有方块形式和物品形式
- 墙面有多种颜色变体（16 种羊毛/建筑泡沫颜色）
- 电缆有多种类型和绝缘等级
- 某些物品在 JSON 中具有中文翻译（参考 `lang/zh_cn.json`）
