package ic2_120.client.screen

import ic2_120.client.t
import ic2_120.content.screen.LimiterCableScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.components.Button
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.client.Minecraft

@ModScreen(handler = "limiter_cable")
class LimiterCableScreen(
    val handler: LimiterCableScreenHandler,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<LimiterCableScreenHandler>(handler, playerInventory, title) {

    init {
        imageWidth = 176
        imageHeight = 166
    }

    override fun renderBg(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(TEXTURE, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, TEX_SIZE, TEX_SIZE)
    }

    override fun init() {
        super.init()
        val client = minecraft ?: return

        // Row 1: -1, -10, -100, -1000 (y=16)
        addRenderableWidget(Button.builder(Component.literal("-1")) {
            Minecraft.getInstance().getConnection()?.send(ServerboundContainerButtonClickPacket(menu.containerId, LimiterCableScreenHandler.BUTTON_MINUS_1))
        }.bounds(leftPos + 6, topPos + 16, 15, 15).build())

        addRenderableWidget(Button.builder(Component.literal("-10")) {
            Minecraft.getInstance().getConnection()?.send(ServerboundContainerButtonClickPacket(menu.containerId, LimiterCableScreenHandler.BUTTON_MINUS_10))
        }.bounds(leftPos + 24, topPos + 16, 25, 15).build())

        addRenderableWidget(Button.builder(Component.literal("-100")) {
            Minecraft.getInstance().getConnection()?.send(ServerboundContainerButtonClickPacket(menu.containerId, LimiterCableScreenHandler.BUTTON_MINUS_100))
        }.bounds(leftPos + 52, topPos + 16, 30, 15).build())

        addRenderableWidget(Button.builder(Component.literal("-1000")) {
            Minecraft.getInstance().getConnection()?.send(ServerboundContainerButtonClickPacket(menu.containerId, LimiterCableScreenHandler.BUTTON_MINUS_1000))
        }.bounds(leftPos + 85, topPos + 16, 35, 15).build())

        // Row 2: +1, +10, +100, +1000 (y=41)
        addRenderableWidget(Button.builder(Component.literal("+1")) {
            Minecraft.getInstance().getConnection()?.send(ServerboundContainerButtonClickPacket(menu.containerId, LimiterCableScreenHandler.BUTTON_PLUS_1))
        }.bounds(leftPos + 6, topPos + 41, 15, 15).build())

        addRenderableWidget(Button.builder(Component.literal("+10")) {
            Minecraft.getInstance().getConnection()?.send(ServerboundContainerButtonClickPacket(menu.containerId, LimiterCableScreenHandler.BUTTON_PLUS_10))
        }.bounds(leftPos + 24, topPos + 41, 25, 15).build())

        addRenderableWidget(Button.builder(Component.literal("+100")) {
            Minecraft.getInstance().getConnection()?.send(ServerboundContainerButtonClickPacket(menu.containerId, LimiterCableScreenHandler.BUTTON_PLUS_100))
        }.bounds(leftPos + 52, topPos + 41, 30, 15).build())

        addRenderableWidget(Button.builder(Component.literal("+1000")) {
            Minecraft.getInstance().getConnection()?.send(ServerboundContainerButtonClickPacket(menu.containerId, LimiterCableScreenHandler.BUTTON_PLUS_1000))
        }.bounds(leftPos + 85, topPos + 41, 35, 15).build())
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        context.drawString(font, title, leftPos + (imageWidth - font.width(title)) / 2, topPos + 6, 0x404040, false)

        val limit = menu.limit
        val limitDisplay = if (limit > 0) {
            t("gui.ic2_120.limiter_cable.limit_value", limit)
        } else {
            t("gui.ic2_120.limiter_cable.limit_unlimited")
        }
        val textWidth = font.width(limitDisplay)
        val areaCenterX = 41 + (136 - 41) / 2
        val areaCenterY = 65 + (79 - 65) / 2
        context.drawString(font, limitDisplay,
            leftPos + areaCenterX - textWidth / 2,
            topPos + areaCenterY - font.lineHeight / 2,
            0x55FF55, false)

        renderTooltip(context, mouseX, mouseY)
    }

    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui/guilimitercable.png")
        private const val TEX_SIZE = 256
    }
}
