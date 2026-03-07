# ComposeUI — 声明式 GUI 渲染中间层

基于 Minecraft `DrawContext` 的类 Jetpack Compose 声明式 UI DSL。用于在 `Screen.render()` 中以声明式写法构建 GUI，自动完成布局计算与绘制。
DrawContext如何使用可以查看./drawcontext-methods.md

**源码位置**: `src/client/kotlin/ic2_120/client/compose/`

---

## 快速开始

```kotlin
class MyScreen(...) : HandledScreen<...>(...) {
    private val ui = ComposeUI()

    override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(ctx, mouseX, mouseY, delta)
        ui.render(ctx, textRenderer, mouseX, mouseY) {
            Column(x = 10, y = 10, spacing = 4) {
                Text("Hello IC2")
                Row(spacing = 8) {
                    Button("确认") { println("clicked") }
                    Button("取消") { }
                }
            }
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)
}
```

---

## 架构

每帧三阶段，无状态，无 diff/recomposition：

```
DSL 构建 → 节点树 → Measure(自底向上) → Render(自顶向下) → DrawContext 绘制
```

**两遍布局算法**确保每个节点恰好被访问两次（O(n)），避免父子元素反复更新：

| 阶段 | 方向 | 作用 |
|------|------|------|
| Measure | 自底向上 | 叶子先算自身尺寸，容器从子节点推导尺寸 |
| Render | 自顶向下 | 父容器分配子节点坐标，调用 DrawContext 绘制 |

---

## 定位模式

所有元素都支持 `x`、`y`、`absolute` 三个定位参数：

### 文档流（默认）

```kotlin
Text("hello", x = 5, y = 2)  // x/y 作为偏移量微调，元素仍由父容器排列
```

`x`/`y` 是相对于父容器分配位置的偏移量（`Position.Flow(offsetX, offsetY)`）。元素参与父容器的布局计算。

### 绝对定位

```kotlin
Text("overlay", x = 100, y = 50, absolute = true)
```

`absolute = true` 时，`x`/`y` 是相对于**父容器原点**的坐标（`Position.Absolute(x, y)`）。元素脱离文档流，不影响兄弟元素排列，也不参与父容器尺寸计算。

### 顶层画布

`ComposeUI.render { }` 的顶层是画布模式（`RootNode`）——每个顶层元素的 `x`/`y` 直接作为屏幕坐标，互不干扰。

---

## 元素参考

### Text

渲染单行文本。

```kotlin
Text(
    text: String,
    color: Int = 0xFFFFFF,      // ARGB 颜色
    shadow: Boolean = true,      // 是否带阴影
    x: Int = 0, y: Int = 0,
    absolute: Boolean = false,
    modifier: Modifier = Modifier.EMPTY
)
```

尺寸由 `TextRenderer` 自动计算（宽 = 文字宽度，高 = `fontHeight` 即 9px）。

### Image

渲染 PNG 纹理。

```kotlin
// 完整纹理
Image(
    texture: Identifier,         // 如 Identifier("minecraft", "textures/block/stone.png")
    width: Int, height: Int      // 屏幕绘制尺寸
)

// Sprite sheet 裁剪
Image(
    texture: Identifier,
    width: Int, height: Int,
    u: Float = 0f, v: Float = 0f,           // 纹理源裁剪起点
    regionWidth: Int, regionHeight: Int,     // 裁剪区域大小
    textureWidth: Int, textureHeight: Int,   // 纹理文件实际尺寸
    x: Int = 0, y: Int = 0,
    absolute: Boolean = false,
    modifier: Modifier = Modifier.EMPTY
)
```

### Button

带交互的按钮，自动检测 hover 高亮，支持点击回调。

```kotlin
Button(
    text: String,
    x: Int = 0, y: Int = 0,
    absolute: Boolean = false,
    modifier: Modifier = Modifier.EMPTY,
    onClick: () -> Unit = {}
)
```

默认样式：灰色背景 + 边框，hover 时变亮。可通过 `modifier.background()` / `.border()` 覆盖颜色。

