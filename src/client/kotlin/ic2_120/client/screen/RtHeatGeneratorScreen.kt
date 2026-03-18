package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.RtHeatGeneratorBlock
import ic2_120.content.screen.RtHeatGeneratorScreenHandler
import ic2_120.registry.annotation.ModScreen
import ic2_120.registry.type
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text as McText

@ModScreen(block = RtHeatGeneratorBlock::class)
class RtHeatGeneratorScreen(
    handler: RtHeatGeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: McText
) : HandledScreen<RtHeatGeneratorScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = 176
        backgroundHeight = 166
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)

        // 绘制 6 个燃料槽边框
        val borderColor = GuiBackground.BORDER_COLOR
        val slotSize = 18
        val borderOffset = 1
        val fuelSlotPositions = listOf(
            8 to 36, 26 to 36, 44 to 36,
            8 to 54, 26 to 54, 44 to 54
        )
        for ((sx, sy) in fuelSlotPositions) {
            context.drawBorder(x + sx - borderOffset, y + sy - borderOffset, slotSize, slotSize, borderColor)
        }

        GuiBackground.drawPlayerInventorySlotBorders(context, x, y, 84, 142, 18)
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
