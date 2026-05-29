package ic2_120.client.screen

import ic2_120.client.t
import ic2_120.content.screen.LimiterCableScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(handler = "limiter_cable")
class LimiterCableScreen(
    handler: LimiterCableScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<LimiterCableScreenHandler>(handler, playerInventory, title) {

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

        // Row 1: -1, -10, -100, -1000 (y=16)
        addDrawableChild(ButtonWidget.builder(Text.literal("-1")) {
            client.networkHandler?.sendPacket(ButtonClickC2SPacket(handler.syncId, LimiterCableScreenHandler.BUTTON_MINUS_1))
        }.dimensions(x + 6, y + 16, 15, 15).build())

        addDrawableChild(ButtonWidget.builder(Text.literal("-10")) {
            client.networkHandler?.sendPacket(ButtonClickC2SPacket(handler.syncId, LimiterCableScreenHandler.BUTTON_MINUS_10))
        }.dimensions(x + 24, y + 16, 25, 15).build())

        addDrawableChild(ButtonWidget.builder(Text.literal("-100")) {
            client.networkHandler?.sendPacket(ButtonClickC2SPacket(handler.syncId, LimiterCableScreenHandler.BUTTON_MINUS_100))
        }.dimensions(x + 52, y + 16, 30, 15).build())

        addDrawableChild(ButtonWidget.builder(Text.literal("-1000")) {
            client.networkHandler?.sendPacket(ButtonClickC2SPacket(handler.syncId, LimiterCableScreenHandler.BUTTON_MINUS_1000))
        }.dimensions(x + 85, y + 16, 35, 15).build())

        // Row 2: +1, +10, +100, +1000 (y=41)
        addDrawableChild(ButtonWidget.builder(Text.literal("+1")) {
            client.networkHandler?.sendPacket(ButtonClickC2SPacket(handler.syncId, LimiterCableScreenHandler.BUTTON_PLUS_1))
        }.dimensions(x + 6, y + 41, 15, 15).build())

        addDrawableChild(ButtonWidget.builder(Text.literal("+10")) {
            client.networkHandler?.sendPacket(ButtonClickC2SPacket(handler.syncId, LimiterCableScreenHandler.BUTTON_PLUS_10))
        }.dimensions(x + 24, y + 41, 25, 15).build())

        addDrawableChild(ButtonWidget.builder(Text.literal("+100")) {
            client.networkHandler?.sendPacket(ButtonClickC2SPacket(handler.syncId, LimiterCableScreenHandler.BUTTON_PLUS_100))
        }.dimensions(x + 52, y + 41, 30, 15).build())

        addDrawableChild(ButtonWidget.builder(Text.literal("+1000")) {
            client.networkHandler?.sendPacket(ButtonClickC2SPacket(handler.syncId, LimiterCableScreenHandler.BUTTON_PLUS_1000))
        }.dimensions(x + 85, y + 41, 35, 15).build())
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        context.drawText(textRenderer, title, x + (backgroundWidth - textRenderer.getWidth(title)) / 2, y + 6, 0x404040, false)

        val limit = handler.limit
        val limitDisplay = if (limit > 0) {
            t("gui.ic2_120.limiter_cable.limit_value", limit)
        } else {
            t("gui.ic2_120.limiter_cable.limit_unlimited")
        }
        val textWidth = textRenderer.getWidth(limitDisplay)
        val areaCenterX = 41 + (136 - 41) / 2
        val areaCenterY = 65 + (79 - 65) / 2
        context.drawText(textRenderer, limitDisplay,
            x + areaCenterX - textWidth / 2,
            y + areaCenterY - textRenderer.fontHeight / 2,
            0x55FF55, false)

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    companion object {
        private val TEXTURE = Identifier.of("ic2", "textures/gui/guilimitercable.png")
        private const val TEX_SIZE = 256
    }
}