需在 Screen 中转发点击事件：
```kotlin
override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
    ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)
```

默认 padding 为 `Padding(6, 4, 6, 4)`，可通过 `modifier.padding()` 覆盖。

### EnergyBar（client/ui）

能量条组件，渐变色：空电端红、满电端绿。需 `import ic2_120.client.ui.EnergyBar` 后使用。

```kotlin
EnergyBar(fraction = 0.5f)                    // 50%，默认 100×8
EnergyBar(0.25f, barWidth = 80, barHeight = 6)
```

---

## 容器参考

### Column

纵向排列子元素。

```kotlin
Column(
    x: Int = 0, y: Int = 0,
    spacing: Int = 0,            // 子元素间距
    absolute: Boolean = false,
    modifier: Modifier = Modifier.EMPTY
) {
    Text("第一行")
    Text("第二行")
}
```

尺寸计算：`width = max(子元素宽)`, `height = sum(子元素高) + spacing * (n-1)`

### Row

横向排列子元素。

```kotlin
Row(
    x: Int = 0, y: Int = 0,
    spacing: Int = 0,
    absolute: Boolean = false,
    modifier: Modifier = Modifier.EMPTY
) {
    Button("A") {}
    Button("B") {}
}
```

尺寸计算：`width = sum(子元素宽) + spacing * (n-1)`, `height = max(子元素高)`

### Table

表格布局：按行×列放置单元格，同一列对齐，**行内垂直居中**（适合标签+能量条等混排，避免错位）。

```kotlin
Table(
    columnWidths = listOf(28, 100),   // 可选固定列宽；null 则按内容取每列 max 宽
    columnSpacing = 8,
    rowSpacing = 4
) {
    row { Text("0%", color = 0xAAAAAA); EnergyBar(0f) }
    row { Text("25%", color = 0xAAAAAA); EnergyBar(0.25f) }
}
```

- 用 `row { }` 添加一行，行内按顺序为第 1、2、… 列单元格。
- 列数取所有行中最大单元格数；行高 = 该行单元格最大高度；单元格在行内垂直居中。

### Flex

CSS Flexbox 语义的高级容器，支持主轴/交叉轴对齐。

```kotlin
Flex(
    x: Int = 0, y: Int = 0,
    direction: FlexDirection = FlexDirection.ROW,     // ROW | COLUMN
    justifyContent: JustifyContent = JustifyContent.START,
    alignItems: AlignItems = AlignItems.START,
    gap: Int = 0,
    absolute: Boolean = false,
    modifier: Modifier = Modifier.EMPTY
) {
    // 子元素
}
```

#### FlexDirection

| 值 | 主轴 | 交叉轴 |
|----|------|--------|
| `ROW` | 水平 → | 垂直 ↓ |
| `COLUMN` | 垂直 ↓ | 水平 → |

#### JustifyContent（主轴分布）

| 值 | 效果 |
|----|------|
| `START` | 靠主轴起点 |
| `CENTER` | 居中 |
| `END` | 靠主轴终点 |
| `SPACE_BETWEEN` | 首尾贴边，中间均分 |
| `SPACE_AROUND` | 每个元素两侧等间距 |
| `SPACE_EVENLY` | 所有间隙（含首尾）等间距 |

需要为 Flex 指定明确尺寸（通过 `modifier.width()` / `.height()`）才能看到对齐效果，否则容器会紧贴内容。

#### AlignItems（交叉轴对齐）

| 值 | 效果 |
|----|------|
| `START` | 靠交叉轴起点 |
| `CENTER` | 交叉轴居中 |
| `END` | 靠交叉轴终点 |

#### 示例：按钮栏两端对齐

```kotlin
Flex(
    x = left, y = top + 170,
    direction = FlexDirection.ROW,
    justifyContent = JustifyContent.SPACE_BETWEEN,
    alignItems = AlignItems.CENTER,
    modifier = Modifier.EMPTY.width(176).height(20)
) {
    Button("启动") { }
    Text("状态：就绪", color = 0x999999)
    Button("停止") { }
}
```

---

## Modifier

