package ic2_120.client.screen

import ic2_120.client.t
import ic2_120.content.screen.SplitterCableScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(handler = "splitter_cable")
class SplitterCableScreen(
    handler: SplitterCableScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<SplitterCableScreenHandler>(handler, playerInventory, title) {

    private lateinit var invertedButton: ButtonWidget

    init {
        backgroundWidth = 176
        backgroundHeight = 166
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, backgroundWidth, backgroundHeight, TEX_SIZE, TEX_SIZE)
    }

    override fun init() {
        super.init()
        val client = client ?: return

        addDrawableChild(ButtonWidget.builder(Text.literal("-")) {
            client.networkHandler?.sendPacket(
                ButtonClickC2SPacket(handler.syncId, SplitterCableScreenHandler.BUTTON_THRESHOLD_MINUS)
            )
        }.dimensions(x + 35, y + 19, 24, 20).build())

        addDrawableChild(ButtonWidget.builder(Text.literal("+")) {
            client.networkHandler?.sendPacket(
                ButtonClickC2SPacket(handler.syncId, SplitterCableScreenHandler.BUTTON_THRESHOLD_PLUS)
            )
        }.dimensions(x + 117, y + 19, 24, 20).build())

        invertedButton = addDrawableChild(ButtonWidget.builder(invertedButtonText()) {
            client.networkHandler?.sendPacket(
                ButtonClickC2SPacket(handler.syncId, SplitterCableScreenHandler.BUTTON_TOGGLE_INVERTED)
            )
        }.dimensions(x + 48, y + 47, 80, 20).build())
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
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

    private fun invertedButtonText(): Text = if (handler.inverted) {
        Text.translatable("gui.ic2_120.splitter_cable.inverted.on")
    } else {
        Text.translatable("gui.ic2_120.splitter_cable.inverted.off")
    }

    companion object {
        private val TEXTURE = Identifier("ic2", "textures/gui/guilimitercable.png")
        private const val TEX_SIZE = 256
    }
}
