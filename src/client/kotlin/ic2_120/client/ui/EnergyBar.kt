package ic2_120.client.ui

import ic2_120.client.compose.*

/**
 * 能量条 UI 组件。渐变色：没电一端为红，满电一端为绿；仅显示 [0, fraction] 的填充部分。
 * 基于 Compose 框架的 UiNode，通过 UiScope 扩展在 DSL 中使用。
 */
enum class EnergyBarOrientation {
    HORIZONTAL,
    VERTICAL
}

class EnergyBarNode(
    val fraction: Float,
    val barWidth: Int?,
    val barHeight: Int?,
    val orientation: EnergyBarOrientation = EnergyBarOrientation.HORIZONTAL,
    val shortEdge: Int = 16,
    val emptyColor: Int = 0xFFCC0000.toInt(),
    val fullColor: Int = 0xFF00CC00.toInt(),
    val gradient: Boolean = true
) : UiNode() {

    private var effectiveBarWidth: Int = 0
    private var effectiveBarHeight: Int = 0

    override fun measure(ctx: RenderContext, constraints: Constraints) {
        val pad = modifier.padding
        val boundedMaxW = if (constraints.maxWidth == Int.MAX_VALUE) null else constraints.maxWidth
        val boundedMaxH = if (constraints.maxHeight == Int.MAX_VALUE) null else constraints.maxHeight

        val defaultW = when (orientation) {
            EnergyBarOrientation.HORIZONTAL -> boundedMaxW ?: 100
            EnergyBarOrientation.VERTICAL -> shortEdge
        }
        val defaultH = when (orientation) {
            EnergyBarOrientation.HORIZONTAL -> shortEdge
            EnergyBarOrientation.VERTICAL -> boundedMaxH ?: 100
        }

        measuredWidth = modifier.width ?: (barWidth ?: defaultW) + pad.horizontal
        measuredHeight = modifier.height ?: (barHeight ?: defaultH) + pad.vertical
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

        val clamped = fraction.coerceIn(0f, 1f)
        val fillW = if (orientation == EnergyBarOrientation.HORIZONTAL) (clamped * w).toInt() else w
        val fillH = if (orientation == EnergyBarOrientation.VERTICAL) (clamped * h).toInt() else h
        if (fillW > 0 && fillH > 0) {
            if (orientation == EnergyBarOrientation.HORIZONTAL) {
                ctx.drawContext.enableScissor(x, y, x + fillW, y + h)
            } else {
                ctx.drawContext.enableScissor(x, y + (h - fillH), x + w, y + h)
            }
            if (gradient) {
                val strips = if (orientation == EnergyBarOrientation.HORIZONTAL) maxOf(2, w) else maxOf(2, h)
                for (i in 0 until strips) {
                    val t = i.toFloat() / (strips - 1).coerceAtLeast(1)
                    val color = lerpArgb(emptyColor, fullColor, t)
                    if (orientation == EnergyBarOrientation.HORIZONTAL) {
                        val x1 = x + (i * w / strips)
                        val x2 = x + ((i + 1) * w / strips).coerceAtMost(x + w)
                        ctx.drawContext.fill(x1, y, x2, y + h, color)
                    } else {
                        val y2 = y + h - (i * h / strips)
                        val y1 = y + h - ((i + 1) * h / strips)
                        ctx.drawContext.fill(x, y1, x + w, y2, color)
                    }
                }
            } else {
                if (orientation == EnergyBarOrientation.HORIZONTAL) {
                    ctx.drawContext.fill(x, y, x + fillW, y + h, fullColor)
                } else {
                    ctx.drawContext.fill(x, y + (h - fillH), x + w, y + h, fullColor)
                }
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
 * @param barWidth  可选宽度；为空时自动按方向推导
 * @param barHeight 可选高度；为空时自动按方向推导
 * @param orientation 方向（默认横向）
 * @param shortEdge 未指定宽高时短边长度（默认 16）
 * @param gradient 是否渐变（true=红到绿，false=纯色）
 */
fun UiScope.EnergyBar(
    fraction: Float,
    barWidth: Int? = null,
    barHeight: Int? = null,
    orientation: EnergyBarOrientation = EnergyBarOrientation.HORIZONTAL,
    shortEdge: Int = 16,
    emptyColor: Int = 0xFFCC0000.toInt(),
    fullColor: Int = 0xFF00CC00.toInt(),
    gradient: Boolean = true,
    x: Int = 0,
    y: Int = 0,
    absolute: Boolean = false,
    modifier: Modifier = Modifier.EMPTY
) {
    val node = EnergyBarNode(
        fraction = fraction,
        barWidth = barWidth,
        barHeight = barHeight,
        orientation = orientation,
        shortEdge = shortEdge,
        emptyColor = emptyColor,
        fullColor = fullColor,
        gradient = gradient
    ).apply {
        this.position = if (absolute) Position.Absolute(x, y) else Position.Flow(x, y)
        this.modifier = modifier
    }
    children += node
}
