package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.t
import ic2_120.client.EnergyFormatUtils
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.sync.ChunkLoaderSync
import ic2_120.content.block.ChunkLoaderBlock
import ic2_120.content.screen.ChunkLoaderScreenHandler
import ic2_120.content.screen.GuiSize
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
import net.minecraft.text.Text

@ModScreen(block = ChunkLoaderBlock::class)
class ChunkLoaderScreen(
    handler: ChunkLoaderScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<ChunkLoaderScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = 176
        backgroundHeight = 240
    }

    /** 玩家背包 Y 坐标 */
    private val playerInvY = 158
    /** 快捷栏 Y 坐标 */
    private val hotbarY = 216

    // 网格常量
    private val cellSize = 16
    private val cellGap = 2
    private val gridCols = ChunkLoaderSync.GRID_SIZE
    private val gridRows = ChunkLoaderSync.GRID_SIZE
    private val gridWidth = gridCols * (cellSize + cellGap) - cellGap   // 88
    private val gridHeight = gridRows * (cellSize + cellGap) - cellGap  // 88

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context,
            x,
            y,
            playerInvY,
            hotbarY,
            GuiSize.SLOT_SIZE
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = ChunkLoaderSync.ENERGY_CAPACITY
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val contentW = (backgroundWidth - 16).coerceAtLeast(0)
        val chunkCount = handler.sync.getChunkCount()
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val consumeRate = handler.sync.getSyncedConsumedAmount()

        val energyText = "$energy / $cap EU"
        val inputText = t("gui.ic2_120.input_eu", EnergyFormatUtils.formatEu(inputRate))
        val consumeText = t("gui.ic2_120.chunk_loader.consume_chunks", EnergyFormatUtils.formatEu(consumeRate), chunkCount)
        val sideTextWidth = maxOf(
            textRenderer.getWidth(energyText),
            textRenderer.getWidth(inputText),
            textRenderer.getWidth(consumeText)
        )
        val sideTextX = left - sideTextWidth - 4

        // 网格位置（居中于内容区域）
        val gridX = left + (backgroundWidth - gridWidth) / 2
        val gridY = top + 58

        val content: UiScope.() -> Unit = {
            Column(
                x = left + 8,
                y = top + 8,
                spacing = 4,
                modifier = Modifier().width(contentW),
            ) {
                Row(spacing = 8) {
                    Text(title.string, color = 0xFFFFFF)
                    Text(energyText, color = 0xFFFFFF)
                }
                EnergyBar(energyFraction)

                Flex(justifyContent = JustifyContent.CENTER, alignItems = AlignItems.CENTER) {
                    SlotAnchor(id = "slot.${ChunkLoaderScreenHandler.SLOT_DISCHARGING_INDEX}")
                }
            }

            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = ChunkLoaderScreenHandler.PLAYER_INV_START,
                playerInvY = playerInvY,
                hotbarY = hotbarY
            )
        }

        // 1) 预布局
        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)

        // 2) 锚点写回 slot 相对坐标
        handler.slots.forEachIndexed { index, slot ->
            val anchor = layout.anchors["slot.$index"] ?: return@forEachIndexed
            slot.x = anchor.x - left
            slot.y = anchor.y - top
        }

        // 3) 原生 slot 渲染 + 交互
        super.render(context, mouseX, mouseY, delta)

        // 4) Compose overlay
        ui.render(context, textRenderer, mouseX, mouseY, content = content)
        context.drawText(textRenderer, inputText, sideTextX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, consumeText, sideTextX, top + 20, 0xAAAAAA, false)

        // 5) 绘制 5×5 区块网格
        val bitmask = handler.sync.chunkBitmask
        for (row in 0 until gridRows) {
            for (col in 0 until gridCols) {
                val index = row * gridCols + col
                val cx = gridX + col * (cellSize + cellGap)
                val cy = gridY + row * (cellSize + cellGap)
                val enabled = (bitmask and (1 shl index)) != 0
                val isCenter = row == 2 && col == 2
                val isHovered = mouseX >= cx && mouseX < cx + cellSize && mouseY >= cy && mouseY < cy + cellSize

                val color = when {
                    isCenter && enabled -> if (isHovered) 0xE070D070.toInt() else 0xC050B050.toInt()
                    enabled -> if (isHovered) 0xE070FF70.toInt() else 0xC050FF50.toInt()
                    isCenter -> if (isHovered) 0xE0707070.toInt() else 0xC0505050.toInt()
                    else -> if (isHovered) 0xE0606060.toInt() else 0xC0404040.toInt()
                }
                context.fill(cx, cy, cx + cellSize, cy + cellSize, color)

                // 边框
                val borderColor = if (isCenter) 0xFFAAAAAA.toInt() else 0xFF666666.toInt()
                context.fill(cx, cy, cx + cellSize, cy + 1, borderColor)
                context.fill(cx, cy + cellSize - 1, cx + cellSize, cy + cellSize, borderColor)
                context.fill(cx, cy, cx + 1, cy + cellSize, borderColor)
                context.fill(cx + cellSize - 1, cy, cx + cellSize, cy + cellSize, borderColor)
            }
        }

        // 6) 网格悬浮提示
        val gridTooltip = getGridTooltip(mouseX, mouseY, gridX, gridY)
        if (gridTooltip != null) {
            context.drawTooltip(textRenderer, gridTooltip, mouseX, mouseY)
        } else {
            drawMouseoverTooltip(context, mouseX, mouseY)
        }
    }

    /** 获取鼠标所在网格单元格的 tooltip，不在网格内返回 null */
    private fun getGridTooltip(mouseX: Int, mouseY: Int, gridX: Int, gridY: Int): List<Text>? {
        if (mouseX < gridX || mouseX >= gridX + gridWidth || mouseY < gridY || mouseY >= gridY + gridHeight) {
            return null
        }
        val col = (mouseX - gridX) / (cellSize + cellGap)
        val row = (mouseY - gridY) / (cellSize + cellGap)
        if (col !in 0 until gridCols || row !in 0 until gridRows) return null
        val cellLocalX = (mouseX - gridX) - col * (cellSize + cellGap)
        val cellLocalY = (mouseY - gridY) - row * (cellSize + cellGap)
        if (cellLocalX >= cellSize || cellLocalY >= cellSize) return null

        val offsetX = col - 2
        val offsetZ = row - 2
        val index = row * gridCols + col
        val enabled = handler.sync.isChunkEnabled(index)
        val statusText = if (enabled) "§a" + t("gui.ic2_120.chunk_loader.loaded")
                         else "§7" + t("gui.ic2_120.chunk_loader.unloaded")
        return listOf(
            Text.literal("($offsetX, $offsetZ)"),
            Text.literal(statusText)
        )
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) {
            val mx = mouseX.toInt()
            val my = mouseY.toInt()
            val gridX = x + (backgroundWidth - gridWidth) / 2
            val gridY = y + 58
            if (mx >= gridX && mx < gridX + gridWidth && my >= gridY && my < gridY + gridHeight) {
                val col = (mx - gridX) / (cellSize + cellGap)
                val row = (my - gridY) / (cellSize + cellGap)
                if (col in 0 until gridCols && row in 0 until gridRows) {
                    val cellLocalX = (mx - gridX) - col * (cellSize + cellGap)
                    val cellLocalY = (my - gridY) - row * (cellSize + cellGap)
                    if (cellLocalX < cellSize && cellLocalY < cellSize) {
                        val index = row * gridCols + col
                        // 中心区块(index=12)恒定加载，不可切换
                        if (index != 12) {
                            client?.player?.networkHandler?.sendPacket(
                                ButtonClickC2SPacket(handler.syncId, index)
                            )
                        }
                        return true
                    }
                }
            }
        }
        return ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)
    }
}
