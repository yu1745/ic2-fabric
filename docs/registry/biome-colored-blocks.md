# 创建带有生物群系颜色的方块

如果需要创建一个根据生物群系改变颜色的方块（类似草方块），需要完成以下步骤。

## 1. 创建方块类（使用注解注册）

本项目使用**类级别注解自动注册系统**，创建方块非常简单：

```kotlin
package ic2_120.content.block

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import net.minecraft.block.Block
import net.minecraft.block.AbstractBlock.Settings
import net.minecraft.block.Blocks

@ModBlock(
    name = "rubber_leaves",
    registerItem = true,
    tab = CreativeTab.IC2_MATERIALS,
    group = "wood",
    transparent = false
)
class RubberLeavesBlock : Block(
    AbstractBlock.Settings.copy(Blocks.OAK_LEAVES)
        .strength(0.2f)
        .nonOpaque()
        .allowsSpawning { _, _, _, _ -> true }
)
```

**说明**：
- `@ModBlock` 注解会自动注册方块和物品
- `Settings.copy(Blocks.OAK_LEAVES)` 复制原版方块的属性
- 使用 `CreativeTab` 枚举而非字符串
- 使用注册表获取方块和物品实例，而非类引用
- 项目已有实现参考：`RubberLeavesBlock.kt` 和 `RubberLeavesColorProvider.kt`

## 2. 注册颜色提供器（客户端）

在客户端入口点注册 `BlockColorProvider`：

**文件位置**：`src/client/kotlin/ic2_120/client/colorprovider/RubberLeavesColorProvider.kt`

```kotlin
package ic2_120.client.colorprovider

import ic2_120.Ic2_120
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry
import net.minecraft.client.color.world.BiomeColors
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

object RubberLeavesColorProvider {
    private const val DEFAULT_RUBBER_LEAVES_COLOR = 0xc4b848  // 金黄绿色

    fun register() {
        val block = Registries.BLOCK.get(Identifier(Ic2_120.MOD_ID, "rubber_leaves"))
        val item = Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "rubber_leaves"))

        // 方块颜色提供器
        ColorProviderRegistry.BLOCK.register({ _, world, pos, _ ->
            if (world != null && pos != null) {
                val baseBrightness = BiomeColors.getFoliageColor(world, pos)
                applyBiomeBrightness(DEFAULT_RUBBER_LEAVES_COLOR, baseBrightness)
            } else {
                DEFAULT_RUBBER_LEAVES_COLOR
            }
        }, block)

        // 物品颜色提供器
        ColorProviderRegistry.ITEM.register({ _, _ ->
            DEFAULT_RUBBER_LEAVES_COLOR
        }, item)
    }

    private fun applyBiomeBrightness(baseColor: Int, targetBrightness: Int): Int {
        val targetRGB = targetBrightness and 0xFFFFFF
        val currentRGB = baseColor and 0xFFFFFF

        val currentBrightness = getLuminance(currentRGB)
        val targetLuminance = getLuminance(targetRGB)

        return when {
            currentBrightness == 0 -> baseColor
            else -> {
                val r = ((currentRGB ushr 16) * targetLuminance / currentBrightness).coerceAtMost(255)
                val g = ((currentRGB ushr 8 and 0xFF) * targetLuminance / currentBrightness).coerceAtMost(255)
                val b = ((currentRGB and 0xFF) * targetLuminance / currentBrightness).coerceAtMost(255)
                (r shl 16) or (g shl 8) or b
            }
        }
    }

    private fun getLuminance(rgb: Int): Int {
        val r = (rgb ushr 16) and 0xFF
        val g = (rgb ushr 8) and 0xFF
        val b = rgb and 0xFF
        return (r * 299 + g * 587 + b * 114) / 1000
    }
}
```

**在客户端入口调用**：

```kotlin
// src/client/kotlin/ic2_120/Ic2_120Client.kt
override fun onInitializeClient() {
    RubberLeavesColorProvider.register()
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
