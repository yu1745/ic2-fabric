# GeneratorScreen 重构：混合渲染 → ComposeUI + SlotAnchor

## 目标

将 `GeneratorScreen` 从**混合渲染**（部分 ComposeUI + 部分手绘槽边框）重构为**全 ComposeUI 两阶段渲染**，借助 `SlotAnchor` 导出锚点，彻底去掉机器槽的手动边框绘制。

---

## 重构前 vs 重构后

### 重构前

```kotlin
// drawBackground: 背景 + 玩家背包边框 + 机器槽边框（手绘）
override fun drawBackground(context: DrawContext, ...) {
    GuiBackground.draw(context, x, y, ...)
    GuiBackground.drawPlayerInventorySlotBorders(...)
    context.drawBorder(...)  // 燃料槽边框
    context.drawBorder(...)  // 电池槽边框
}

// render: 跳过原生渲染，仅 overlay
override fun render(context: DrawContext, ...) {
    super.render(context, ...)   // ← 跳过原生渲染
    ui.render(context, ...) {   // 标题、能量条、统计文字
        Column(x = left + 8, y = top + 8, spacing = 4) { ... }
    }
}
```

### 重构后：四阶段渲染

```
ui.layout()            → 预布局，导出 SlotAnchor 锚点
applyAnchoredSlots()   → 锚点坐标写回 handler.slots
super.render()         → 原生 HandledScreen 渲染：背景 + slot（边框由 SlotAnchor 绘制）
ui.render()            → Compose overlay：标题、能量条、槽位锚点、燃烧条
```

---

## 实际代码解析

### 1. Slot 反射字段

```kotlin
private val slotXField by lazy {
    Slot::class.java.getDeclaredField("x").apply { isAccessible = true }
}
private val slotYField by lazy {
    Slot::class.java.getDeclaredField("y").apply { isAccessible = true }
}
```

用于将 SlotAnchor 锚点坐标写回原生 slot 的 x/y 字段，使 `super.render()` 能渲染在正确位置。

### 2. drawBackground

仅保留玩家背包边框（位置固定，无需 compose 编排）。机器槽边框由 SlotAnchor 的 `showBorder = true`（默认）在 overlay 阶段绘制。

```kotlin
override fun drawBackground(context: DrawContext, ...) {
    GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
    GuiBackground.drawPlayerInventorySlotBorders(...)
    // 机器槽边框 → 交给 SlotAnchor，不在此手绘
}
```

### 3. 两阶段 render

```kotlin
override fun render(context: DrawContext, ...) {
    val left = x
    val top = y

    // 数据准备
    val energy = handler.sync.energy.toLong().coerceAtLeast(0)
    val inputRate = handler.sync.getSyncedInsertedAmount()
    val outputRate = handler.sync.getSyncedExtractedAmount()
    val cap = GeneratorSync.ENERGY_CAPACITY
    val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
    val totalBurn = handler.sync.totalBurnTime.coerceAtLeast(1)
    val burnTime = handler.sync.burnTime.coerceIn(0, totalBurn)
    val burnFrac = (burnTime.toFloat() / totalBurn).coerceIn(0f, 1f)

    val energyText = "${formatEu(energy)} / ${formatEu(cap)} EU"
    val statusText1 = "发电 ${formatEu(inputRate)} EU/t"
    val statusText2 = "输出 ${formatEu(outputRate)} EU/t"

    // 侧边文字宽度（用于右侧面板外显示状态信息）
    val sideTextWidth = maxOf(
        textRenderer.getWidth(energyText),
        textRenderer.getWidth(statusText1),
        textRenderer.getWidth(statusText2)
    )
    val sideTextX = left - sideTextWidth - 4

    // Compose DSL 内容
    val content: UiScope.() -> Unit = {
        Column(
            x = left + 8,
            y = top + 8,
            spacing = 6,
            modifier = Modifier().width(backgroundWidth - 16).height(backgroundHeight - 16),
        ) {
            // 标题 + 能量数字（横向排列）
            Row(spacing = 8) {
                Text(title.string, color = 0xFFFFFF)
                Text(energyText)
            }

            // 能量条
            EnergyBar(energyFraction)

            // 机器槽行：燃料槽+燃烧条 | 电池槽（两端分布）
            Flex(
                direction = FlexDirection.ROW,
                justifyContent = JustifyContent.SPACE_BETWEEN,
            ) {
                // 左侧：燃料槽 + 竖向燃烧进度条
                Row(spacing = 8) {
                    SlotAnchor(id = "slot.${MachineBlockEntity.FUEL_SLOT}")
                    EnergyBar(
                        burnFrac,
                        orientation = EnergyBarOrientation.VERTICAL,
                        shortEdge = 8,
                        barHeight = 18,
                        emptyColor = 0xFF0033CC.toInt(),
                        fullColor = 0xFFCC0000.toInt(),
                    )
                }
                // 右侧：电池槽
                SlotAnchor(id = "slot.${MachineBlockEntity.BATTERY_SLOT}")
            }
        }
    }

    // 阶段 1: 预布局
    val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)

    // 阶段 2: 锚点写回 slot
    handler.slots.forEachIndexed { index, slot ->
        val anchor = layout.anchors["slot.$index"] ?: return@forEachIndexed
        slotXField.setInt(slot, anchor.x - left)
        slotYField.setInt(slot, anchor.y - top)
    }

    // 阶段 3: 原生渲染（slot 已在正确位置）
    super.render(context, mouseX, mouseY, delta)

    // 阶段 4: Compose overlay
    ui.render(context, textRenderer, mouseX, mouseY, content = content)

    // 侧边状态文字（面板右侧外）
    context.drawText(textRenderer, statusText1, sideTextX, top + 8, 0xAAAAAA, false)
    context.drawText(textRenderer, statusText2, sideTextX, top + 20, 0xAAAAAA, false)

    drawMouseoverTooltip(context, mouseX, mouseY)
}
```

