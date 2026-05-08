# 方块掉落途径一览

> 本文档系统梳理本项目所有方块在破坏时的掉落控制机制。
> 维护者注：本项目的掉落途径确实较多，这份清单有助于在新增方块时选择正确的途径、避免遗漏。

---

## 一、总览：掉落途径全景

```
破坏方块
 │
 ├─ 途径① 自动生成掉落表（默认）
 │   ModBlockLootTableProvider 在 datagen 时生成 JSON，
 │   按方块类型自动选择规则：
 │   ├─ MachineBlock / DirectionalMachineBlock → 扳手 or 外壳
 │   ├─ 矿石（铅/锡/铀） → 粗矿 + 时运
 │   ├─ reinforced_door → 门掉落
 │   ├─ CreativeGeneratorBlock → 始终掉自身
 │   └─ 其他所有方块 → 掉自身
 │
 ├─ 途径② 完全不掉落（dropsNothing）
 │   配合 generateBlockLootTable = false
 │
 ├─ 途径③ 自定义 JSON 战利品表
 │   手写 resources 中的 loot_table JSON
 │
 ├─ 途径④ onStateReplaced + NBT 保留
 │   用于储物箱、储罐（保留物品/流体 NBT）
 │
 ├─ 途径⑤ WrenchHandler 特殊逻辑
 │   用于储电盒（保留 80% 能量）、PatternStorage（拆分晶体）
 │
 ├─ 途径⑥ 作物收获（右击触发）
 │   CropBlockEntity.performHarvest()
 │
 └─ 途径⑦ onBreak 自定义逻辑
     用于 ManualKineticGenerator（掉曲柄）、LeashKineticGenerator（解拴绳）
```

所有途径在 **datagen** 生成时由 `ModBlockLootTableProvider` 扫描 `@ModBlock` 注解的
`generateBlockLootTable` 参数决定是否参与自动生成。

---

## 二、各途径详解

### 途径①：自动生成掉落表（默认，`generateBlockLootTable = true`）

绝大多数方块走此路径。生成入口：

| 模块 | 生成器 |
|------|--------|
| 核心 | `core/.../recipes/ModBlockLootTableProvider.kt` |
| 高级太阳能 | `advanced-solar-addon/.../recipes/ModBlockLootTableProvider.kt` |

生成器在 `generate()` 中按 `is` 类型匹配优先级：

#### ①-a：MachineBlock / DirectionalMachineBlock → 扳手 or 外壳

两个战利品池：

| 条件 | 掉落物 |
|------|--------|
| 手持扳手（含电扳手）破坏 | 掉**方块物品自身**（注册的 BlockItem） |
| 非扳手破坏 | 掉**外壳物品**（`getCasingDrop()` 返回值） |

**外壳物品对照表：**

| `getCasingDrop()` 返回值 | 适用方块 |
|--------------------------|----------|
| `ic2_120:machine`（默认） | **绝大多数机器**：发电机类（Generator、SolarGenerator、GeoGenerator、WaterGenerator、WindGenerator、StirlingGenerator、SemifluidGenerator、KineticGenerator、RtGenerator、ElectricHeatGenerator、FluidHeatGenerator、SolidHeatGenerator、RtHeatGenerator）、处理机械类（Macerator、Compressor、Extractor、ElectricFurnace、Canner、FluidCanner、SolidCanner、Fermenter、BlastFurnace、BlockCutter、OreWashingPlant、MetalFormer、Recycler）、Miner、Pump、ChunkLoader、TeslaCoil、Magnetizer、CropHarvester、Cropmatron、AnimalSlaughterer、Animalmatron、SolarDistiller、WaterKineticGenerator、WindKineticGenerator、NuclearReactor、**高级太阳能全部**（MolecularTransformer、AdvancedSolarPanel、HybridSolarPanel、QuantumSolarPanel、UltimateSolarPanel、QuantumGenerator） |
| `ic2_120:advanced_machine` | Mfsu、MfsuChargepad、Centrifuge、InductionFurnace、MatterGenerator、UuScanner、Replicator、Teleporter、PatternStorage、AdvancedMiner |
| `asItem()` → 自身 | CokeKiln、IronFurnace |

扳手匹配的底层机制：
- `WrenchHandler` 拦截左键攻击，以 `world.breakBlock(pos, false, player)`（不掉落）破坏方块，
  然后手动调用 `Block.dropStacks(stateBefore, world, pos, be, player, stack.copy())`，
  将扳手作为 tool 传入，使 `MatchToolLootCondition(wrenchPredicate)` 匹配成功。
- 如果不通过 `WrenchHandler`（玩家用非扳手工具破坏），不传入扳手 → 条件不命中 → 掉外壳。

