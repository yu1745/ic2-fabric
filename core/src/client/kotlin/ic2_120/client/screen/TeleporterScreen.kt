package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.t
import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.TeleporterBlock
import ic2_120.content.screen.TeleporterScreenHandler
import ic2_120.content.screen.GuiSize
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
import net.minecraft.text.Text as McText

@ModScreen(block = TeleporterBlock::class)
class TeleporterScreen(
    handler: TeleporterScreenHandler,
    playerInventory: PlayerInventory,
    title: McText
) : HandledScreen<TeleporterScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = GUI_SIZE.width
        backgroundHeight = GUI_SIZE.height
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context,
            x,
            y,
            GUI_SIZE.playerInvY,
            GUI_SIZE.hotbarY,
            GuiSize.SLOT_SIZE
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y
        val targetText = if (handler.sync.targetSet != 0) {
            t("gui.ic2_120.teleporter.target_set", handler.sync.targetX, handler.sync.targetY, handler.sync.targetZ)
        } else {
            t("gui.ic2_120.teleporter.target_unset")
        }
        val cooldownText = t("gui.ic2_120.teleporter.cooldown", handler.sync.cooldown)
        val range = handler.sync.teleportRange.coerceIn(1, 3)
        val rangeText = t("gui.ic2_120.teleporter.range", range)

        val content: UiScope.() -> Unit = {
            Column(
                x = left + 8,
                y = top + 8,
                spacing = 6,
                modifier = Modifier().width(GUI_SIZE.contentWidth)
            ) {
                Text(title.string, color = 0xFFFFFF)
                Flex(
                    justifyContent = JustifyContent.SPACE_BETWEEN,
                    alignItems = AlignItems.CENTER,
                ) {
                    Text(targetText, color = 0xFFFFFF)
                    Text(cooldownText, color = 0xFFFFFF)
                }
                Flex(
                    justifyContent = JustifyContent.CENTER,
                    alignItems = AlignItems.CENTER,
                    gap = 6
                ) {
                    Button("-", onClick = {
                        client?.player?.networkHandler?.sendPacket(
                            ButtonClickC2SPacket(handler.syncId, TeleporterScreenHandler.BUTTON_ID_RANGE_DEC)
                        )
                    })
                    Text(rangeText, color = 0xFFFFFF)
                    Button("+", onClick = {
                        client?.player?.networkHandler?.sendPacket(
                            ButtonClickC2SPacket(handler.syncId, TeleporterScreenHandler.BUTTON_ID_RANGE_INC)
                        )
                    })
                }
            }

            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = TeleporterScreenHandler.PLAYER_INV_START,
                playerInvY = GUI_SIZE.playerInvY,
                hotbarY = GUI_SIZE.hotbarY
            )
        }

        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)
        handler.slots.forEachIndexed { index, slot ->
            val anchor = layout.anchors["slot.$index"] ?: return@forEachIndexed
            slot.x = anchor.x - left
            slot.y = anchor.y - top
        }

        super.render(context, mouseX, mouseY, delta)
        ui.render(context, textRenderer, mouseX, mouseY, content = content)
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    companion object {
        private val GUI_SIZE = GuiSize.STANDARD
    }
}
