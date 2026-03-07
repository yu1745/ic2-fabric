package ic2_120.client.compose

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

    fun Column(
        x: Int = 0,
        y: Int = 0,
        spacing: Int = 0,
        absolute: Boolean = false,
        modifier: Modifier = Modifier.EMPTY,
        content: UiScope.() -> Unit
    ) {
        val inner = UiScope().apply(content)
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
        val inner = UiScope().apply(content)
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
        x: Int = 0,
        y: Int = 0,
        absolute: Boolean = false,
        modifier: Modifier = Modifier.EMPTY
    ) {
        val node = TextNode(text, color, shadow).apply {
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
        val inner = UiScope().apply(content)
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

    fun Button(
        text: String,
        x: Int = 0,
        y: Int = 0,
        absolute: Boolean = false,
        modifier: Modifier = Modifier.EMPTY,
        onClick: () -> Unit = {}
    ) {
        val node = ButtonNode(text, onClick).apply {
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
        val scope = TableScope().apply(content)
        val node = TableNode(scope.rows, columnSpacing, rowSpacing, columnWidths).apply {
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

    fun row(content: UiScope.() -> Unit) {
        val inner = UiScope().apply(content)
        rows += inner.children
    }
}
