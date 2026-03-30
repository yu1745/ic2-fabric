package ic2_120.content.screen

/**
 * 标准 176×166 GUI 的面板与玩家栏槽位几何（与 [GuiSize.STANDARD] 一致）。
 *
 * 主模块 ScreenHandler 应优先从此处引用槽位常量；整体尺寸与按枚举分档的
 * [GuiSize.playerInvY] / [GuiSize.hotbarY] 见 [GuiSize] 文档。
 */
object StandardGuiLayout {
    const val WIDTH = 176
    const val HEIGHT = 166
    const val PLAYER_INV_X = 8
    const val PLAYER_INV_Y = 84
    const val HOTBAR_Y = 142
    const val SLOT_SIZE = 18

    private const val HORIZONTAL_PADDING = 8
    val contentWidth: Int get() = WIDTH - 2 * HORIZONTAL_PADDING

    /** 标题区下方第一行机器槽常用 Y */
    const val FIRST_MACHINE_ROW_Y = 18

    /** 在标准内容宽内将 `columns` 列槽位水平居中后的左边缘 X */
    fun centeredSlotGridStartX(columns: Int): Int =
        PLAYER_INV_X + (contentWidth - columns * SLOT_SIZE) / 2
}
