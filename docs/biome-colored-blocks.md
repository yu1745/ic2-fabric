# 创建带有生物群系颜色的方块

如果需要创建一个根据生物群系改变颜色的方块（类似草方块），需要完成以下步骤。

## 1. 创建方块类

使用 `AbstractBlock.Settings.create()` 创建方块：

```kotlin
package ic2_120.block

import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block

class ExampleBlock : Block(AbstractBlock.Settings.create().strength(4.0f))
```

## 2. 注册方块和物品

在主入口点注册方块并添加到创造标签：

```kotlin
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.item.ItemGroups

// 注册方块
val EXAMPLE_BLOCK = ExampleBlock()
Registry.register(Registries.BLOCK, Identifier(MODID, "example_block"), EXAMPLE_BLOCK)

// 注册物品
val blockItem = Registry.register(Registries.ITEM, Identifier(MODID, "example_block"),
    BlockItem(EXAMPLE_BLOCK, FabricItemSettings()))

// 添加到创造标签
ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register { content ->
    content.add(blockItem)
}
```

## 3. 创建方块模型（关键）

**重要**：模型中必须指定 `tintindex`，否则颜色提供器不会生效：

**models/block/example_block.json**:
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

## 4. 注册颜色提供器（客户端）

在客户端入口点注册 `BlockColorProvider`：

**src/client/kotlin/ic2_120/Ic2_120Client.kt**:
```kotlin
package ic2_120

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry
import net.minecraft.client.color.world.BiomeColors

object Ic2_120Client : ClientModInitializer {
    override fun onInitializeClient() {
        // 注册草方块颜色提供器
        ColorProviderRegistry.BLOCK.register({ state, world, pos, tintIndex ->
            if (world != null && pos != null) {
                BiomeColors.getGrassColor(world, pos)
            } else {
                // 默认草颜色（用于物品渲染）
                0x91bd59
            }
        }, Ic2_120.EXAMPLE_BLOCK)
    }
}
```

## 5. 创建方块状态和物品模型

**blockstates/example_block.json**:
```json
{
  "variants": {
    "": {
      "model": "ic2_120:block/example_block"
    }
  }
}
```

**models/item/example_block.json**:
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
