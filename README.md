# IC2-120

IndustrialCraft 2 的 Minecraft 1.20.1 Fabric 移植版本，使用 Kotlin 编写。

## 项目特性

- 📦 基于 Fabric Loader 和 Fabric API
- 🔧 使用 Kotlin 2.3.10 开发
- ⚡ 类级别注解注册系统，简化模组开发
- 🎨 自定义 ComposeUI 声明式 GUI 系统
- 🔌 EU 能量网络系统
- 🏭 完整的工业机器套件（发电机、加工机器、储能设备等）

## 开发环境要求

- JDK 17 或更高版本
- Minecraft 1.20.1
- Fabric Loader
- Fabric API

## 构建命令

**重要**: 所有 Gradle 命令不要添加 `--no-daemon` 参数，以利用 Gradle Daemon 加快构建速度。

```bash
# 构建模组
./gradlew build

# 清理构建
./gradlew clean build

# 运行客户端
./gradlew runClient

# Windows 中文乱码修复: 使用 runClient.bat（会先设置控制台为 UTF-8 再启动客户端）
./runClient.bat

# 运行服务端
./gradlew runServer

# 生成源码
./gradlew genSources

# 生成数据
./gradlew runDatagen
```

## 技术栈

- **Kotlin** 2.3.10 - 主要开发语言
- **Fabric Loom** - Gradle 构建插件
- **Fabric API** - Minecraft 模组 API
- **ComposeUI** - 自定义声明式 GUI 系统

## 项目结构

```
ic2-fabric/
├── src/
│   ├── main/kotlin/     # 通用/服务端代码
│   ├── client/kotlin/   # 客户端专用代码
│   ├── main/java/       # 通用/服务端 Mixin 类
│   └── client/java/     # 客户端 Mixin 类
├── docs/                # 技术文档
└── assets/              # 模组资源（模型、纹理、语言文件等）
```

## 文档

详细的技术文档请查看 [docs/](docs/) 目录：

- [类级别注解注册系统](docs/class_based_registry.md) - 使用注解和枚举的自动注册系统
- [同步系统](docs/sync-system.md) - 客户端/服务端属性同步
- [ComposeUI 声明式 GUI](docs/compose-ui.md) - GUI 布局与绘制系统
- [DrawContext 绘制方法参考](docs/drawcontext-methods.md) - 绘制 API 说明
- [Assets 清单](docs/assets-inventory.md) - 模组方块/物品资源清单
- [生物群系颜色方块](docs/biome-colored-blocks.md) - 实现随生物群系变色的方块
- [方块变体系统](docs/block-variants.md) - 方块状态与模型变体
- [能量网络系统](docs/energy-network.md) - EU 能量传输与存储

## 贡献

欢迎提交 Issue 和 Pull Request！

## 版权与许可证

**⚠️ 重要声明**

本项目是基于 IndustrialCraft 2（IC2）的逆向工程项目。IC2 原版模组**并非开源软件**，其源代码和资源未经官方授权公开使用。

本项目仅供**学习和研究目的**，不得用于商业用途。如果您是 IC2 的版权持有者并认为本项目侵犯了您的权益，请联系我们进行处理。

使用本项目可能存在的法律风险由使用者自行承担。

## 相关链接

- [Fabric 官方文档](https://fabricmc.net/wiki/)
- [Fabric API GitHub](https://github.com/FabricMC/fabric)
- [Minecraft 1.20.1 版本](https://www.minecraft.net/)
