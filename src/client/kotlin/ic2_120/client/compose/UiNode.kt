package ic2_120.client.compose

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.util.Identifier

/**
 * 渲染上下文，在整棵节点树中共享，避免逐层传参。
 * measure 阶段设置 textRenderer；render 阶段额外设置 drawContext / mouseX / mouseY。
 */
class RenderContext {
    lateinit var textRenderer: TextRenderer
    lateinit var drawContext: DrawContext
    var mouseX: Int = 0
    var mouseY: Int = 0

    val buttonHits = mutableListOf<ButtonHit>()

    data class ButtonHit(
        val x: Int, val y: Int,
        val w: Int, val h: Int,
        val onClick: () -> Unit
    )
}

// ──────────────────────────── Base ────────────────────────────

abstract class UiNode {
    var position: Position = Position.Flow()
    var modifier: Modifier = Modifier.EMPTY
    var measuredWidth: Int = 0
    var measuredHeight: Int = 0

    val isAbsolute get() = position is Position.Absolute

    /** Pass 1: 自底向上测量。节点将结果写入 measuredWidth / measuredHeight。 */
    abstract fun measure(ctx: RenderContext, constraints: Constraints)

    /** Pass 2: 自顶向下绘制。originX/originY 是父容器分配给本节点的左上角坐标。 */
    abstract fun render(ctx: RenderContext, originX: Int, originY: Int)

    /** 绘制 Modifier 上的背景与边框（容器和叶子共用）。 */
    protected fun renderModifierDecoration(ctx: RenderContext, x: Int, y: Int) {
        modifier.backgroundColor?.let { color ->
            ctx.drawContext.fill(x, y, x + measuredWidth, y + measuredHeight, color)
        }
        modifier.borderColor?.let { color ->
            ctx.drawContext.drawBorder(x, y, measuredWidth, measuredHeight, color)
        }
    }
}

// ──────────────────────────── Text ────────────────────────────

class TextNode(
    val text: String,
    val color: Int = 0xFFFFFF,
    val shadow: Boolean = true
) : UiNode() {

    override fun measure(ctx: RenderContext, constraints: Constraints) {
        val pad = modifier.padding
        val textW = ctx.textRenderer.getWidth(text)
        val textH = ctx.textRenderer.fontHeight
        measuredWidth = modifier.width ?: (textW + pad.horizontal)
        measuredHeight = modifier.height ?: (textH + pad.vertical)
    }

    override fun render(ctx: RenderContext, originX: Int, originY: Int) {
        renderModifierDecoration(ctx, originX, originY)
        val pad = modifier.padding
        val tx = originX + pad.left
        val ty = originY + pad.top
        if (shadow) {
            ctx.drawContext.drawTextWithShadow(ctx.textRenderer, text, tx, ty, color)
        } else {
            ctx.drawContext.drawText(ctx.textRenderer, text, tx, ty, color, false)
        }
    }
}

// ──────────────────────────── Image ────────────────────────────

/**
 * 纹理图片节点。渲染指定 Identifier 的 PNG 纹理（或其中一个区域）。
 *
 * @param texture      纹理资源路径，如 Identifier("ic2", "textures/gui/slot.png")
 * @param width        绘制宽度（像素）
 * @param height       绘制高度（像素）
 * @param u            纹理裁剪起始 U（默认 0）
 * @param v            纹理裁剪起始 V（默认 0）
 * @param regionWidth  纹理区域宽度（默认 = width，用于 sprite sheet 裁剪）
 * @param regionHeight 纹理区域高度（默认 = height）
 * @param textureWidth 纹理文件实际宽度（默认 = width）
 * @param textureHeight 纹理文件实际高度（默认 = height）
 */
