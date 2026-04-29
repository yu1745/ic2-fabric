package ic2_120.content.screen

/**
 * 项目通用 GUI 尺寸枚举，提供预定义的宽高组合。
 *
 * ## 使用约定
 *
 * - **普通 HandledScreen**：与本枚举某档一致时，用 `val gui = GuiSize.XXX`，绘制玩家栏边框与 Compose 锚点
 *   优先使用 `gui.playerInvY` / `gui.hotbarY`，避免再手写 `84`/`142` 或与 Handler 重复两套数。
 * - **动态高度 GUI**（如可变行数储物箱）：用 [computeHeight] 得到总高，背包 Y 可与滚动区绑定，不要求与枚举
 *   [height] 互逆。
 * - **Screen 侧锚点布局**：槽位可在 Handler 里初始为 `0,0`，每帧由客户端 Screen 根据锚点写回 `slot.x/y`
 *   （见 `ReplicatorScreenHandler` 等）。
 * - **特例**：反应堆格网、附件小窗体等几何复杂界面可继续用 Handler 专用常量，不强行套单一公式。
 *
 * ## 不变量（建议保持）
 *
 * - [contentWidth] = [width] - 16（左右各 8px，与 [StandardGuiLayout] 一致）。
 * - [contentHeight] = [playerInvY] - 8（标题/顶边至玩家背包之间的主内容区）。
 * - [hotbarY] - [playerInvY] = 58（与 [StandardGuiLayout.HOTBAR_Y] - [StandardGuiLayout.PLAYER_INV_Y] 一致）。
 * - [UPGRADE_COLUMN_WIDTH] = [STANDARD_UPGRADE] 与 [STANDARD] 的 contentWidth 之差（等价于 8 间距 + 18 槽宽）。
 *
 * ## 尺寸分类
 *
 * | 枚举值 | 尺寸 | 适用场景 |
 * |---|---|---|
 * | [STANDARD] | 176×166 | 发电机、基础机器（无升级槽） |
 * | [STANDARD_UPGRADE] | 202×190 | 标准机器 + 4 个升级槽（176 + 8 间距 + 18 升级列） |
 * | [STANDARD_TALL] | 176×190 | 标准宽度 + 加高 |
 * | [UPGRADE_TALL] | 202×220 | 升级机器 + 加高（与 STANDARD_UPGRADE 同宽，220 高） |
 * | [ATTACHMENT] | 176×140 | 附件类小界面 |
 * | [LARGE] | 256×256 | 大型 GUI，如扫描仪 |
 * | [DEBUG] | 240×200 | 调试面板 |
 * | [REACTOR] | 220×220 | 核反应堆基础框体宽度 |
 * | [COMPACT] | 176×120 | 预留紧凑尺寸；若启用需重新定义玩家栏 Y 与总高关系 |
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
enum class GuiSize(
    /** GUI 宽度（像素） */
    val width: Int,

    /** GUI 高度（像素） */
    val height: Int
) {
    /** 标准机器（176×166），如发电机、基础机器 */
    STANDARD(StandardGuiLayout.WIDTH, StandardGuiLayout.HEIGHT),

    /** 标准机器 + 升级槽（202×190），如压缩机、冶炼炉 */
    STANDARD_UPGRADE(StandardGuiLayout.WIDTH + 8 + StandardGuiLayout.SLOT_SIZE, 190),

    /** 标准宽度 + 加高（176×190） */
    STANDARD_TALL(STANDARD.width, 190),

    /** 升级机器 + 加高（与 STANDARD_UPGRADE 同宽，220 高） */
    UPGRADE_TALL(STANDARD_UPGRADE.width, 220),

    /** 附件类小界面（176×140） */
    ATTACHMENT(176, 140),

    /** 核反应堆基础框体宽度（220×220） */
    REACTOR(220, 220),

    /**
     * 紧凑尺寸（176×120），当前工程中较少使用；使用专用背包 Y（38）以保证底部仍有标准留白。
     */
    COMPACT(176, 120),

    /** 大型 GUI（256×256），如扫描仪 */
    LARGE(256, 256),

    /** 调试面板（240×200） */
    DEBUG(240, 200);

    /**
     * GUI 内容区域宽度，等于 [width] - 16（两侧各 8px padding）。
     */
    val contentWidth: Int get() = width - 16

    /**
     * GUI 内容区域高度，等于玩家背包上方可用的垂直空间。
     */
    val contentHeight: Int get() = playerInvY - 8

    /**
     * 玩家背包起始 Y 坐标。
     *
     * 这里不直接按 `height - StandardGuiLayout.HEIGHT` 线性推导，
     * 而是对齐项目内已有 ScreenHandler 的实际布局常量，
     * 避免升级列/加高界面与老界面出现 6px 左右的系统性错位。
     */
    val playerInvY: Int get() = when (this) {
        STANDARD, LARGE, DEBUG, REACTOR -> StandardGuiLayout.PLAYER_INV_Y
        STANDARD_UPGRADE, STANDARD_TALL -> 108
        UPGRADE_TALL -> 138
        ATTACHMENT -> 58
        COMPACT -> 38
    }

    /**
     * 快捷栏起始 Y 坐标，随 GUI 高度自动偏移。
     */
    val hotbarY: Int get() = playerInvY + (StandardGuiLayout.HOTBAR_Y - StandardGuiLayout.PLAYER_INV_Y)

    companion object {
        /** 玩家背包 Y 起始坐标（标准高度 GUI 中） */
        const val PLAYER_INVENTORY_Y = StandardGuiLayout.PLAYER_INV_Y

        /** 快捷栏 Y 起始坐标 */
        const val HOTBAR_Y = StandardGuiLayout.HOTBAR_Y

        /** 槽位边长（与 [StandardGuiLayout.SLOT_SIZE] 一致） */
        const val SLOT_SIZE = StandardGuiLayout.SLOT_SIZE

        /**
         * 根据玩家背包上方的主内容区高度，自动计算 GUI 总高度。
         * 用于需要动态高度的 GUI（如储物箱）。
         *
         * @param contentHeight 主内容区高度（不含玩家背包）
         * @return GUI 总高度 = contentHeight + 顶部间距(8) + 玩家背包与快捷栏(76)
         */
        fun computeHeight(contentHeight: Int): Int = contentHeight + 84

        /** 升级槽列宽度（STANDARD_UPGRADE.contentWidth - STANDARD.contentWidth） */
        val UPGRADE_COLUMN_WIDTH: Int get() = STANDARD_UPGRADE.contentWidth - STANDARD.contentWidth
    }
}
