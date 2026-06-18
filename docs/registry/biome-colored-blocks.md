# 创建带有生物群系颜色的方块

如果需要创建一个根据生物群系改变颜色的方块（类似草方块），需要完成以下步骤。

## 1. 创建方块类（使用注解注册）

> ⚠️ **历史变更**：橡胶树叶（`rubber_leaves`）曾经是本机制的代表范例（灰度 `oak_leaves` 纹理 + 运行时金黄绿 tint）。
> 现已改为**预烤纹理**：用 `scripts/generate_rubber_leaves.py` 离线把 `灰度 × tintColor(0xc4b848)` 烤进 `assets/ic2_120/textures/block/rubber_leaves.png`，模型不带 `tintindex`，不再注册 ColorProvider。
> 如需创建随生物群系动态变色的方块，继续参考下面的范式；当前仍在用 tint 的范例见 `PeatOreColorProvider.kt`、`PipeColorProvider.kt`。

本项目使用**类级别注解自动注册系统**，创建方块非常简单：

```kotlin
package ic2_120.content.block

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import net.minecraft.block.Block
import net.minecraft.block.AbstractBlock.Settings
import net.minecraft.block.Blocks

@ModBlock(
    name = "peat_ore",
    registerItem = true,
    tab = CreativeTab.IC2_MATERIALS,
    group = "resource"
)
class PeatOreBlock : Block(
    AbstractBlock.Settings.copy(Blocks.STONE)
        .strength(3.0f)
)
```

**说明**：
- `@ModBlock` 注解会自动注册方块和物品
- `Settings.copy(Blocks.STONE)` 复制原版方块的属性
- 使用 `CreativeTab` 枚举而非字符串
- 使用注册表获取方块和物品实例，而非类引用
- 项目已有实现参考：`PeatOreColorProvider.kt`（固定色 tint）、`PipeColorProvider.kt`（按 NBT 动态色 tint）

## 2. 注册颜色提供器（客户端）

在客户端入口点注册 `BlockColorProvider`：

**文件位置**：`src/client/kotlin/ic2_120/client/colorprovider/PeatOreColorProvider.kt`

```kotlin
package ic2_120.client.colorprovider

import ic2_120.Ic2_120
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

object PeatOreColorProvider {
    /** 深褐色，用于将锡矿石纹理染成泥炭色 */
    private const val PEAT_COLOR = 0x4A3728

    fun register() {
        val block = Registries.BLOCK.get(Identifier(Ic2_120.MOD_ID, "peat_ore"))
        val item = Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "peat_ore"))

        ColorProviderRegistry.BLOCK.register({ _, _, _, _ -> PEAT_COLOR }, block)
        ColorProviderRegistry.ITEM.register({ _, _ -> PEAT_COLOR }, item)
    }
}
```

> 若需要随生物群系动态变色（草方块/树叶风格），把上面的固定色 lambda 换成调用 `BiomeColors.getFoliageColor(world, pos)` / `getGrassColor(...)` 即可（注意 `world`/`pos` 可能为 null）。

**在客户端入口调用**：

```kotlin
// src/client/kotlin/ic2_120/Ic2_120Client.kt
override fun onInitializeClient() {
    PeatOreColorProvider.register()
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
