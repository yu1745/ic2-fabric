package ic2_120.client.compose

import ic2_120.client.ui.GuiBackground
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
    var drawEnabled: Boolean = true
    var interactionEnabled: Boolean = true

    val buttonHits = mutableListOf<ButtonHit>()
    val tooltipHits = mutableListOf<TooltipHit>()
    val scrollHits = mutableListOf<ScrollHit>()
    val anchors = linkedMapOf<String, AnchorRect>()

    private val scrollOffsets = mutableMapOf<Int, Int>()

    fun getScrollOffset(nodeId: Int): Int = scrollOffsets.getOrDefault(nodeId, 0)

    fun setScrollOffset(nodeId: Int, offset: Int) {
        scrollOffsets[nodeId] = offset
    }

    data class AnchorRect(
        val x: Int,
        val y: Int,
        val w: Int,
        val h: Int
    )

    data class ButtonHit(
        val x: Int, val y: Int,
        val w: Int, val h: Int,
        val onClick: () -> Unit
    )

    data class TooltipHit(
        val x: Int, val y: Int,
        val w: Int, val h: Int,
        val lines: List<net.minecraft.text.Text>
    )

    data class ScrollHit(
        val viewportX: Int, val viewportY: Int,
        val viewportW: Int, val viewportH: Int,
        val trackX: Int, val trackY: Int,
        val trackW: Int, val trackH: Int,
        val nodeId: Int,
        val maxScroll: Int,
        val thumbY: Int,
        val thumbH: Int
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
        if (!ctx.drawEnabled) return
        modifier.backgroundColor?.let { color ->
            ctx.drawContext.fill(x, y, x + measuredWidth, y + measuredHeight, color)
        }
        modifier.borderColor?.let { color ->
            ctx.drawContext.drawBorder(x, y, measuredWidth, measuredHeight, color)
        }
    }
}

// ──────────────────────────── Panel（可复用空背景）────────────────────────────

/**
 * 纯 Compose 绘制的空背景面板，与 [GuiBackground] 同风格。
 */
class PanelNode(
    val panelWidth: Int,
    val panelHeight: Int,
    val fillColor: Int = GuiBackground.FILL_COLOR,
    val borderColor: Int = GuiBackground.BORDER_COLOR
) : UiNode() {

    override fun measure(ctx: RenderContext, constraints: Constraints) {
        val pad = modifier.padding
        measuredWidth = modifier.width ?: (panelWidth + pad.horizontal)
        measuredHeight = modifier.height ?: (panelHeight + pad.vertical)
    }

    override fun render(ctx: RenderContext, originX: Int, originY: Int) {
        renderModifierDecoration(ctx, originX, originY)
        val pad = modifier.padding
        val x = originX + pad.left
        val y = originY + pad.top
        if (!ctx.drawEnabled) return
        GuiBackground.draw(ctx.drawContext, x, y, panelWidth, panelHeight, fillColor, borderColor)
    }
}

// ──────────────────────────── Text ────────────────────────────

class TextNode(
    val text: String,
    val color: Int = 0xFFFFFF,
    val shadow: Boolean = true,
    val tooltip: List<net.minecraft.text.Text>? = null
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
        if (!ctx.drawEnabled) return
        if (shadow) {
            ctx.drawContext.drawTextWithShadow(ctx.textRenderer, text, tx, ty, color)
        } else {
            ctx.drawContext.drawText(ctx.textRenderer, text, tx, ty, color, false)
        }
        if (tooltip != null && ctx.interactionEnabled) {
            ctx.tooltipHits += RenderContext.TooltipHit(originX, originY, measuredWidth, measuredHeight, tooltip)
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
        if (!ctx.drawEnabled) return
        ctx.drawContext.drawTexture(
            texture,
            originX + pad.left, originY + pad.top,
            u, v,
            regionWidth, regionHeight,
            textureWidth, textureHeight
        )
    }
}

// ──────────────────────────── ItemStack ────────────────────────────

/**
 * 物品/方块图标节点。仅渲染 ItemStack 的贴图，不绘制数量。
 * 数量可用外部 DSL 拼接，例如：`Row(spacing = 4) { ItemStack(stack); Text("${stack.count}") }`
 */
