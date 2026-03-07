# 类级别注解注册系统（枚举版本）

## 概述

本项目使用**基于类级别注解和枚举**的自动注册系统，提供类型安全的方块、物品和创造模式物品栏注册。直接在类上添加注解即可自动注册，无需手动编写注册代码。

## 核心特性

- ✅ **类级别注解**：直接在 Block/BlockEntity/Item/Tab/ScreenHandler/HandledScreen 类上添加注解
- ✅ **类型安全枚举**：使用 `CreativeTab` 枚举替代字符串
- ✅ **自动扫描注册**：扫描指定包中的类，自动创建实例并注册
- ✅ **编译时检查**：IDE 自动补全，编译器检查类型
- ✅ **命名转换**：支持从类名自动推导注册名（驼峰转下划线）
- ✅ **OOP 设计**：类定义和注册信息在同一位置

## 快速开始

### 1. 注册方块

直接创建 Block 子类并添加 `@ModBlock` 注解：

```kotlin
package ic2_120.content.block

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import net.minecraft.block.Block
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Blocks

@ModBlock(
    name = "copper_block",
    registerItem = true,
    tab = CreativeTab.IC2_MATERIALS
)
class CopperBlock : Block(
    AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)
)
```

**注解参数**：
- `name`: 注册名（不含命名空间）。为空时自动使用类名转换（如 `CopperBlock` → `copper_block`）
- `registerItem`: 是否自动注册方块物品（默认 `true`）
- `tab`: 创造模式物品栏位置，使用 `CreativeTab` 枚举常量

**可用的物品栏枚举**：
```kotlin
CreativeTab.IC2_MATERIALS        // IC2 材料物品栏
CreativeTab.IC2_MACHINES         // IC2 机器物品栏
CreativeTab.MINECRAFT_BUILDING_BLOCKS   // 原版建筑方块
CreativeTab.MINECRAFT_DECORATIONS       // 原版装饰品
CreativeTab.MINECRAFT_REDSTONE          // 原版红石
CreativeTab.MINECRAFT_MISC              // 原版杂项
// ... 更多见 CreativeTab 枚举定义
```

### 2. 注册物品

创建 Item 子类并添加 `@ModItem` 注解：

```kotlin
package ic2_120.content.item

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import net.minecraft.item.Item
import net.fabricmc.fabric.api.item.v1.FabricItemSettings

@ModItem(
    name = "copper_ingot",
    tab = CreativeTab.IC2_MATERIALS
)
class CopperIngot : Item(FabricItemSettings())
```

**注解参数**：
- `name`: 注册名（不含命名空间）。为空时自动使用类名转换
- `tab`: 创造模式物品栏位置，使用 `CreativeTab` 枚举常量

### 3. 注册创造模式物品栏

创建类并添加 `@ModCreativeTab` 注解：

```kotlin
package ic2_120.content.tab

import ic2_120.registry.annotation.ModCreativeTab

@ModCreativeTab(
    name = "ic2_materials",
    iconItem = "copper_ingot"
)
class Ic2MaterialsTab
```

**注解参数**：
- `name`: 注册名（不含命名空间）
- `iconItem`: 图标物品的注册名（不含命名空间），物品必须已注册

**说明**：物品栏在扫描时一并收集，实际注册在方块、方块实体、物品之后执行，以便引用已注册的物品。

### 4. 注册方块实体类型（BlockEntityType）

在 BlockEntity 子类上添加 `@ModBlockEntity` 注解，扫描器会自动创建并注册 `BlockEntityType`，并通过 `ModBlockEntities.getType(Class)` 提供访问：

```kotlin
package ic2_120.content.block

import ic2_120.content.ModBlockEntities
import ic2_120.registry.annotation.ModBlockEntity
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.util.math.BlockPos

@ModBlockEntity(name = "electric_furnace")
class ElectricFurnaceBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state) {

    constructor(pos: BlockPos, state: BlockState) : this(
        ModBlockEntities.getType(ElectricFurnaceBlockEntity::class),
        pos,
        state
    )
}
```

**注解参数**：
- `name`: 注册名（不含命名空间），需与对应方块的注册名一致，以便扫描器从 `Registries.BLOCK` 获取方块。为空时使用类名转换

**约定**：
- 方块实体需提供 `(BlockPos, BlockState)` 的副构造函数，内部通过 `ModBlockEntities.getType(当前类::class)` 调用主构造
- 对应方块在 `createBlockEntity` 中应使用该副构造创建实体（如 `ElectricFurnaceBlockEntity(pos, state)`）

### 5. 处理多个变体

如果有多个只是参数不同的变体，创建多个类即可：

```kotlin
@ModBlock(name = "copper_ore", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class CopperOre : Block(Settings.create().strength(3.0f))

@ModBlock(name = "deepslate_copper_ore", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class DeepslateCopperOre : Block(Settings.create().strength(4.5f))
```

