package ic2_120.client.compose

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext

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

    /**
     * 构建节点树 → 测量 → 绘制，三阶段在一帧内完成。
     * [content] 中使用 [UiScope] DSL 声明 UI 结构。
     */
    fun render(
        drawContext: DrawContext,
        textRenderer: TextRenderer,
        mouseX: Int,
        mouseY: Int,
        content: UiScope.() -> Unit
    ) {
        renderCtx.drawContext = drawContext
        renderCtx.textRenderer = textRenderer
        renderCtx.mouseX = mouseX
        renderCtx.mouseY = mouseY
        renderCtx.buttonHits.clear()

        val scope = UiScope().apply(content)
        val root = RootNode(scope.children)

        val constraints = Constraints(
            maxWidth = drawContext.scaledWindowWidth,
            maxHeight = drawContext.scaledWindowHeight
        )
        root.measure(renderCtx, constraints)
        root.render(renderCtx, 0, 0)
    }

    /**
     * 在 Screen.mouseClicked 中调用，检测是否命中了某个 Button。
     * 返回 true 表示事件已消费。
     */
    fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0) return false
        val mx = mouseX.toInt()
        val my = mouseY.toInt()
        val hit = renderCtx.buttonHits.lastOrNull { h ->
            mx in h.x until (h.x + h.w) && my in h.y until (h.y + h.h)
        } ?: return false
        hit.onClick()
        return true
    }
}
