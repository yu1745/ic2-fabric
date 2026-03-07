package ic2_120.client.ui

import net.minecraft.client.gui.DrawContext

/**
 * 可复用的 GUI 空背景：纯 Compose 绘制，不使用 PNG 纹理。
 * 用于机器界面（电炉、MFSU 等）的统一背景样式。
 */
object GuiBackground {

    /** 内层填充色（深灰） */
    const val FILL_COLOR = 0xFF3C3C3C.toInt()

    /** 边框色（浅灰） */
    const val BORDER_COLOR = 0xFF8B8B8B.toInt()

    /** 外缘高光（更浅，可选） */
    const val HIGHLIGHT_COLOR = 0xFFAAAAAA.toInt()

    /**
     * 在指定区域绘制空背景：填充 + 单线边框。
     * 可在 [DrawContext] 的 drawBackground 中直接调用。
     */
    @JvmStatic
    fun draw(
        context: DrawContext,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        fillColor: Int = FILL_COLOR,
        borderColor: Int = BORDER_COLOR
    ) {
        context.fill(x, y, x + width, y + height, fillColor)
        context.drawBorder(x, y, width, height, borderColor)
        // drawPlayerInventorySlotBorders(context, x, y, width, height, borderColor)
    }

    private const val BORDER_OFFSET = 1
    private const val PLAYER_INV_COLS = 9
    private const val PLAYER_INV_MAIN_ROWS = 3
    private const val PLAYER_INV_HOTBAR_ROWS = 1
    private const val PLAYER_INV_START_X = 8

    /**
     * 绘制玩家背包所有槽位的边框（3 行主背包 + 1 行快捷栏）。
     * [screenX]、[screenY] 为 GUI 面板左上角坐标；[playerInvY]、[hotbarY] 为背包区域与快捷栏的 Y 偏移；[slotSize] 为槽尺寸（通常 18）。
     */
    @JvmStatic
    fun drawPlayerInventorySlotBorders(
        context: DrawContext,
        screenX: Int,
        screenY: Int,
        playerInvY: Int,
        hotbarY: Int,
        slotSize: Int,
        borderColor: Int = BORDER_COLOR
    ) {
        val w = slotSize
        // 主背包 3 行 x 9 列
        for (row in 0 until PLAYER_INV_MAIN_ROWS) {
            for (col in 0 until PLAYER_INV_COLS) {
                val slotX = PLAYER_INV_START_X + col * slotSize
                val slotY = playerInvY + row * slotSize
                context.drawBorder(
                    screenX + slotX - BORDER_OFFSET,
                    screenY + slotY - BORDER_OFFSET,
                    w,
                    w,
                    borderColor
                )
            }
        }
        // 快捷栏 1 行 x 9 列
        for (col in 0 until PLAYER_INV_COLS) {
            val slotX = PLAYER_INV_START_X + col * slotSize
            context.drawBorder(
                screenX + slotX - BORDER_OFFSET,
                screenY + hotbarY - BORDER_OFFSET,
                w,
                w,
                borderColor
            )
        }
    }
}
