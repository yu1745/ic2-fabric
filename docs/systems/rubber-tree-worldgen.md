# 橡胶树世界生成链路

本文档对应当前实现，描述“橡胶树从注册到实际落地”的完整链路，以及哪些部分受 `Ic2Config` 控制。

相关实现：

- `src/main/kotlin/ic2_120/Ic2_120.kt`
- `src/main/kotlin/ic2_120/config/Ic2Config.kt`
- `src/main/kotlin/ic2_120/content/worldgen/ModWorldgen.kt`
- `src/main/kotlin/ic2_120/content/worldgen/RubberTreeGeneration.kt`
- `src/main/kotlin/ic2_120/content/worldgen/RubberTreeFeature.kt`
- `src/main/kotlin/ic2_120/content/worldgen/RubberTreeFoliagePlacer.kt`
- `src/main/kotlin/ic2_120/content/worldgen/RubberHoleTreeDecorator.kt`
- `src/main/kotlin/ic2_120/content/worldgen/RubberTreePlacementModifiers.kt`
- `src/main/kotlin/ic2_120/content/block/RubberBlocks.kt`
- `src/main/resources/data/ic2_120/worldgen/configured_feature/rubber_tree.json`
- `src/main/resources/data/ic2_120/worldgen/placed_feature/rubber_tree.json`
- `src/main/resources/ic2_120.accesswidener`

## 1. 总览

当前橡胶树生成分成 4 层：

1. 启动注册层：注册自定义 worldgen 类型，并把 `rubber_tree` placed feature 挂到指定生物群系。
2. data 基底层：`configured_feature` 和 `placed_feature` 仍保留 JSON 入口。
3. 运行时覆盖层：树高、树冠、尝试次数、稀有度、水深限制等参数在生成时读取 `Ic2Config`。
4. 落地方块层：树叶形状、橡胶孔初始化、与其他树重叠时的清理逻辑由 Kotlin 自定义实现处理。

不是“纯 JSON 世界生成”，也不是“全代码硬编码”。
当前实现是“JSON 提供注册入口 + Kotlin 在运行时覆盖关键参数”。

## 2. 启动时怎么接进原版世界生成

模组初始化入口在 `Ic2_120.onInitialize()`：

1. 先执行 `Ic2Config.loadOrThrow()` 读取配置。
2. 调用 `RubberTreeGeneration.register()`。
3. `RubberTreeGeneration.register()` 内部先确保这些自定义类型已注册：
   - `rubber_tree_feature`
   - `rubber_tree_foliage_placer`
   - `rubber_hole_tree_decorator`
   - `rubber_tree_config_placement`
   - `rubber_tree_config_water_depth_filter`
4. 然后读取 `Ic2Config.current.worldgen.rubberTree.normalized()`。
5. 若 `enabled=false`，直接不向 biome modification 注册橡胶树。
6. 若 `biomes` 为空或全是非法 id，也不会注册。
7. 否则调用 `BiomeModifications.addFeature(...)`，把 `ic2_120:rubber_tree` 挂到这些生物群系的 `VEGETAL_DECORATION` 阶段。

这里有一个关键点：

- `enabled`
- `biomes`

这两个字段只在模组启动时参与 `BiomeModifications.addFeature(...)` 注册。
`/ic2config reload` 不会重新注册或撤销 biome modification。

## 3. 注册与数据入口

### 3.1 `ModWorldgen`

`ModWorldgen.kt` 注册了 5 个自定义 worldgen 类型：

- `Feature<TreeFeatureConfig>`: `ic2_120:rubber_tree_feature`
- `FoliagePlacerType`: `ic2_120:rubber_tree_foliage_placer`
- `TreeDecoratorType`: `ic2_120:rubber_hole_tree_decorator`
- `PlacementModifierType`: `ic2_120:rubber_tree_config_placement`
- `PlacementModifierType`: `ic2_120:rubber_tree_config_water_depth_filter`

