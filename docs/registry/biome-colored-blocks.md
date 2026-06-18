# 创建带有生物群系颜色的方块

如果需要创建一个根据生物群系改变颜色的方块（类似草方块），需要完成以下步骤。

## 1. 创建方块类（使用注解注册）

> ⚠️ **历史变更 — 预烤纹理替代固定色 tint**：
> 下列方块原本用「灰度纹理 + 运行时固定色 tint + ColorProvider」，现已改为**离线预烤纹理**（模型不带 `tintindex`，删除 ColorProvider）。颜色固定、无运行时开销，但**失去动态着色能力**（如随生物群系变色）。
> - **橡胶树叶** `rubber_leaves`：`scripts/generate_rubber_leaves.py` 把 `灰度 × 0xc4b848` 烤进 `assets/ic2_120/textures/block/rubber_leaves.png`
> - **泥炭矿** `peat_ore`：`scripts/generate_peat_ore.py` 把 `锡矿石 × 0x4A3728` 烤进 `assets/ic2_120/textures/block/resource/ore/peat_ore.png`
>
> **何时仍需运行时 tint（不能用预烤替代）**：颜色需要**动态**变化时——例如随生物群系变色（草/树叶）、随物品 NBT 变色（多材质管道）。本仓库当前唯一保留的 tint 案例是 `PipeColorProvider.kt`（青铜/碳纤维管道按方块 ID 分支上色）。下面的范式适用于这类动态场景。

本项目使用**类级别注解自动注册系统**，创建方块非常简单：

```kotlin
package ic2_120.content.block

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import net.minecraft.block.Block
import net.minecraft.block.AbstractBlock.Settings
import net.minecraft.block.Blocks

@ModBlock(
    name = "bronze_pipe_medium",
    registerItem = true,
    tab = CreativeTab.IC2_MATERIALS,
    group = "resource"
)
class BronzePipeMediumBlock : Block(
    AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)
        .strength(3.0f)
)
```

**说明**：
- `@ModBlock` 注解会自动注册方块和物品
- `Settings.copy(...)` 复制原版方块的属性
- 使用 `CreativeTab` 枚举而非字符串
- 使用注册表获取方块和物品实例，而非类引用
- 项目当前仍在用 tint 的实现参考：`PipeColorProvider.kt`（按方块 ID 分支上色：青铜 `0x854200` / 碳纤维 `0x191919`）

## 2. 注册颜色提供器（客户端）

在客户端入口点注册 `BlockColorProvider`。下面是按方块 ID 分支上色的范式（完整实现见 `PipeColorProvider.kt`）：

```kotlin
package ic2_120.client.colorprovider

import ic2_120.Ic2_120
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry
import net.minecraft.client.color.block.BlockColorProvider
import net.minecraft.client.color.item.ItemColorProvider
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

object PipeColorProvider {
    private const val BRONZE_COLOR = 0x854200
    private const val CARBON_COLOR = 0x191919

    fun register() {
        // 方块提供器：按 blockId 分支返回颜色
        val blockProvider = BlockColorProvider { state, _, _, tintIndex ->
            if (tintIndex != 0) return@BlockColorProvider 0xFFFFFF
            val path = Registries.BLOCK.getId(state.block).path
            when {
                path.startsWith("bronze_pipe") -> BRONZE_COLOR
                path.startsWith("carbon_pipe") -> CARBON_COLOR
                else -> 0xFFFFFF
            }
        }
        // 物品提供器：同上逻辑（手持/物品栏/掉落物）
        val itemProvider = ItemColorProvider { stack, tintIndex ->
            if (tintIndex != 0) return@ItemColorProvider 0xFFFFFF
            val path = Registries.ITEM.getId(stack.item).path
            when {
                path.startsWith("bronze_pipe") -> BRONZE_COLOR
                path.startsWith("carbon_pipe") -> CARBON_COLOR
                else -> 0xFFFFFF
            }
        }

        val id = Identifier(Ic2_120.MOD_ID, "bronze_pipe_medium")
        val block = Registries.BLOCK.get(id)
        val item = Registries.ITEM.get(id)
        ColorProviderRegistry.BLOCK.register(blockProvider, block)
        ColorProviderRegistry.ITEM.register(itemProvider, item)
    }
}
```

> 若需要随生物群系动态变色（草方块/树叶风格），把 `blockProvider` lambda 里的返回值换成 `BiomeColors.getFoliageColor(world, pos)` / `getGrassColor(world, pos)` 即可（注意 `world`/`pos` 在物品栏/世界外可能为 null，需判空回退到默认色）。

**在客户端入口调用**：

```kotlin
// src/client/kotlin/ic2_120/Ic2_120Client.kt
override fun onInitializeClient() {
    PipeColorProvider.register()
    // ... 其他客户端注册
}
```

## 3. 创建方块模型（关键）

**重要**：模型中必须指定 `tintindex`，否则颜色提供器不会生效：

**models/block/rubber_leaves.json**:
```json
{
  "parent": "minecraft:block/cube_bottom_top",
  "textures": {
    "bottom": "minecraft:block/dirt",
    "top": "minecraft:block/grass_block_top",
    "side": "minecraft:block/grass_block_side"
  },
  "elements": [
    {
      "from": [0, 0, 0],
      "to": [16, 16, 16],
      "faces": {
        "down":  {"texture": "#bottom", "cullface": "down"},
        "up":    {"texture": "#top", "cullface": "up", "tintindex": 0},
        "north": {"texture": "#side", "cullface": "north"},
        "south": {"texture": "#side", "cullface": "south"},
        "west":  {"texture": "#side", "cullface": "west"},
        "east":  {"texture": "#side", "cullface": "east"}
      }
    }
  ]
}
```

**关键点**：`"tintindex": 0` 告诉渲染引擎这个面需要应用颜色着色。

## 4. 创建方块状态和物品模型

**blockstates/rubber_leaves.json**:
```json
{
  "variants": {
    "": {
      "model": "ic2_120:block/rubber_leaves"
    }
  }
}
```

**models/item/rubber_leaves.json**:
```json
{
  "parent": "ic2_120:block/example_block"
}
```

## 常见问题

### 方块显示黑白色
检查模型中是否添加了 `tintindex`。这是最常见的问题。

### 颜色不随群系变化
确认客户端代码中正确注册了 `ColorProviderRegistry.BLOCK`。

### 纹理问题
使用原版纹理时，确保纹理路径正确（如 `minecraft:block/grass_block_top`）。

## 其他颜色类型

除了草颜色，还可以使用其他生物群系颜色：

- **树叶颜色**：`BiomeColors.getFoliageColor()`
- **水颜色**：`BiomeColors.getWaterColor()`
- **自定义颜色**：根据条件返回任何 RGB 颜色值（格式：`0xRRGGBB`）

## 为什么草方块纹理需要颜色提供器

- **草方块纹理**（`grass_block_top`）使用了 **tint index（着色索引）**
- 纹理本身是灰度的
- 需要通过 `BlockColorProvider` 动态上色
- 颜色根据**生物群系**变化（比如沙漠是黄色，森林是绿色）

**苔藓块纹理**（`moss_block`）没有着色索引，直接使用原始颜色，不需要颜色提供器。
