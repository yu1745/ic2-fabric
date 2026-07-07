package ic2_120.client.screen

import ic2_120.content.block.EnergyOMatBlock
import ic2_120.content.screen.EnergyOMatScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = EnergyOMatBlock::class)
class EnergyOMatScreen(
    handler: EnergyOMatScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<EnergyOMatScreenHandler>(handler, playerInventory, title) {

    private val isOwner = handler.isOwner
    private val buttons = mutableListOf<ButtonWidget>()

    init {
        backgroundWidth = 176
        backgroundHeight = 166
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
            val btn = ButtonWidget.builder(Text.literal(label)) {
                client?.player?.networkHandler?.sendPacket(
                    ButtonClickC2SPacket(handler.syncId, buttonId)
                )
            }.dimensions(x + 88 + col * 30, y + 16 + row * 20, 28, 18).build()
            buttons.add(addDrawableChild(btn))
        }
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(
            if (isOwner) TEXTURE_OPEN else TEXTURE_CLOSED,
            x, y, 0f, 0f, backgroundWidth, backgroundHeight, 256, 256
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        context.drawText(textRenderer, title,
            x + (backgroundWidth - textRenderer.getWidth(title)) / 2, y + 6, 0x404040, false)

        if (isOwner) {
            // EU 单价
            val offerText = Text.literal("${handler.sync.euOffer} EU / item")
            context.drawText(textRenderer, offerText,
                x + 88, y + 60, 0x404040, false)

            // 已支付额度
            val paidText = Text.literal("Paid: ${handler.sync.paidFor} EU")
            context.drawText(textRenderer, paidText,
                x + 88, y + 72, 0x404040, false)

            // 缓冲能量
            val bufText = Text.literal("Buffer: ${handler.sync.energy} EU")
            context.drawText(textRenderer, bufText,
                x + 88, y + 84, 0x404040, false)
        }

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    companion object {
        private val TEXTURE_OPEN = Identifier("ic2_120", "textures/gui/guienergyomatopen.png")
        private val TEXTURE_CLOSED = Identifier("ic2_120", "textures/gui/guienergyomatclosed.png")
    }
}
