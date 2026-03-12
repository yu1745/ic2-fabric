package ic2_120.client

import ic2_120.client.compose.*
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.WindKineticGeneratorBlock
import ic2_120.content.screen.WindKineticGeneratorScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text as McText

@ModScreen(block = WindKineticGeneratorBlock::class)
class WindKineticGeneratorScreen(
    handler: WindKineticGeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: McText
) : HandledScreen<WindKineticGeneratorScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = PANEL_WIDTH
        backgroundHeight = PANEL_HEIGHT
        titleY = -1000
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y,
            WindKineticGeneratorScreenHandler.PLAYER_INV_Y,
            WindKineticGeneratorScreenHandler.HOTBAR_Y,
            WindKineticGeneratorScreenHandler.SLOT_SIZE
        )
        val borderColor = GuiBackground.BORDER_COLOR
        val slotSize = WindKineticGeneratorScreenHandler.SLOT_SIZE
        val borderOffset = 1
        val rotorSlot = handler.slots[0]
        context.drawBorder(x + rotorSlot.x - borderOffset, y + rotorSlot.y - borderOffset, slotSize, slotSize, borderColor)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        val left = x
        val top = y
        ui.render(context, textRenderer, mouseX, mouseY) {
            Text(title.string, x = left + 8, y = top + 8, color = 0xFFFFFF, absolute = true)
            Text("转子槽", x = left + 72, y = top + 34, color = 0xAAAAAA, absolute = true, shadow = false)
            Text("放入木/铁/钢/碳转子后显示叶片", x = left + 8, y = top + 70, color = 0xAAAAAA, absolute = true, shadow = false)
        }
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    companion object {
        private const val PANEL_WIDTH = 176
        private const val PANEL_HEIGHT = 166
    }
}
