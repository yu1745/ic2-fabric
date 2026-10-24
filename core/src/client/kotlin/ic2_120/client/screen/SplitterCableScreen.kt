package ic2_120.client.screen

import ic2_120.client.t
import ic2_120.content.screen.SplitterCableScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.components.Button
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.client.Minecraft

@ModScreen(handler = "splitter_cable")
class SplitterCableScreen(
    val handler: SplitterCableScreenHandler,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<SplitterCableScreenHandler>(handler, playerInventory, title) {

    private lateinit var invertedButton: Button

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

        addRenderableWidget(Button.builder(Component.literal("-")) {
            Minecraft.getInstance().getConnection()?.send(
                ServerboundContainerButtonClickPacket(menu.containerId, SplitterCableScreenHandler.BUTTON_THRESHOLD_MINUS)
            )
        }.bounds(leftPos + 35, topPos + 19, 24, 20).build())

        addRenderableWidget(Button.builder(Component.literal("+")) {
            Minecraft.getInstance().getConnection()?.send(
                ServerboundContainerButtonClickPacket(menu.containerId, SplitterCableScreenHandler.BUTTON_THRESHOLD_PLUS)
            )
        }.bounds(leftPos + 117, topPos + 19, 24, 20).build())

        invertedButton = addRenderableWidget(Button.builder(invertedButtonText()) {
            Minecraft.getInstance().getConnection()?.send(
                ServerboundContainerButtonClickPacket(menu.containerId, SplitterCableScreenHandler.BUTTON_TOGGLE_INVERTED)
            )
        }.bounds(leftPos + 48, topPos + 47, 80, 20).build())
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        if (::invertedButton.isInitialized) invertedButton.message = invertedButtonText()
        super.render(context, mouseX, mouseY, delta)

        context.drawString(
            font, title,
            leftPos + (imageWidth - font.width(title)) / 2, topPos + 6,
            0x404040, false
        )

        val thresholdText = t("gui.ic2_120.splitter_cable.threshold", menu.threshold)
        context.drawString(
            font, thresholdText,
            leftPos + (imageWidth - font.width(thresholdText)) / 2, topPos + 25,
            0x55FF55, false
        )
        renderTooltip(context, mouseX, mouseY)
    }

    private fun invertedButtonText(): Component = if (menu.inverted) {
        Component.translatable("gui.ic2_120.splitter_cable.inverted.on")
    } else {
        Component.translatable("gui.ic2_120.splitter_cable.inverted.off")
    }

    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui/guilimitercable.png")
        private const val TEX_SIZE = 256
    }
}
