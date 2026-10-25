package ic2_120.client.screen

import ic2_120.content.block.EnergyOMatBlock
import ic2_120.content.screen.EnergyOMatScreenHandler
import ic2_120.registry.annotation.ModScreen
import ic2_120.client.compat.*
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.components.Button
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.client.Minecraft

@ModScreen(block = EnergyOMatBlock::class)
class EnergyOMatScreen(
    val handler: EnergyOMatScreenHandler,
    playerInventory: Inventory,
    title: Component
) : LegacyContainerScreen26<EnergyOMatScreenHandler>(handler, playerInventory, title, 176, 166) {

    private val isOwner = handler.isOwner
    private val buttons = mutableListOf<Button>()

    init {
    }

    override fun init() {
        super.init()
        buttons.clear()

        if (!isOwner) return

        val defs = listOf(
            EnergyOMatScreenHandler.BUTTON_OFFER_DOWN_BIG to "-100K",
            EnergyOMatScreenHandler.BUTTON_OFFER_DOWN_MID to "-10K",
            EnergyOMatScreenHandler.BUTTON_OFFER_DOWN_SMALL to "-1K",
            EnergyOMatScreenHandler.BUTTON_OFFER_UP_SMALL to "+1K",
            EnergyOMatScreenHandler.BUTTON_OFFER_UP_MID to "+10K",
            EnergyOMatScreenHandler.BUTTON_OFFER_UP_BIG to "+100K"
        )

       defs.forEach { (buttonId, label) ->
            val row = buttons.size / 3
            val col = buttons.size % 3
            val btn = Button.builder(Component.literal(label)) {
                Minecraft.getInstance().connection?.send(
                    ServerboundContainerButtonClickPacket(menu.containerId, buttonId)
                )
            }.bounds(leftPos + 88 + col * 30, topPos + 16 + row * 20, 28, 18).build()
            buttons.add(addRenderableWidget(btn))
        }
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

        if (isOwner) {
            // EU 单价
            val offerText = Component.literal("${menu.sync.euOffer} EU / item")
            context.drawString(font, offerText,
                leftPos + 88, topPos + 60, 0x404040, false)

            // 已支付额度
            val paidText = Component.literal("Paid: ${menu.sync.paidFor} EU")
            context.drawString(font, paidText,
                leftPos + 88, topPos + 72, 0x404040, false)

            // 缓冲能量
            val bufText = Component.literal("Buffer: ${menu.sync.energy} EU")
            context.drawString(font, bufText,
                leftPos + 88, topPos + 84, 0x404040, false)
        }

        renderTooltip(context, mouseX, mouseY)
    }

    companion object {
        private val TEXTURE_OPEN = Identifier.fromNamespaceAndPath("ic2_120", "textures/gui/guienergyomatopen.png")
        private val TEXTURE_CLOSED = Identifier.fromNamespaceAndPath("ic2_120", "textures/gui/guienergyomatclosed.png")
    }
}
