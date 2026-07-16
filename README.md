# IC2-120

<big>[English](README_EN.md) | 简体中文</big>

> 📢 Q群交流：638382077（反馈bug、讨论开发进度、与作者交流等）

> **⚠️ 必读：安装说明** 使用本模组需要同时复制 release 中的**两个文件**到 Minecraft 的 mods 文件夹：
> - `ic2_120-*.jar` - 模组本体（已内置 Energy API）
> - `fabric-language-kotlin-*.jar` - Fabric Kotlin 语言支持库
> 两个文件缺一不可，否则模组无法正常运行。

> **📢 Sinytra Connector(信雅互联) 支持**：本模组已针对 [Sinytra Connector](https://github.com/Sinytra/connector) 进行适配，Forge 端也可通过 Connector 运行。关于如何在 Forge 环境中使用 Connector 加载 Fabric Mod，请参阅 [Sinytra Connector 官方文档](https://github.com/Sinytra/connector)。

IndustrialCraft 2 Experimental 的 Minecraft 1.20.1 Fabric 移植版本，使用 Kotlin 编写。

## 安装统计

下图展示近 30 天每日运行本模组的独立安装数（匿名统计，仅计数不含任何个人信息，可在配置中关闭）：

![每日安装人数](https://ic2-analytics.wangyu174551226.workers.dev/chart.svg?days=30)

## 文档分区

### 玩家文档（游戏内 Guidebook）

玩家向文档统一维护在游戏内任务书/Guidebook：

- `core/src/main/resources/assets/ic2_120/guidebook/main/index.md`
- `core/src/main/resources/assets/ic2_120/guidebook/main/i18n/zh_cn/index.md`

### 开发者文档（docs/）

本文档其余内容与 `docs/` 目录面向开发者协作与实现细节。

## 项目特性

- 📦 基于 Fabric Loader 和 Fabric API
- 🔧 使用 Kotlin 2.3.10 开发
- ⚡ 类级别注解注册系统，简化模组开发
- 🎨 内嵌自研 ComposeUI 声明式 GUI 系统
- 🔌 EU 能量网络系统
- 🏭 完整的工业机器套件（发电机、加工机器、储能设备等）

## 开发环境要求

- JDK 17 或更高版本
- Minecraft 1.20.1
- Fabric Loader
- Fabric API

## 技术栈

- **Kotlin** 2.3.10 - 主要开发语言
- **Fabric Loom** - Gradle 构建插件
- **Fabric API** - Minecraft 模组 API
- **ComposeUI** - 内嵌自研声明式 GUI DSL（基于 DrawContext，非 JetBrains Compose）
- **[mcdebug](https://github.com/yu1745/mcdebug)** - 游戏内方块/机器自动化测试工具（拉起 dev 服务端跑 TS 测试）

## 项目结构

多模块 Gradle 项目，`core` 为本体，其余为附属模组：

```
ic2-fabric/
├── core/                    # 模组本体（IC2 主内容、能量/流体/动能网络、机器、GUI）
│   └── src/{main,client}/   #   main=通用/服务端, client=客户端（kotlin + java）
├── advanced-solar-addon/    # 高级太阳能附属
├── advanced-weapons-addon/  # 高级武器附属
├── buildcraft-addon/        # BuildCraft 联动（引擎/液泵/石油世界生成）
├── addon-template/          # 附属模组模板（不参与构建）
├── docs/                    # 技术文档
├── libs/                    # 本地依赖
├── scripts/                 # 辅助脚本
└── tests/                   # 测试（含 mcdebug 机器/方块测试）
```

## 开发者文档入口

> 文档主索引的单一来源是 [`AGENTS.md`](AGENTS.md)（= `CLAUDE.md`，§1/§5/§6 含 guides/systems/ui/registry/pitfalls 全量索引与硬性约束）。下面只列人类起步常用入口：

- [机器实现指南](docs/guides/machine-implementation-guide.md) - 完整的 Block → BlockEntity → ScreenHandler → Screen 实现流程
- [类级别注解注册系统](docs/registry/CLASS_BASED_REGISTRY.md) - 使用注解和枚举的自动注册系统
- [机器组合复用](docs/guides/machine-composition-reuse.md) - 机器逻辑组合与可复用设计
- [ComposeUI 声明式 GUI](docs/ui/compose-ui.md) - GUI 布局与绘制系统
- [能量网络系统](docs/systems/energy-network.md) - EU 能量传输与存储
- [流体系统](docs/systems/fluid-system.md) - 流体管道、泵附件与传输规则
- [已实现物品清单](docs/guides/item-implemented.md) - 当前已落地的物品列表

## 暂不实现的功能

以下功能明确不在本移植的计划内，建议搭配对应模组使用：

### 🏔️ 地形改造系列（建议使用 Create）
- 地形转换机（Terraformer）及各类模板（耕地、森林、沙漠、蘑菇等）、建筑模板

### 📦 物流系列（建议使用 AE2）
- 物品缓冲器、高级物品分配器、电动分拣机、物流管道与过滤器

> 蒸汽系列、高炉、动能发电（风/水/手摇/拴绳动能发生机）等均已实现，玩法与配方详见游戏内 Guidebook。

## 贡献

欢迎提交 Issue 和 Pull Request！

## 版权与许可证

**⚠️ 重要声明**

本项目是基于 IndustrialCraft 2（IC2）的逆向工程项目。IC2 原版模组**并非开源软件**，其源代码和资源未经官方授权公开使用。

本仓库中 `core/src/main/resources/assets/ic2` 与 `core/src/main/resources/assets/minecraft` 目录下的资产（包括但不限于模型、纹理、语言文件、配方与相关数据）均为通过逆向分析方式整理得到，仅用于兼容性研究与技术验证，不代表获得任何 IC2 原项目、Minecraft 原项目或相关权利人的授权。

本项目仅供**学习和研究目的**，不得用于商业用途。如果您是 IC2 的版权持有者并认为本项目侵犯了您的权益，请联系我们进行处理。

除法律另有强制性规定外，本仓库作者与贡献者不对使用、分发、修改或二次发布本项目所产生的任何直接或间接法律后果承担责任；使用者应自行确认其行为在所在司法辖区内的合法性并自行承担全部风险。

## 相关链接

- [Fabric 官方文档](https://fabricmc.net/wiki/)
- [Fabric API GitHub](https://github.com/FabricMC/fabric)
- [Minecraft 1.20.1 版本](https://www.minecraft.net/)

## 版本与依赖

- 游戏版本：Minecraft `1.20.1`
- 加载器：Fabric Loader `0.18.4`
- 依赖：Fabric API、Fabric Language Kotlin `1.13.9+kotlin.2.3.10`（Energy API 已内置）
- 模组版本：`0.3`
- 兼容：Sinytra Connector（可在 Forge + Connector 环境运行）