class ImageNode(
    val texture: Identifier,
    val imgWidth: Int,
    val imgHeight: Int,
    val u: Float = 0f,
    val v: Float = 0f,
    val regionWidth: Int = imgWidth,
    val regionHeight: Int = imgHeight,
    val textureWidth: Int = imgWidth,
    val textureHeight: Int = imgHeight,
) : UiNode() {

    override fun measure(ctx: RenderContext, constraints: Constraints) {
        val pad = modifier.padding
        measuredWidth = modifier.width ?: (imgWidth + pad.horizontal)
        measuredHeight = modifier.height ?: (imgHeight + pad.vertical)
    }

    override fun render(ctx: RenderContext, originX: Int, originY: Int) {
        renderModifierDecoration(ctx, originX, originY)
        val pad = modifier.padding
        ctx.drawContext.drawTexture(
            texture,
            originX + pad.left, originY + pad.top,
            u, v,
            regionWidth, regionHeight,
            textureWidth, textureHeight
        )
    }
}

// ──────────────────────────── Button ────────────────────────────

class ButtonNode(
    val text: String,
    val onClick: () -> Unit = {}
) : UiNode() {

    override fun measure(ctx: RenderContext, constraints: Constraints) {
        val pad = modifier.padding.let {
            if (it == Padding.ZERO) Padding(6, 4, 6, 4) else it
        }
        val textW = ctx.textRenderer.getWidth(text)
        val textH = ctx.textRenderer.fontHeight
        measuredWidth = modifier.width ?: (textW + pad.horizontal)
        measuredHeight = modifier.height ?: (textH + pad.vertical)
    }

    override fun render(ctx: RenderContext, originX: Int, originY: Int) {
        val pad = modifier.padding.let {
            if (it == Padding.ZERO) Padding(6, 4, 6, 4) else it
        }

        val hovered = ctx.mouseX in originX until originX + measuredWidth
                && ctx.mouseY in originY until originY + measuredHeight

        val bgColor = modifier.backgroundColor
            ?: if (hovered) 0xFF666666.toInt() else 0xFF555555.toInt()
        val bdColor = modifier.borderColor
            ?: if (hovered) 0xFFFFFFFF.toInt() else 0xFFAAAAAA.toInt()

        ctx.drawContext.fill(originX, originY, originX + measuredWidth, originY + measuredHeight, bgColor)
        ctx.drawContext.drawBorder(originX, originY, measuredWidth, measuredHeight, bdColor)

        val tx = originX + pad.left
        val ty = originY + pad.top
        ctx.drawContext.drawTextWithShadow(ctx.textRenderer, text, tx, ty, 0xFFFFFF)

        ctx.buttonHits += RenderContext.ButtonHit(originX, originY, measuredWidth, measuredHeight, onClick)
    }
}

// ──────────────────────────── Column ────────────────────────────

class ColumnNode(
    val spacing: Int = 0,
    val children: List<UiNode> = emptyList()
) : UiNode() {

    override fun measure(ctx: RenderContext, constraints: Constraints) {
        val pad = modifier.padding
        val innerMaxW = (modifier.width?.let { it - pad.horizontal } ?: constraints.maxWidth)
            .coerceAtLeast(0)
        val innerMaxH = (modifier.height?.let { it - pad.vertical } ?: constraints.maxHeight)
            .coerceAtLeast(0)
        val childConstraints = Constraints(innerMaxW, innerMaxH)

        var contentW = 0
        var contentH = 0
        val flowChildren = children.filter { !it.isAbsolute }

        flowChildren.forEach { child ->
            child.measure(ctx, childConstraints)
            contentW = maxOf(contentW, child.measuredWidth)
            contentH += child.measuredHeight
        }
        if (flowChildren.size > 1) {
            contentH += spacing * (flowChildren.size - 1)
        }

        children.filter { it.isAbsolute }.forEach { it.measure(ctx, childConstraints) }

        measuredWidth = modifier.width ?: (contentW + pad.horizontal)
        measuredHeight = modifier.height ?: (contentH + pad.vertical)
    }

    override fun render(ctx: RenderContext, originX: Int, originY: Int) {
        renderModifierDecoration(ctx, originX, originY)

        val pad = modifier.padding
        val innerX = originX + pad.left
        var cursorY = originY + pad.top

        for (child in children) {
            if (child.isAbsolute) continue
            val pos = child.position as Position.Flow
            child.render(ctx, innerX + pos.offsetX, cursorY + pos.offsetY)
            cursorY += child.measuredHeight + spacing
        }

        for (child in children) {
            if (!child.isAbsolute) continue
            val pos = child.position as Position.Absolute
            child.render(ctx, originX + pos.x, originY + pos.y)
        }
    }
}

