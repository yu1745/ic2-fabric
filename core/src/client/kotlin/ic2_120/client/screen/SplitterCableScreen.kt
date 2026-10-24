package ic2_120.client.screen

import ic2_120.client.t
import ic2_120.content.screen.SplitterCableScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.components.Button
import net.minecraft.world.entity.player.Container
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

@ModScreen(handler = "splitter_cable")
class SplitterCableScreen(
    handler: SplitterCableScreenHandler,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<SplitterCableScreenHandler>(handler, playerInventory, title) {

    private lateinit var invertedButton: Button

    init {
        backgroundWidth = 176
        backgroundHeight = 166
    }

    override fun drawBackground(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, backgroundWidth, backgroundHeight, TEX_SIZE, TEX_SIZE)
    }

    override fun init() {
        super.init()
        val client = client ?: return

        addDrawableChild(Button.builder(Component.literal("-")) {
            client.networkHandler?.sendPacket(
                ServerboundContainerButtonClickPacket(handler.syncId, SplitterCableScreenHandler.BUTTON_THRESHOLD_MINUS)
            )
        }.dimensions(x + 35, y + 19, 24, 20).build())

        addDrawableChild(Button.builder(Component.literal("+")) {
            client.networkHandler?.sendPacket(
                ServerboundContainerButtonClickPacket(handler.syncId, SplitterCableScreenHandler.BUTTON_THRESHOLD_PLUS)
            )
        }.dimensions(x + 117, y + 19, 24, 20).build())

        invertedButton = addDrawableChild(Button.builder(invertedButtonText()) {
            client.networkHandler?.sendPacket(
                ServerboundContainerButtonClickPacket(handler.syncId, SplitterCableScreenHandler.BUTTON_TOGGLE_INVERTED)
            )
        }.dimensions(x + 48, y + 47, 80, 20).build())
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        if (::invertedButton.isInitialized) invertedButton.message = invertedButtonText()
        super.render(context, mouseX, mouseY, delta)

        context.drawText(
            textRenderer, title,
            x + (backgroundWidth - textRenderer.getWidth(title)) / 2, y + 6,
            0x404040, false
        )

        val thresholdText = t("gui.ic2_120.splitter_cable.threshold", handler.threshold)
        context.drawText(
            textRenderer, thresholdText,
            x + (backgroundWidth - textRenderer.getWidth(thresholdText)) / 2, y + 25,
            0x55FF55, false
        )
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun invertedButtonText(): Component = if (handler.inverted) {
        Component.translatable("gui.ic2_120.splitter_cable.inverted.on")
    } else {
        Component.translatable("gui.ic2_120.splitter_cable.inverted.off")
    }

    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui/guilimitercable.png")
        private const val TEX_SIZE = 256
    }
}