### 3.2 `configured_feature`

`data/ic2_120/worldgen/configured_feature/rubber_tree.json` 提供树的“基底结构”：

- 树干方块：`ic2_120:rubber_log`
- 树叶方块：`ic2_120:rubber_leaves`
- 树干放置器：`minecraft:straight_trunk_placer`
- 树叶放置器：`ic2_120:rubber_tree_foliage_placer`
- 装饰器：`ic2_120:rubber_hole_tree_decorator`
- `ignore_vines`
- `force_dirt`

这个 JSON 现在主要承担两件事：

- 给注册表和树苗生长提供标准 `ConfiguredFeature` 入口。
- 提供一个“基底配置”，后续再由 Kotlin 按 `Ic2Config` 覆盖关键数值。

### 3.3 `placed_feature`

`data/ic2_120/worldgen/placed_feature/rubber_tree.json` 定义放置链路：

- `ic2_120:rubber_tree_config_placement`
- `minecraft:in_square`
- `minecraft:surface_relative_threshold_filter`
- `ic2_120:rubber_tree_config_water_depth_filter`
- `minecraft:heightmap`

其中真正受配置动态控制的是：

- 每区块尝试次数
- 每次尝试通过概率
- 允许的地表水深

## 4. 配置项分别影响哪里

配置结构在 `Ic2Config.worldgen.rubberTree`：

- `enabled`
- `biomes`
- `countPerChunk`
- `rarityChance`
- `maxWaterDepth`
- `baseHeight`
- `heightRandA`
- `heightRandB`
- `foliageRadius`
- `foliageOffset`
- `foliageHeight`
- `zeroHoleWeight`
- `singleHoleWeight`
- `doubleHoleWeight`
- `ignoreVines`
- `forceDirt`

`normalized()` 会先做约束，避免异常值直接把 worldgen 打崩。

各字段落点如下：

- `enabled`：启动时决定是否注册 biome feature；运行时 placement modifier 也会再次检查。
- `biomes`：启动时转换为 `RegistryKey<Biome>` 并参与 `BiomeSelectors.includeByKey(...)`。
- `countPerChunk`：`RubberTreeConfigPlacementModifier.getPositions()`。
- `rarityChance`：`RubberTreeConfigPlacementModifier.getPositions()`。
- `maxWaterDepth`：`RubberTreeConfigWaterDepthFilterPlacementModifier.shouldPlace()`。
- `baseHeight` / `heightRandA` / `heightRandB`：`RubberTreeFeature.buildRuntimeConfig()` 中覆盖 `StraightTrunkPlacer`。
- `foliageRadius` / `foliageOffset` / `foliageHeight`：`RubberTreeFeature.buildRuntimeConfig()` 中覆盖 `RubberTreeFoliagePlacer`。
- `zeroHoleWeight` / `singleHoleWeight` / `doubleHoleWeight`：`RubberLogBlock.initializeNaturalState()` 中决定每根自然原木刷出 `0/1/2` 个湿孔的权重。
- `ignoreVines` / `forceDirt`：`RubberTreeFeature.buildRuntimeConfig()` 中覆盖 `TreeFeatureConfig.Builder`。

## 5. 自然世界生成时的实际执行顺序

当某个区块在 `VEGETAL_DECORATION` 阶段尝试生成橡胶树时，链路如下：

1. `placed_feature/rubber_tree.json` 开始执行 placement modifier 链。
2. `RubberTreeConfigPlacementModifier` 读取当前配置，决定本区块要不要给出候选落点。
3. `in_square` 把候选点随机扩散到区块内。
4. `surface_relative_threshold_filter` 限制候选点不能高于地表。
5. `RubberTreeConfigWaterDepthFilterPlacementModifier` 检查当前位置的水深是否允许。
6. `heightmap` 最终把 y 调整到 `WORLD_SURFACE_WG`。
7. 满足条件后，进入 `ic2_120:rubber_tree_feature`，也就是 `RubberTreeFeature.generate()`。

