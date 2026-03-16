# 配方注册系统重构实施计划

## 概述

将配方注册从集中式 `ModRecipeProvider.kt` 改造为基于类级别方法的分布式系统，使用类型安全的扩展方法替代字符串 ID 引用。

### 核心设计

- **ClassScanner** 作为类型访问中心，持有所有 Block/Item 实例
- **扩展方法** 提供类型安全的 API（`Block::class.instance()`）
- **约定方法**：在 Block/Item 的 companion object 中定义 `generateRecipes()`
- **渐进式迁移**：新旧系统可以共存

---

## 阶段一：核心基础设施改造

### 目标

1. 创建类型安全的扩展方法
2. 修改 ClassScanner 持有实例引用
3. 简化 ModRecipeProvider
4. 删除旧的中介类（ModBlockEntities、ModScreenHandlers 等）

---

### 1.1 新增文件

#### 文件：`src/main/kotlin/ic2_120/registry/RegistryExtensions.kt`

提供类型安全的扩展方法，替代旧的字符串 ID 方式。

```kotlin
package ic2_120.registry

import net.minecraft.block.Block
import net.minecraft.block.entity.BlockEntity
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.util.Identifier
import kotlin.reflect.KClass

// ========== Block 扩展 ==========

/**
 * 获取 Block 单例实例
 */
fun <T : Block> KClass<T>.instance(): T =
    ClassScanner.getBlockInstance(this) as T

/**
 * 获取 Block 的注册 ID
 */
fun <T : Block> KClass<T>.id(): Identifier =
    Registries.BLOCK.getId(instance())

/**
 * 获取 Block 对应的 Item
 */
fun <T : Block> KClass<T>.item(): Item =
    instance().asItem()

// ========== Item 扩展 ==========

/**
 * 获取 Item 单例实例
 */
fun <T : Item> KClass<T>.instance(): T =
    ClassScanner.getItemInstance(this) as T

/**
 * 获取 Item 的注册 ID
 */
fun <T : Item> KClass<T>.id(): Identifier =
    Registries.ITEM.getId(instance())

// ========== BlockEntity 扩展 ==========

/**
 * 获取 BlockEntityType
 */
fun <T : BlockEntity> KClass<T>.type(): BlockEntityType<T> =
    ClassScanner.getBlockEntityType(this)

// ========== ScreenHandler 扩展 ==========

/**
 * 获取 ScreenHandlerType
 */
fun <T : ScreenHandler> KClass<T>.type(): ScreenHandlerType<T> =
    ClassScanner.getScreenHandlerType(this)
```

---

### 1.2 修改 ClassScanner.kt

#### 1.2.1 添加导入

在文件开头的导入区域添加：

```kotlin
import java.util.function.Consumer
import net.minecraft.data.server.recipe.RecipeJsonProvider
```

#### 1.2.2 添加成员变量

在 `tabItems` 和 `blockClassToName` 声明后添加：

```kotlin
/** 存储 Block 实例（供扩展方法访问） */
private val blockInstances = mutableMapOf<kotlin.reflect.KClass<*>, Block>()

/** 存储 Item 实例（供扩展方法访问） */
private val itemInstances = mutableMapOf<kotlin.reflect.KClass<*>, Item>()

/** 配方生成器列表 */
private val recipeGenerators = mutableListOf<(Consumer<RecipeJsonProvider>) -> Unit>()
```

#### 1.2.3 修改 scanAndRegister() 方法

**位置**：约第 77 行开始的方法

**步骤 1**：在清空映射部分（约第 94-97 行）添加新的清空：

```kotlin
tabItems.clear()
blockClassToName.clear()
TransparentBlockRegistry.clear()
blockInstances.clear()       // 新增
itemInstances.clear()        // 新增
recipeGenerators.clear()     // 新增
```

**步骤 2**：在方法末尾，`registerCreativeTabs` 之后添加：

```kotlin
registerBlocks(modId, blockClasses)
registerBlockEntities(modId, blockEntityClasses)
registerScreenHandlers(modId, screenHandlerClasses)
registerItems(modId, itemClasses)
registerCreativeTabs(modId, tabClasses)
collectRecipeGenerators(blockClasses, itemClasses)  // 新增
```

#### 1.2.4 修改 registerBlocks() 方法

