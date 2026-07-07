package ic2_120.client.screen

import ic2_120.content.block.TradeOMatBlock
import ic2_120.content.screen.TradeOMatScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(handlers = ["trade_o_mat"])
class TradeOMatScreen(
    handler: TradeOMatScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<TradeOMatScreenHandler>(handler, playerInventory, title) {

    private val isOwner = handler.isOwner

    init {
        backgroundWidth = 176
        backgroundHeight = 166
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(
            if (isOwner) TEXTURE_OPEN else TEXTURE_CLOSED,
            x, y, 0f, 0f, backgroundWidth, backgroundHeight, 256, 256
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)
        context.drawText(textRenderer, title,
            x + (backgroundWidth - textRenderer.getWidth(title)) / 2, y + 6, 0x404040, false)
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    companion object {
        private val TEXTURE_OPEN = Identifier("ic2_120", "textures/gui/guitradeomatopen.png")
        private val TEXTURE_CLOSED = Identifier("ic2_120", "textures/gui/guitradeomatclosed.png")
    }
}
