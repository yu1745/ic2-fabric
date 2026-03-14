package ic2_120.client.ui

import ic2_120.client.compose.*

/**
 * 流体条 UI 组件。支持水平和竖直方向。
 * 空的一端为灰，满的一端为蓝色（水）；仅显示 [0, fraction] 的填充部分。
 * 基于 Compose 框架的 UiNode，通过 UiScope 扩展在 DSL 中使用。
 */

class FluidBarNode(
    val fraction: Float,
    val barWidth: Int,
    val barHeight: Int,
    val emptyColor: Int = 0xFF666666.toInt(),
    val fullColor: Int = 0xFF0066CC.toInt(),  // 蓝色代表水
    val gradient: Boolean = true,
    val vertical: Boolean = false  // 是否竖直显示
) : UiNode() {

    /** 实际绘制宽度：若 modifier 指定了 width 则自适应，否则用 barWidth（竖直时用 barHeight 作为宽度） */
    private var effectiveBarWidth: Int = if (vertical) barHeight else barWidth
    /** 实际绘制高度：若 modifier 指定了 height 则自适应，否则用 barHeight（竖直时用 barWidth 作为高度） */
    private var effectiveBarHeight: Int = if (vertical) barWidth else barHeight

    override fun measure(ctx: RenderContext, constraints: Constraints) {
        val pad = modifier.padding
        measuredWidth = modifier.width ?: (effectiveBarWidth + pad.horizontal)
        measuredHeight = modifier.height ?: (effectiveBarHeight + pad.vertical)
        effectiveBarWidth = (measuredWidth - pad.horizontal).coerceAtLeast(0)
        effectiveBarHeight = (measuredHeight - pad.vertical).coerceAtLeast(0)
    }

    override fun render(ctx: RenderContext, originX: Int, originY: Int) {
        renderModifierDecoration(ctx, originX, originY)
        val pad = modifier.padding
        val x = originX + pad.left
        val y = originY + pad.top
        val w = effectiveBarWidth
        val h = effectiveBarHeight

        // 背景（深灰）
        ctx.drawContext.fill(x, y, x + w, y + h, 0xFF333333.toInt())

        if (vertical) {
            // 竖直方向：从下往上填充
            val filledH = (fraction.coerceIn(0f, 1f) * h).toInt()
            if (filledH > 0) {
                ctx.drawContext.enableScissor(x, y + h - filledH, x + w, y + h)
                if (gradient) {
                    val strips = maxOf(2, h)
                    for (i in 0 until strips) {
                        val t = i.toFloat() / (strips - 1).coerceAtLeast(1)
                        val color = lerpArgb(emptyColor, fullColor, t)
                        val y1 = y + h - (i * h / strips)
                        val y2 = y + h - ((i + 1) * h / strips).coerceAtLeast(y)
                        ctx.drawContext.fill(x, y1, x + w, y2, color)
                    }
                } else {
                    ctx.drawContext.fill(x, y + h - filledH, x + w, y + h, fullColor)
                }
                ctx.drawContext.disableScissor()
            }
        } else {
            // 水平方向：从左往右填充
            val filledW = (fraction.coerceIn(0f, 1f) * w).toInt()
            if (filledW > 0) {
                ctx.drawContext.enableScissor(x, y, x + filledW, y + h)
                if (gradient) {
                    val strips = maxOf(2, w)
                    for (i in 0 until strips) {
                        val t = i.toFloat() / (strips - 1).coerceAtLeast(1)
                        val color = lerpArgb(emptyColor, fullColor, t)
                        val x1 = x + (i * w / strips)
                        val x2 = x + ((i + 1) * w / strips).coerceAtMost(x + w)
                        ctx.drawContext.fill(x1, y, x2, y + h, color)
                    }
                } else {
                    ctx.drawContext.fill(x, y, x + filledW, y + h, fullColor)
                }
                ctx.drawContext.disableScissor()
            }
        }

        ctx.drawContext.drawBorder(x, y, w, h, 0xFF888888.toInt())
    }

    private fun lerpArgb(a: Int, b: Int, t: Float): Int {
        val aa = (a shr 24) and 0xFF
        val ar = (a shr 16) and 0xFF
        val ag = (a shr 8) and 0xFF
        val ab = a and 0xFF
        val ba = (b shr 24) and 0xFF
        val br = (b shr 16) and 0xFF
        val bg = (b shr 8) and 0xFF
        val bb = b and 0xFF
        val u = t.coerceIn(0f, 1f)
        return ((aa + (ba - aa) * u).toInt() and 0xFF shl 24) or
            ((ar + (br - ar) * u).toInt() and 0xFF shl 16) or
            ((ag + (bg - ag) * u).toInt() and 0xFF shl 8) or
            ((ab + (bb - ab) * u).toInt() and 0xFF)
    }
}

/**
 * DSL：在 Compose 布局中插入流体条。
 *
 * @param fraction 当前流体比例，0f～1f
 * @param barWidth  条带宽度（默认 100，竖直时这是高度）
 * @param barHeight 条带高度（默认 8，竖直时这是宽度）
 * @param gradient 是否渐变（true=灰到蓝，false=纯色）
 * @param vertical 是否竖直显示（默认false=水平）
 */
fun UiScope.FluidBar(
    fraction: Float,
    barWidth: Int = 100,
    barHeight: Int = 8,
    emptyColor: Int = 0xFF666666.toInt(),
    fullColor: Int = 0xFF0066CC.toInt(),
    gradient: Boolean = true,
    vertical: Boolean = false,
    x: Int = 0,
    y: Int = 0,
    absolute: Boolean = false,
    modifier: Modifier = Modifier.EMPTY
) {
    val node = FluidBarNode(fraction, barWidth, barHeight, emptyColor, fullColor, gradient, vertical).apply {
        this.position = if (absolute) Position.Absolute(x, y) else Position.Flow(x, y)
        this.modifier = modifier
    }
    children += node
}