**位置**：约第 453-488 行

在注册方块后，保存实例：

```kotlin
// 注册方块
Registry.register(Registries.BLOCK, id, instance)
blockClassToName[clazz] = name
blockInstances[clazz] = instance  // 新增：保存实例
if (annotation.transparent) {
    TransparentBlockRegistry.add(id)
}
```

#### 1.2.5 修改 registerItems() 方法

**位置**：约第 490-514 行

在注册物品后，保存实例：

```kotlin
// 注册物品
Registry.register(Registries.ITEM, id, instance)
itemInstances[clazz] = instance  // 新增：保存实例
logger.debug("已注册物品: {}", id)
```

#### 1.2.6 修改 registerBlockEntities() 方法

**位置**：约第 410-451 行

将 `BlockEntityTypeStore.registerType(clazz, type)` 替换为：

```kotlin
blockEntityTypes[clazz] = type  // 替换：保存到内部映射
```

同时删除导入：
```kotlin
import ic2_120.registry.BlockEntityTypeStore
```

#### 1.2.7 修改 registerScreenHandlers() 方法

**位置**：约第 356-397 行

将 `ScreenHandlerTypeStore.registerType(clazz, type)` 替换为：

```kotlin
screenHandlerTypes[clazz] = type  // 替换：保存到内部映射
```

同时删除导入：
```kotlin
import ic2_120.registry.ScreenHandlerTypeStore
```

#### 1.2.8 添加新方法

**位置**：文件末尾（最后一个 `private data class` 之前，约第 547 行）

添加以下 5 个新方法：

```kotlin
/**
 * 收集 Block/Item 类 companion 中的配方生成器
 */
private fun collectRecipeGenerators(
    blockClasses: List<BlockClassInfo>,
    itemClasses: List<ItemClassInfo>
) {
    val allClasses = (blockClasses.map { it.clazz } + itemClasses.map { it.clazz })

    for (clazz in allClasses) {
        try {
            val companion = clazz.companionObjectInstance ?: continue
            val generateRecipesMethod = clazz.companionObject?.memberFunctions?.find {
                it.name == "generateRecipes"
            } ?: continue

            // 验证方法签名
            val parameters = generateRecipesMethod.parameters
            if (parameters.size != 2 ||
                parameters[1].type.classifier != Consumer::class) {
                logger.warn("类 {} 的 generateRecipes 方法签名不正确，应为 (Consumer<RecipeJsonProvider>) -> Unit", clazz.simpleName)
                continue
            }

            // 创建生成器闭包
            val generator: (Consumer<RecipeJsonProvider>) -> Unit = { exporter ->
                @Suppress("UNCHECKED_CAST")
                generateRecipesMethod.call(companion, exporter as Any?)
            }

            recipeGenerators.add(generator)
            logger.debug("已收集配方生成器: {}", clazz.simpleName)
        } catch (e: Exception) {
            logger.debug("收集配方生成器失败 {}: {}", clazz.simpleName, e.message)
        }
    }

    logger.info("共收集 {} 个配方生成器", recipeGenerators.size)
}

/**
 * 执行所有配方生成
 * 供 ModRecipeProvider 调用
 */
fun generateAllRecipes(recipeExporter: Consumer<RecipeJsonProvider>) {
    logger.info("开始生成配方...")
    recipeGenerators.forEach { it(recipeExporter) }
    logger.info("配方生成完成，共生成 {} 个配方", recipeGenerators.size)
}

/**
 * 获取 Block 实例
 * 供扩展方法调用
 */
internal fun getBlockInstance(clazz: kotlin.reflect.KClass<out Block>): Block =
    blockInstances[clazz] ?: error("Block instance not found: ${clazz.simpleName}")

/**
 * 获取 Item 实例
 * 供扩展方法调用
 */
internal fun getItemInstance(clazz: kotlin.reflect.KClass<out Item>): Item =
    itemInstances[clazz] ?: error("Item instance not found: ${clazz.simpleName}")

/**
 * 获取 BlockEntityType
 * 供扩展方法调用
 */
@Suppress("UNCHECKED_CAST")
internal fun <T : BlockEntity> getBlockEntityType(clazz: kotlin.reflect.KClass<T>): BlockEntityType<T> =
    blockEntityTypes[clazz] as? BlockEntityType<T>
        ?: error("BlockEntityType not found: ${clazz.simpleName}")

/**
 * 获取 ScreenHandlerType
 * 供扩展方法调用
 */
@Suppress("UNCHECKED_CAST")
internal fun <T : ScreenHandler> getScreenHandlerType(clazz: kotlin.reflect.KClass<T>): ScreenHandlerType<T> =
    screenHandlerTypes[clazz] as? ScreenHandlerType<T>
        ?: error("ScreenHandlerType not found: ${clazz.simpleName}")
```

