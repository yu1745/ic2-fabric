# DrawContext 绘制方法参考

**适用版本**: Fabric 1.20.4附近版本

---

## 完整方法列表

### 基础绘图方法

#### `fill(int x1, int y1, int x2, int y2, int color)`
绘制填充矩形。

```kotlin
// 红色矩形：左上角 (10, 10)，宽 100 高 50
drawContext.fill(10, 10, 110, 60, 0xFFFF0000)
```

#### `drawBorder(int x, int y, int width, int height, int color)`
绘制矩形边框轮廓。

```kotlin
// 绘制红色边框，宽 100 高 50
drawContext.drawBorder(10, 10, 100, 50, 0xFFFF0000)
```

#### `drawVerticalLine(int x, int y1, int y2, int color)`
绘制垂直线（从上到下）。

```kotlin
// 在 x=100 位置绘制一条红色垂直线从 y=10 到 y=200
drawContext.drawVerticalLine(100, 10, 200, 0xFFFF0000)
```

#### `drawHorizontalLine(int x, int y, int x2, int color)`
绘制水平线（从左到右）。

```kotlin
// 在 y=50 位置绘制一条红色水平线从 x=10 到 x=200
drawContext.drawHorizontalLine(10, 50, 200, 0xFFFF0000)
```

---

### 纹理绘制方法

#### `drawTexture(Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight)`
绘制指定区域或完整的纹理。

```kotlin
val texture = Identifier.of("minecraft", "textures/block/deepslate.png")

// 绘制完整 16x16 的纹理块
drawContext.drawTexture(texture, 90, 90, 0, 0, 16, 16, 16, 16)
```

参数说明:
- `texture` - 纹理资源路径
- `x, y` - 屏幕上的绘制位置
- `u, v` - 纹理源坐标（要裁剪的起始位置）
- `width, height` - 要绘制的大小
- `textureWidth, textureHeight` - 纹理的实际尺寸

---

### 文本绘制方法

#### `drawString(TextRenderer textRenderer, String text, int x, int y, int color, boolean shadow)`
绘制单行文本。

```kotlin
// 白色无阴影文本
drawContext.drawString(textRenderer, "Hello World", 10, 10, 0xFFFFFFFF, false)

// 黄色带阴影文本
drawContext.drawString(textRenderer, "Warning!", 10, 30, 0xFFFFFF00, true)
```

---

### 矩阵变换方法

#### `matrices(): PoseStack`
获取当前的 PoseStack 用于变换操作。

```kotlin
matrices().translate(50f, 50f, 0f)   // 平移
matrices().scale(2f, 2f, 1f)         // 缩放
matrices().pushPose()                // 压入矩阵
matrices().popPose()                 // 弹出矩阵
```

#### `pushPose()` / `popPose()`
通过 `matrices()` 获取的 PoseStack 调用：
- `pushPose()` - 压入新的变换矩阵（保存当前状态）
- `popPose()` - 弹出当前的变换矩阵（恢复之前状态）

---

### 裁剪区域方法

#### `enableScissor(int x, int y, int width, int height)`
启用裁剪区域，后续绘制只会在该区域内显示。

```kotlin
drawContext.enableScissor(10, 10, 100, 100)
// 此处的绘制只会显示在 10x10 100x100 区域内
drawContext.disableScissor()
```

#### `disableScissor()`
禁用裁剪区域。

---

## 颜色格式

Minecraft 1.20.x 使用 **ARGB** 格式（Alpha-Red-Green-Blue）：
- `0xAARRGGBB` - 十六进制颜色值
- 示例:
  - `0xFFFFFFFF` - 白色
  - `0xFFFF0000` - 红色
  - `0xFF00FF00` - 绿色
  - `0xFF0000FF` - 蓝色
  - `0xAA000000` - 半透明黑色

---

## 实用代码模板

### 屏幕半透明覆盖层
```kotlin
drawContext.fill(0, 0, screenWidth, screenHeight, 0xAA000000)  // 黑色半透明遮罩
```

### 按钮背景
```kotlin
// 灰色背景
drawContext.fill(x, y, x + width, y + height, 0xFFAAAAAA)
// 顶部高光
drawContext.drawHorizontalLine(x, y, x + width, 0xFFDDDDDD)
// 底部阴影
drawContext.drawHorizontalLine(x, y + height, x + width, 0xFF888888)
// 左右边框
drawContext.drawVerticalLine(x, y, y + height, 0xFFDDDDDD)
drawContext.drawVerticalLine(x + width, y, y + height, 0xFF888888)
```

### 旋转元素
```kotlin
drawContext.matrices().pushPose()
drawContext.matrices().translate((screenWidth / 2).toFloat(), (screenHeight / 2).toFloat(), 0f)
drawContext.matrices().rotateDegrees(45f)
drawContext.drawString(textRenderer, "Rotated", -50, 0, 0xFFFFFFFF, false)
drawContext.matrices().popPose()
```

### 动态进度条
```kotlin
// 背景
drawContext.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF444444)
// 进度
val filledWidth = (progressPercent / 100f * barWidth).toInt()
drawContext.fill(barX, barY, barX + filledWidth, barY + barHeight, 0xFF00AA00)
// 边框
drawContext.drawBorder(barX, barY, barWidth, barHeight, 0xFF888888)
```

---

## 参考资料
- [Fabric 1.20.4 DrawContext 文档](https://docs.fabricmc.net/1.20.4/develop/rendering/draw-context)
