package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.ComposeDebugBlock
import ic2_120.content.screen.ComposeDebugScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

/**
 * Compose UI 调试屏幕。
 * 展示 ScrollView 的完整功能：滚轮滚动、track 跳转、thumb 拖拽。
 *
 * 内容：
 * - 标题区（按钮：增减测试数据）
 * - ScrollView：模拟矿物扫描结果，每行显示矿物图标+名称+数量
 * - 底部信息栏
 */
@ModScreen(block = ComposeDebugBlock::class)
class ComposeDebugScreen(
    handler: ComposeDebugScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<ComposeDebugScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = GuiSize.DEBUG.width
        backgroundHeight = GuiSize.DEBUG.height
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y
        val gui = GuiSize.DEBUG

        val results = buildDemoResults()
        val itemCount = results.size
        val uiContent: UiScope.() -> Unit = {
            buildUi(left, top, gui, results, itemCount)
        }

        // 1) 预布局（不绘制）→ 2) 套用 slot 坐标
        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = uiContent)
        applyAnchoredSlots(layout, left, top)

        // 3) 原生渲染（slot+交互）
        super.render(context, mouseX, mouseY, delta)

        // 4) Compose overlay 绘制
        ui.render(context, textRenderer, mouseX, mouseY, content = uiContent)

        val tooltip = ui.getTooltipAt(mouseX, mouseY)
        if (tooltip != null) {
            context.drawTooltip(textRenderer, tooltip, mouseX, mouseY)
        } else {
            drawMouseoverTooltip(context, mouseX, mouseY)   
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        amount: Double
    ): Boolean =
        ui.mouseScrolled(mouseX, mouseY, 0.0, amount)
            || super.mouseScrolled(mouseX, mouseY, amount)

    override fun mouseDragged(
        mouseX: Double,
        mouseY: Double,
        button: Int,
        deltaX: Double,
        deltaY: Double
    ): Boolean =
        ui.mouseDragged(mouseX, mouseY, button) || super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)

    override fun mouseReleased(
        mouseX: Double,
        mouseY: Double,
        button: Int
    ): Boolean {
        ui.stopDrag()
        return super.mouseReleased(mouseX, mouseY, button)
    }

    private data class OreEntry(val name: String, val count: Int, val index: Int)

    private fun UiScope.buildUi(
        left: Int,
        top: Int,
        gui: GuiSize,
        results: List<OreEntry>,
        itemCount: Int
    ) {
        Flex(
            x = left + 8,
            y = top + 8,
            direction = FlexDirection.COLUMN,
            gap = 8,
            modifier = Modifier.EMPTY.width(gui.contentWidth).height(gui.height - 16)
        ) {
            // 标题栏（固定高度 22）
            Column(spacing = 6) {
                Text("Compose UI 调试面板", color = 0xFFFFFF)
                Text("ScrollView 演示 — 滚轮/track/thumb", color = 0xAAAAAA, shadow = false)
            }

            // 顶部按钮行（固定高度 18）
            Row(spacing = 8) {
                Button(
                    text = "按钮 A",
                    modifier = Modifier.EMPTY.width(60),
                    onClick = { /* demo */ }
                )
                Button(
                    text = "按钮 B",
                    modifier = Modifier.EMPTY.width(60),
                    onClick = { /* demo */ }
                )
                Text(
                    "Hover 我看 Tooltip！",
                    modifier = Modifier.EMPTY.width(gui.contentWidth - 140).padding(4, 0, 0, 0),
                    tooltip = listOf(Text.translatable("gui.ic2_120.compose_debug.tooltip_hover"))
                )
            }

            // Slot 锚点（固定高度 18）
            Flex(justifyContent = JustifyContent.SPACE_BETWEEN) {
                SlotHost(ComposeDebugScreenHandler.SLOT_LEFT_INDEX)
                SlotHost(ComposeDebugScreenHandler.SLOT_RIGHT_INDEX)
            }

            // 标签说明（固定高度 14）
            Text(
                "扫描结果（${results.size} 种矿物）:",
                color = 0xCCCCCC,
                shadow = false
            )

            // ScrollView（flex-1：填充根 Flex 剩余所有空间）
            ScrollView(
                width = gui.contentWidth,
                height = 1,
                modifier = Modifier.EMPTY.fractionHeight(1.0f)
            ) {
                Column(spacing = 2) {
                    for (entry in results) {
                        Text(
                            "${entry.name} × ${entry.count}",
                            color = 0xFFAA33,
                            shadow = false,
                            modifier = Modifier.EMPTY.height(14)
                        )
                    }
                }
            }

            // 底部状态栏（固定高度 14）
            Row(spacing = 4) {
                Text("行数: $itemCount", color = 0x666666, shadow = false)
                Text(" | ", color = 0x444444, shadow = false)
                Text("右键关闭", color = 0x666666, shadow = false)
            }
        }
    }

    private fun UiScope.SlotHost(slotIndex: Int, size: Int = 18, showBorder: Boolean = true) {
        SlotAnchor(slotAnchorId(slotIndex), width = size, height = size, showBorder = showBorder)
    }

    private fun applyAnchoredSlots(layout: ComposeUI.LayoutSnapshot, left: Int, top: Int) {
        handler.slots.forEachIndexed { index, slot ->
            val anchor = layout.anchors[slotAnchorId(index)] ?: return@forEachIndexed
            slot.x = anchor.x - left
            slot.y = anchor.y - top
        }
    }

    private fun slotAnchorId(slotIndex: Int): String = "compose.slot.$slotIndex"

    private fun buildDemoResults(): List<OreEntry> = buildList {
        for (i in 1..40) {
            val oreName = when (i % 8) {
                0 -> "煤矿" to 128
                1 -> "铁矿" to 64
                2 -> "铜矿" to 95
                3 -> "锡矿" to 87
                4 -> "银矿" to 31
                5 -> "铅矿" to 29
                6 -> "金矿" to 18
                else -> "钻石矿" to 7
            }
            add(OreEntry(oreName.first, oreName.second, i))
        }
    }
}