---

### 1.3 简化 ModRecipeProvider.kt

#### 文件：`src/main/kotlin/ic2_120/content/recipes/ModRecipeProvider.kt`

**完全替换为**：

```kotlin
package ic2_120.content.recipes

import ic2_120.registry.ClassScanner
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider
import java.util.function.Consumer

/**
 * 工作台配方数据生成器
 * 从 ClassScanner 收集的配方生成器生成配方 JSON 文件
 *
 * 注意：ClassScanner 在 Ic2_120.onInitialize() 时扫描并收集配方生成器，
 * datagen 任务运行时会调用 generate() 方法执行实际的配方生成。
 */
class ModRecipeProvider(output: FabricDataOutput) : FabricRecipeProvider(output) {

    override fun generate(recipeExporter: Consumer<net.minecraft.data.server.recipe.RecipeJsonProvider>) {
        // 调用 ClassScanner 中收集到的所有配方生成器
        ClassScanner.generateAllRecipes(recipeExporter)
    }
}
```

---

### 1.4 删除旧文件

删除以下 4 个文件：

1. `src/main/kotlin/ic2_120/content/ModBlockEntities.kt`
2. `src/main/kotlin/ic2_120/content/screen/ModScreenHandlers.kt`
3. `src/main/kotlin/ic2_120/registry/BlockEntityTypeStore.kt`
4. `src/main/kotlin/ic2_120/registry/ScreenHandlerTypeStore.kt`

---

### 1.5 批量替换代码

#### 替换 1：BlockEntityType 访问

```
查找: ModBlockEntities\.getType\((\w+BlockEntity::class)\)
替换: $1.type()
```

#### 替换 2：ScreenHandlerType 访问

```
查找: ModScreenHandlers\.getType\((\w+ScreenHandler::class)\)
替换: $1.type()
```

#### 替换 3：删除 import 语句

```
查找: import ic2_120\.content\.ModBlockEntities
替换: (删除)

查找: import ic2_120\.content\.screen\.ModScreenHandlers
替换: (删除)
```

#### 添加新的 import

```
查找: import ic2_120\.registry\.annotation\.
在之后添加:
import ic2_120.registry.type
```

---

### 1.6 阶段一验证

```bash
# 1. 编译
./gradlew compileKotlin
./gradlew compileClientKotlin

# 2. 运行 datagen
./gradlew runDatagen

# 3. 检查生成的配方 JSON 文件
ls src/main/generated/resources/data/ic2_120/recipe/

# 4. 完整构建
./gradlew build
```

---

## 阶段二：配方迁移

### 目标

将 `ModRecipeProvider.kt` 中的配方代码迁移到对应的 Block/Item 文件。

---

### 2.1 迁移策略

在每个配方对应的 Block/Item 类的 companion object 中添加 `generateRecipes()` 方法。

**方法签名**：
```kotlin
fun generateRecipes(exporter: Consumer<RecipeJsonProvider>)
```

---

### 2.2 配方示例

#### 示例 1：简单无序配方（板类）

**文件**：`src/main/kotlin/ic2_120/content/item/Plates.kt`

```kotlin
@ModItem(name = "iron_plate", tab = CreativeTab.IC2_MATERIALS, group = "plates")
class IronPlate : Item(FabricItemSettings()) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, IronPlate::class.instance(), 1)
                .input(ForgeHammer::class.instance())
                .input(Items.IRON_INGOT)
                .criterion(hasItem(Items.IRON_INGOT), conditionsFromItem(Items.IRON_INGOT))
                .offerTo(exporter, IronPlate::class.id())
        }
    }
}
```

#### 示例 2：有序配方（管道）

**文件**：`src/main/kotlin/ic2_120/content/block/PipeBlocks.kt`（或相关文件）