#### ①-b：矿石 → 粗矿 + 时运加成

| 方块 | 掉落物 |
|------|--------|
| LeadOreBlock / DeepslateLeadOreBlock | `raw_lead` |
| TinOreBlock / DeepslateTinOreBlock | `raw_tin` |
| UraniumOreBlock / DeepslateUraniumOreBlock | `raw_uranium` |

- 附魔时运：时运 I → 1.2×、时运 II → 1.4×、时运 III → 1.6×
- 精准采集 → 掉矿石方块自身
- 爆炸抗性衰减：应用 `ExplosionDecay`

#### ①-c：reinforced_door → 门掉落

使用 `doorDrops(block)` 生成，保证破坏时掉落门物品（双格方块的掉落处理与原版门一致）。

#### ①-d：CreativeGeneratorBlock → 始终掉自身

无论是否持扳手，`CreativeGeneratorBlock` 始终掉落自身的 BlockItem。不经过扳手/外壳二选一逻辑。

#### ①-e：其他所有方块 → 掉自身

所有不在以上分类的 `@ModBlock(generateBlockLootTable = true)` 方块：
- 建筑方块：reinforced_stone、各种染色 wall、refractory_bricks、basalt 等
- 橡胶木系列：rubber_log、rubber_planks、rubber_stairs、rubber_slab、rubber_fence、rubber_door、rubber_trapdoor 等
- 金属块：bronze_block、steel_block、tin_block、lead_block、uranium_block、coal_block、silver_block
- 粗矿块：raw_lead_block、raw_tin_block、raw_uranium_block
- 线缆：copper_cable、tin_cable、gold_cable、iron_cable、glass_fibre_cable 及各绝缘变种
- 管道：bronze_pipe_*、carbon_pipe_*、各 pump_attachment
- 传动轴：wood_transmission_shaft、iron_transmission_shaft、steel_transmission_shaft、carbon_transmission_shaft
- 变压器：LV / MV / HV / EV transformer
- 脚手架/栅栏/铁栅栏：wooden_scaffold、iron_scaffold、iron_fence
- 其他：foam、reinforced_foam、mining_pipe、wool_sheet、rubber_sheet、resin_sheet、luminator_flat、bevel_gear、coke_kiln_grate、coke_kiln_hatch、reactor_*、crop_stick、compose_debug 等

---

### 途径②：完全不掉落（`dropsNothing`）

| 方块 | 注解 | Block Settings |
|------|------|---------------|
| ReinforcedGlassBlock | `generateBlockLootTable = false` | `.dropsNothing()` |

两层保障：注解层阻止 datagen 生成战利品表，代码层声明不掉落。

---

### 途径③：自定义 JSON 战利品表

目前仅有一个独立手写的 JSON 战利品表：

| 方块 | 文件 |
|------|------|
| RubberLeavesBlock | `core/src/main/resources/data/ic2_120/loot_tables/blocks/rubber_leaves.json` |

掉落逻辑：
- **Pool 1**（精准采集 / 剪刀）：掉落 `ic2_120:rubber_leaves` 自身
- **Pool 2**（非精准采集）：概率掉落 rubber_sapling（1.5%）、stick（5%），其他情况不掉落

---

### 途径④：`onStateReplaced` + NBT 保留（储物箱/储罐）

适用于需要保留 BlockEntity 数据的方块。流程：

1. `generateBlockLootTable = false` 禁用自动生成
2. `onStacksDropped()` **置空**，阻止默认掉落
3. `onStateReplaced()` 手动构造带 NBT 的 ItemStack 并 `spawnEntity()`
4. `getPickStack()` 支持创造模式中键获取带 NBT 的方块

| 方块 | 保留的 NBT 数据 |
|------|----------------|
| WoodenStorageBoxBlock | 完整物品栏（`BlockEntityTag.Items`） |
| BronzeStorageBoxBlock | 同上 |
| IronStorageBoxBlock | 同上 |
| SteelStorageBoxBlock | 同上 |
| IridiumStorageBoxBlock | 同上 |
| BronzeTankBlock | 流体数据（`FluidTag` + `AmountTag`） |
| IronTankBlock | 同上 |
| SteelTankBlock | 同上 |
| IridiumTankBlock | 同上 |

**注意**：这些方块的 `WrenchHandler` 行为：
- 储物箱：`WrenchHandler` 直接 `breakBlock(pos, false)` 触发 `onStateReplaced` → 自动处理 NBT 掉落
- 储罐：`WrenchHandler` 在 break 前调用 `retainFluidPercent(0.8)` 保留 80% 流体

---

