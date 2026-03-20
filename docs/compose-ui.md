# ComposeUI — DSL 总览与导航

ComposeUI 是一个基于 `DrawContext` 的声明式 UI DSL，适用于 `HandledScreen` 的布局与渲染。

源码目录：`src/client/kotlin/ic2_120/client/compose/`

---

## 近期更新（2026-03）

- `SlotAnchor` 的默认槽位框绘制已更新为“原版风格”：
  - 不再是单线 `drawBorder`。
  - 现在使用亮左上 + 暗右下 + 灰色内底的凹槽样式，尽量贴近原版容器槽位视觉。
  - 若传入自定义 `borderColor`，会在原版槽位样式外再叠加该边框色。
- 背景工具新增可复用原版风格绘制方法（`GuiBackground`）：
  - `drawVanillaLikePanel(context, x, y, width, height)`：程序化绘制原版亮灰面板（可随 `GuiSize` 拉伸，无贴图空洞）。
  - `drawVanillaLikeSlot(context, x, y, width, height)`：程序化绘制原版槽位框（可拉伸）。

---

## 你应该先看哪篇

- 快速上手：`docs/compose-ui/quick-start.md`
- SlotAnchor 全链路（ScreenHandler + Screen）：`docs/compose-ui/slot-anchor-pipeline.md`
- 元素（Text / Image / Button / ItemStack / SlotAnchor）：`docs/compose-ui/elements.md`
- 容器（Column / Row / Flex / Table）：`docs/compose-ui/containers.md`
- ScrollView：`docs/compose-ui/scrollview.md`
- 架构与文件结构：`docs/compose-ui/architecture.md`

---

## DSL 总览

```kotlin
ui.render(context, textRenderer, mouseX, mouseY) {
    Column(x = left + 8, y = top + 8, spacing = 6) {
        Text("标题")

        Row(spacing = 8) {
            Button("确认") { }
            Button("取消") { }
        }

        Flex(
            direction = FlexDirection.ROW,
            justifyContent = JustifyContent.SPACE_BETWEEN,
            alignItems = AlignItems.CENTER,
            modifier = Modifier.EMPTY.width(176)
        ) {
            Text("左")
            Text("右")
        }

        Table(columnSpacing = 8, rowSpacing = 4) {
            row {
                Text("能量")
                Text("128 / 512 EU")
            }
        }

        ScrollView(width = 160, height = 80, scrollbarWidth = 8) {
            Column(spacing = 2) {
                Text("可滚动内容")
            }
        }

        ItemStack(stack)
        Image(texture, width = 16, height = 16)
        SlotAnchor("machine.input")
    }
}
```

---

## 两种渲染模式

1. 纯 Compose 绘制：直接 `ui.render(...)`。
2. Slot 融合（推荐）：
   1) `ui.layout(...)` 产出锚点
   2) 将锚点写回 `handler.slots`
   3) `super.render(...)`
   4) `ui.render(...)` 画 overlay

详细见：`docs/compose-ui/slot-anchor-pipeline.md`

---

## 布局原则：让容器自己安排宽度

**原则：除非明确要求，否则不主动指定子元素的 `x` / `y`。**

正确的做法是：根容器通过 `modifier = Modifier.EMPTY.width(gui.contentWidth)` 固定宽度，内部的 Column / Row / Flex 等容器依靠 flow 布局自行排列。子元素只管自己内部的内容，不需要知道父容器放在屏幕哪个位置。

```kotlin
// ✅ 正确：根容器定宽，子元素不写 x/y
ui.render(context, textRenderer, mouseX, mouseY) {
    Flex(
        x = x + 8,
        y = y + 6,
        direction = FlexDirection.ROW,
        gap = 12,
        modifier = Modifier.EMPTY.width(gui.contentWidth)
    ) {
        Column(spacing = 4) { ... }       // 不写 x/y
        Column(spacing = 4) { ... }       // 不写 x/y
    }
}

// ❌ 错误：子元素自己算 left/right，每个都带 x/y
Flex(direction = FlexDirection.ROW, gap = 12) {
    Column(spacing = 4) {
        Text("信息")
    }
    Column(x = left + 132, spacing = 4) {   // 自己算 left，呼吸刻度不一致
        ScrollView(width = contentW - 128) { ... }
    }
}
```

**何时需要主动指定 `x` / `y`：**
- 根容器定位到屏幕坐标时（`x = this.x + 8`, `y = this.y + 6`）
- 需要精确覆盖特定屏幕区域时（如与背景纹理对齐的局部面板）
- `absolute = true` 时表示绝对定位，绕开 flow 布局