```kotlin
@ModBlock(name = "bronze_pipe_tiny", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class BronzePipeTinyBlock : Block(Settings.create()) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, BronzePipeTinyBlock::class.item(), 6)
                .pattern("XXX").pattern("   ").pattern("XXX")
                .input('X', BronzeCasingBlock::class.item())
                .criterion(
                    hasItem(BronzeCasingBlock::class.item()),
                    conditionsFromItem(BronzeCasingBlock::class.item())
                )
                .offerTo(exporter, BronzePipeTinyBlock::class.id())
        }
    }
}
```

#### 示例 3：多配方（锡罐）

**文件**：`src/main/kotlin/ic2_120/content/item/TinCan.kt`（或相关文件）

```kotlin
@ModItem(name = "tin_can", ...)
class TinCan : Item(FabricItemSettings()) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val tinIngot = TinIngot::class.instance()
            val tinCan = TinCan::class.instance()

            // 配方 1：8 锡锭 -> 16 空锡罐
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, tinCan, 16)
                .pattern("TTT").pattern("T T").pattern("TTT")
                .input('T', tinIngot)
                .criterion(hasItem(tinIngot), conditionsFromItem(tinIngot))
                .offerTo(exporter, Identifier(Ic2_120.MOD_ID, "tin_can_from_ingots_8"))

            // 配方 2：6 锡锭 -> 4 空锡罐
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, tinCan, 4)
                .pattern(" T ").pattern("T T").pattern("TTT")
                .input('T', tinIngot)
                .criterion(hasItem(tinIngot), conditionsFromItem(tinIngot))
                .offerTo(exporter, Identifier(Ic2_120.MOD_ID, "tin_can_from_ingots_6"))
        }
    }
}
```

#### 示例 4：混合原版物品

```kotlin
ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, TinCan::class.instance(), 4)
    .pattern(" T ").pattern("T T").pattern("TTT")
    .input('T', TinIngot::class.instance())
    .input('S', Items.STICK)  // 原版物品直接使用
    .criterion(hasItem(Items.STICK), conditionsFromItem(Items.STICK))
    .offerTo(exporter, Identifier(Ic2_120.MOD_ID, "tin_can_from_ingots_6"))
```

---

### 2.3 迁移优先级

建议按以下顺序迁移配方：

#### 1. 简单无序配方（优先）
- 板类（*Plate）
- 导线类（*Cable）
- 绝缘导线（*Insulated*）

#### 2. 简单有序配方
- 管道类（*Pipe）
- 工具类（*Tool）
- 青铜工具（*Bronze*）

#### 3. 机器配方
- 发电机（*Generator）
- 机器（*Furnace, *Macerator, *Compressor 等）
- 基础机器

#### 4. 复杂配方
- 变压器（*Transformer）
- 核反应堆（*Reactor）
- 斯特林发电机

---

### 2.4 阶段二验证

```bash
# 1. 运行 datagen
./gradlew runDatagen

# 2. 验证配方 JSON 是否正确生成
cat src/main/generated/resources/data/ic2_120/recipe/iron_plate_from_hammer.json

# 3. 游戏内测试
./gradlew runClient
# 测试配方是否能正常合成
```

---

## 阶段三：清理旧配方代码

### 目标

清理 `ModRecipeProvider.kt` 中的旧配方代码。

---

### 3.1 清理步骤

1. **删除旧配方代码**：
   - 删除所有 `createShapeless` 和 `ShapedRecipeJsonBuilder` 调用
   - 保留简化后的版本（见 1.3 节）

2. **删除辅助方法**（如果在类中定义了）：
   - 删除 `createShapeless()` 方法
   - 删除其他临时辅助方法

---

### 3.2 最终的 ModRecipeProvider.kt

```kotlin
package ic2_120.content.recipes

import ic2_120.registry.ClassScanner
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider
import java.util.function.Consumer

/**
 * 工作台配方数据生成器
 * 从 ClassScanner 收集的配方生成器生成配方 JSON 文件
 *
 * 注意：ClassScanner 在 Ic2_120.onInitialize() 时扫描并收集配方生成器，
 * datagen 任务运行时会调用 generate() 方法执行实际的配方生成。
 */
class ModRecipeProvider(output: FabricDataOutput) : FabricRecipeProvider(output) {

    override fun generate(recipeExporter: Consumer<net.minecraft.data.server.recipe.RecipeJsonProvider>) {
        // 调用 ClassScanner 中收集到的所有配方生成器
        ClassScanner.generateAllRecipes(recipeExporter)
    }
}
```

