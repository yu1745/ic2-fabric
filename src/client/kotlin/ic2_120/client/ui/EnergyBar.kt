package ic2_120.client.ui

import ic2_120.client.compose.*

/**
 * 能量条 UI 组件。渐变色：没电一端为红，满电一端为绿；仅显示 [0, fraction] 的填充部分。
 * 基于 Compose 框架的 UiNode，通过 UiScope 扩展在 DSL 中使用。
 */

class EnergyBarNode(
    val fraction: Float,
    val barWidth: Int,
    val barHeight: Int,
    val emptyColor: Int = 0xFFCC0000.toInt(),
    val fullColor: Int = 0xFF00CC00.toInt()
) : UiNode() {

    override fun measure(ctx: RenderContext, constraints: Constraints) {
        val pad = modifier.padding
        measuredWidth = modifier.width ?: (barWidth + pad.horizontal)
        measuredHeight = modifier.height ?: (barHeight + pad.vertical)
    }

    override fun render(ctx: RenderContext, originX: Int, originY: Int) {
        renderModifierDecoration(ctx, originX, originY)
        val pad = modifier.padding
        val x = originX + pad.left
        val y = originY + pad.top
        val w = barWidth
        val h = barHeight

        // 背景（深灰）
        ctx.drawContext.fill(x, y, x + w, y + h, 0xFF333333.toInt())

        val filledW = (fraction.coerceIn(0f, 1f) * w).toInt()
        if (filledW > 0) {
            ctx.drawContext.enableScissor(x, y, x + filledW, y + h)
            // 用竖条模拟从左（红）到右（绿）的渐变
            val strips = maxOf(2, w)
            for (i in 0 until strips) {
                val t = i.toFloat() / (strips - 1).coerceAtLeast(1)
                val color = lerpArgb(emptyColor, fullColor, t)
                val x1 = x + (i * w / strips)
                val x2 = x + ((i + 1) * w / strips).coerceAtMost(x + w)
                ctx.drawContext.fill(x1, y, x2, y + h, color)
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
 * DSL：在 Compose 布局中插入能量条。
 *
 * @param fraction 当前能量比例，0f～1f
 * @param barWidth  条带宽度（默认 100）
 * @param barHeight 条带高度（默认 8）
 */
fun UiScope.EnergyBar(
    fraction: Float,
    barWidth: Int = 100,
    barHeight: Int = 8,
    emptyColor: Int = 0xFFCC0000.toInt(),
    fullColor: Int = 0xFF00CC00.toInt(),
    x: Int = 0,
    y: Int = 0,
    absolute: Boolean = false,
    modifier: Modifier = Modifier.EMPTY
) {
    val node = EnergyBarNode(fraction, barWidth, barHeight, emptyColor, fullColor).apply {
        this.position = if (absolute) Position.Absolute(x, y) else Position.Flow(x, y)
        this.modifier = modifier
    }
    children += node
}
