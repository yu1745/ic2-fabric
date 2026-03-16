package ic2_120.client

import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.ElectricHeatGeneratorBlock
import ic2_120.content.screen.ElectricHeatGeneratorScreenHandler
import ic2_120.content.sync.ElectricHeatGeneratorSync
import ic2_120.registry.annotation.ModScreen
import ic2_120.registry.type
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text as McText

@ModScreen(block = ElectricHeatGeneratorBlock::class)
class ElectricHeatGeneratorScreen(
    handler: ElectricHeatGeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: McText
) : HandledScreen<ElectricHeatGeneratorScreenHandler>(handler, playerInventory, title) {
    private val ui = ComposeUI()

    init {
        backgroundWidth = 176
        backgroundHeight = 166
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(context, x, y, 84, 142, 18)
        val borderColor = GuiBackground.BORDER_COLOR
        for (i in 0 until 10) {
            val slot = handler.slots[i]
            context.drawBorder(x + slot.x - 1, y + slot.y - 1, 18, 18, borderColor)
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = ElectricHeatGeneratorSync.ENERGY_CAPACITY
        val fraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val coils = (0 until 10).count { handler.slots[it].hasStack() }
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
                Flex(direction = FlexDirection.ROW, gap = 8, modifier = Modifier.EMPTY.width(160)) {
                    Text("能量", color = 0xAAAAAA)
                    EnergyBar(fraction, barWidth = 120, barHeight = 9)
                }
                Row(spacing = 8) {
                    Text("$energy / $cap EU", color = 0xCCCCCC, shadow = false)
                    Text("线圈 $coils/10", color = 0xAAAAAA, shadow = false)
                }
            }
        }
        drawMouseoverTooltip(context, mouseX, mouseY)
    }
}