---

### 3.3 阶段三验证

```bash
# 1. 编译
./gradlew compileKotlin
./gradlew compileClientKotlin

# 2. 运行 datagen
./gradlew runDatagen

# 3. 完整构建
./gradlew build
```

---

## 注意事项

### 1. 配方方法命名

- **方法名**：`generateRecipes`（必须）
- **位置**：Block/Item 类的 companion object 中
- **签名**：`fun generateRecipes(exporter: Consumer<RecipeJsonProvider>)`

### 2. 类型安全

使用扩展方法而非字符串 ID：

- ✅ `IronPlate::class.instance()` - 类型安全
- ❌ `Registries.ITEM.get(Identifier("ic2_120:iron_plate"))` - 字符串

### 3. 混合原版物品

原版物品直接使用：

```kotlin
ShapedRecipeJsonBuilder.create(...)
    .input('S', Items.STICK)           // 原版物品
    .input('T', TinIngot::class.instance())  // 模组物品
```

### 4. 错误处理

如果 ClassScanner 未正确扫描到类，扩展方法会抛出错误：

```
Block instance not found: IronPlate
```

**可能原因**：
1. 类未被扫描（检查包名）
2. 类未注册（检查注解）
3. 注册顺序问题（Block/Item 在配方生成前注册）

### 5. IDE 自动补全

使用扩展方法后，IDE 可以提供：
- ✅ 类引用自动补全（输入 `Iron` → `IronPlate`）
- ✅ 编译时检查（拼写错误会立即报错）
- ✅ 重构支持（重命名类会自动更新配方）

---

## 预期结果

### 阶段一完成后

- ✅ 可以使用 `Block::class.type()` 替代 `ModBlockEntities.getType(...)`
- ✅ 可以使用 `Item::class.instance()` 获取物品实例
- ✅ ClassScanner 持有所有类型引用
- ✅ 可以在 Block/Item 类中定义 `generateRecipes()` 方法
- ✅ 删除了 4 个中介文件
- ✅ 所有使用旧 API 的代码已替换

### 阶段二完成后

- ✅ 所有配方从 ModRecipeProvider.kt 迁移到对应 Block/Item 文件
- ✅ 配方使用类型安全的类引用
- ✅ 便于维护，配方与类定义在同一文件

### 阶段三完成后

- ✅ ModRecipeProvider.kt 简化为调用 ClassScanner
- ✅ 所有旧配方代码已清理
- ✅ 配方系统完全分布式

---

## 总结

### 改造前

```kotlin
// 集中式配方：所有配方在一个文件
class ModRecipeProvider {
    override fun generate(exporter) {
        // 500+ 行配方代码
        createShapeless(exporter, "iron_plate_from_hammer", ...)
        createShapeless(exporter, "gold_plate_from_hammer", ...)
        // ... 更多配方
    }
}

// 字符串 ID 引用
val item = Registries.ITEM.get(Identifier("ic2_120:iron_plate"))

// 中介类访问
ModBlockEntities.getType(ElectricFurnaceBlockEntity::class)
```

### 改造后

```kotlin
// 分布式配方：配方在各自类中
@ModItem(name = "iron_plate")
class IronPlate : Item(...) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(
                RecipeCategory.MISC,
                IronPlate::class.instance(),  // 类型安全
                1
            )
                .input(ForgeHammer::class.instance())
                .input(Items.IRON_INGOT)
                .criterion(hasItem(Items.IRON_INGOT), conditionsFromItem(Items.IRON_INGOT))
                .offerTo(exporter, IronPlate::class.id())
        }
    }
}

// 简化的 Provider
class ModRecipeProvider(output: FabricDataOutput) : FabricRecipeProvider(output) {
    override fun generate(exporter) {
        ClassScanner.generateAllRecipes(exporter)
    }
}

// 扩展方法访问
ElectricFurnaceBlockEntity::class.type()
```

### 优势对比