链式修饰符，用于设置尺寸、内边距、背景和边框。所有元素和容器都支持。

```kotlin
Modifier.EMPTY                           // 空修饰符
    .width(100)                          // 固定宽度
    .height(20)                          // 固定高度
    .size(100, 20)                       // 同时设置宽高
    .padding(4)                          // 四边等距内边距
    .padding(8, 4)                       // 水平 8, 垂直 4
    .padding(4, 2, 4, 2)                // left, top, right, bottom
    .background(0x80FF0000.toInt())      // 背景色（ARGB）
    .border(0xFFFFFFFF.toInt())          // 边框色（ARGB）
```

修饰符影响布局：
- `width` / `height`：覆盖自动计算的尺寸，容器内子元素的约束也会据此调整
- `padding`：内容区域向内缩进，增加元素总尺寸

---

## 文件结构

**Compose 框架**（仅布局与绘制原语）：

```
src/client/kotlin/ic2_120/client/compose/
├── ComposeUI.kt      入口类：render() 三阶段编排，mouseClicked() 按钮命中检测
├── Constraints.kt    Constraints（最大宽高约束）、Size
├── Modifier.kt       Modifier 链式修饰符、Padding
├── Position.kt       Position 密封类：Flow（文档流）、Absolute（绝对定位）
├── UiNode.kt         节点基类 + TextNode / ImageNode / ButtonNode /
│                      ColumnNode / RowNode / FlexNode / TableNode / RootNode
└── UiScope.kt        @UiDsl 作用域：Column / Row / Flex / Table / Text / Image / Button；TableScope.row
```

**业务 UI 组件**（基于 compose 的扩展，与框架分离）：

```
src/client/kotlin/ic2_120/client/ui/
└── EnergyBar.kt      能量条：EnergyBarNode + UiScope.EnergyBar() 渐变色红→绿
```

---

## 完整示例

```kotlin
ui.render(context, textRenderer, mouseX, mouseY) {
    // 标题区（Column 纵向排列）
    Column(x = left + 8, y = top - 16, spacing = 2) {
        Text("IC2 电炉", color = 0xFFFFFF)
        Text("Status: Ready", color = 0xAAAAAA, shadow = false)
    }

    // 槽位信息（Row + 嵌套 Column，带背景色和内边距）
    Row(x = left + 8, y = top + 8, spacing = 4) {
        Column(
            spacing = 2,
            modifier = Modifier.EMPTY.background(0x80FF0000.toInt()).padding(4)
        ) {
            Text("Input", color = 0xFFFFFF)
            Text("Slot 0", color = 0xCCCCCC)
        }
        Column(
            spacing = 2,
            modifier = Modifier.EMPTY.background(0x8000FF00.toInt()).padding(4)
        ) {
            Text("Output", color = 0xFFFFFF)
            Text("Slot 1", color = 0xCCCCCC)
        }
    }

    // 底部操作栏（Flex 两端对齐）
    Flex(
        x = left, y = top + backgroundHeight + 2,
        direction = FlexDirection.ROW,
        justifyContent = JustifyContent.SPACE_BETWEEN,
        alignItems = AlignItems.CENTER,
        modifier = Modifier.EMPTY.width(backgroundWidth).height(20)
    ) {
        Button("启动", modifier = Modifier.EMPTY.background(0xFF006600.toInt())) {
            println("启动")
        }
        Text("Electric Furnace", color = 0x999999)
        Button("停止", modifier = Modifier.EMPTY.background(0xFF660000.toInt())) {
            println("停止")
        }
    }

    // 图标（Image 渲染纹理）
    Column(x = left + backgroundWidth + 4, y = top, spacing = 2) {
        Image(Identifier("minecraft", "textures/block/diamond_block.png"), width = 16, height = 16)
        Image(Identifier("minecraft", "textures/block/gold_block.png"), width = 16, height = 16)
    }

    // 绝对定位（脱离文档流）
    Text("绝对定位测试", x = left + backgroundWidth - 60, y = top - 10,
         absolute = true, color = 0xFFFF00)
}
```
