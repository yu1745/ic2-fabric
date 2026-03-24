# JEI 支持实现说明

本文总结 `ic2_120` 当前 JEI 集成的实现结构、数据流和扩展步骤。

## 1. 入口与生命周期

- 插件入口类：`src/main/kotlin/ic2_120/integration/jei/Ic2JeiPlugin.kt`
- 通过 `@JeiPlugin` + `IModPlugin` 接入 JEI。
- `getPluginUid()` 返回 `ic2_120:main` 作为插件唯一标识。

当前实现主要覆盖以下生命周期回调：

1. `registerItemSubtypes`
2. `registerExtraIngredients`
3. `registerCategories`
4. `registerRecipes`
5. `registerRecipeCatalysts`


## 2. RecipeType 中心（核心约定）

- 类型定义类：`src/main/kotlin/ic2_120/integration/jei/Ic2JeiRecipeTypes.kt`
- 每个 JEI 分类都在这里集中定义 `RecipeType<T>`，通过 `RecipeType.create(namespace, path, clazz)` 创建。

作用：

1. 作为 `IRecipeCategory#getRecipeType()` 的返回值；
2. 作为 `registration.addRecipes(type, list)` 的目标类型；
3. 作为 `registration.addRecipeCatalyst(stack, type)` 的绑定类型。

这保证了“分类 UI、配方数据、催化剂方块”三者通过同一类型键一致关联。

## 3. 分类层（Category）

分类实现位于 `src/main/kotlin/ic2_120/integration/jei/*RecipeCategory.kt`，统一实现 `IRecipeCategory<T>`，职责固定为：

1. 定义标题、图标、背景；
2. 返回对应 `RecipeType`；
3. 在 `setRecipe(...)` 中声明输入输出槽布局。

示例差异：

- 常规单入单出（如打粉机、压缩机、提取机）使用 140x50 空白画布。
- 高炉是单输入双输出。
- 离心机、洗矿机是单输入多输出（右侧纵向输出槽）。
- 金属成型机拆分为 3 个独立分类（辊压/切割/挤压）。

## 4. JEI 配方 DTO 层

`src/main/kotlin/ic2_120/integration/jei/*JeiRecipe.kt` 为 JEI 展示专用数据结构（通常是 `data class`），用于把游戏内部配方数据转换成 JEI 需要的最小字段集合。

常见字段：

- `Ingredient` 输入
- `ItemStack` 输出（单个或列表）
- 某些机器额外属性（如离心最小热值、水耗等）

这层把 JEI 展示模型与原始机器配方实现解耦，避免 JEI 直接依赖运行时 recipe 细节。

## 5. 配方数据来源与映射

在 `Ic2JeiPlugin.registerRecipes(...)` 中，当前做法是：

1. 从各机器 `*RecipeDatagen.allEntries()` 读取条目；
2. 映射为对应 `*JeiRecipe`；
3. 调用 `registration.addRecipes(recipeType, mappedList)` 注册到 JEI。

涉及数据源：

- `MaceratorRecipeDatagen`
- `CompressorRecipeDatagen`
- `ExtractorRecipeDatagen`
- `CentrifugeRecipeDatagen`
- `BlastFurnaceRecipeDatagen`
- `OreWashingRecipeDatagen`
- `BlockCutterRecipeDatagen`
- `MetalFormerRecipeDatagen`

金属成型机额外按 `mode` 拆分三份列表，分别注册到 rolling/cutting/extruding 三个 JEI 类型。

## 6. 催化剂（Catalyst）绑定

`registerRecipeCatalysts(...)` 将机器方块 `ItemStack` 绑定到对应 `RecipeType`，使玩家在 JEI 中可通过机器图标进入相关分类。

关键约定：

- 一台机器可绑定多个分类（例如金属成型机绑定 3 个 mode 分类）。
- 方块通过 `Registries.ITEM.get(Identifier(...))` 获取。

## 7. 物品子类型与额外物品栈

### 7.1 子类型解释器

`registerItemSubtypes(...)` 为以下物品注册 `IIngredientSubtypeInterpreter<ItemStack>`：

- `IBatteryItem`
- `IElectricTool`
- `JetpackItem`

解释器根据电量/燃料返回 subtype key（empty/full/partial:*），让 JEI 区分同物品不同能量状态。

### 7.2 额外展示栈

`registerExtraIngredients(...)` 主动补充“空/满”版本：

- 电池：0 与 maxCapacity
- 电动工具：0 与 maxCapacity
- 喷气背包：0 与 MAX_FUEL

这样 JEI 列表可稳定显示关键状态版本，而不是只展示一个默认 NBT 状态。

## 8. 新增一台机器的 JEI 支持清单

1. 在 `Ic2JeiRecipeTypes` 新增 `RecipeType<NewMachineJeiRecipe>`。
2. 新增 `NewMachineJeiRecipe`（展示 DTO）。
3. 新增 `NewMachineRecipeCategory` 并实现槽位布局。
4. 在 `Ic2JeiPlugin.registerCategories` 注册新分类。
5. 在 `Ic2JeiPlugin.registerRecipes` 从数据源映射并 `addRecipes`。
6. 在 `Ic2JeiPlugin.registerRecipeCatalysts` 绑定对应机器方块。
7. 若机器物品需要状态区分（如能量/燃料），补充 subtype interpreter 与 extra stacks。

## 9. 现状边界与注意事项

1. JEI 展示数据当前依赖 datagen 条目；若运行时配方来源改变，需同步调整映射入口。
2. `Ic2JeiRecipeTypes` 是强约束中心：删除该类可以，但必须保留等价的 `RecipeType.create(...)` 定义与统一引用。