| 方面 | 改造前 | 改造后 |
|------|--------|--------|
| 配方位置 | 集中式（1 个文件） | 分布式（各自类中） |
| 物品引用 | 字符串 ID | 类型安全的类引用 |
| 维护性 | 需要查找 500+ 行 | 配方与类在一起 |
| 类型安全 | ❌ 运行时检查 | ✅ 编译时检查 |
| IDE 支持 | ❌ 无自动补全 | ✅ 完整支持 |
| 重构 | ❌ 手动更新 | ✅ 自动更新 |

---

## 附录：完整的 API 对比

### Block 实例访问

```kotlin
// 旧方式
ModBlocks.BRONZE_CASING  // 需要单独维护

// 新方式
BronzeCasingBlock::class.instance()  // 直接从类获取
```

### Item 实例访问

```kotlin
// 旧方式
val item = Registries.ITEM.get(Identifier("ic2_120:tin_ingot"))

// 新方式
val item = TinIngot::class.instance()
```

### BlockEntityType 访问

```kotlin
// 旧方式
ModBlockEntities.getType(ElectricFurnaceBlockEntity::class)

// 新方式
ElectricFurnaceBlockEntity::class.type()
```

### 配方中的物品引用

```kotlin
// 旧方式（字符串 ID）
ShapedRecipeJsonBuilder.create(RecipeCategory.MISC,
    Registries.ITEM.get(Identifier("ic2_120:bronze_pipe_tiny")), 6
)
    .input('X', Registries.ITEM.get(Identifier("ic2_120:bronze_casing")))

// 新方式（类型安全）
ShapedRecipeJsonBuilder.create(RecipeCategory.MISC,
    BronzePipeTinyBlock::class.item(), 6
)
    .input('X', BronzeCasingBlock::class.item())
```

---

## 执行检查清单

### 阶段一

- [ ] 创建 `RegistryExtensions.kt` 文件
- [ ] 修改 `ClassScanner.kt`（添加成员变量、修改方法、添加新方法）
- [ ] 简化 `ModRecipeProvider.kt`
- [ ] 删除 4 个旧文件
- [ ] 批量替换所有使用旧 API 的代码
- [ ] 编译验证：`./gradlew compileKotlin compileClientKotlin`
- [ ] Datagen 验证：`./gradlew runDatagen`
- [ ] 完整构建：`./gradlew build`

### 阶段二

- [ ] 迁移无序配方（板类、导线类）
- [ ] 迁移有序配方（管道类、工具类）
- [ ] 迁移机器配方（发电机、机器）
- [ ] 迁移复杂配方（变压器、核反应堆）
- [ ] Datagen 验证：`./gradlew runDatagen`
- [ ] 游戏内测试：`./gradlew runClient`

### 阶段三

- [ ] 清理 `ModRecipeProvider.kt` 中的旧配方代码
- [ ] 删除辅助方法（如果存在）
- [ ] 最终验证：`./gradlew runDatagen && ./gradlew build`

---

## 常见问题

### Q1: 为什么不使用注解？

**A**: 约定方法名更简单灵活，避免了注解的复杂性。如果需要注解，可以后续添加 `@ModRecipe` 标记。

### Q2: 如何处理原版物品？

**A**: 原版物品直接使用 `Items.XXX`，无需转换。

### Q3: 配方方法必须在 companion object 中吗？

**A**: 是的，这是约定。这样可以避免实例化开销。

### Q4: 如果配方生成失败怎么办？

**A**: 查看日志，ClassScanner 会输出详细错误信息：
```
[ERROR] 收集配方生成器失败 IronPlate: ...
[WARN] 类 IronPlate 的 generateRecipes 方法签名不正确
```

### Q5: 支持多个配方吗？

**A**: 支持。在一个 `generateRecipes()` 方法中可以创建多个配方（参见示例 3）。

---

## 后续优化（可选）

1. **辅助函数**：提供 `createShaped()` 和 `createShapeless()` 简化配方代码
2. **配方验证**：添加配方验证逻辑（检查物品是否存在）
3. **文档生成**：自动生成配方列表文档
4. **配方热重载**：开发环境支持配方热重载

---

**文档版本**: 1.0
**创建时间**: 2025-03-16
**作者**: Claude (opencode)
**项目**: ic2_120 (Minecraft 1.20.1 Fabric Mod)