### 途径⑤：`WrenchHandler` 特殊逻辑（储电盒/PatternStorage）

#### 储电盒系列（BatBox / CESU / MFE / MFSU 及 Chargepad 变种）

使用扳手左键破坏时的完整流程：

1. 从 BlockEntity 读取当前能量（`getStoredEnergy()`）
2. `world.breakBlock(pos, false, player)`（不掉落）
3. 计算保留能量 = 当前 × 80%
4. 构造 BlockItem ItemStack，写入 `BlockEntityTag.Energy` NBT
5. `spawnEntity()` 掉落带能量 NBT 的方块物品

**涉及的方块：**

| 方块 | 注册名 |
|------|--------|
| BatBoxBlock | `batbox` |
| CESUBlock | `cesu` |
| MFEBlock | `mfe` |
| MFSUBlock | `mfsu` |
| BatBoxChargepadBlock | `batbox_chargepad` |
| CESUChargepadBlock | `cesu_chargepad` |
| MFEChargepadBlock | `mfe_chargepad` |
| MFSUChargepadBlock | `mfsu_chargepad` |

> 注意：非扳手破坏时走普通 MachineBlock 逻辑 → 掉外壳。能量不保留。

#### PatternStorageBlock

使用扳手破坏时：

1. 从 BlockEntity 读取 UU 模板数据 + 分离晶体物品
2. `world.breakBlock(pos, false, player)`
3. 掉落带 NBT（UU 模板）的 PatternStorage 物品
4. **额外**掉落晶体物品实体
5. `onStacksDropped()` 也会散落剩余物品栏

---

### 途径⑥：作物收获（CropBlock）

CropBlock 不通过破坏方块来收获，而是**右击交互**触发收获。

- `generateBlockLootTable = false`
- 无 loot_table JSON
- `CropBlockEntity.performHarvest()` 计算掉落
- 掉落数量取决于：`CropSystem.dropGainChance()` × crop gain stat × 高斯随机
- `createGainStacks()` 将 CropType 映射到具体物品（橡胶树苗、染料、金属粉等）
- 杂草压力过高时作物可能变回 CropStickBlock 而不掉落物品

---

### 途径⑦：`onBreak` 自定义逻辑

| 方块 | 行为 |
|------|------|
| ManualKineticGeneratorBlock | `onBreak` 中调用 `dropCrank()` 在破坏前掉落曲柄物品 |
| LeashKineticGeneratorBlock | `onBreak` 中分离拴绳生物、移除拴绳桩 |

---

## 三、方块 → 掉落途径快速索引

### 核心模块（ic2_120）

| 途径 | 方块列表 |
|------|----------|
| **①-a 默认外壳** | generator、solar_generator、geo_generator、water_generator、wind_generator、stirling_generator、semifluid_generator、kinetic_generator、rt_generator、electric_heat_generator、fluid_heat_generator、solid_heat_generator、rt_heat_generator、macerator、compressor、extractor、electric_furnace、canner、fluid_canner、solid_canner、fermenter、blast_furnace、block_cutter、ore_washing_plant、metal_former、recycler、miner、pump、chunk_loader、tesla_coil、magnetizer、crop_harvester、cropmatron、animal_slaughterer、animalmatron、solar_distiller、water_kinetic_generator、wind_kinetic_generator、nuclear_reactor、reactor_access_hatch、reactor_chamber、reactor_fluid_port、reactor_redstone_port、reactor_vessel、liquid_heat_exchanger、lv_transformer、mv_transformer、hv_transformer、ev_transformer、batbox、cesu、mfe、mfsu、batbox_chargepad、cesu_chargepad、mfe_chargepad、mfsu_chargepad（非扳手时） |
| **①-a 高级外壳** | mfsu、mfsu_chargepad、centrifuge、induction_furnace、matter_generator、uu_scanner、replicator、teleporter、pattern_storage、advanced_miner |
| **①-a 掉自身（铁炉/焦炉）** | iron_furnace、coke_kiln（及 grate/hatch） |
| **①-b 矿石+时运** | lead_ore、tin_ore、uranium_ore、deepslate_lead_ore、deepslate_tin_ore、deepslate_uranium_ore |
| **①-c 门** | reinforced_door |
| **①-d 始终掉自身** | creative_generator |
| **①-e 掉自身** | 建筑方块（reinforced_stone、各色wall、refractory_bricks、basalt）、橡胶木全系列（log、planks、stairs、slab、fence、gate、door、trapdoor、pressure_plate、button）、金属块（bronze、steel、tin、lead、uranium、coal、silver）、粗矿块（raw_lead、raw_tin、raw_uranium）、线缆全系列、管道全系列、传动轴全系列、脚手架/栅栏（wooden_scaffold、iron_scaffold、iron_fence、rubber_fence）、其他（foam、mining_pipe、wool_sheet、rubber_sheet、resin_sheet、luminator_flat、bevel_gear、crop_stick、compose_debug） |
| **② 不掉落** | reinforced_glass |
| **③ JSON 战利品表** | rubber_leaves |
| **④ NBT 保留** | wooden_storage_box、bronze_storage_box、iron_storage_box、steel_storage_box、iridium_storage_box、bronze_tank、iron_tank、steel_tank、iridium_tank |
| **⑤ 能量 NBT** | batbox、cesu、mfe、mfsu、batbox_chargepad、cesu_chargepad、mfe_chargepad、mfsu_chargepad（仅扳手时） |
| **⑤ 晶体拆分** | pattern_storage（仅扳手时） |
| **⑥ 作物收获** | crop |
| **⑦ onBreak 自定义** | manual_kinetic_generator、leash_kinetic_generator |