## 6. `RubberTreeFeature` 做了什么

`RubberTreeFeature` 不是直接照抄原版树，而是在调用 `Feature.TREE.generate(...)` 前后加了额外逻辑。

### 6.1 先构建运行时配置

`buildRuntimeConfig(baseConfig)` 以 JSON 里的 `TreeFeatureConfig` 为基底，然后按当前配置重建：

- `StraightTrunkPlacer`
- `RubberTreeFoliagePlacer`
- `ignoreVines`
- `forceDirt`

因此：

- 树形参数不是固定写死在 JSON。
- `/ic2config reload` 后，新生成的橡胶树会读取新参数。

### 6.2 区分“树苗长树”和“自然世界生成”

`isSaplingGrowth(...)` 通过 `origin` 位置是否还是 `RubberSaplingBlock` 来判断：

- 是树苗生长：直接走 `Feature.TREE.generate(runtimeContext)`，不做挪位、清理或替换周围树木。
- 是自然世界生成：执行额外的避让和清理逻辑。

这意味着树苗长树也会吃到新的“树形参数”，但不会吃到“替换周围树木”的行为。

### 6.3 自然世界生成时的避让逻辑

`findOverlappingTreeBlocks(...)` 会扫描一个固定包围盒：

- 半径：`CLEARANCE_RADIUS = 3`
- 高度：`CLEARANCE_HEIGHT = 12`

规则如下：

- 若遇到 `logs` 或 `leaves` 标签方块，先记下来，稍后允许直接清掉。
- 若 `ignoreVines=true` 且遇到藤蔓，则忽略。
- 若遇到其他不可替换硬障碍，则本次生成直接失败。

如果扫描通过，会先把记录下来的旧树木方块替换为空气，再调用原版 `Feature.TREE.generate(...)`。

### 6.4 生成后清理残叶

生成成功后，`cleanupOrphanLeaves(...)` 会在更大范围内清理“被替换旧树留下来的残叶”：

- 只处理 `leaves` 标签方块。
- 不清理 `RubberLeavesBlock`。
- 只有在 6 格连通搜索内找不到任何原木支撑时才删除。

目标是避免橡胶树把别的树半截顶掉以后，留下浮空树叶。

## 7. 树叶形状怎么生成

`RubberTreeFoliagePlacer` 继承 `BlobFoliagePlacer`，但不是完全照搬原版。

当前行为：

- 底层和中层先按 blob 逻辑生成到缓存。
- 最高两层只保留中心一片树叶。
- 若顶两层中心叶不存在，会强制补上。

结果是：

- 下部仍是较自然的团状树冠。
- 顶部变成 IC2 风格的“尖顶单叶”。

## 8. 橡胶孔是怎么初始化的

橡胶孔初始化由 `RubberHoleTreeDecorator` 完成。

执行时机：

- 原版树干与树叶已经决定要放哪些位置。
- 装饰器在 tree feature 生成流程中，对最终保留下来的原木位置再次写状态。

具体行为：

1. 只处理当前仍然是 `RubberLogBlock` 的位置。
2. 先构造 `axis=y, natural=true` 的基底状态。
3. 对每根原木调用 `RubberLogBlock.initializeNaturalState(...)`，按 `zeroHoleWeight` / `singleHoleWeight` / `doubleHoleWeight` 随机得到 0 到 2 个湿橡胶孔。
4. 若整棵树一根湿孔都没刷出来，则强制在树干下半部随机补一个湿孔。
5. 最终把这些状态写回日志方块。

这里的目的有两个：

- 保证自然生成的树通常在生成时就有可提取树脂的孔。
- 不再依赖 `onBlockAdded` 或 BlockEntity tick 去“补初始化”。

## 9. `RubberLogBlock` 在这条链上的作用

`RubberLogBlock` 本身也保留了一层兜底逻辑：

