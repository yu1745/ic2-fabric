package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.FluidBar
import ic2_120.client.ui.GuiBackground
import ic2_120.client.ui.ProgressBar
import ic2_120.content.block.FluidBottlerBlock
import ic2_120.content.screen.FluidBottlerScreenHandler
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.sync.FluidBottlerSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

@ModScreen(block = FluidBottlerBlock::class)
class FluidBottlerScreen(
    handler: FluidBottlerScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<FluidBottlerScreenHandler>(handler, playerInventory, title) {

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
            FluidBottlerScreenHandler.PLAYER_INV_Y,
            FluidBottlerScreenHandler.HOTBAR_Y,
            FluidBottlerScreenHandler.SLOT_SIZE
        )

        val borderColor = GuiBackground.BORDER_COLOR
        val slotSize = FluidBottlerScreenHandler.SLOT_SIZE
        val borderOffset = 1

        val inputFilledSlot = handler.slots[FluidBottlerScreenHandler.SLOT_INPUT_FILLED_INDEX]
        val inputEmptySlot = handler.slots[FluidBottlerScreenHandler.SLOT_INPUT_EMPTY_INDEX]
        val outputSlot = handler.slots[FluidBottlerScreenHandler.SLOT_OUTPUT_INDEX]
        val dischargingSlot = handler.slots[FluidBottlerScreenHandler.SLOT_DISCHARGING_INDEX]

        context.drawBorder(x + inputFilledSlot.x - borderOffset, y + inputFilledSlot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + inputEmptySlot.x - borderOffset, y + inputEmptySlot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + outputSlot.x - borderOffset, y + outputSlot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + dischargingSlot.x - borderOffset, y + dischargingSlot.y - borderOffset, slotSize, slotSize, borderColor)

        for (i in FluidBottlerScreenHandler.SLOT_UPGRADE_INDEX_START..FluidBottlerScreenHandler.SLOT_UPGRADE_INDEX_END) {
            val slot = handler.slots[i]
            context.drawBorder(x + slot.x - borderOffset, y + slot.y - borderOffset, slotSize, slotSize, borderColor)
        }

        val progress = handler.sync.progress.coerceIn(0, FluidBottlerSync.PROGRESS_MAX)
        val progressFrac = if (FluidBottlerSync.PROGRESS_MAX > 0) (progress.toFloat() / FluidBottlerSync.PROGRESS_MAX).coerceIn(0f, 1f) else 0f
        val barX = x + inputFilledSlot.x + slotSize + 2
        val barW = outputSlot.x - (inputFilledSlot.x + slotSize) - 4
        val barH = 8
        val barY = y + inputFilledSlot.y + (slotSize - barH) / 2
        ProgressBar.draw(context, barX, barY, barW, barH, progressFrac)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        val left = x
        val top = y
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val fluidAmount = handler.sync.fluidAmountMb.toLong()
        val fluidCapacity = handler.sync.fluidCapacityMb.toLong().coerceAtLeast(1)
        val fluidFraction = if (fluidCapacity > 0) (fluidAmount.toFloat() / fluidCapacity).coerceIn(0f, 1f) else 0f
        val contentW = (backgroundWidth - 16).coerceAtLeast(0)
        val fluidBarW = 6
        val fluidBarH = 52
        val fluidSectionW = 18
        val rightReservedForUpgrades = 30
        val topRowW = (contentW - rightReservedForUpgrades).coerceAtLeast(0)
        val titleLabel = title.string
        val energyText = "$energy / $cap EU"
        val energyTextW = textRenderer.getWidth(energyText)
        val titleW = textRenderer.getWidth(titleLabel)
        val energyBarW = (topRowW - titleW - energyTextW - fluidSectionW - 16).coerceAtLeast(24)

        val inputRate = handler.sync.getSyncedInsertedAmount()
        val consumeRate = handler.sync.getSyncedConsumedAmount()
        val inputText = "输入 ${formatEu(inputRate)} EU/t"
        val consumeText = "耗能 ${formatEu(consumeRate)} EU/t"
        val inputTextWidth = inputText.length * 6
        val consumeTextWidth = consumeText.length * 6
        val textX = left - maxOf(inputTextWidth, consumeTextWidth) - 4
        context.drawText(textRenderer, inputText, textX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, consumeText, textX, top + 20, 0xAAAAAA, false)

        ui.render(context, textRenderer, mouseX, mouseY) {
            Column(x = left + 8, y = top + 8, spacing = 2, modifier = Modifier.EMPTY.width(contentW)) {
                Flex(
                    direction = FlexDirection.ROW,
                    justifyContent = JustifyContent.SPACE_BETWEEN,
                    alignItems = AlignItems.START,
                    gap = 8,
                    modifier = Modifier.EMPTY.width(topRowW)
                ) {
                    Flex(
                        direction = FlexDirection.ROW,
                        alignItems = AlignItems.CENTER,
                        gap = 6,
                        modifier = Modifier.EMPTY.width((topRowW - fluidSectionW).coerceAtLeast(0))
                    ) {
                        Text(titleLabel, color = 0xFFFFFF)
                        EnergyBar(
                            energyFraction,
                            barWidth = energyBarW,
                            barHeight = 9,
                            modifier = Modifier.EMPTY.width(energyBarW)
                        )
                        Text(energyText, color = 0xCCCCCC, shadow = false)
                    }
                    Column(spacing = 2, modifier = Modifier.EMPTY.width(fluidSectionW)) {
                        Text("流体", color = 0xAAAAAA)
                        FluidBar(
                            fluidFraction,
                            barWidth = fluidBarH,
                            barHeight = fluidBarW,
                            modifier = Modifier.EMPTY.width(fluidBarW).height(fluidBarH),
                            vertical = true
                        )
                        Text("$fluidAmount/$fluidCapacity mB", color = 0xCCCCCC, shadow = false)
                    }
                }
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
