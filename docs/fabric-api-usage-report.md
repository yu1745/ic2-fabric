# Fabric API 使用统计报告

## 概述

本文档统计了 IC2-120 项目中 Kotlin 代码所使用的 Fabric API 引用情况。

**统计日期**: 2026-04-01

**总计**: 58 种不同的 Fabric API（按完整导入路径统计）

---

## 按功能模块分类

| 模块 | API 数量 | 主要用途 |
|------|----------|----------|
| **Transfer API** | 14 | 物品和流体存储系统 |
| **Datagen API** | 7 | 数据生成（配方、标签、战利品表） |
| **Client API** | 12 | 客户端渲染、事件处理、UI |
| **Event API** | 6 | 游戏事件监听和处理 |
| **Networking API** | 3 | 网络数据包通信 |
| **ScreenHandler API** | 2 | GUI 屏幕处理器扩展 |
| **Renderer API** | 3 | 自定义渲染模型 |
| **ItemGroup API** | 2 | 创造模式物品栏 |
| **Item API** | 1 | 物品设置 |
| **Biome API** | 2 | 生物群系修改 |
| **Command API** | 1 | 命令注册 |
| **Registry API** | 1 | 燃料注册表 |
| **Loot API** | 1 | 战利品表事件 |
| **BlockRenderLayer API** | 1 | 渲染层映射 |
| **Object Builder API** | 1 | BlockEntity 类型构建 |
| **Core API** | 3 | Mod 初始化和环境注解 |
| **Loader API** | 1 | Fabric Loader 访问 |

---

## 详细 API 列表

### 1. Transfer API (14 种)
用于物品和流体存储的核心 API，是 IC2 电力系统和流体系统的基础。

| API 类 | 使用次数 |
|--------|----------|
| `net.fabricmc.fabric.api.transfer.v1.storage.Storage` | 49 |
| `net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext` | 48 |
| `net.fabricmc.fabric.api.transfer.v1.storage.StorageView` | 44 |
| `net.fabricmc.fabric.api.transfer.v1.item.ItemVariant` | 44 |
| `net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage` | 39 |
| `net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants` | 30 |
| `net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant` | 25 |
| `net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage` | 18 |
| `net.fabricmc.fabric.api.transfer.v1.transaction.Transaction` | 14 |
| `net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil` | 11 |
| `net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext` | 11 |
| `net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions` | 10 |
| `net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage` | 1 |
| `net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant` | 4 |
| `net.fabricmc.fabric.api.transfer.v1.item.ItemStorage` | 1 |
| `net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes` | 1 |

### 2. Datagen API (7 种)
用于自动生成配方、标签和战利品表。

| API 类 | 使用次数 |
|--------|----------|
| `net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem` | 85 |
| `net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem` | 85 |
| `net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider` | 3 |
| `net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider` | 2 |
| `net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider` | 1 |
| `net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator` | 1 |
| `net.fabricmc.fabric.api.datagen.v1.FabricDataOutput` | 4 |
| `net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint` | 1 |

### 3. Client API (12 种)
客户端专用功能，包括渲染、事件、输入等。

| API 类 | 使用次数 |
|--------|----------|
| `net.fabricmc.api.Environment` | 14 |
| `net.fabricmc.api.EnvType` | 14 |
| `net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents` | 5 |
| `net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry` | 4 |
| `net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking` | 4 |
| `net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback` | 4 |
| `net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper` | 3 |
| `net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry` | 3 |
| `net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry` | 1 |
| `net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback` | 1 |
| `net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler` | 1 |
| `net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents` | 2 |
| `net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin` | 1 |
| `net.fabricmc.fabric.api.client.model.loading.v1.DelegatingUnbakedModel` | 1 |
| `net.fabricmc.fabric.api.client.model.loading.v1.BlockStateResolver` | 1 |
| `net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap` | 2 |

### 4. Event API (6 种)
游戏生命周期和玩家事件。

| API 类 | 使用次数 |
|--------|----------|
| `net.fabricmc.fabric.api.event.player.UseBlockCallback` | 3 |
| `net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents` | 1 |
| `net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents` | 1 |
| `net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents` | 1 |
| `net.fabricmc.fabric.api.event.player.UseItemCallback` | 1 |
| `net.fabricmc.fabric.api.event.player.AttackBlockCallback` | 1 |