// ──────────────────────────── Row ────────────────────────────

class RowNode(
    val spacing: Int = 0,
    val children: List<UiNode> = emptyList()
) : UiNode() {

    override fun measure(ctx: RenderContext, constraints: Constraints) {
        val pad = modifier.padding
        val innerMaxW = (modifier.width?.let { it - pad.horizontal } ?: constraints.maxWidth)
            .coerceAtLeast(0)
        val innerMaxH = (modifier.height?.let { it - pad.vertical } ?: constraints.maxHeight)
            .coerceAtLeast(0)
        val childConstraints = Constraints(innerMaxW, innerMaxH)

        var contentW = 0
        var contentH = 0
        val flowChildren = children.filter { !it.isAbsolute }

        flowChildren.forEach { child ->
            child.measure(ctx, childConstraints)
            contentW += child.measuredWidth
            contentH = maxOf(contentH, child.measuredHeight)
        }
        if (flowChildren.size > 1) {
            contentW += spacing * (flowChildren.size - 1)
        }

        children.filter { it.isAbsolute }.forEach { it.measure(ctx, childConstraints) }

        measuredWidth = modifier.width ?: (contentW + pad.horizontal)
        measuredHeight = modifier.height ?: (contentH + pad.vertical)
    }

    override fun render(ctx: RenderContext, originX: Int, originY: Int) {
        renderModifierDecoration(ctx, originX, originY)

        val pad = modifier.padding
        var cursorX = originX + pad.left
        val innerY = originY + pad.top

        for (child in children) {
            if (child.isAbsolute) continue
            val pos = child.position as Position.Flow
            child.render(ctx, cursorX + pos.offsetX, innerY + pos.offsetY)
            cursorX += child.measuredWidth + spacing
        }

        for (child in children) {
            if (!child.isAbsolute) continue
            val pos = child.position as Position.Absolute
            child.render(ctx, originX + pos.x, originY + pos.y)
        }
    }
}

// ──────────────────────────── Flex ────────────────────────────

enum class FlexDirection { ROW, COLUMN }

enum class JustifyContent {
    START, CENTER, END,
    SPACE_BETWEEN, SPACE_AROUND, SPACE_EVENLY
}

enum class AlignItems { START, CENTER, END }

