# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是一个使用 Kotlin 编写的 Minecraft 1.20.1 Fabric 模组 (`ic2_120`)。项目使用 Fabric Loom Gradle 插件进行构建和开发。

## 开发参考

开发时应参考 Fabric 官方文档。使用 context7 MCP 服务器查询最新的 Fabric API 文档:

**Context7 库 ID（按优先级排序）**:
1. `/websites/fabricmc_net_develop` - Fabric 开发文档（优先，533 个代码示例，基准分 76.18）
2. `/fabricmc/fabric-docs` - 官方精选文档（2274 个代码示例）

使用方法：通过 context7 的 `query-docs` 工具查询这些库 ID，获取 Fabric API 的类、方法和示例代码。

## 构建命令

**重要**: 所有 Gradle 命令不要添加 `--no-daemon` 参数，以利用 Gradle Daemon 加快构建速度。

- **构建模组**: `./gradlew build` (不要cd,直接执行)
- **清理构建**: `./gradlew clean build`
- **运行客户端**: `./gradlew runClient` - 启动加载了该模组的 Minecraft 进行测试
  - **Windows 中文乱码修复**: 使用 `runClient.bat`（会先设置控制台为 UTF-8 再启动客户端）：
- **运行服务端**: `./gradlew runServer` - 启动加载了该模组的服务器
- **生成源码**: `./gradlew genSources` - 反混淆 Minecraft 源码
- **生成数据**: `./gradlew runDatagen` - 运行数据生成器生成资源/模型

## 架构

### 源码集
项目使用分离的环境源码集:
- `src/main/kotlin` - 通用/服务端代码 (入口点: `Ic2_120`)
- `src/client/kotlin` - 客户端专用代码 (入口点: `Ic2_120Client`)
- `src/main/java` - 通用/服务端的 Mixin 类
- `src/client/java` - 客户端 Mixin 类

### 入口点
所有入口点在 `fabric.mod.json` 中定义:
- **Main**: `Ic2_120` - ModInitializer, 在客户端和服务端上运行
- **Client**: `Ic2_120Client` - ClientModInitializer, 仅客户端初始化
- **DataGen**: `Ic2_120DataGenerator` - DataGeneratorEntrypoint, 生成资源/数据

### Mixins
- `ic2_120.mixins.json` - 通用/服务端 mixins
- `ic2_120.client.mixins.json` - 仅客户端 mixins
- Mixin 类应放在 `src/main/java/ic2_120/mixin/` 或 `src/client/java/ic2_120/mixin/client/`

## 版本配置

关键版本在 `gradle.properties` 中管理:
- `minecraft_version` - 目标 Minecraft 版本 (1.20.1)
- `loader_version` - Fabric Loader 版本
- `fabric_api_version` - Fabric API 版本
- `fabric_kotlin_version` - Fabric Language Kotlin 版本
- `mod_version` - 构建的模组版本

## Kotlin/JVM 配置

- Kotlin 版本: 2.3.10
- JVM 目标: Java 17
- Kotlin 编译选项在 `build.gradle` 中配置

## CI/CD

GitHub Actions 工作流 (`.github/workflows/build.yml`) 会在每次推送和拉取请求时自动使用 JDK 25 构建项目。

## 技术文档

- **[类级别注解注册系统](docs/CLASS_BASED_REGISTRY.md)** - 使用类级别注解和枚举的自动注册系统（推荐）
- **[同步系统（C/S 属性同步）](docs/sync-system.md)** - 基于 ScreenHandler/PropertyDelegate 的整型同步（SyncedData、SyncedDataView）
- **[ComposeUI 声明式 GUI](docs/compose-ui.md)** - 基于 DrawContext 的声明式 UI DSL，用于 Screen 布局与绘制
- **[DrawContext 绘制方法参考](docs/drawcontext-methods.md)** - fill、drawBorder、纹理等绘制 API 说明
- **[Assets 清单](docs/assets-inventory.md)** - 模组方块/物品资源清单与实现状态

**材质路径约定**：ic2_120 模组内所有模型 JSON 引用纹理时，命名空间路径**不带复数 s**——方块纹理用 `ic2:block/...`（不用 `blocks`），物品纹理用 `ic2:item/...`（不用 `items`）。例如：`ic2:block/resource/copper_block`、`ic2:item/resource/ingot/copper`。
- **[创建带有生物群系颜色的方块](docs/biome-colored-blocks.md)** - 如何实现根据生物群系改变颜色的方块（如草方块）

## 注册系统架构

本项目使用**类级别注解 + 枚举**的注册系统：

```kotlin
// 直接在类上添加注解
@ModBlock(name = "copper_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class CopperBlock : Block(Settings.create())

@ModBlockEntity(name = "electric_furnace")
class ElectricFurnaceBlockEntity(...) : BlockEntity(...)

// 类型安全的枚举
enum class CreativeTab(val id: String) {
    IC2_MATERIALS("ic2_materials"),
    IC2_MACHINES("ic2_machines"),
    // ...
}
```

**关键特性**：
- 类级别注解：Block / BlockEntity / Item / Tab 均在类上注解，无需维护注册表对象
- 类型安全枚举：编译时检查，IDE 自动补全
- 自动扫描：`ClassScanner.scanAndRegister()` 扫描指定包
- 注册顺序：方块 → 方块实体类型 → 物品 → 物品栏（详见 `docs/CLASS_BASED_REGISTRY.md`）

**枚举的好处**（用 `CreativeTab` 等枚举替代字符串 id）：
- **类型安全**：编译时检查，错误枚举值直接报错
- **IDE 自动补全**：输入 `CreativeTab.` 即可列出所有选项，无需记忆字符串
- **重构友好**：重命名枚举值时，IDE 自动更新所有引用
- **避免拼写错误**：使用常量而非手写字符串
- **无需维护注册表对象**：类定义与注册合一，不必单独维护 `ModBlocks`/`ModItems` 等对象