> 注：储电盒（batbox ~ mfsu_chargepad）在"非扳手"时走途径①-a（掉外壳），在"扳手"时走途径⑤（掉自身+80%能量 NBT），
> 因此同时出现在两个途径的列表中。

### 高级太阳能模块（ic2_120_advanced_solar_addon）

| 途径 | 方块列表 |
|------|----------|
| **①-a 默认外壳** | advanced_solar_panel、hybrid_solar_panel、quantum_solar_panel、ultimate_solar_panel、molecular_transformer、quantum_generator |

高级太阳能所有方块都是 MachineBlock 子类且均**未重写** `getCasingDrop()`，所以均使用 `ic2_120:machine` 外壳。

---

## 四、相关辅助机制

### 4.1 挖掘等级标签（不影响掉落内容）

只影响挖掘速度，不影响掉落物品：

| 标签 | 适用方块 |
|------|----------|
| `PICKAXE_MINEABLE` + `NEEDS_IRON_TOOL` | 所有 MachineBlock（含高级太阳能）、储物箱、储罐、矿石 |
| `PICKAXE_MINEABLE`（无等级要求） | 线缆、管道、变压器 |
| `PICKAXE_MINEABLE` + `NEEDS_STONE_TOOL` | reinforced_door |
| `HOE_MINEABLE` | rubber_leaves |
| `AXE_MINEABLE` + `PICKAXE_MINEABLE` | 橡胶木木材类 |
| **无标签** | creative_generator（空手可挖） |

### 4.2 爆炸抗性衰减

所有途径①-a、①-b、①-c 的掉落表自动应用 `ExplosionDecayLootFunction`：
- 爆炸破坏时有概率不掉落

### 4.3 拆机散落物品

`MachineBlock.onStateReplaced()` 和 `DirectionalMachineBlock.onStateReplaced()` 在方块被破坏时
自动调用 `ItemScatterer.spawn()` 散落 `Inventory` 中的物品。

自定义覆盖：
- `BaseMinerBlock.onStateReplaced()` 额外回收采掘管道
- `PatternStorageBlock.onStacksDropped()` 额外散落剩余物品栏
- 储物箱/储罐：将 `onStacksDropped()` 置空，由 `onStateReplaced()` 统一处理 NBT

### 4.4 宝箱战利品注入（非方块掉落）

`ChestLootInjector` 使用 `LootTableEvents.MODIFY` 向 60+ 种原版宝箱战利品表注入 IC2 物品。
这与方块掉落无关，仅作提及避免混淆。

### 4.5 模拟掉落（AnimalSlaughtererBlockEntity）

动物屠宰机使用硬编码的 entity → item 映射模拟掉落，不经过战利品表系统。
这与方块掉落无关。

---

## 五、新增方块时的决策流程

```
新增一个方块时，按此流程选择掉落途径：

Q1: 是否需要保留 BlockEntity 数据（物品栏/流体/能量）？
 ├─ 是 → 走途径④（onStateReplaced + NBT）
 │   └─ 储电盒场景 → 走途径⑤（WrenchHandler 特殊逻辑）
 └─ 否 → Q2

Q2: 是否需要在特定条件下掉不同物品？
 ├─ 是 → Q3
 └─ 否 → Q4

Q3: 掉落逻辑是否复杂（条件/概率/自定义 NBT）？
 ├─ 是 → 途径③：generateBlockLootTable=false + 手写 JSON
 └─ 否 → 途径①-a/b/c：自动生成（按类型匹配）

Q4: 是否需要完全不掉落？
 ├─ 是 → 途径②：dropsNothing + generateBlockLootTable=false
 └─ 否 → 途径①-e：自动生成，掉自身
```