- 如果某个原木以 `natural=true` 且“四面都没有孔”的状态被放进世界，
- `onBlockAdded(...)` 会再调用一次 `initializeNaturalState(...)`。

但按当前实现，主路径仍然是 `RubberHoleTreeDecorator`。
`onBlockAdded(...)` 更像防御性兜底，而不是主初始化来源。

另外，`initializeNaturalState(...)` 最终会把 `NATURAL` 改回 `false`。
因此 `NATURAL` 在当前实现里更像“待初始化标记”，不是持久的“这根原木来自自然生成”标记。

## 10. 树苗生长链路

橡胶树苗不走 placed feature，而是直接取 configured feature：

1. `RubberSaplingBlock` 使用自定义 `RubberSaplingGenerator`。
2. `RubberSaplingGenerator.getTreeFeature(...)` 返回 `RegistryKey<ConfiguredFeature<*, *>>(ic2_120:rubber_tree)`。
3. 原版树苗成长流程据此拉取 configured feature。
4. 最终仍进入 `RubberTreeFeature.generate()`。

所以：

- 树苗成长共享同一份 `configured_feature/rubber_tree`。
- 也会共享 `RubberTreeFeature` 的树形运行时覆盖逻辑。
- 但不会经过 placed feature 的 count/rarity/water/biome 过滤链。

## 11. Access Widener 依赖

`ic2_120.accesswidener` 当前放宽了两个原版构造器：

- `FoliagePlacerType(Codec)`
- `TreeDecoratorType(Codec)`

原因是当前需要直接注册：

- `RubberTreeFoliagePlacer`
- `RubberHoleTreeDecorator`

如果将来重构世界生成而删除或改动这部分自定义类型，需要一并检查 `accesswidener` 是否还能收敛。

## 12. `/ic2config reload` 的真实影响范围

`/ic2config reload` 只做一件事：刷新 `Ic2Config.current`。

重载后会立即影响的内容：

- `countPerChunk`
- `rarityChance`
- `maxWaterDepth`
- `baseHeight`
- `heightRandA`
- `heightRandB`
- `foliageRadius`
- `foliageOffset`
- `foliageHeight`
- `zeroHoleWeight`
- `singleHoleWeight`
- `doubleHoleWeight`
- `ignoreVines`
- `forceDirt`

重载后不会自动重新注册的内容：

- `enabled`
- `biomes`

另外，配置重载只影响“之后的新生成/新生长”：

- 已经生成好的区块不会回滚。
- 已经存在的树不会按新配置重建。

## 13. 排查橡胶树不生成时优先看哪里

1. 看 `Ic2Config.worldgen.rubberTree.enabled` 是否为 `true`。
2. 看 `biomes` 是否填了合法生物群系 id。
3. 若修改了 `enabled` 或 `biomes`，确认是否已经重启游戏或服务端，而不是只执行了 `/ic2config reload`。
4. 看 `countPerChunk` 是否为 `0`。
5. 看 `rarityChance` 是否设得过高。
6. 看 `maxWaterDepth` 是否把沼泽等地形过滤掉了。
7. 看周围是否有不可替换硬障碍，导致 `RubberTreeFeature` 在避让阶段直接失败。
8. 看是否把 `accesswidener`、自定义注册或 JSON id 改坏了。

## 14. 当前设计取舍

当前方案的优点：

- 保留了原版 `ConfiguredFeature` / `PlacedFeature` 体系入口，便于树苗和世界生成共用。
- 大部分平衡参数可由 `Ic2Config` 热重载后影响未来生成。
- 树叶形状、橡胶孔和避让逻辑可以继续留在 Kotlin 中做复杂控制。

当前方案的限制：

- `enabled` 和 `biomes` 不是热更新字段，改完需要重启后重新注册 biome modification。
- 世界生成仍部分依赖 JSON 作为注册入口，不是单一来源。
- `NATURAL` 当前只是初始化标记，不是稳定的“自然生成来源”标记。