### 5. Networking API (3 种)
服务端和客户端之间的网络通信。

| API 类 | 使用次数 |
|--------|----------|
| `net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking` | 3 |
| `net.fabricmc.fabric.api.networking.v1.PacketSender` | 1 |

### 6. ScreenHandler API (2 种)
扩展 GUI 屏幕处理器功能。

| API 类 | 使用次数 |
|--------|----------|
| `net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory` | 45 |
| `net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType` | 1 |

### 7. Renderer API (3 种)
自定义模型和渲染。

| API 类 | 使用次数 |
|--------|----------|
| `net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel` | 1 |
| `net.fabricmc.fabric.api.renderer.v1.render.RenderContext` | 1 |
| `net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView` | 1 |

### 8. ItemGroup API (2 种)
创造模式物品栏组织。

| API 类 | 使用次数 |
|--------|----------|
| `net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup` | 1 |
| `net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents` | 3 |

### 9. Item API (1 种)
| API 类 | 使用次数 |
|--------|----------|
| `net.fabricmc.fabric.api.item.v1.FabricItemSettings` | 49 |

### 10. Biome API (2 种)
生物群系修改和世界生成。

| API 类 | 使用次数 |
|--------|----------|
| `net.fabricmc.fabric.api.biome.v1.BiomeSelectors` | 2 |
| `net.fabricmc.fabric.api.biome.v1.BiomeModifications` | 2 |

### 11. Command API (1 种)
| API 类 | 使用次数 |
|--------|----------|
| `net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback` | 2 |

### 12. Registry API (1 种)
| API 类 | 使用次数 |
|--------|----------|
| `net.fabricmc.fabric.api.registry.FuelRegistry` | 7 |

### 13. Loot API (1 种)
| API 类 | 使用次数 |
|--------|----------|
| `net.fabricmc.fabric.api.loot.v2.LootTableEvents` | 1 |

### 14. Object Builder API (1 种)
| API 类 | 使用次数 |
|--------|----------|
| `net.fabricmc.fabric.api.\`object\`.builder.v1.block.entity.FabricBlockEntityTypeBuilder` | 3 |

### 15. Core API (3 种)
Mod 初始化和环境标记。

| API 类 | 使用次数 |
|--------|----------|
| `net.fabricmc.api.ModInitializer` | 1 |
| `net.fabricmc.api.ClientModInitializer` | 1 |

### 16. Loader API (1 种)
访问 Fabric Loader。

| API 类 | 使用次数 |
|--------|----------|
| `net.fabricmc.loader.api.FabricLoader` | 3 |

---

## 使用频率 Top 10

| 排名 | API | 使用次数 | 用途 |
|------|-----|----------|------|
| 1 | `FabricRecipeProvider.hasItem/conditionsFromItem` | 85 | 配方条件生成 |
| 2 | `Storage` | 49 | 存储接口 |
| 3 | `FabricItemSettings` | 49 | 物品配置 |
| 4 | `TransactionContext` | 48 | 事务上下文 |
| 5 | `ExtendedScreenHandlerFactory` | 45 | GUI 工厂 |
| 6 | `StorageView` | 44 | 存储视图 |
| 7 | `ItemVariant` | 44 | 物品变体 |
| 8 | `FluidStorage` | 39 | 流体存储 |
| 9 | `FluidConstants` | 30 | 流体常量 |
| 10 | `FluidVariant` | 25 | 流体变体 |

---

## 依赖分析

### 核心依赖
以下 API 是项目运行的基础，不可替代：

1. **Transfer API** - Team Reborn Energy 的替代，IC2 电力系统的核心
2. **ScreenHandler API** - 所有机器 GUI 的基础
3. **Event API** - 机器同步和网络通信

### 可选依赖
以下 API 可考虑用原版 API 替代：

1. **Renderer API** - 用于电池等物品的特殊渲染，可用 Atlas 替代
2. **Biome API** - 世界生成，可用原版 BiomeModifier 替代

---

## 版本信息

项目配置的 Fabric API 版本见 `gradle.properties`：

```properties
fabric_api_version=0.90.0+1.20.1
```

---

## 生成方法

本报告通过以下命令生成：

```bash
find src -name "*.kt" -exec grep -h "^import" {} \; | grep -i fabric | sort -u
```

生成日期: 2026-04-01