### 6. 注册 ScreenHandler 与客户端 Screen（UI）

**服务端 / 共用**：在 ScreenHandler 类上添加 `@ModScreenHandler`，并约定类有 companion 方法 `fromBuffer(syncId, playerInventory, buf)`：

```kotlin
@ModScreenHandler(name = "electric_furnace")
class ElectricFurnaceScreenHandler(...) : ScreenHandler(
    ModScreenHandlers.getType(ElectricFurnaceScreenHandler::class), syncId
) {
    companion object {
        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): ElectricFurnaceScreenHandler { ... }
    }
}
```

**客户端**：在 HandledScreen 子类上添加 `@ModScreen(handler = "注册名")`，与对应 ScreenHandler 的 name 一致：

```kotlin
@ModScreen(handler = "electric_furnace")
class ElectricFurnaceScreen(handler: ElectricFurnaceScreenHandler, ...) : HandledScreen<...>(...)
```

主入口扫描包需包含 `ic2_120.content.screen`；客户端入口调用 `ClientScreenRegistrar.registerScreens(Ic2_120.MOD_ID, listOf("ic2_120.client"))`。

### 7. 在主入口点启用自动注册

```kotlin
package ic2_120

import ic2_120.registry.ClassScanner
import net.fabricmc.api.ModInitializer

object Ic2_120 : ModInitializer {
    const val MOD_ID = "ic2_120"

    override fun onInitialize() {
        // 扫描包含物品栏、方块、方块实体和物品类的包
        // 同一包内可同时包含 @ModBlock 与 @ModBlockEntity 类
        ClassScanner.scanAndRegister(
            MOD_ID,
            listOf(
                "ic2_120.content.tab",    // 扫描物品栏类（优先）
                "ic2_120.content.block",  // 扫描方块类与方块实体类
                "ic2_120.content.screen", // 扫描 ScreenHandler 类
                "ic2_120.content.item"    // 扫描物品类
            )
        )
    }
}
```

## 工作原理

### 扫描流程

1. **包扫描**：`ClassScanner` 扫描指定包中的所有 `.class` 文件
2. **类加载**：使用 `ClassLoader` 加载每个类
3. **注解检测**：检查类是否带有 `@ModCreativeTab`、`@ModBlock`、`@ModBlockEntity` 或 `@ModItem` 注解
4. **类型验证**：确保带注解的类是正确的子类
5. **实例化**：使用 `clazz.createInstance()` 创建实例
6. **注册**：将实例注册到 Minecraft 注册表

### 注册顺序

自动按以下顺序执行注册：
1. **方块**（`@ModBlock`）
2. **方块实体类型**（`@ModBlockEntity`）— 依赖方块已注册，以便按同名 ID 查找方块
3. **ScreenHandler 类型**（`@ModScreenHandler`）— 需 companion 提供 `fromBuffer(syncId, playerInventory, buf)`
4. **物品**（`@ModItem`）
5. **物品栏**（`@ModCreativeTab`）— 最后注册，以便引用已注册的物品

客户端在 `ClientModInitializer` 中调用 `ClientScreenRegistrar.registerScreens(modId, listOf("ic2_120.client"))`，会扫描带 `@ModScreen(handler = "…")` 的 HandledScreen 子类并注册到 HandledScreens。

这确保了方块实体类型能正确关联方块，且方块物品和物品可以正确添加到物品栏。

### 命名转换

如果注解的 `name` 参数为空，系统会自动转换类名：

| 类名 | 注册名 |
|------|--------|
| `CopperBlock` | `copper_block` |
| `ElectricFurnace` | `electric_furnace` |
| `DeepslateCopperOre` | `deepslate_copper_ore` |

转换规则：驼峰命名 → 下划线小写

### 枚举的优势

使用枚举而非字符串：

```kotlin
// ❌ 旧方式：字符串（容易出错）
@ModBlock(tab = "ic2_120:ic2_materials")

// ✅ 新方式：枚举（类型安全）
@ModBlock(tab = CreativeTab.IC2_MATERIALS)
```

**优势**：
- ✅ **类型安全**：编译时检查，避免拼写错误
- ✅ **IDE 支持**：自动补全所有选项
- ✅ **重构友好**：重命名枚举值时自动更新引用
- ✅ **可读性强**：代码更清晰易读

## 目录结构

推荐的目录结构：