class ItemStackNode(
    val stack: net.minecraft.item.ItemStack,
    val size: Int = 16
) : UiNode() {

    override fun measure(ctx: RenderContext, constraints: Constraints) {
        val pad = modifier.padding
        measuredWidth = modifier.width ?: (size + pad.horizontal)
        measuredHeight = modifier.height ?: (size + pad.vertical)
    }

    override fun render(ctx: RenderContext, originX: Int, originY: Int) {
        renderModifierDecoration(ctx, originX, originY)
        val pad = modifier.padding
        val px = originX + pad.left
        val py = originY + pad.top
        if (!ctx.drawEnabled) return
        if (!stack.isEmpty) {
            ctx.drawContext.drawItemWithoutEntity(stack, px, py)
            if (ctx.interactionEnabled) {
                ctx.tooltipHits += RenderContext.TooltipHit(px, py, size, size, listOf(stack.getName()))
            }
        }
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

        if (ctx.drawEnabled) {
            ctx.drawContext.fill(originX, originY, originX + measuredWidth, originY + measuredHeight, bgColor)
            ctx.drawContext.drawBorder(originX, originY, measuredWidth, measuredHeight, bdColor)
        }

        val tx = originX + pad.left
        val ty = originY + pad.top
        if (ctx.drawEnabled) {
            ctx.drawContext.drawTextWithShadow(ctx.textRenderer, text, tx, ty, 0xFFFFFF)
        }

        if (ctx.interactionEnabled) {
            ctx.buttonHits += RenderContext.ButtonHit(originX, originY, measuredWidth, measuredHeight, onClick)
        }
    }
}

