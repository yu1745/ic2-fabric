package ic2_120.client.screen

import ic2_120.content.screen.ContainmentBoxScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

@ModScreen(handler = "containment_box")
class ContainmentBoxScreen(
    val handler: ContainmentBoxScreenHandler,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<ContainmentBoxScreenHandler>(handler, playerInventory, title) {

    init {
        imageWidth = 176
        imageHeight = 166
    }

    override fun drawBackground(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(TEXTURE, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, TEX_SIZE, TEX_SIZE)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        context.drawString(font, title, leftPos + (imageWidth - font.width(title)) / 2, topPos + 6, 0x404040, false)

        renderTooltip(context, mouseX, mouseY)
    }

    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui/guicontainmentbox.png")
        private const val TEX_SIZE = 256
    }
}
