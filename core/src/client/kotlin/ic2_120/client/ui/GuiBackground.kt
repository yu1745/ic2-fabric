package ic2_120.client.ui

import net.minecraft.client.gui.DrawContext

/**
 * 可复用的 GUI 空背景：纯 Compose 绘制，不使用 PNG 纹理。
 * 用于机器界面（电炉、MFSU 等）的统一背景样式。
 */
object GuiBackground {

    /**
     * 槽位程序化绘制时相对 [net.minecraft.screen.slot.Slot] 坐标的内缩（与原版容器一致）。
     * [drawVanillaLikeSlot]、[drawPlayerInventorySlotBorders] 均使用此值。
     */
    const val SLOT_ANCHOR_INSET = 1

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
    }

    private const val VANILLA_BASE_GRAY = 0xFFC6C6C6.toInt()
    private const val VANILLA_HILIGHT_OUTER = 0xFFFFFFFF.toInt()
    private const val VANILLA_HILIGHT_INNER = 0xFFE6E6E6.toInt()
    private const val VANILLA_SHADOW_INNER = 0xFF555555.toInt()
    private const val VANILLA_SHADOW_OUTER = 0xFF373737.toInt()
    private const val VANILLA_SLOT_INNER = 0xFF8B8B8B.toInt()
    private const val VANILLA_SLOT_DARK = 0xFF373737.toInt()
    private const val VANILLA_SLOT_HILIGHT = 0xFFFFFFFF.toInt()

    /**
     * 程序化复刻原版容器亮灰背景（无贴图空洞），并支持任意尺寸拉伸。
     */
    @JvmStatic
    fun drawVanillaLikePanel(
        context: DrawContext,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ) {
        val w = width.coerceAtLeast(4)
        val h = height.coerceAtLeast(4)
        val x1 = x + w
        val y1 = y + h

        // 主体填充：原版亮灰底色。
        context.fill(x, y, x1, y1, VANILLA_BASE_GRAY)

        // 外层浮雕边框。
        context.fill(x, y, x1, y + 1, VANILLA_HILIGHT_OUTER) // top
        context.fill(x, y, x + 1, y1, VANILLA_HILIGHT_OUTER) // left
        context.fill(x, y1 - 1, x1, y1, VANILLA_SHADOW_OUTER) // bottom
        context.fill(x1 - 1, y, x1, y1, VANILLA_SHADOW_OUTER) // right

        // 内层次级边框。
        if (w > 3 && h > 3) {
            context.fill(x + 1, y + 1, x1 - 1, y + 2, VANILLA_HILIGHT_INNER)
            context.fill(x + 1, y + 1, x + 2, y1 - 1, VANILLA_HILIGHT_INNER)
            context.fill(x + 1, y1 - 2, x1 - 1, y1 - 1, VANILLA_SHADOW_INNER)
            context.fill(x1 - 2, y + 1, x1 - 1, y1 - 1, VANILLA_SHADOW_INNER)
        }
    }

    /**
     * 程序化绘制原版风格槽位（亮左上、暗右下、灰色内底），可随尺寸拉伸。
     */
    @JvmStatic
    fun drawVanillaLikeSlot(
        context: DrawContext,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ) {
        val w = width.coerceAtLeast(4)
        val h = height.coerceAtLeast(4)
        val x1 = x + w
        val y1 = y + h

        // Vanilla-like slot: flat gray fill with dark top/left and light bottom/right.
        context.fill(x, y, x1, y1, VANILLA_SLOT_INNER)
        context.fill(x, y, x1, y + 1, VANILLA_SLOT_DARK)     // top
        context.fill(x, y, x + 1, y1, VANILLA_SLOT_DARK)     // left
        context.fill(x, y1 - 1, x1, y1, VANILLA_SLOT_HILIGHT) // bottom
        context.fill(x1 - 1, y, x1, y1, VANILLA_SLOT_HILIGHT) // right
    }

    private const val PLAYER_INV_COLS = 9
    private const val PLAYER_INV_MAIN_ROWS = 3
    private const val PLAYER_INV_HOTBAR_ROWS = 1
    private const val PLAYER_INV_START_X = 8

    /**
     * 绘制玩家背包所有槽位的边框（3 行主背包 + 1 行快捷栏）。
     * [screenX]、[screenY] 为 GUI 面板左上角坐标；[playerInvY]、[hotbarY] 为背包区域与快捷栏的 Y 偏移；[slotSize] 为槽尺寸（通常 18）。
     * [playerInvX] 为背包区域 X 偏移（默认 8，核反应堆等固定框界面可传入居中值）。
     */
    @JvmStatic
    fun drawPlayerInventorySlotBorders(
        context: DrawContext,
        screenX: Int,
        screenY: Int,
        playerInvY: Int,
        hotbarY: Int,
        slotSize: Int,
        borderColor: Int = BORDER_COLOR,
        playerInvX: Int = PLAYER_INV_START_X
    ) {
        val w = slotSize
        // 主背包 3 行 x 9 列
        for (row in 0 until PLAYER_INV_MAIN_ROWS) {
            for (col in 0 until PLAYER_INV_COLS) {
                val slotX = playerInvX + col * slotSize
                val slotY = playerInvY + row * slotSize
                drawVanillaLikeSlot(
                    context,
                    screenX + slotX - SLOT_ANCHOR_INSET,
                    screenY + slotY - SLOT_ANCHOR_INSET,
                    w,
                    w
                )
                if (borderColor != BORDER_COLOR) {
                    context.drawBorder(
                        screenX + slotX - SLOT_ANCHOR_INSET,
                        screenY + slotY - SLOT_ANCHOR_INSET,
                        w,
                        w,
                        borderColor
                    )
                }
            }
        }
        // 快捷栏 1 行 x 9 列
        for (col in 0 until PLAYER_INV_COLS) {
            val slotX = playerInvX + col * slotSize
            drawVanillaLikeSlot(
                context,
                screenX + slotX - SLOT_ANCHOR_INSET,
                screenY + hotbarY - SLOT_ANCHOR_INSET,
                w,
                w
            )
            if (borderColor != BORDER_COLOR) {
                context.drawBorder(
                    screenX + slotX - SLOT_ANCHOR_INSET,
                    screenY + hotbarY - SLOT_ANCHOR_INSET,
                    w,
                    w,
                    borderColor
                )
            }
        }
    }
}
