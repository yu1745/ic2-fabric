package ic2_120.client

import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.client.ui.ProgressBar
import ic2_120.content.sync.CompressorSync
import ic2_120.content.block.CompressorBlock
import ic2_120.content.screen.CompressorScreenHandler
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

@ModScreen(block = CompressorBlock::class)
class CompressorScreen(
    handler: CompressorScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<CompressorScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = PANEL_WIDTH
        backgroundHeight = PANEL_HEIGHT
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y,
            CompressorScreenHandler.PLAYER_INV_Y,
            CompressorScreenHandler.HOTBAR_Y,
            CompressorScreenHandler.SLOT_SIZE
        )
        val borderColor = GuiBackground.BORDER_COLOR
        val slotSize = CompressorScreenHandler.SLOT_SIZE
        val borderOffset = 1
        val inputSlot = handler.slots[CompressorScreenHandler.SLOT_INPUT_INDEX]
        val outputSlot = handler.slots[CompressorScreenHandler.SLOT_OUTPUT_INDEX]
        context.drawBorder(x + inputSlot.x - borderOffset, y + inputSlot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + outputSlot.x - borderOffset, y + outputSlot.y - borderOffset, slotSize, slotSize, borderColor)
        for (i in CompressorScreenHandler.SLOT_UPGRADE_INDEX_START..CompressorScreenHandler.SLOT_UPGRADE_INDEX_END) {
            val slot = handler.slots[i]
            context.drawBorder(x + slot.x - borderOffset, y + slot.y - borderOffset, slotSize, slotSize, borderColor)
        }
        val progress = handler.sync.progress.coerceIn(0, CompressorSync.PROGRESS_MAX)
        val progressFrac = if (CompressorSync.PROGRESS_MAX > 0) (progress.toFloat() / CompressorSync.PROGRESS_MAX).coerceIn(0f, 1f) else 0f
        val barX = x + inputSlot.x + slotSize + 2
        val barW = outputSlot.x - (inputSlot.x + slotSize) - 4
        val barH = 8
        val barY = y + inputSlot.y + (slotSize - barH) / 2
        ProgressBar.draw(context, barX, barY, barW, barH, progressFrac)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        val left = x
        val top = y
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1L)
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val contentW = (backgroundWidth - 16).coerceAtLeast(0)
        val barW = (contentW - 36).coerceAtLeast(0)
        ui.render(context, textRenderer, mouseX, mouseY) {
            Column(x = left + 8, y = top + 8, spacing = 6) {
                Text(title.string, color = 0xFFFFFF)
                Flex(
                    direction = FlexDirection.ROW,
                    alignItems = AlignItems.CENTER,
                    gap = 8,
                    modifier = Modifier.EMPTY.width(contentW)
                ) {
                    Text("能量", color = 0xAAAAAA)
                    EnergyBar(
                        energyFraction,
                        barWidth = 0,
                        barHeight = 9,
                        modifier = Modifier.EMPTY.width(barW)
                    )
                }
                Text(
                    "$energy / $cap EU",
                    color = 0xCCCCCC,
                    shadow = false
                )
            }
        }
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    companion object {
        /** 原版 UI 宽度 + 升级槽列宽度 */
        private val PANEL_WIDTH = UpgradeSlotLayout.VANILLA_UI_WIDTH + UpgradeSlotLayout.SLOT_SPACING
        private const val PANEL_HEIGHT = 166
    }
}
