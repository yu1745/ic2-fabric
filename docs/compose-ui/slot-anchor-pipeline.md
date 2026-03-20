# SlotAnchor 全链路（ScreenHandler + Screen）

本文给出 `SlotAnchor` + `SlotSpec` 的完整落地流程：

1. `ScreenHandler` 先声明真实 `PredicateSlot(SlotSpec)`
2. `Screen` 用 `SlotAnchor` 放置锚点
3. `ui.layout(...)` 导出锚点
4. 将锚点写回 `slot.x / slot.y`
5. `super.render(...)` 让原生系统渲染与交互
6. `ui.render(...)` 画 Compose overlay

---

## 1) ScreenHandler：先有真实 PredicateSlot（基于 SlotSpec）

```kotlin
@ModScreenHandler(name = "my_compose_demo")
class MyComposeDemoScreenHandler(
    syncId: Int,
    val playerInventory: PlayerInventory
) : ScreenHandler(MyComposeDemoScreenHandler::class.type(), syncId) {

    private val leftSlotSpec = SlotSpec(
        canInsert = { stack -> true },  // 按需替换为机器输入规则
        canTake = { true }
    )
    private val rightSlotSpec = SlotSpec(
        canInsert = { stack -> stack.count <= 1 },
        maxItemCount = 1
    )

    init {
        // 初始坐标无意义，后续由 SlotAnchor 每帧覆盖
        addSlot(PredicateSlot(playerInventory, 0, 0, 0, leftSlotSpec))
        addSlot(PredicateSlot(playerInventory, 1, 0, 0, rightSlotSpec))
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var result = ItemStack.EMPTY
        val slot = slots.getOrNull(index) ?: return ItemStack.EMPTY
        if (!slot.hasStack()) return ItemStack.EMPTY

        val inSlot = slot.stack
        result = inSlot.copy()
        when (index) {
            SLOT_LEFT_INDEX, SLOT_RIGHT_INDEX -> {
                if (!insertItem(inSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
            }
            in PLAYER_INV_START..HOTBAR_END -> {
                val moved = SlotMoveHelper.insertIntoTargets(
                    inSlot,
                    listOf(
                        SlotTarget(slots[SLOT_LEFT_INDEX], leftSlotSpec),
                        SlotTarget(slots[SLOT_RIGHT_INDEX], rightSlotSpec)
                    )
                )
                if (!moved) return ItemStack.EMPTY
            }
            else -> return ItemStack.EMPTY
        }

        if (inSlot.isEmpty) slot.stack = ItemStack.EMPTY else slot.markDirty()
        if (inSlot.count == result.count) return ItemStack.EMPTY
        slot.onTakeItem(player, inSlot)
        return result
    }

    override fun canUse(player: PlayerEntity): Boolean = true

    companion object {
        const val SLOT_LEFT_INDEX = 0
        const val SLOT_RIGHT_INDEX = 1
        const val PLAYER_INV_START = 2
        const val HOTBAR_END = 37
    }
}
```

说明：
- 通过 `SlotSpec` 统一声明“能放什么、能拿走吗、最大堆叠”。
- 通过 `PredicateSlot` 承载规则，避免文档出现裸 `Slot(...)`。
- `quickMove` 推荐用 `SlotMoveHelper + SlotTarget(spec)`，保持与手动放入一致的判定逻辑。

---

## 2) Screen：两阶段渲染 + SlotAnchor

```kotlin
class MyComposeDemoScreen(
    handler: MyComposeDemoScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<MyComposeDemoScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()
    private val slotXField by lazy {
        net.minecraft.screen.slot.Slot::class.java.getDeclaredField("x").apply { isAccessible = true }
    }
    private val slotYField by lazy {
        net.minecraft.screen.slot.Slot::class.java.getDeclaredField("y").apply { isAccessible = true }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y

        val uiContent: UiScope.() -> Unit = {
            Column(x = left + 8, y = top + 8, spacing = 6) {
                Text("SlotAnchor Demo")
                Row(spacing = 8) {
                    SlotHost(MyComposeDemoScreenHandler.SLOT_LEFT_INDEX)
                    SlotHost(MyComposeDemoScreenHandler.SLOT_RIGHT_INDEX, showBorder = false)
                }
            }
        }

        // 1) 预布局，不绘制
        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = uiContent)

        // 2) 锚点写回 slot 相对坐标
        applyAnchoredSlots(layout, left, top)

        // 3) 原生 slot 渲染 + 交互
        super.render(context, mouseX, mouseY, delta)

        // 4) Compose overlay
        ui.render(context, textRenderer, mouseX, mouseY, content = uiContent)
    }

    private fun UiScope.SlotHost(slotIndex: Int, size: Int = 18, showBorder: Boolean = true) {
        SlotAnchor(
            id = slotAnchorId(slotIndex),
            width = size,
            height = size,
            showBorder = showBorder
        )
    }

    private fun applyAnchoredSlots(layout: ComposeUI.LayoutSnapshot, left: Int, top: Int) {
        handler.slots.forEachIndexed { index, slot ->
            val anchor = layout.anchors[slotAnchorId(index)] ?: return@forEachIndexed
            slotXField.setInt(slot, anchor.x - left)
            slotYField.setInt(slot, anchor.y - top)
        }
    }

    private fun slotAnchorId(slotIndex: Int): String = "slot.$slotIndex"
}
```

---

## 3) SlotAnchor 参数

```kotlin
SlotAnchor(
    id = "machine.input",
    width = 18,
    height = 18,
    showBorder = true,                         // 默认 true
    borderColor = GuiBackground.BORDER_COLOR,  // 默认边框色
    x = 0,
    y = 0,
    absolute = false,
    modifier = Modifier.EMPTY
)
```

- `id`：锚点唯一标识，建议稳定命名（如 `slot.0`、`machine.input`）
- `showBorder`：是否绘制默认边框
- `borderColor`：边框颜色
- 默认边框为**内缩绘制**（与锚点矩形同起点，不再向外偏移 1 像素）

---

## 坐标系注意

- `layout.anchors` 是**屏幕绝对坐标**
- `slot.x / slot.y` 是**相对 GUI 左上角**坐标
- 所以要做：`slot = anchor - (screenLeft, screenTop)`