class FlexNode(
    val direction: FlexDirection = FlexDirection.ROW,
    val justifyContent: JustifyContent = JustifyContent.START,
    val alignItems: AlignItems = AlignItems.START,
    val gap: Int = 0,
    val children: List<UiNode> = emptyList()
) : UiNode() {

    override fun measure(ctx: RenderContext, constraints: Constraints) {
        val pad = modifier.padding
        val innerMaxW = (modifier.width?.let { it - pad.horizontal } ?: constraints.maxWidth)
            .coerceAtLeast(0)
        val innerMaxH = (modifier.height?.let { it - pad.vertical } ?: constraints.maxHeight)
            .coerceAtLeast(0)
        val childConstraints = Constraints(innerMaxW, innerMaxH)

        val flowChildren = children.filter { !it.isAbsolute }
        flowChildren.forEach { it.measure(ctx, childConstraints) }
        children.filter { it.isAbsolute }.forEach { it.measure(ctx, childConstraints) }

        val totalGap = if (flowChildren.size > 1) gap * (flowChildren.size - 1) else 0

        val (contentW, contentH) = when (direction) {
            FlexDirection.ROW -> {
                val w = flowChildren.sumOf { it.measuredWidth } + totalGap
                val h = flowChildren.maxOfOrNull { it.measuredHeight } ?: 0
                w to h
            }
            FlexDirection.COLUMN -> {
                val w = flowChildren.maxOfOrNull { it.measuredWidth } ?: 0
                val h = flowChildren.sumOf { it.measuredHeight } + totalGap
                w to h
            }
        }

        measuredWidth = modifier.width ?: (contentW + pad.horizontal)
        measuredHeight = modifier.height ?: (contentH + pad.vertical)
    }

    override fun render(ctx: RenderContext, originX: Int, originY: Int) {
        renderModifierDecoration(ctx, originX, originY)

        val pad = modifier.padding
        val innerX = originX + pad.left
        val innerY = originY + pad.top
        val innerW = measuredWidth - pad.horizontal
        val innerH = measuredHeight - pad.vertical

        val flowChildren = children.filter { !it.isAbsolute }

        if (flowChildren.isNotEmpty()) {
            val mainAxisSize = if (direction == FlexDirection.ROW) innerW else innerH
            val totalChildMain = flowChildren.sumOf {
                if (direction == FlexDirection.ROW) it.measuredWidth else it.measuredHeight
            }
            val totalBaseGap = if (flowChildren.size > 1) gap * (flowChildren.size - 1) else 0
            val remaining = (mainAxisSize - totalChildMain - totalBaseGap).coerceAtLeast(0)

            val (startOffset, extraGap) = computeJustify(remaining, flowChildren.size)

            var cursor = startOffset
            for (child in flowChildren) {
                val childMain = if (direction == FlexDirection.ROW) child.measuredWidth else child.measuredHeight
                val childCross = if (direction == FlexDirection.ROW) child.measuredHeight else child.measuredWidth
                val crossAxisSize = if (direction == FlexDirection.ROW) innerH else innerW

                val crossOffset = when (alignItems) {
                    AlignItems.START -> 0
                    AlignItems.CENTER -> (crossAxisSize - childCross) / 2
                    AlignItems.END -> crossAxisSize - childCross
                }

                val pos = child.position as Position.Flow
                when (direction) {
                    FlexDirection.ROW ->
                        child.render(ctx, innerX + cursor + pos.offsetX, innerY + crossOffset + pos.offsetY)
                    FlexDirection.COLUMN ->
                        child.render(ctx, innerX + crossOffset + pos.offsetX, innerY + cursor + pos.offsetY)
                }
                cursor += childMain + gap + extraGap
            }
        }

        for (child in children) {
            if (!child.isAbsolute) continue
            val pos = child.position as Position.Absolute
            child.render(ctx, originX + pos.x, originY + pos.y)
        }
    }

    private fun computeJustify(remaining: Int, count: Int): Pair<Int, Int> {
        if (count <= 0) return 0 to 0
        return when (justifyContent) {
            JustifyContent.START -> 0 to 0
            JustifyContent.END -> remaining to 0
            JustifyContent.CENTER -> remaining / 2 to 0
            JustifyContent.SPACE_BETWEEN ->
                if (count > 1) 0 to remaining / (count - 1) else remaining / 2 to 0
            JustifyContent.SPACE_AROUND -> {
                val each = remaining / count
                each / 2 to each
            }
            JustifyContent.SPACE_EVENLY -> {
                val each = remaining / (count + 1)
                each to each
            }
        }
    }
}

// ──────────────────────────── Table ────────────────────────────

/**
 * 表格节点。按行×列放置子节点，列宽可固定或按内容取 max，行高按该行单元格 max 高度；
 * 单元格在行内垂直居中对齐，解决标签与能量条等混排时的错位。
 *
 * @param rows 每行为一组单元格（UiNode 列表），列数取所有行中最大 size
 * @param columnSpacing 列间距
 * @param rowSpacing 行间距
 * @param columnWidths 可选固定列宽，若为 null 则按每列单元格最大宽度计算
 */
