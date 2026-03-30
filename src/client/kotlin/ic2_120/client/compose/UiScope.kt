package ic2_120.client.compose

import ic2_120.client.ui.GuiBackground
import ic2_120.client.ui.HeatProgressBar
import net.minecraft.util.Identifier

/**
 * DSL 作用域。每个容器的 content lambda 接收一个 UiScope，
 * 在其中调用 Column / Row / Text / Button 来添加子节点。
 */
@DslMarker
annotation class UiDsl

@UiDsl
class UiScope {
    @PublishedApi
    internal val children = mutableListOf<UiNode>()

    /** Shared node ID generator across the whole tree. Set by ComposeUI.render(). */
    var nodeIdGen: () -> Int = { -1 }

    fun Column(
        x: Int = 0,
        y: Int = 0,
        spacing: Int = 0,
        absolute: Boolean = false,
        modifier: Modifier = Modifier.EMPTY,
        content: UiScope.() -> Unit
    ) {
        // nodeIdGen is shared closure over parent's counter.
        // After inner content runs (which may call nodeIdGen for nested ScrollViews),
        // parent's counter = initial + N. We call nodeIdGen() once more for this Column's ID.
        val inner = UiScope().apply { nodeIdGen = this@UiScope.nodeIdGen }.apply(content)
        val nodeId = nodeIdGen()
        val node = ColumnNode(spacing, inner.children).apply {
            this.position = if (absolute) Position.Absolute(x, y) else Position.Flow(x, y)
            this.modifier = modifier
        }
        children += node
    }

    fun Row(
        x: Int = 0,
        y: Int = 0,
        spacing: Int = 0,
        absolute: Boolean = false,
        modifier: Modifier = Modifier.EMPTY,
        content: UiScope.() -> Unit
    ) {
        val inner = UiScope().apply { nodeIdGen = this@UiScope.nodeIdGen }.apply(content)
        val nodeId = nodeIdGen()
        val node = RowNode(spacing, inner.children).apply {
            this.position = if (absolute) Position.Absolute(x, y) else Position.Flow(x, y)
            this.modifier = modifier
        }
        children += node
    }

    fun Text(
        text: String,
        color: Int = 0xFFFFFF,
        shadow: Boolean = true,
        center: Boolean = false,
        tooltip: List<net.minecraft.text.Text>? = null,
        x: Int = 0,
        y: Int = 0,
        absolute: Boolean = false,
        modifier: Modifier = Modifier.EMPTY
    ) {
        val node = TextNode(text, color, shadow, center, tooltip).apply {
            this.position = if (absolute) Position.Absolute(x, y) else Position.Flow(x, y)
            this.modifier = modifier
        }
        children += node
    }

    fun Flex(
        x: Int = 0,
        y: Int = 0,
        direction: FlexDirection = FlexDirection.ROW,
        justifyContent: JustifyContent = JustifyContent.START,
        alignItems: AlignItems = AlignItems.START,
        gap: Int = 0,
        absolute: Boolean = false,
        modifier: Modifier = Modifier.EMPTY,
        content: UiScope.() -> Unit
    ) {
        val inner = UiScope().apply { nodeIdGen = this@UiScope.nodeIdGen }.apply(content)
        val nodeId = nodeIdGen()
        val node = FlexNode(direction, justifyContent, alignItems, gap, inner.children).apply {
            this.position = if (absolute) Position.Absolute(x, y) else Position.Flow(x, y)
            this.modifier = modifier
        }
        children += node
    }

    /**
     * 渲染一张纹理图片。
     *
     * 最简用法（完整纹理）：
     * ```
     * Image(Identifier("ic2", "textures/gui/icon.png"), width = 16, height = 16)
     * ```
     *
     * Sprite sheet 裁剪：
     * ```
     * Image(texture, width = 16, height = 16,
     *       u = 32f, v = 0f, regionWidth = 16, regionHeight = 16,
     *       textureWidth = 256, textureHeight = 256)
     * ```
     */
    fun Image(
        texture: Identifier,
        width: Int,
        height: Int,
        u: Float = 0f,
        v: Float = 0f,
        regionWidth: Int = width,
        regionHeight: Int = height,
        textureWidth: Int = width,
        textureHeight: Int = height,
        x: Int = 0,
        y: Int = 0,
        absolute: Boolean = false,
        modifier: Modifier = Modifier.EMPTY
    ) {
        val node = ImageNode(texture, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight).apply {
            this.position = if (absolute) Position.Absolute(x, y) else Position.Flow(x, y)
            this.modifier = modifier
        }
        children += node
    }

    /**
     * 渲染物品/方块图标（仅贴图，不绘制数量）。
     * 数量可用外部 DSL 拼接，例如：`Row(spacing = 4) { ItemStack(stack); Text("${stack.count}") }`
     */
    fun ItemStack(
        stack: net.minecraft.item.ItemStack,
        size: Int = 16,
        x: Int = 0,
        y: Int = 0,
        absolute: Boolean = false,
        modifier: Modifier = Modifier.EMPTY
    ) {
        val node = ItemStackNode(stack, size).apply {
            this.position = if (absolute) Position.Absolute(x, y) else Position.Flow(x, y)
            this.modifier = modifier
        }
        children += node
    }

