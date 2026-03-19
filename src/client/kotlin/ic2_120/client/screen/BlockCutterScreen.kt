package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.client.ui.ProgressBar
import ic2_120.content.sync.BlockCutterSync
import ic2_120.content.block.BlockCutterBlock
import ic2_120.content.screen.BlockCutterScreenHandler
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

@ModScreen(block = BlockCutterBlock::class)
class BlockCutterScreen(
    handler: BlockCutterScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<BlockCutterScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = PANEL_WIDTH
        backgroundHeight = PANEL_HEIGHT
        titleY = 4
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y,
            BlockCutterScreenHandler.PLAYER_INV_Y,
            BlockCutterScreenHandler.HOTBAR_Y,
            BlockCutterScreenHandler.SLOT_SIZE
        )

        val borderColor = GuiBackground.BORDER_COLOR
        val slotSize = BlockCutterScreenHandler.SLOT_SIZE
        val borderOffset = 1

        val inputSlot = handler.slots[BlockCutterScreenHandler.SLOT_INPUT_INDEX]
        val dischargingSlot = handler.slots[BlockCutterScreenHandler.SLOT_DISCHARGING_INDEX]
        val outputSlot = handler.slots[BlockCutterScreenHandler.SLOT_OUTPUT_INDEX]
        val bladeSlot = handler.slots[BlockCutterScreenHandler.SLOT_BLADE_INDEX]

        context.drawBorder(x + inputSlot.x - borderOffset, y + inputSlot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + dischargingSlot.x - borderOffset, y + dischargingSlot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + outputSlot.x - borderOffset, y + outputSlot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + bladeSlot.x - borderOffset, y + bladeSlot.y - borderOffset, slotSize, slotSize, borderColor)

        for (i in BlockCutterScreenHandler.SLOT_UPGRADE_INDEX_START..BlockCutterScreenHandler.SLOT_UPGRADE_INDEX_END) {
            val slot = handler.slots[i]
            context.drawBorder(x + slot.x - borderOffset, y + slot.y - borderOffset, slotSize, slotSize, borderColor)
        }

        val progress = handler.sync.progress.coerceIn(0, BlockCutterSync.PROGRESS_MAX)
        val progressFrac = if (BlockCutterSync.PROGRESS_MAX > 0) (progress.toFloat() / BlockCutterSync.PROGRESS_MAX).coerceIn(0f, 1f) else 0f
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
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val consumeRate = handler.sync.getSyncedConsumedAmount()
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f

        // 顶行：标题 + 能量条 + 数字，占满一行（176px 内容区）
        val topRowW = UpgradeSlotLayout.VANILLA_UI_WIDTH - 16
        val titleW = textRenderer.getWidth(title.string)
        val energyText = "$energy / $cap EU"
        val energyTextW = textRenderer.getWidth(energyText)
        val energyBarW = (topRowW - titleW - energyTextW - 24).coerceAtLeast(40)

        val inputText = "输入 ${formatEu(inputRate)} EU/t"
        val consumeText = "耗能 ${formatEu(consumeRate)} EU/t"
        val inputTextWidth = inputText.length * 6
        val consumeTextWidth = consumeText.length * 6
        val textX = left - maxOf(inputTextWidth, consumeTextWidth) - 4
        context.drawText(textRenderer, inputText, textX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, consumeText, textX, top + 20, 0xAAAAAA, false)

        val bladeSlot = handler.slots[BlockCutterScreenHandler.SLOT_BLADE_INDEX]
        if (handler.sync.bladeTooWeak != 0) {
            val warningText = Text.translatable("gui.ic2_120.block_cutter.blade_too_weak").string
            context.drawText(textRenderer, warningText, left + 8, top + bladeSlot.y + BlockCutterScreenHandler.SLOT_SIZE + 4, 0xFF5555, false)
        }

        ui.render(context, textRenderer, mouseX, mouseY) {
            Flex(
                x = left + 8,
                y = top + 8,
                direction = FlexDirection.ROW,
                alignItems = AlignItems.CENTER,
                gap = 8,
                modifier = Modifier.EMPTY.width(topRowW)
            ) {
                Text(title.string, color = 0xFFFFFF)
                EnergyBar(
                    energyFraction,
                    barWidth = energyBarW,
                    barHeight = 9,
                    modifier = Modifier.EMPTY.width(energyBarW)
                )
                Text(energyText, color = 0xCCCCCC, shadow = false)
            }
        }
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    companion object {
        private val PANEL_WIDTH = UpgradeSlotLayout.VANILLA_UI_WIDTH + UpgradeSlotLayout.SLOT_SPACING
        private const val PANEL_HEIGHT = 184
    }

    private fun formatEu(value: Long): String {
        return when {
            value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
            else -> value.toString()
        }
    }
}
