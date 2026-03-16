package ic2_120.client

import ic2_120.client.compose.*
import ic2_120.client.ui.GuiBackground
import ic2_120.client.ui.ProgressBar
import ic2_120.content.block.SolidHeatGeneratorBlock
import ic2_120.content.screen.SolidHeatGeneratorScreenHandler
import ic2_120.registry.annotation.ModScreen
import ic2_120.registry.type
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text as McText

@ModScreen(block = SolidHeatGeneratorBlock::class)
class SolidHeatGeneratorScreen(
    handler: SolidHeatGeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: McText
) : HandledScreen<SolidHeatGeneratorScreenHandler>(handler, playerInventory, title) {
    private val ui = ComposeUI()

    init {
        backgroundWidth = 176
        backgroundHeight = 166
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(context, x, y, 84, 142, 18)
        val fuelSlot = handler.slots[0]
        context.drawBorder(x + fuelSlot.x - 1, y + fuelSlot.y - 1, 18, 18, GuiBackground.BORDER_COLOR)

        val total = handler.sync.burnTotal.coerceAtLeast(1)
        val current = handler.sync.burnTime.coerceIn(0, total)
        val frac = (current.toFloat() / total).coerceIn(0f, 1f)
        ProgressBar.drawVerticalFuelBar(context, x + fuelSlot.x + 20, y + fuelSlot.y, 6, 18, frac)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        val generatedRate = handler.sync.getSyncedGeneratedHeat()
        val outputRate = handler.sync.getSyncedOutputHeat()

        val generatedText = "产热 $generatedRate HU/t"
        val outputText = "输出 $outputRate HU/t"
        val generatedTextWidth = generatedText.length * 6
        val outputTextWidth = outputText.length * 6
        val textX = x - maxOf(generatedTextWidth, outputTextWidth) - 4
        context.drawText(textRenderer, generatedText, textX, y + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, outputText, textX, y + 20, 0xAAAAAA, false)

        ui.render(context, textRenderer, mouseX, mouseY) {
            Column(x = x + 8, y = y + 8, spacing = 6) {
                Text(title.string, color = 0xFFFFFF)
            }
        }
        drawMouseoverTooltip(context, mouseX, mouseY)
    }
}