class TableNode(
    val rows: List<List<UiNode>>,
    val columnSpacing: Int = 4,
    val rowSpacing: Int = 4,
    val columnWidths: List<Int>? = null
) : UiNode() {

    private var columnW: IntArray = intArrayOf()
    private var rowH: IntArray = intArrayOf()

    override fun measure(ctx: RenderContext, constraints: Constraints) {
        val pad = modifier.padding
        val innerMaxW = (modifier.width?.let { it - pad.horizontal } ?: constraints.maxWidth).coerceAtLeast(0)
        val innerMaxH = (modifier.height?.let { it - pad.vertical } ?: constraints.maxHeight).coerceAtLeast(0)
        val cellConstraints = Constraints(innerMaxW, innerMaxH)

        if (rows.isEmpty()) {
            measuredWidth = pad.horizontal
            measuredHeight = pad.vertical
            return
        }

        rows.forEach { row -> row.forEach { it.measure(ctx, cellConstraints) } }

        val colCount = rows.maxOfOrNull { it.size } ?: 0
        if (colCount == 0) {
            measuredWidth = pad.horizontal
            measuredHeight = pad.vertical
            return
        }

        columnW = IntArray(colCount) { c ->
            columnWidths?.getOrNull(c) ?: rows.maxOfOrNull { row ->
                row.getOrNull(c)?.measuredWidth ?: 0
            } ?: 0
        }
        rowH = IntArray(rows.size) { r ->
            rows[r].maxOfOrNull { it.measuredHeight } ?: 0
        }

        val totalW = columnW.sum() + (colCount - 1).coerceAtLeast(0) * columnSpacing
        val totalH = rowH.sum() + (rows.size - 1).coerceAtLeast(0) * rowSpacing
        measuredWidth = modifier.width ?: (totalW + pad.horizontal)
        measuredHeight = modifier.height ?: (totalH + pad.vertical)
    }

    override fun render(ctx: RenderContext, originX: Int, originY: Int) {
        renderModifierDecoration(ctx, originX, originY)
        val pad = modifier.padding
        var cursorY = originY + pad.top

        rows.forEachIndexed { r, row ->
            var cursorX = originX + pad.left
            val rowHeight = rowH.getOrNull(r) ?: 0
            row.forEachIndexed { c, cell ->
                val colWidth = columnW.getOrNull(c) ?: 0
                val cellH = cell.measuredHeight
                val cellY = cursorY + (rowHeight - cellH) / 2
                cell.render(ctx, cursorX, cellY)
                cursorX += colWidth + columnSpacing
            }
            cursorY += rowHeight + rowSpacing
        }
    }
}

// ──────────────────────────── Root ────────────────────────────

/**
 * 虚拟根节点，本身不绘制任何内容，仅作为顶层子节点的画布容器。
 * 每个子节点的 Flow offset 直接作为相对于 origin 的坐标（不堆叠），
 * 使得 composeUI { } 顶层像画布一样，各元素独立定位。
 */
class RootNode(val children: List<UiNode>) : UiNode() {

    override fun measure(ctx: RenderContext, constraints: Constraints) {
        var contentW = 0
        var contentH = 0

        children.forEach { child ->
            child.measure(ctx, constraints)
            if (!child.isAbsolute) {
                val pos = child.position as Position.Flow
                contentW = maxOf(contentW, pos.offsetX + child.measuredWidth)
                contentH = maxOf(contentH, pos.offsetY + child.measuredHeight)
            }
        }

        measuredWidth = contentW
        measuredHeight = contentH
    }

    override fun render(ctx: RenderContext, originX: Int, originY: Int) {
        for (child in children) {
            when (val pos = child.position) {
                is Position.Flow ->
                    child.render(ctx, originX + pos.offsetX, originY + pos.offsetY)
                is Position.Absolute ->
                    child.render(ctx, pos.x, pos.y)
            }
        }
    }
}
