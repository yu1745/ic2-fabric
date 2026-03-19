package ic2_120.client.ui

import ic2_120.client.compose.*

/**
 * 横向进度条 UI 组件。
 * 支持渐变色填充、边框，可用于燃烧进度、周期进度等。
 * 基于 Compose 框架的 UiNode，通过 UiScope 扩展在 DSL 中使用。
 */
class ProgressBarNode(
    val fraction: Float,
    val barWidth: Int,
    val barHeight: Int,
    val startColor: Int = 0xFFCC0000.toInt(),
    val endColor: Int = 0xFFCC0000.toInt(),
    val gradient: Boolean = true
) : UiNode() {

    private var effectiveBarWidth: Int = barWidth

    override fun measure(ctx: RenderContext, constraints: Constraints) {
        val pad = modifier.padding
        measuredWidth = modifier.width ?: (barWidth + pad.horizontal)
        measuredHeight = modifier.height ?: (barHeight + pad.vertical)
        effectiveBarWidth = (measuredWidth - pad.horizontal).coerceAtLeast(0)
    }

    override fun render(ctx: RenderContext, originX: Int, originY: Int) {
        renderModifierDecoration(ctx, originX, originY)
        val pad = modifier.padding
        val x = originX + pad.left
        val y = originY + pad.top
        val w = effectiveBarWidth
        val h = barHeight

        // 背景
        ctx.drawContext.fill(x, y, x + w, y + h, 0xFF333333.toInt())

        val filledW = (fraction.coerceIn(0f, 1f) * w).toInt()
        if (filledW > 0) {
            ctx.drawContext.enableScissor(x, y, x + filledW, y + h)
            if (gradient) {
                val strips = maxOf(2, w)
                for (i in 0 until strips) {
                    val t = i.toFloat() / (strips - 1).coerceAtLeast(1)
                    val color = lerpArgb(startColor, endColor, t)
                    val x1 = x + (i * w / strips)
                    val x2 = x + ((i + 1) * w / strips).coerceAtMost(x + w)
                    ctx.drawContext.fill(x1, y, x2, y + h, color)
                }
            } else {
                ctx.drawContext.fill(x, y, x + filledW, y + h, endColor)
            }
            ctx.drawContext.disableScissor()
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
 * DSL：在 Compose 布局中插入横向进度条。
 *
 * @param fraction   当前进度比例，0f～1f
 * @param barWidth    条带宽度（默认 60）
 * @param barHeight   条带高度（默认 8）
 * @param startColor  渐变起始色（默认红色）
 * @param endColor    渐变结束色（默认红色）
 * @param gradient    是否渐变（默认 true）
 */
fun UiScope.HeatProgressBar(
    fraction: Float,
    barWidth: Int = 60,
    barHeight: Int = 8,
    startColor: Int = 0xFFCC0000.toInt(),
    endColor: Int = 0xFFCC0000.toInt(),
    gradient: Boolean = true,
    x: Int = 0,
    y: Int = 0,
    absolute: Boolean = false,
    modifier: Modifier = Modifier.EMPTY
) {
    val node = ProgressBarNode(fraction, barWidth, barHeight, startColor, endColor, gradient).apply {
        this.position = if (absolute) Position.Absolute(x, y) else Position.Flow(x, y)
        this.modifier = modifier
    }
    children += node
}
