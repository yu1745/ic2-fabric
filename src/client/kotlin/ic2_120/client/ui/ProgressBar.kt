package ic2_120.client.ui

import net.minecraft.client.gui.DrawContext

/**
 * 原版风格的进度条：深色底、浅色填充、细边框，从左到右填充。
 * 用于熔炉烧炼进度等，可直接在 drawBackground 中按坐标绘制。
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
     */
    @JvmStatic
    fun draw(
        context: DrawContext,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        fraction: Float
    ) {
        val f = fraction.coerceIn(0f, 1f)
        context.fill(x, y, x + width, y + height, BG_COLOR)
        val filledW = (f * width).toInt()
        if (filledW > 0) {
            context.fill(x, y, x + filledW, y + height, FILL_COLOR)
        }
        context.drawBorder(x, y, width, height, BORDER_COLOR)
    }
}
