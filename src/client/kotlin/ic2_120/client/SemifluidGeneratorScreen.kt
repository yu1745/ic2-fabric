package ic2_120.client

import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.client.ui.ProgressBar
import ic2_120.content.block.SemifluidGeneratorBlock
import ic2_120.content.block.machines.SemifluidGeneratorBlockEntity
import ic2_120.content.block.misc.FilteredValue
import ic2_120.content.screen.SemifluidGeneratorScreenHandler
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.sync.SemifluidGeneratorSync
import ic2_120.registry.annotation.ModScreen
import ic2_120.registry.type
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.registry.Registries
import net.minecraft.text.Text as McText

@ModScreen(block = SemifluidGeneratorBlock::class)
class SemifluidGeneratorScreen(
    handler: SemifluidGeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: McText
) : HandledScreen<SemifluidGeneratorScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()
    private var filteredOutputRate by FilteredValue()

    init {
        backgroundWidth = UpgradeSlotLayout.VANILLA_UI_WIDTH + UpgradeSlotLayout.SLOT_SPACING
        backgroundHeight = PANEL_HEIGHT
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y,
            SemifluidGeneratorScreenHandler.PLAYER_INV_Y,
            SemifluidGeneratorScreenHandler.HOTBAR_Y,
            SemifluidGeneratorScreenHandler.SLOT_SIZE
        )
        val borderColor = GuiBackground.BORDER_COLOR
        val slotSize = SemifluidGeneratorScreenHandler.SLOT_SIZE
        val borderOffset = 1

        val fuelSlot = handler.slots[SemifluidGeneratorBlockEntity.FUEL_SLOT]
        context.drawBorder(x + fuelSlot.x - borderOffset, y + fuelSlot.y - borderOffset, slotSize, slotSize, borderColor)
        val emptyContainerSlot = handler.slots[SemifluidGeneratorBlockEntity.EMPTY_CONTAINER_SLOT]
        context.drawBorder(x + emptyContainerSlot.x - borderOffset, y + emptyContainerSlot.y - borderOffset, slotSize, slotSize, borderColor)

        val fuelMb = handler.sync.fuelAmountMb.coerceAtLeast(0)
        val fuelCapMb = 8 * 1000
        val fuelFrac = if (fuelCapMb > 0) (fuelMb.toFloat() / fuelCapMb).coerceIn(0f, 1f) else 0f
        val barW = 12
        val barX = x + backgroundWidth - barW - 8
        val barY = y + 8
        val barH = SemifluidGeneratorScreenHandler.BLOCK_SLOTS_Y + slotSize - 8
        val fluidRawId = handler.sync.fuelFluidRawId
        val sampledColor = if (fluidRawId >= 0) {
            val fluid = Registries.FLUID.get(fluidRawId)
            FluidUtils.getFluidColor(fluid)
        } else -1
        val fuelColor = if (sampledColor != -1) bgrToArgb(sampledColor) else handler.sync.fuelColorArgb
        handler.sync.fuelColorArgb = fuelColor
        ProgressBar.drawVerticalFuelBar(
            context,
            barX,
            barY,
            barW,
            barH,
            fuelFrac,
            gradient = false,
            solidColor = fuelColor,
            showTicks = true
        )

        val batterySlot = handler.slots[SemifluidGeneratorBlockEntity.BATTERY_SLOT]
        context.drawBorder(x + batterySlot.x - borderOffset, y + batterySlot.y - borderOffset, slotSize, slotSize, borderColor)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        val left = x
        val top = y
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val inputRate = handler.sync.getSyncedInsertedAmount()
        filteredOutputRate = handler.sync.getSyncedExtractedAmount()
        val cap = SemifluidGeneratorSync.ENERGY_CAPACITY
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val rightBarsWidth = 12 + 8
        val contentW = (backgroundWidth - 16 - rightBarsWidth).coerceAtLeast(0)
        val barW = (contentW - 36).coerceAtLeast(0)

        val generatedText = "发电 ${formatEu(inputRate)} EU/t"
        val outputText = "输出 ${formatEu(filteredOutputRate)} EU/t"
        val generatedTextWidth = generatedText.length * 6
        val outputTextWidth = outputText.length * 6
        val textX = left - maxOf(generatedTextWidth, outputTextWidth) - 4
        context.drawText(textRenderer, generatedText, textX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, outputText, textX, top + 20, 0xAAAAAA, false)

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
                Text("${formatEu(energy)} / ${formatEu(cap)} EU", color = 0xCCCCCC, shadow = false)
            }
        }
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    private fun formatEu(value: Long): String {
        return when {
            value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
            else -> value.toString()
        }
    }

    private fun bgrToArgb(color: Int): Int {
        val a = (color ushr 24) and 0xFF
        val b = (color ushr 16) and 0xFF
        val g = (color ushr 8) and 0xFF
        val r = color and 0xFF
        val alpha = if (a == 0) 0xFF else a
        return (alpha shl 24) or (r shl 16) or (g shl 8) or b
    }

    companion object {
        private const val PANEL_HEIGHT = 166
    }
}
