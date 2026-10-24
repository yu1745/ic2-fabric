package ic2_120.client.screen

import ic2_120.client.t
import ic2_120.content.screen.LimiterCableScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.components.Button
import net.minecraft.world.entity.player.Container
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

@ModScreen(handler = "limiter_cable")
class LimiterCableScreen(
    handler: LimiterCableScreenHandler,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<LimiterCableScreenHandler>(handler, playerInventory, title) {

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

        // Row 1: -1, -10, -100, -1000 (y=16)
        addDrawableChild(Button.builder(Component.literal("-1")) {
            client.networkHandler?.sendPacket(ServerboundContainerButtonClickPacket(handler.syncId, LimiterCableScreenHandler.BUTTON_MINUS_1))
        }.dimensions(x + 6, y + 16, 15, 15).build())

        addDrawableChild(Button.builder(Component.literal("-10")) {
            client.networkHandler?.sendPacket(ServerboundContainerButtonClickPacket(handler.syncId, LimiterCableScreenHandler.BUTTON_MINUS_10))
        }.dimensions(x + 24, y + 16, 25, 15).build())

        addDrawableChild(Button.builder(Component.literal("-100")) {
            client.networkHandler?.sendPacket(ServerboundContainerButtonClickPacket(handler.syncId, LimiterCableScreenHandler.BUTTON_MINUS_100))
        }.dimensions(x + 52, y + 16, 30, 15).build())

        addDrawableChild(Button.builder(Component.literal("-1000")) {
            client.networkHandler?.sendPacket(ServerboundContainerButtonClickPacket(handler.syncId, LimiterCableScreenHandler.BUTTON_MINUS_1000))
        }.dimensions(x + 85, y + 16, 35, 15).build())

        // Row 2: +1, +10, +100, +1000 (y=41)
        addDrawableChild(Button.builder(Component.literal("+1")) {
            client.networkHandler?.sendPacket(ServerboundContainerButtonClickPacket(handler.syncId, LimiterCableScreenHandler.BUTTON_PLUS_1))
        }.dimensions(x + 6, y + 41, 15, 15).build())

        addDrawableChild(Button.builder(Component.literal("+10")) {
            client.networkHandler?.sendPacket(ServerboundContainerButtonClickPacket(handler.syncId, LimiterCableScreenHandler.BUTTON_PLUS_10))
        }.dimensions(x + 24, y + 41, 25, 15).build())

        addDrawableChild(Button.builder(Component.literal("+100")) {
            client.networkHandler?.sendPacket(ServerboundContainerButtonClickPacket(handler.syncId, LimiterCableScreenHandler.BUTTON_PLUS_100))
        }.dimensions(x + 52, y + 41, 30, 15).build())

        addDrawableChild(Button.builder(Component.literal("+1000")) {
            client.networkHandler?.sendPacket(ServerboundContainerButtonClickPacket(handler.syncId, LimiterCableScreenHandler.BUTTON_PLUS_1000))
        }.dimensions(x + 85, y + 41, 35, 15).build())
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
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
        private val TEXTURE = ResourceLocation.of("ic2", "textures/gui/guilimitercable.png")
        private const val TEX_SIZE = 256
    }
}
