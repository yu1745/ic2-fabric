package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.t
import ic2_120.content.screen.GuiSize
import ic2_120.content.screen.LimiterCableScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
import ic2_120.client.ui.GuiBackground

@ModScreen(handler = "limiter_cable")
class LimiterCableScreen(
    handler: LimiterCableScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<LimiterCableScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()
    private val gui = GuiSize.STANDARD

    init {
        backgroundWidth = gui.width
        backgroundHeight = gui.height
        titleY = 6
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y
        val client = client!!
        val limit = handler.limit

        val limitDisplay = if (limit >= 0) {
            t("gui.ic2_120.limiter_cable.limit_value", limit)
        } else {
            t("gui.ic2_120.limiter_cable.limit_unlimited")
        }

        val content: UiScope.() -> Unit = {
            // Title: current limit value
            Text(
                x = left + 8,
                y = top + 18,
                text = limitDisplay,
                color = 0xFFFFFF,
                shadow = false
            )

            // Row 1: -1000, -100, -10, -1
            Flex(
                x = left + 8,
                y = top + 38,
                gap = 4
            ) {
                Button(
                    text = "-1000",
                    modifier = Modifier().width(46),
                    onClick = {
                        client.networkHandler?.sendPacket(
                            ButtonClickC2SPacket(handler.syncId, LimiterCableScreenHandler.BUTTON_MINUS_1000)
                        )
                    }
                )
                Button(
                    text = "-100",
                    modifier = Modifier().width(40),
                    onClick = {
                        client.networkHandler?.sendPacket(
                            ButtonClickC2SPacket(handler.syncId, LimiterCableScreenHandler.BUTTON_MINUS_100)
                        )
                    }
                )
                Button(
                    text = "-10",
                    modifier = Modifier().width(36),
                    onClick = {
                        client.networkHandler?.sendPacket(
                            ButtonClickC2SPacket(handler.syncId, LimiterCableScreenHandler.BUTTON_MINUS_10)
                        )
                    }
                )
                Button(
                    text = "-1",
                    modifier = Modifier().width(32),
                    onClick = {
                        client.networkHandler?.sendPacket(
                            ButtonClickC2SPacket(handler.syncId, LimiterCableScreenHandler.BUTTON_MINUS_1)
                        )
                    }
                )
            }

            // Row 2: +1, +10, +100, +1000
            Flex(
                x = left + 8,
                y = top + 60,
                gap = 4
            ) {
                Button(
                    text = "+1",
                    modifier = Modifier().width(32),
                    onClick = {
                        client.networkHandler?.sendPacket(
                            ButtonClickC2SPacket(handler.syncId, LimiterCableScreenHandler.BUTTON_PLUS_1)
                        )
                    }
                )
                Button(
                    text = "+10",
                    modifier = Modifier().width(36),
                    onClick = {
                        client.networkHandler?.sendPacket(
                            ButtonClickC2SPacket(handler.syncId, LimiterCableScreenHandler.BUTTON_PLUS_10)
                        )
                    }
                )
                Button(
                    text = "+100",
                    modifier = Modifier().width(40),
                    onClick = {
                        client.networkHandler?.sendPacket(
                            ButtonClickC2SPacket(handler.syncId, LimiterCableScreenHandler.BUTTON_PLUS_100)
                        )
                    }
                )
                Button(
                    text = "+1000",
                    modifier = Modifier().width(46),
                    onClick = {
                        client.networkHandler?.sendPacket(
                            ButtonClickC2SPacket(handler.syncId, LimiterCableScreenHandler.BUTTON_PLUS_1000)
                        )
                    }
                )
            }

            // Player inventory
            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = 0,
                playerInvY = gui.playerInvY,
                hotbarY = gui.hotbarY
            )
        }

        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)
        applyAnchoredSlots(layout, left, top)

        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y, gui.playerInvY, gui.hotbarY, GuiSize.SLOT_SIZE
        )

        ui.render(context, textRenderer, mouseX, mouseY, content = content)
        super.render(context, mouseX, mouseY, delta)
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    private fun applyAnchoredSlots(layout: ComposeUI.LayoutSnapshot, left: Int, top: Int) {
        handler.slots.forEachIndexed { index, slot ->
            val anchor = layout.anchors[slotAnchorId(index)] ?: return@forEachIndexed
            slot.x = anchor.x - left
            slot.y = anchor.y - top
        }
    }

    private fun slotAnchorId(slotIndex: Int): String = "slot.$slotIndex"
}