### 4. Handler 槽位占位符

```kotlin
// GeneratorScreenHandler.kt
// 初始坐标 (0, 0)，由 Screen 的 SlotAnchor 每帧覆盖
addSlot(PredicateSlot(blockInventory, MachineBlockEntity.FUEL_SLOT, 0, 0, FUEL_SLOT_SPEC))
addSlot(PredicateSlot(blockInventory, MachineBlockEntity.BATTERY_SLOT, 0, 0, BATTERY_SLOT_SPEC))
```

---

## 布局结构图

```
GUI 面板 (176×166)
├── Column (x=8, y=8, spacing=6)
│   ├── Row: [标题] [能量文字]
│   ├── EnergyBar: 横向能量条
│   └── Flex ROW SPACE_BETWEEN:
│       ├── Row:
│       │   ├── SlotAnchor(FUEL_SLOT)  ← 燃料槽
│       │   └── EnergyBar VERTICAL     ← 燃烧进度（红→蓝）
│       └── SlotAnchor(BATTERY_SLOT)    ← 电池槽
│
│── 原生玩家背包槽（drawBackground 绘制）
│── 原生机器槽（位置由 SlotAnchor 锚点确定）
│── 侧边状态文字（super.render 之后手绘）
```

---

## 涉及文件

| 文件 | 改动 |
|---|---|
| `GeneratorScreen.kt` | 两阶段渲染 + SlotAnchor + Flex 布局 |
| `GeneratorScreenHandler.kt` | 槽位初始坐标改为 `(0, 0)` 占位符 |

---

## 关键设计决策

1. **`drawBackground` 保留玩家背包边框** — 位置固定（`PLAYER_INV_Y`、`HOTBAR_Y`），无需 compose 编排；compose-ui 暂无等效组件。

2. **机器槽边框交给 SlotAnchor** — `showBorder = true`（默认），边框在 overlay 阶段由 `SlotAnchorNode` 自己绘制，不再手绘。

3. **`Flex SPACE_BETWEEN` 布局槽位行** — 燃料槽+燃烧条为一组，电池槽为一组，两端对齐，自动分配间距，无需手动计算坐标。

4. **侧边状态文字（发电/输出）放在 compose 外部** — `super.render()` 之后直接用 `context.drawText` 绘在面板右侧外侧，不进入 compose 树，避免额外布局复杂度。

5. **`Column` 仍用显式 xy + 固定宽高** — 当前 compose-ui 不支持根节点 flow 布局，`Column` 需要锚定起点；宽高通过 `Modifier().width().height()` 固定为内容区尺寸。

---

## 可复用到其他 Screen 的模板

```kotlin
override fun render(context: DrawContext, ...) {
    val left = x
    val top = y

    val content: UiScope.() -> Unit = {
        Column(
            x = left + 8,
            y = top + 8,
            spacing = 6,
            modifier = Modifier().width(backgroundWidth - 16).height(backgroundHeight - 16),
        ) {
            // ... SlotAnchor(id = "slot.$index") ...
        }
    }

    // 阶段 1: 预布局
    val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)

    // 阶段 2: 锚点写回 slot
    handler.slots.forEachIndexed { index, slot ->
        val anchor = layout.anchors["slot.$index"] ?: return@forEachIndexed
        slotXField.setInt(slot, anchor.x - left)
        slotYField.setInt(slot, anchor.y - top)
    }

    // 阶段 3: 原生渲染
    super.render(context, mouseX, mouseY, delta)

    // 阶段 4: Compose overlay
    ui.render(context, textRenderer, mouseX, mouseY, content = content)
}
```
