package ic2_120.client.ui

import net.minecraft.client.gui.DrawContext

/**
 * 原版风格的进度条：深色底、浅色填充、细边框。
 * 支持横向和竖向进度条，可用于燃烧进度、能量条等。
 */
object ProgressBar {

    /** 背景色（原版 GUI 深灰） */
    const val BG_COLOR = 0xFF555555.toInt()

    /** 填充色（原版熔炉箭头/火焰的暖色） */
    const val FILL_COLOR = 0xFFB0B0B0.toInt()

    /** 边框色（与 GuiBackground 一致） */
    const val BORDER_COLOR = 0xFF8B8B8B.toInt()

    /**
     * 在指定区域绘制横向进度条。
     * @param context DrawContext（如 drawBackground 的 context）
     * @param x 左上角 X（屏幕坐标）
     * @param y 左上角 Y
     * @param width 总宽度
     * @param height 高度（建议 8～10）
     * @param fraction 进度 0f～1f
     * @param gradient 是否渐变（true=从左到右渐变，false=纯色）
     * @param startColor 渐变起始色（仅 gradient=true 时生效，左端）
     * @param endColor 渐变结束色（仅 gradient=true 时生效，右端）
     * @param solidColor 纯色填充色（仅 gradient=false 时生效）
     */
    @JvmStatic
    fun draw(
        context: DrawContext,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        fraction: Float,
        gradient: Boolean = false,
        startColor: Int = 0xFFCC0000.toInt(),
        endColor: Int = 0xFF00CC00.toInt(),
        solidColor: Int = FILL_COLOR
    ) {
        val f = fraction.coerceIn(0f, 1f)
        context.fill(x, y, x + width, y + height, BG_COLOR)
        val filledW = (f * width).toInt()
        if (filledW > 0) {
            if (gradient) {
                val strips = maxOf(2, filledW)
                for (i in 0 until strips) {
                    val t = i.toFloat() / (strips - 1).coerceAtLeast(1)
                    val color = interpolateColor(startColor, endColor, t)
                    val x1 = x + (i * filledW / strips)
                    val x2 = x + ((i + 1) * filledW / strips).coerceAtMost(x + filledW)
                    context.fill(x1, y, x2, y + height, color)
                }
            } else {
                context.fill(x, y, x + filledW, y + height, solidColor)
            }
        }
        context.drawBorder(x, y, width, height, BORDER_COLOR)
    }

    /** 岩浆条纯色（橙红） */
    const val LAVA_SOLID_COLOR = 0xFFCC4400.toInt()

    /** 水/流体条纯色（蓝） */
    const val WATER_SOLID_COLOR = 0xFF4488CC.toInt()

    /**
     * 绘制竖向燃料/容量条。
     * @param context DrawContext
     * @param x 左上角 X
     * @param y 左上角 Y
     * @param width 宽度（建议 6～12）
     * @param height 总高度
     * @param fraction 剩余比例 0f～1f（1=满，0=空）
     * @param gradient 是否渐变（true=红到蓝，false=纯色）
     * @param solidColor 非渐变时的填充色（仅 gradient=false 时生效）
     * @param showTicks 是否绘制刻度线（25%、50%、75% 位置）
     */
    @JvmStatic
    fun drawVerticalFuelBar(
        context: DrawContext,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        fraction: Float,
        gradient: Boolean = true,
        solidColor: Int = LAVA_SOLID_COLOR,
        showTicks: Boolean = false
    ) {
        val f = fraction.coerceIn(0f, 1f)

        // 绘制背景
        context.fill(x, y, x + width, y + height, BG_COLOR)

        // 计算填充高度
        val filledH = (f * height).toInt()
        if (filledH > 0) {
            if (gradient) {
                // 从底部向上绘制渐变（红 -> 蓝）
                for (row in 0 until filledH) {
                    val progress = row.toFloat() / filledH.coerceAtLeast(1)
                    val color = interpolateColor(0xFFFF0000.toInt(), 0xFF0000FF.toInt(), progress)
                    val drawY = y + height - filledH + row
                    context.fill(x, drawY, x + width, drawY + 1, color)
                }
            } else {
                context.fill(x, y + height - filledH, x + width, y + height, solidColor)
            }
        }

        // 刻度线：25%、50%、75% 位置（在边框内绘制）
        if (showTicks) {
            for (pct in 1..3) {
                val tickY = y + (height * (4 - pct) / 4)
                context.fill(x - 2, tickY, x, tickY + 1, BORDER_COLOR)
            }
        }

        // 绘制边框
        context.drawBorder(x, y, width, height, BORDER_COLOR)
    }

    /**
     * 在两个 ARGB 颜色之间插值
     * @param color1 起始颜色（ARGB 格式，如 0xFFFF0000）
     * @param color2 结束颜色（ARGB 格式，如 0xFF0000FF）
     * @param t 插值因子 0f～1f
     * @return 插值后的 ARGB 颜色
     */
    private fun interpolateColor(color1: Int, color2: Int, t: Float): Int {
        val a1 = (color1 shr 24) and 0xFF
        val r1 = (color1 shr 16) and 0xFF
        val g1 = (color1 shr 8) and 0xFF
        val b1 = color1 and 0xFF

        val a2 = (color2 shr 24) and 0xFF
        val r2 = (color2 shr 16) and 0xFF
        val g2 = (color2 shr 8) and 0xFF
        val b2 = color2 and 0xFF

        val a = (a1 + (a2 - a1) * t).toInt().coerceIn(0, 255)
        val r = (r1 + (r2 - r1) * t).toInt().coerceIn(0, 255)
        val g = (g1 + (g2 - g1) * t).toInt().coerceIn(0, 255)
        val b = (b1 + (b2 - b1) * t).toInt().coerceIn(0, 255)

        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}
