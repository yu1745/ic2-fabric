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

- **构建模组**: `./gradlew build` (不要cd,直接执行)
- **清理构建**: `./gradlew clean build`
- **运行客户端**: `./gradlew runClient` - 启动加载了该模组的 Minecraft 进行测试
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

- **[创建带有生物群系颜色的方块](docs/biome-colored-blocks.md)** - 如何实现根据生物群系改变颜色的方块（如草方块）
