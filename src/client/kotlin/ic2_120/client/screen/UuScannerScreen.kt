package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.client.ui.HeatProgressBar
import ic2_120.content.block.UuScannerBlock
import ic2_120.content.screen.UuScannerScreenHandler
import ic2_120.content.screen.GuiSize
import ic2_120.content.sync.UuScannerSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

@ModScreen(block = UuScannerBlock::class)
class UuScannerScreen(
    handler: UuScannerScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<UuScannerScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = GUI_SIZE.width
        backgroundHeight = GUI_SIZE.height
        titleY = 4
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y, GUI_SIZE.playerInvY, GUI_SIZE.hotbarY, GuiSize.SLOT_SIZE
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y
        val energy = handler.sync.energy.toLong().coerceAtLeast(0L)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1L)
        val fraction = (energy.toFloat() / cap.toFloat()).coerceIn(0f, 1f)
        val progressFraction = (handler.sync.progress.toFloat() / UuScannerSync.PROGRESS_MAX.toFloat()).coerceIn(0f, 1f)
        val inputText = "输入 ${EnergyFormatUtils.formatEu(handler.sync.getSyncedInsertedAmount())} EU/t"
        val consumeText = "耗能 ${EnergyFormatUtils.formatEu(handler.sync.getSyncedConsumedAmount())} EU/t"
        val sideTextWidth = maxOf(textRenderer.getWidth(inputText), textRenderer.getWidth(consumeText))
        val sideTextX = left - sideTextWidth - 4

        val content: UiScope.() -> Unit = {
            Row(
                x = left + 8,
                y = top + 6,
                spacing = 8,
                modifier = Modifier.EMPTY.width(GUI_SIZE.contentWidth)
            ) {
                Column(spacing = 4, modifier = Modifier.EMPTY.width(GuiSize.STANDARD.contentWidth)) {
                    Flex {
                        Text(title.string, color = 0xFFFFFF)
                        HeatProgressBar(fraction, barHeight = 6, modifier = Modifier.EMPTY.fractionWidth(1f))
                        Text(
                            "${EnergyFormatUtils.formatEu(energy)} / ${EnergyFormatUtils.formatEu(cap)} EU",
                            color = 0xFFFFFF,
                            shadow = false
                        )
                    }
                    Text(statusText(handler.sync.status), color = statusColor(handler.sync.status), shadow = false)
                    Text("${handler.sync.currentCostUb} uB", color = 0xFFAA33, shadow = false)
                    Text(
                        "扫描进度: ${handler.sync.progress} / ${UuScannerSync.PROGRESS_MAX}",
                        color = 0xAAAAAA,
                        shadow = false
                    )
                    EnergyBar(progressFraction, modifier = Modifier.EMPTY.width(120), barHeight = 6)

                    Row(spacing = 6) {
                        Column(spacing = 4) {
                            Text("物品", color = 0xAAAAAA, shadow = false)
                            SlotAnchor(
                                id = slotAnchorId(UuScannerScreenHandler.SLOT_INPUT_INDEX),
                                width = 18,
                                height = 18
                            )
                        }
                        Column(spacing = 4) {
                            Text("电池", color = 0xAAAAAA, shadow = false)
                            SlotAnchor(
                                id = slotAnchorId(UuScannerScreenHandler.SLOT_BATTERY_INDEX),
                                width = 18,
                                height = 18
                            )
                        }
                    }
                }
                Column(spacing = 4, modifier = Modifier.EMPTY.width(GuiSize.UPGRADE_COLUMN_WIDTH).padding(0, 8, 0, 0)) {
                    for (i in UuScannerScreenHandler.SLOT_UPGRADE_INDEX_START..UuScannerScreenHandler.SLOT_UPGRADE_INDEX_END) {
                        SlotAnchor(id = slotAnchorId(i), width = 18, height = 18)
                    }
                }
            }

            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = UuScannerScreenHandler.PLAYER_INV_START,
                playerInvY = GUI_SIZE.playerInvY,
                hotbarY = GUI_SIZE.hotbarY
            )
        }

        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)
        applyAnchoredSlots(layout, left, top)

        super.render(context, mouseX, mouseY, delta)
        ui.render(context, textRenderer, mouseX, mouseY, content = content)
        val tooltip = ui.getTooltipAt(mouseX, mouseY)
        if (!tooltip.isNullOrEmpty()) {
            context.drawTooltip(textRenderer, tooltip, mouseX, mouseY)
        }
        context.drawText(textRenderer, inputText, sideTextX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, consumeText, sideTextX, top + 20, 0xAAAAAA, false)
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    private fun slotAnchorId(slotIndex: Int): String = "slot.$slotIndex"

    private fun applyAnchoredSlots(layout: ComposeUI.LayoutSnapshot, left: Int, top: Int) {
        handler.slots.forEachIndexed { index, slot ->
            val anchor = layout.anchors[slotAnchorId(index)] ?: return@forEachIndexed
            slot.x = anchor.x - left
            slot.y = anchor.y - top
        }
    }

    private fun statusText(status: Int): String = when (status) {
        UuScannerSync.STATUS_NO_STORAGE -> "未连接唯一存储机"
        UuScannerSync.STATUS_NO_INPUT -> "请放入待扫描物品"
        UuScannerSync.STATUS_NOT_WHITELISTED -> "物品不在白名单"
        UuScannerSync.STATUS_NO_ENERGY -> "能量不足"
        UuScannerSync.STATUS_SCANNING -> "扫描中"
        UuScannerSync.STATUS_COMPLETE -> "扫描完成"
        else -> "待机"
    }

    private fun statusColor(status: Int): Int = when (status) {
        UuScannerSync.STATUS_COMPLETE -> 0x55FF55
        UuScannerSync.STATUS_SCANNING -> 0x55AAFF
        UuScannerSync.STATUS_NO_STORAGE,
        UuScannerSync.STATUS_NOT_WHITELISTED,
        UuScannerSync.STATUS_NO_ENERGY -> 0xFF5555

        else -> 0xAAAAAA
    }

    companion object {
        private val GUI_SIZE = GuiSize.UPGRADE_TALL
    }
}
