package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.StirlingGeneratorBlock
import ic2_120.content.block.machines.StirlingGeneratorBlockEntity
import ic2_120.content.screen.StirlingGeneratorScreenHandler
import ic2_120.content.sync.StirlingGeneratorSync
import ic2_120.registry.annotation.ModScreen
import ic2_120.registry.type
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text as McText

@ModScreen(block = StirlingGeneratorBlock::class)
class StirlingGeneratorScreen(
    handler: StirlingGeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: McText
) : HandledScreen<StirlingGeneratorScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = 176
        backgroundHeight = 166
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(context, x, y, 84, 142, 18)

        val borderColor = GuiBackground.BORDER_COLOR
        val slotSize = 18
        val borderOffset = 1

        val batterySlot = handler.slots[0]
        context.drawBorder(x + batterySlot.x - borderOffset, y + batterySlot.y - borderOffset, slotSize, slotSize, borderColor)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        val left = x
        val top = y
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = StirlingGeneratorSync.ENERGY_CAPACITY
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val generationRate = handler.sync.getSyncedInsertedAmount().coerceAtLeast(0L)
        val heatConsumeRate = (generationRate * StirlingGeneratorBlockEntity.HU_PER_EU).coerceAtLeast(0L)
        val outputRate = handler.sync.getSyncedExtractedAmount()

        val generationText = "发电 ${formatEu(generationRate)} EU/t"
        val heatConsumeText = "耗热 ${formatHu(heatConsumeRate)} HU/t"
        val outputText = "输出 ${formatEu(outputRate)} EU/t"
        val generationTextWidth = generationText.length * 6
        val heatConsumeTextWidth = heatConsumeText.length * 6
        val outputTextWidth = outputText.length * 6
        val textX = left - maxOf(generationTextWidth, heatConsumeTextWidth, outputTextWidth) - 4
        context.drawText(textRenderer, generationText, textX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, heatConsumeText, textX, top + 20, 0xAAAAAA, false)
        context.drawText(textRenderer, outputText, textX, top + 32, 0xAAAAAA, false)

        ui.render(context, textRenderer, mouseX, mouseY) {
            Column(x = left + 8, y = top + 8, spacing = 6) {
                Flex(
                    direction = FlexDirection.ROW,
                    alignItems = AlignItems.CENTER,
                    gap = 8,
                    modifier = Modifier.EMPTY.width(160)
                ) {
                    Text(title.string, color = 0xFFFFFF)
                    Text("$energy / $cap EU", color = 0xCCCCCC, shadow = false)
                }
                Flex(
                    direction = FlexDirection.ROW,
                    alignItems = AlignItems.CENTER,
                    gap = 8,
                    modifier = Modifier.EMPTY.width(160)
                ) {
                    Text("能量", color = 0xAAAAAA)
                    EnergyBar(
                        energyFraction,
                        barWidth = 120,
                        barHeight = 9
                    )
                }
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

    private fun formatHu(value: Long): String {
        return when {
            value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
            else -> value.toString()
        }
    }
}
