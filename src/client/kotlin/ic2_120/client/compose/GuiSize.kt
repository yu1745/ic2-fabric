package ic2_120.client.compose

/**
 * 项目通用 GUI 尺寸枚举，提供预定义的宽高组合。
 *
 * ## 尺寸分类
 *
 * | 枚举值 | 尺寸 | 适用场景 |
 * |---|---|---|
 * | [STANDARD] | 176×166 | 发电机、基础机器（无升级槽） |
 * | [STANDARD_UPGRADE] | 194×184 | 标准机器 + 4 个升级槽（176+18） |
 * | [TALL] | 194×214 | 加高机器，如灌装机（含流体槽） |
 * | [COMPACT] | 176×120 | 紧凑机器，如变压器 |
 * | [LARGE] | 256×256 | 大型 GUI，如扫描仪 |
 * | [DEBUG] | 240×200 | 调试面板 |
 *
 * ## 配合 Flex 布局使用
 *
 * 根节点使用 [GuiSize] 固定宽高，子组件中 ScrollView 使用 `fractionHeight(1.0f)` 填充剩余空间：
 * ```
 * val gui = GuiSize.STANDARD_UPGRADE
 * Flex(
 *     direction = FlexDirection.COLUMN,
 *     gap = 8,
 *     modifier = Modifier.EMPTY.width(gui.width).height(gui.height)
 * ) {
 *     // 固定头部
 *     Title()
 *     // flex-1: 填充剩余空间
 *     ScrollView(width = gui.contentWidth, modifier = Modifier.EMPTY.fractionHeight(1.0f)) { ... }
 *     // 固定底部
 *     StatusBar()
 * }
 * ```
 *
 * ## 扩展
 *
 * 如需自定义尺寸，使用 `GuiSize(宽度, 高度)` 构造：
 * ```
 * val custom = GuiSize(210, 180)
 * ```
 *
 * @param width  GUI 宽度（像素）
 * @param height GUI 高度（像素）
 */
enum class GuiSize(val width: Int, val height: Int) {

    /** 标准机器（176×166），如发电机、基础机器 */
    STANDARD(176, 166),

    /** 标准机器 + 升级槽（194×184），如压缩机、冶炼炉 */
    STANDARD_UPGRADE(176+18+8, 184),

    /** 加高机器（194×214），如灌装机 */
    // TALL(194, 214),

    /** 紧凑机器（176×120），如变压器 */
    // COMPACT(176, 120),

    /** 大型 GUI（256×256），如扫描仪 */
    LARGE(256, 256),

    /** 调试面板（240×200） */
    DEBUG(240, 200);

    /**
     * GUI 内容区域宽度，等于 [width] - 16（两侧各 8px padding）。
     */
    val contentWidth: Int get() = width - 16

    companion object {
        /** 玩家背包 Y 起始坐标（标准高度 GUI 中） */
        const val PLAYER_INVENTORY_Y = 84

        /** 快捷栏 Y 起始坐标 */
        const val HOTBAR_Y = 142

        /**
         * 根据玩家背包上方的主内容区高度，自动计算 GUI 总高度。
         * 用于需要动态高度的 GUI（如储物箱）。
         *
         * @param contentHeight 主内容区高度（不含玩家背包）
         * @return GUI 总高度 = contentHeight + 玩家背包区域高度(67) + 底部 padding(8)
         */
        fun computeHeight(contentHeight: Int): Int = contentHeight + 67 + 8
        val UPGRADE_COLUMN_WIDTH = STANDARD_UPGRADE.contentWidth - STANDARD.contentWidth
    }
}