```
src/main/kotlin/ic2_120/
├── content/
│   ├── tab/                 # 创造模式物品栏
│   │   └── ...
│   ├── block/               # 方块类与方块实体类
│   │   ├── MetalBlocks.kt
│   │   ├── ElectricFurnaceBlock.kt
│   │   ├── ElectricFurnaceBlockEntity.kt  # @ModBlockEntity
│   │   └── ...
│   ├── item/                # 物品类
│   │   └── ...
│   └── ModBlockEntities.kt  # 方块实体类型访问入口（getType）
├── registry/
│   ├── annotation/          # 注解定义
│   │   ├── ModCreativeTab.kt
│   │   ├── ModBlock.kt
│   │   ├── ModBlockEntity.kt
│   │   ├── ModItem.kt
│   │   ├── ModScreenHandler.kt
│   │   └── ModScreen.kt
│   ├── ClassScanner.kt      # 类扫描器
│   ├── BlockEntityTypeStore.kt  # 存储扫描得到的 BlockEntityType
│   ├── ScreenHandlerTypeStore.kt  # 存储扫描得到的 ScreenHandlerType
│   └── CreativeTab.kt       # 物品栏枚举
└── Ic2_120.kt               # 主入口
```

## 常见问题

### Q: 如何注册没有物品的方块？

```kotlin
@ModBlock(name = "hidden_block", registerItem = false, tab = CreativeTab.MINECRAFT_MISC)
class HiddenBlock : Block(Settings.create())
```

### Q: 如何添加新的方块实体？

在 BlockEntity 类上添加 `@ModBlockEntity(name = "与方块相同的注册名")`，并提供 `(BlockPos, BlockState)` 副构造，内部通过 `ModBlockEntities.getType(当前类::class)` 调用主构造。对应方块在 `createBlockEntity` 中调用该副构造即可。详见上文「4. 注册方块实体类型」。

### Q: 如何将物品添加到原版创造模式标签？

```kotlin
@ModItem(name = "my_item", tab = CreativeTab.MINECRAFT_REDSTONE)
class MyItem : Item(Settings.create())
```

### Q: 类的构造函数需要参数怎么办？

当前版本支持无参构造函数或所有参数都有默认值：

```kotlin
class MachineBlock(
    hardness: Float = 3.5f
) : Block(Settings.create().strength(hardness))
```

### Q: 如何查看所有可用的物品栏枚举？

查看 `CreativeTab.kt` 枚举定义，或使用 IDE 自动补全：

```kotlin
CreativeTab.  // 输入点号后查看所有选项
```

### Q: 如何调试注册问题？

查看日志文件 `logs/latest.log`，搜索 `ic2_120/ClassScanner`：

```
[INFO] 开始扫描包进行自动注册: [ic2_120.content.tab, ic2_120.content.block, ...]
[INFO] 扫描包: ic2_120.content.block
[DEBUG] 发现 @ModBlock: ic2_120.content.block.CopperBlock
[INFO] 已注册方块: ic2_120:copper_block
[INFO] 已注册方块物品: ic2_120:copper_block
[DEBUG] 已将 ic2_120:copper_block 添加到物品栏: IC2_MATERIALS
```

## 限制

1. **无参构造**：类必须有无参构造函数或所有参数都有默认值
2. **包扫描**：需要在入口点明确指定要扫描的包
3. **注册顺序**：物品栏必须先于方块和物品注册
4. **Kotlin 反射**：使用 Kotlin 反射，注解必须是 `RUNTIME` 保留

## 类型安全示例

### 编译时检查

```kotlin
// ✅ 正确：使用枚举
@ModBlock(tab = CreativeTab.IC2_MATERIALS)
class MyBlock : Block(...)

// ❌ 编译错误：枚举值不存在
@ModBlock(tab = CreativeTab.INVALID_TAB)
class MyBlock : Block(...)
```

### IDE 自动补全

输入 `CreativeTab.` 后，IDE 自动显示所有可用选项：

```
CreativeTab.IC2_MATERIALS
CreativeTab.IC2_MACHINES
CreativeTab.MINECRAFT_BUILDING_BLOCKS
CreativeTab.MINECRAFT_DECORATIONS
...
```

## 完整示例

### 方块类

```kotlin
package ic2_120.content.block

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import net.minecraft.block.Block
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Blocks

@ModBlock(
    name = "copper_block",
    registerItem = true,
    tab = CreativeTab.IC2_MATERIALS
)
class CopperBlock : Block(
    AbstractBlock.Settings
        .copy(Blocks.IRON_BLOCK)
        .strength(5.0f, 6.0f)
        .requiresTool()
)
```

### 物品类

```kotlin
package ic2_120.content.item

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import net.minecraft.item.Item
import net.fabricmc.fabric.api.item.v1.FabricItemSettings

@ModItem(
    name = "copper_ingot",
    tab = CreativeTab.IC2_MATERIALS
)
class CopperIngot : Item(
    FabricItemSettings()
)
```

### 物品栏类

```kotlin
package ic2_120.content.tab

import ic2_120.registry.annotation.ModCreativeTab

@ModCreativeTab(
    name = "ic2_materials",
    iconItem = "copper_ingot"
)
class Ic2MaterialsTab
```

## 未来改进

- [ ] 支持带参数的构造函数
- [ ] 支持 `@Repeatable` 注解（同一类多个注册）
- [ ] 添加更多物品栏枚举选项
