package ic2_120.client.screen

import ic2_120.content.block.TradeOMatBlock
import ic2_120.content.screen.TradeOMatScreenHandler
import ic2_120.registry.annotation.ModScreen
import ic2_120.client.compat.*
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

@ModScreen(handlers = ["trade_o_mat"])
class TradeOMatScreen(
    val handler: TradeOMatScreenHandler,
    playerInventory: Inventory,
    title: Component
) : LegacyContainerScreen26<TradeOMatScreenHandler>(handler, playerInventory, title, 176, 166) {

    private val isOwner = handler.isOwner

    init {
    }

    override fun renderBg(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(
            if (isOwner) TEXTURE_OPEN else TEXTURE_CLOSED,
            leftPos, topPos, 0f, 0f, imageWidth, imageHeight, 256, 256
        )
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)
        context.drawString(font, title,
            leftPos + (imageWidth - font.width(title)) / 2, topPos + 6, 0x404040, false)
        renderTooltip(context, mouseX, mouseY)
    }

    companion object {
        private val TEXTURE_OPEN = Identifier.fromNamespaceAndPath("ic2_120", "textures/gui/guitradeomatopen.png")
        private val TEXTURE_CLOSED = Identifier.fromNamespaceAndPath("ic2_120", "textures/gui/guitradeomatclosed.png")
    }
}
