# IC2-120

<big>[English](README_EN.md) | 简体中文</big>

> 📢 Q群交流：638382077（反馈bug、讨论开发进度、与作者交流等）

> **⚠️ 必读：安装说明** 使用本模组需要同时复制 release 中的**三个文件**到 Minecraft 的 mods 文件夹：
> - `ic2_120-*.jar` - 模组本体
> - `energy-*.jar` - Energy API 运行时依赖
> - `fabric-language-kotlin-*.jar` - Fabric Kotlin 语言支持库
> 三个文件缺一不可，否则模组无法正常运行。

> **📢 Sinytra Connector(信雅互联) 支持**：本模组已针对 [Sinytra Connector](https://github.com/Sinytra/connector) 进行适配，Forge 端也可通过 Connector 运行。关于如何在 Forge 环境中使用 Connector 加载 Fabric Mod，请参阅 [Sinytra Connector 官方文档](https://github.com/Sinytra/connector)。

IndustrialCraft 2 Experimental 的 Minecraft 1.20.1 Fabric 移植版本，使用 Kotlin 编写。

**玩家向说明**：游戏内容与玩法请查看 [mod特性.md](mod特性.md)。本文档面向开发者。

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

完整目录与分类见 [docs/README.md](docs/README.md)。常用入口：

- [类级别注解注册系统](docs/registry/CLASS_BASED_REGISTRY.md) - 使用注解和枚举的自动注册系统
- [同步系统](docs/systems/sync-system.md) - 客户端/服务端属性同步
- [能量流同步](docs/systems/energy-flow-sync.md) - 机器间能量流动与同步逻辑
- [能量网络系统](docs/systems/energy-network.md) - EU 能量传输与存储
- [升级系统](docs/systems/upgrade-system.md) - 机器升级与效果机制
- [槽位规格系统](docs/ui/slot-spec-system.md) - 机器 GUI 槽位定义与约束
- [机器组合复用](docs/guides/machine-composition-reuse.md) - 机器逻辑组合与可复用设计
- [机器实现指南](docs/guides/machine-implementation-guide.md) - 完整的 Block → BlockEntity → ScreenHandler → Screen 实现流程
- [核电系统](docs/systems/nuclear-power.md) - 核能相关机制与实现说明
- [热能系统](docs/systems/heat-system.md) - HU 加热与传热机制
- [流体系统](docs/systems/fluid-system.md) - 流体管道、泵附件与传输规则
- [传动轴系统](docs/archive/transmission_shaft.md) - 机械传动轴与锥齿轮
- [已实现物品清单](docs/guides/item-implemented.md) - 当前已落地的物品列表
- [ComposeUI 声明式 GUI](docs/ui/compose-ui.md) - GUI 布局与绘制系统
- [DrawContext 绘制方法参考](docs/ui/drawcontext-methods.md) - 绘制 API 说明
- [Assets 清单](docs/inventory/assets-inventory.md) - 模组方块/物品资源清单
- [生物群系颜色方块](docs/registry/biome-colored-blocks.md) - 实现随生物群系变色的方块
- [方块变体系统](docs/registry/block-variants.md) - 方块状态与模型变体
- [唯一礼物物品防复制 TODO](docs/archive/unique-gift-item-anti-dup-todo.md) - 防复制方案草案与待办

## 相比原版未实现部分

本移植版本相比原版 IC2，以下功能尚未实现或正在开发中：

### 🔧 动能发电系统（开发中）
- **状态**：基础框架已完成，机械传动逻辑待完善
- **已完成**：
  - 传动轴方块（木制、铁制、钢制、碳纤维）
  - 伞齿轮方块（90度转向）
  - 视觉渲染（BER）
- **待实现**：
  - 机械动能传播系统（速度/扭矩计算）
  - 发电机动能转电能逻辑
  - 相关合成配方

### 🏔️ 地形改造系列机器
- 地形转换机（Terraformer）
- 各种地形改造模板（耕地、森林、沙漠、蘑菇等）
- 建筑模板

### 💨 蒸汽系列机器
- 蒸汽发电机（Steam Generator）
- 蒸汽动能发电机（Steam Kinetic Generator）
- 蒸汽再压机（Steam Repressurizer）
- 蒸汽相关流体与配方

### 📦 物流系列机器
- 物品缓冲器（Item Buffer）
- 高级物品分配器（Weighted Item Distributor）
- 电动分拣机（Sorting Machine）
- 物流管道与过滤器

### 🔥 焦炉多方块结构
- 焦炉（Blast Furnace）多方块结构
- 耐火砖方块
- 高温冶炼逻辑
- 钢铁生产系统

> **注意**：以上功能会逐步实现，具体进度可查看项目 Issue 和 Pull Request。

## 贡献

欢迎提交 Issue 和 Pull Request！

## 版权与许可证

**⚠️ 重要声明**

本项目是基于 IndustrialCraft 2（IC2）的逆向工程项目。IC2 原版模组**并非开源软件**，其源代码和资源未经官方授权公开使用。

本仓库中 `src/main/resources/assets/ic2` 与 `src/main/resources/assets/minecraft` 目录下的资产（包括但不限于模型、纹理、语言文件、配方与相关数据）均为通过逆向分析方式整理得到，仅用于兼容性研究与技术验证，不代表获得任何 IC2 原项目、Minecraft 原项目或相关权利人的授权。

本项目仅供**学习和研究目的**，不得用于商业用途。如果您是 IC2 的版权持有者并认为本项目侵犯了您的权益，请联系我们进行处理。

除法律另有强制性规定外，本仓库作者与贡献者不对使用、分发、修改或二次发布本项目所产生的任何直接或间接法律后果承担责任；使用者应自行确认其行为在所在司法辖区内的合法性并自行承担全部风险。

## 相关链接

- [Fabric 官方文档](https://fabricmc.net/wiki/)
- [Fabric API GitHub](https://github.com/FabricMC/fabric)
- [Minecraft 1.20.1 版本](https://www.minecraft.net/)
