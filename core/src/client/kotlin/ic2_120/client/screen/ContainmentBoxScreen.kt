package ic2_120.client.screen

import ic2_120.content.screen.ContainmentBoxScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(handler = "containment_box")
class ContainmentBoxScreen(
    handler: ContainmentBoxScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<ContainmentBoxScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 176
        backgroundHeight = 166
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, backgroundWidth, backgroundHeight, TEX_SIZE, TEX_SIZE)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        context.drawText(textRenderer, title, x + (backgroundWidth - textRenderer.getWidth(title)) / 2, y + 6, 0x404040, false)

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    companion object {
        private val TEXTURE = Identifier("ic2", "textures/gui/guicontainmentbox.png")
        private const val TEX_SIZE = 256
    }
}