class SlotAnchorNode(
    private val id: String,
    private val anchorWidth: Int,
    private val anchorHeight: Int,
    private val showBorder: Boolean = true,
    private val borderColor: Int = GuiBackground.BORDER_COLOR
) : UiNode() {

    override fun measure(ctx: RenderContext, constraints: Constraints) {
        val pad = modifier.padding
        measuredWidth = modifier.width ?: (anchorWidth + pad.horizontal)
        measuredHeight = modifier.height ?: (anchorHeight + pad.vertical)
    }

    override fun render(ctx: RenderContext, originX: Int, originY: Int) {
        val pad = modifier.padding
        val anchorX = originX + pad.left
        val anchorY = originY + pad.top
        val anchorW = (measuredWidth - pad.horizontal).coerceAtLeast(0)
        val anchorH = (measuredHeight - pad.vertical).coerceAtLeast(0)

        if (ctx.drawEnabled && showBorder) {
            // Align with vanilla slot border convention used across screens: offset by -1, keep slot size.
            ctx.drawContext.drawBorder(anchorX - 1, anchorY - 1, anchorW, anchorH, borderColor)
        }

        ctx.anchors[id] = RenderContext.AnchorRect(
            x = anchorX,
            y = anchorY,
            w = anchorW,
            h = anchorH
        )
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
        val mainBounded = when (direction) {
            FlexDirection.ROW -> innerMaxW < Int.MAX_VALUE
            FlexDirection.COLUMN -> innerMaxH < Int.MAX_VALUE
        }
        val totalGap = if (flowChildren.size > 1) gap * (flowChildren.size - 1) else 0

        fun withResolvedModifier(child: UiNode, resolvedMainAxisSize: Int? = null, block: () -> Unit) {
            val original = child.modifier
            var resolved = original

            // 交叉轴 fraction：按父 Flex 内尺寸直接解析，保证 fractionWidth/fractionHeight 可用。
            if (resolved.fractionWidth != null) {
                val w = (innerMaxW * resolved.fractionWidth!!).toInt().coerceAtLeast(0).coerceAtMost(innerMaxW)
                resolved = resolved.copy(width = w, fractionWidth = null)
            }
            if (resolved.fractionHeight != null) {
                val h = (innerMaxH * resolved.fractionHeight!!).toInt().coerceAtLeast(0).coerceAtMost(innerMaxH)
                resolved = resolved.copy(height = h, fractionHeight = null)
            }

            // 主轴 fraction：在 Flex 内按“剩余空间”分配后覆盖，避免和固定项叠加溢出。
            if (resolvedMainAxisSize != null) {
                resolved = when (direction) {
                    FlexDirection.ROW -> resolved.copy(
                        width = resolvedMainAxisSize.coerceAtLeast(0).coerceAtMost(innerMaxW),
                        fractionWidth = null
                    )
                    FlexDirection.COLUMN -> resolved.copy(
                        height = resolvedMainAxisSize.coerceAtLeast(0).coerceAtMost(innerMaxH),
                        fractionHeight = null
                    )
                }
            }

            child.modifier = resolved
            block()
            child.modifier = original
        }

        val mainFractionChildren = flowChildren.filter { child ->
            when (direction) {
                FlexDirection.ROW -> child.modifier.fractionWidth != null
                FlexDirection.COLUMN -> child.modifier.fractionHeight != null
            }
        }
        val fixedChildren = flowChildren.filterNot { it in mainFractionChildren }

        // Pass 1: 先测固定项，得到主轴已占用空间。
        fixedChildren.forEach { child ->
            withResolvedModifier(child) { child.measure(ctx, childConstraints) }
        }

        // Pass 2: 主轴 fraction 子项按“剩余空间”进行分配（类似 flex-grow）。
        if (mainFractionChildren.isNotEmpty()) {
            if (mainBounded) {
                val fixedMain = fixedChildren.sumOf {
                    if (direction == FlexDirection.ROW) it.measuredWidth else it.measuredHeight
                }
                val mainCapacity = if (direction == FlexDirection.ROW) innerMaxW else innerMaxH
                val remaining = (mainCapacity - fixedMain - totalGap).coerceAtLeast(0)
                val totalFraction = mainFractionChildren.sumOf { child ->
                    when (direction) {
                        FlexDirection.ROW -> child.modifier.fractionWidth?.toDouble() ?: 0.0
                        FlexDirection.COLUMN -> child.modifier.fractionHeight?.toDouble() ?: 0.0
                    }
                }.coerceAtLeast(1e-6)

                var assigned = 0
                mainFractionChildren.forEachIndexed { index, child ->
                    val fraction = when (direction) {
                        FlexDirection.ROW -> child.modifier.fractionWidth?.toDouble() ?: 0.0
                        FlexDirection.COLUMN -> child.modifier.fractionHeight?.toDouble() ?: 0.0
                    }
                    val allocated = if (index == mainFractionChildren.lastIndex) {
                        (remaining - assigned).coerceAtLeast(0)
                    } else {
                        ((remaining * (fraction / totalFraction)).toInt()).coerceAtLeast(0)
                    }
                    assigned += allocated
                    withResolvedModifier(child, resolvedMainAxisSize = allocated) {
                        child.measure(ctx, childConstraints)
                    }
                }
            } else {
                // 主轴无界时无法按剩余空间分配，回退为普通测量。
                mainFractionChildren.forEach { child ->
                    withResolvedModifier(child) { child.measure(ctx, childConstraints) }
                }
            }
        }

        children.filter { it.isAbsolute }.forEach { it.measure(ctx, childConstraints) }
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

        // 主轴方向：modifier 未指定时，若父布局有有界约束则使用之，使 JustifyContent 生效
        val mainAxisFromConstraints = when (direction) {
            FlexDirection.ROW -> constraints.maxWidth < Int.MAX_VALUE
            FlexDirection.COLUMN -> constraints.maxHeight < Int.MAX_VALUE
        }
        measuredWidth = modifier.width ?: (when (direction) {
            FlexDirection.ROW -> if (mainAxisFromConstraints) constraints.maxWidth else contentW
            FlexDirection.COLUMN -> contentW
        } + pad.horizontal)
        measuredHeight = modifier.height ?: (when (direction) {
            FlexDirection.ROW -> contentH
            FlexDirection.COLUMN -> if (mainAxisFromConstraints) constraints.maxHeight else contentH
        } + pad.vertical)

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
        if (!ctx.drawEnabled) return
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

// ──────────────────────────── ScrollView ────────────────────────────

class ScrollViewNode(
    val nodeId: Int,
    val scrollbarWidth: Int = 6,
    val contentEndInset: Int = 1,
    val scrollbarTrackColor: Int = 0xFF333333.toInt(),
    val thumbColor: Int = 0xFF888888.toInt(),
    val thumbHoverColor: Int = 0xFFAAAAAA.toInt(),
    val children: List<UiNode>
) : UiNode() {

    private var contentW: Int = 0
    private var contentH: Int = 0

    override fun measure(ctx: RenderContext, constraints: Constraints) {
        val pad = modifier.padding
        val hasBoundedParentWidth = constraints.maxWidth < Int.MAX_VALUE
        val viewportMaxW = when {
            modifier.width != null ->
                (modifier.width!! - pad.horizontal - scrollbarWidth - contentEndInset).coerceAtLeast(0)
            hasBoundedParentWidth ->
                (constraints.maxWidth - pad.horizontal - scrollbarWidth - contentEndInset).coerceAtLeast(0)
            else -> Int.MAX_VALUE
        }
        // Width constrained; height unconstrained so content can extend freely
        val childConstraints = Constraints(viewportMaxW, Int.MAX_VALUE)

        children.forEach { it.measure(ctx, childConstraints) }
        contentW = children.maxOfOrNull { it.measuredWidth } ?: 0
        contentH = children.sumOf { it.measuredHeight }

        val resolvedViewportW = when {
            modifier.width != null || hasBoundedParentWidth -> viewportMaxW
            else -> contentW
        }
        measuredWidth = (resolvedViewportW + pad.horizontal + scrollbarWidth + contentEndInset)
            .coerceAtMost(constraints.maxWidth)
        // fractionHeight overrides explicit height: resolved from parent constraints (set by Flex's inner size)
        measuredHeight = (when {
            modifier.fractionHeight != null -> (constraints.maxHeight * modifier.fractionHeight!!).toInt()
            modifier.height != null -> modifier.height!!
            else -> contentH + pad.vertical
        }).coerceAtMost(constraints.maxHeight)
    }

    override fun render(ctx: RenderContext, originX: Int, originY: Int) {
        val pad = modifier.padding
        val vpX = originX + pad.left
        val vpY = originY + pad.top
        val vpW = (measuredWidth - pad.horizontal - scrollbarWidth - contentEndInset).coerceAtLeast(0)
        val vpH = measuredHeight - pad.vertical

        val maxScroll = (contentH - vpH).coerceAtLeast(0)
        val scrollY = ctx.getScrollOffset(nodeId).coerceIn(0, maxScroll)

        // 1. Clip → draw children with vertical offset
        if (ctx.drawEnabled) {
            ctx.drawContext.enableScissor(vpX, vpY, vpX + vpW, vpY + vpH)
        }
        var cursorY = 0
        for (child in children) {
            child.render(ctx, vpX, vpY + cursorY - scrollY)
            cursorY += child.measuredHeight
        }
        if (ctx.drawEnabled) {
            ctx.drawContext.disableScissor()
        }

        // 2. Scrollbar
        if (maxScroll > 0) {
            val trackX = vpX + vpW + contentEndInset
            val thumbH = (vpH.toFloat() * vpH / contentH).toInt().coerceAtLeast(10)
            val thumbMaxY = vpH - thumbH
            val thumbY = vpY + (scrollY.toFloat() * thumbMaxY / maxScroll).toInt()

            val hovered = ctx.mouseX in trackX until (trackX + scrollbarWidth)
                    && ctx.mouseY in vpY until (vpY + vpH)

            if (ctx.drawEnabled) {
                ctx.drawContext.fill(trackX, vpY, trackX + scrollbarWidth, vpY + vpH, scrollbarTrackColor)
                ctx.drawContext.fill(trackX, thumbY, trackX + scrollbarWidth, thumbY + thumbH,
                    if (hovered) thumbHoverColor else thumbColor)
                ctx.drawContext.drawBorder(trackX, vpY, scrollbarWidth, vpH, 0xFF444444.toInt())
            }

            if (ctx.interactionEnabled) {
                ctx.scrollHits += RenderContext.ScrollHit(
                    viewportX = vpX,
                    viewportY = vpY,
                    viewportW = vpW,
                    viewportH = vpH,
                    trackX = trackX,
                    trackY = vpY,
                    trackW = scrollbarWidth,
                    trackH = vpH,
                    nodeId = nodeId,
                    maxScroll = maxScroll,
                    thumbY = thumbY,
                    thumbH = thumbH
                )
            }
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
