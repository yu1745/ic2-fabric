package ic2_120.client.compose

import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text

/**
 * Compose-like UI 入口。在 Screen 中持有一个实例，
 * 每帧调用 [render] 构建并绘制 UI 树，通过 [mouseClicked] 转发点击事件。
 *
 * ```kotlin
 * class MyScreen(...) : HandledScreen<...>(...) {
 *     private val ui = ComposeUI()
 *
 *     override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
 *         super.render(ctx, mouseX, mouseY, delta)
 *         ui.render(ctx, textRenderer, mouseX, mouseY) {
 *             Column(spacing = 4) {
 *                 Text("Hello")
 *                 Button("Click me") { println("clicked") }
 *             }
 *         }
 *     }
 *
 *     override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
 *         ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)
 * }
 * ```
 */
class ComposeUI {

    private val renderCtx = RenderContext()
    private var draggingNodeId: Int = -1
    private var thumbHit: Boolean = false
    private val scrollStepPixels: Int = 12
    data class LayoutSnapshot(val anchors: Map<String, RenderContext.AnchorRect>)

    /**
     * 构建节点树 → 测量 → 绘制，三阶段在一帧内完成。
     * [content] 中使用 [UiScope] DSL 声明 UI 结构。
     * [nodeIdGen] is passed from outside so ScrollView nodes get stable IDs across frames.
     */
    fun render(
        drawContext: DrawContext,
        textRenderer: TextRenderer,
        mouseX: Int,
        mouseY: Int,
        content: UiScope.() -> Unit
    ) {
        execute(drawContext, textRenderer, mouseX, mouseY, draw = true, interactive = true, content = content)
    }

    /**
     * 仅执行布局与放置，不绘制也不记录交互命中；用于在 super.render 前拿 slot 锚点。
     */
    fun layout(
        drawContext: DrawContext,
        textRenderer: TextRenderer,
        mouseX: Int,
        mouseY: Int,
        content: UiScope.() -> Unit
    ): LayoutSnapshot {
        execute(drawContext, textRenderer, mouseX, mouseY, draw = false, interactive = false, content = content)
        return LayoutSnapshot(renderCtx.anchors.toMap())
    }

    /**
     * 获取鼠标位置下的 tooltip 文本。在 [render] 之后调用，用于绘制悬停提示。
     * 返回 null 表示无 tooltip。
     */
    fun getTooltipAt(mouseX: Int, mouseY: Int): List<Text>? {
        return renderCtx.tooltipHits.lastOrNull { h ->
            mouseX in h.x until (h.x + h.w) && mouseY in h.y until (h.y + h.h)
        }?.lines
    }

    /**
     * 在 Screen.mouseClicked 中调用，检测是否命中了某个 Button。
     * 返回 true 表示事件已消费。
     */
    fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0) return false
        val mx = mouseX.toInt()
        val my = mouseY.toInt()

        // Button hits
        val btnHit = renderCtx.buttonHits.lastOrNull { h ->
            mx in h.x until (h.x + h.w) && my in h.y until (h.y + h.h)
        }
        if (btnHit != null) {
            MinecraftClient.getInstance().soundManager.play(
                PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f)
            )
            btnHit.onClick()
            return true
        }

        // ScrollView track click → jump to position
        val scrollHit = renderCtx.scrollHits.lastOrNull { h ->
            val inViewport = mx in h.viewportX until (h.viewportX + h.viewportW) &&
                my in h.viewportY until (h.viewportY + h.viewportH)
            val inTrack = mx in h.trackX until (h.trackX + h.trackW) &&
                my in h.trackY until (h.trackY + h.trackH)
            inViewport || inTrack
        }
        if (scrollHit != null) {
            val inTrack = mx in scrollHit.trackX until (scrollHit.trackX + scrollHit.trackW) &&
                my in scrollHit.trackY until (scrollHit.trackY + scrollHit.trackH)
            if (inTrack) {
                val onThumb = my in scrollHit.thumbY until (scrollHit.thumbY + scrollHit.thumbH)
                if (onThumb) {
                    // ScrollView thumb click → start drag
                    draggingNodeId = scrollHit.nodeId
                    thumbHit = true
                    return true
                }
                // Clicked track (not thumb): jump to clicked position
                val trackH = scrollHit.trackH
                val relative = (my - scrollHit.trackY).toFloat() / trackH
                val newScroll = (relative * scrollHit.maxScroll).toInt().coerceIn(0, scrollHit.maxScroll)
                renderCtx.setScrollOffset(scrollHit.nodeId, newScroll)
                return true
            }
        }

        return false
    }

    /**
     * 处理鼠标滚轮滚动。返回 true 表示已消费事件。
     */
    fun mouseScrolled(mouseX: Double, mouseY: Double,
                      horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (verticalAmount == 0.0) return false
        val mx = mouseX.toInt()
        val my = mouseY.toInt()
        val hit = renderCtx.scrollHits.lastOrNull { h ->
            val inViewport = mx in h.viewportX until (h.viewportX + h.viewportW) &&
                my in h.viewportY until (h.viewportY + h.viewportH)
            val inTrack = mx in h.trackX until (h.trackX + h.trackW) &&
                my in h.trackY until (h.trackY + h.trackH)
            inViewport || inTrack
        } ?: return false
        val delta = -verticalAmount.toInt().coerceIn(-1, 1) * scrollStepPixels
        val current = renderCtx.getScrollOffset(hit.nodeId)
        renderCtx.setScrollOffset(hit.nodeId, (current + delta).coerceIn(0, hit.maxScroll))
        return true
    }

    /**
     * 处理鼠标拖拽（用于滚动条 thumb 拖动）。返回 true 表示已消费事件。
     */
    fun mouseDragged(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!thumbHit || button != 0 || draggingNodeId < 0) return false
        val my = mouseY.toInt()
        val hit = renderCtx.scrollHits.lastOrNull { it.nodeId == draggingNodeId } ?: return false
        val trackH = hit.trackH
        if (trackH <= 0) return false
        val relative = (my - hit.trackY).toFloat() / trackH
        val newScroll = (relative * hit.maxScroll).toInt().coerceIn(0, hit.maxScroll)
        renderCtx.setScrollOffset(draggingNodeId, newScroll)
        return true
    }

    /**
     * 在 mouseReleased 中调用，停止 thumb 拖动。
     */
    fun stopDrag() {
        draggingNodeId = -1
        thumbHit = false
    }

    private fun execute(
        drawContext: DrawContext,
        textRenderer: TextRenderer,
        mouseX: Int,
        mouseY: Int,
        draw: Boolean,
        interactive: Boolean,
        content: UiScope.() -> Unit
    ) {
        var counter = 0
        val idGen: () -> Int = { counter++ }

        renderCtx.drawContext = drawContext
        renderCtx.textRenderer = textRenderer
        renderCtx.mouseX = mouseX
        renderCtx.mouseY = mouseY
        renderCtx.drawEnabled = draw
        renderCtx.interactionEnabled = interactive
        renderCtx.buttonHits.clear()
        renderCtx.tooltipHits.clear()
        renderCtx.scrollHits.clear()
        renderCtx.anchors.clear()

        val scope = UiScope().apply {
            nodeIdGen = idGen
        }.apply(content)
        val root = RootNode(scope.children)

        val constraints = Constraints(
            maxWidth = drawContext.scaledWindowWidth,
            maxHeight = drawContext.scaledWindowHeight
        )
        root.measure(renderCtx, constraints)
        root.render(renderCtx, 0, 0)
    }
}
