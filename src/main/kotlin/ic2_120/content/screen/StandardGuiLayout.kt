package ic2_120.content.screen

/**
 * 与客户端 [ic2_120.client.compose.GuiSize.STANDARD] 一致的面板与玩家栏槽位布局。
 * 供主模块 ScreenHandler 使用（不可依赖 client 源的 [ic2_120.client.compose.GuiSize]）。
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