    fun Button(
        text: String,
        x: Int = 0,
        y: Int = 0,
        absolute: Boolean = false,
        modifier: Modifier = Modifier.EMPTY,
        tooltip: List<net.minecraft.text.Text>? = null,
        onClick: () -> Unit = {}
    ) {
        val node = ButtonNode(text, onClick, tooltip).apply {
            this.position = if (absolute) Position.Absolute(x, y) else Position.Flow(x, y)
            this.modifier = modifier
        }
        children += node
    }

    /**
     * 表格布局。用 row { } 添加一行，行内按顺序放单元格；同一列对齐，行内垂直居中。
     *
     * @param columnWidths 可选固定列宽，如 listOf(28, 100) 表示第一列 28px、第二列 100px
     */
    fun Table(
        x: Int = 0,
        y: Int = 0,
        columnSpacing: Int = 8,
        rowSpacing: Int = 4,
        columnWidths: List<Int>? = null,
        absolute: Boolean = false,
        modifier: Modifier = Modifier.EMPTY,
        content: TableScope.() -> Unit
    ) {
        val scope = TableScope().apply {
            nodeIdGen = this@UiScope.nodeIdGen
        }.apply(content)
        val nodeId = nodeIdGen()
        val node = TableNode(scope.rows, columnSpacing, rowSpacing, columnWidths).apply {
            this.position = if (absolute) Position.Absolute(x, y) else Position.Flow(x, y)
            this.modifier = modifier
        }
        children += node
    }

    /**
     * 可复用的空背景面板（Compose 绘制，无 PNG）。
     * 与 [GuiBackground] 同风格，用于界面内局部背景或整屏背景。
     */
    fun Panel(
        width: Int,
        height: Int,
        fillColor: Int = GuiBackground.FILL_COLOR,
        borderColor: Int = GuiBackground.BORDER_COLOR,
        x: Int = 0,
        y: Int = 0,
        absolute: Boolean = false,
        modifier: Modifier = Modifier.EMPTY
    ) {
        val node = PanelNode(width, height, fillColor, borderColor).apply {
            this.position = if (absolute) Position.Absolute(x, y) else Position.Flow(x, y)
            this.modifier = modifier
        }
        children += node
    }

    /**
     * 滚动视图。支持鼠标滚轮滚动、点击 track 跳转、拖拽 thumb 滚动。
     * 注意：不支持嵌套 ScrollView（scissor 无法嵌套）。
     *
     * @param width          视口宽度（不含滚动条）；为 null 时使用父容器/约束推导
     * @param height         视口高度；为 null 时使用父容器/约束推导
     * @param scrollbarWidth 滚动条宽度，默认 6
     * @param contentEndInset 内容区与滚动条之间的间隙，默认 1
     * @param x              布局 x 偏移
     * @param y              布局 y 偏移
     * @param absolute       是否使用绝对定位
     * @param modifier       修饰符
     * @param content        子节点（内容高度可超出 [height]）
     */
    fun ScrollView(
        width: Int? = null,
        height: Int? = null,
        scrollbarWidth: Int = 6,
        contentEndInset: Int = 1,
        x: Int = 0,
        y: Int = 0,
        absolute: Boolean = false,
        modifier: Modifier = Modifier.EMPTY,
        content: UiScope.() -> Unit
    ) {
        val inner = UiScope().apply { nodeIdGen = this@UiScope.nodeIdGen }.apply(content)
        // nodeIdGen is called for any nested ScrollViews already; now call for this one
        val nodeId = nodeIdGen()
        val node = ScrollViewNode(
            nodeId = nodeId,
            scrollbarWidth = scrollbarWidth,
            contentEndInset = contentEndInset,
            children = inner.children
        ).apply {
            this.position = if (absolute) Position.Absolute(x, y) else Position.Flow(x, y)
            var resolvedModifier = modifier
            if (width != null) {
                resolvedModifier = resolvedModifier.width(width + scrollbarWidth + contentEndInset)
            }
            if (height != null) {
                resolvedModifier = resolvedModifier.height(height)
            }
            this.modifier = resolvedModifier
        }
        children += node
    }

    /**
     * Slot 锚点：仅参与布局并导出矩形，不执行绘制。
     */
    fun SlotAnchor(
        id: String,
        width: Int = 18,
        height: Int = 18,
        showBorder: Boolean = true,
        borderColor: Int = GuiBackground.BORDER_COLOR,
        x: Int = 0,
        y: Int = 0,
        absolute: Boolean = false,
        modifier: Modifier = Modifier.EMPTY
    ) {
        val node = SlotAnchorNode(id, width, height, showBorder, borderColor).apply {
            this.position = if (absolute) Position.Absolute(x, y) else Position.Flow(x, y)
            this.modifier = modifier
        }
        children += node
    }
}

/**
 * Table 的 DSL 作用域，仅支持 row { } 添加一行单元格。
 */
@UiDsl
class TableScope {
    @PublishedApi
    internal val rows = mutableListOf<List<UiNode>>()

    /** Shared node ID generator — propagate to inner UiScope. */
    var nodeIdGen: () -> Int = { -1 }

    fun row(content: UiScope.() -> Unit) {
        val inner = UiScope().apply { nodeIdGen = this@TableScope.nodeIdGen }.apply(content)
        rows += inner.children
    }
}
