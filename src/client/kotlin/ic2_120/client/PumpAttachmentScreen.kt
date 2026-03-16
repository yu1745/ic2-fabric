package ic2_120.client

import ic2_120.client.ui.GuiBackground
import ic2_120.content.screen.PumpAttachmentScreenHandler
import ic2_120.registry.annotation.ModScreen
import ic2_120.registry.type
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

@ModScreen(handlers = ["bronze_pump_attachment", "carbon_pump_attachment"])
class PumpAttachmentScreen(
    handler: PumpAttachmentScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<PumpAttachmentScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 176
        backgroundHeight = 140
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(context, x, y, 58, 116, 18)
        val slot = handler.slots[0]
        context.drawBorder(x + slot.x - 1, y + slot.y - 1, 18, 18, GuiBackground.BORDER_COLOR)
        context.drawText(textRenderer, "过滤样本", x + 62, y + 9, 0xCFCFCF, false)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        val stack = handler.slots[0].stack
        val line = if (stack.isEmpty) "当前: 任意流体" else "当前: ${stack.name.string}"
        context.drawText(textRenderer, line, x + 8, y + 42, 0xAAAAAA, false)
        drawMouseoverTooltip(context, mouseX, mouseY)
    }
}
