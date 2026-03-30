package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
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
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1L)
        val fraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val targetText = if (handler.sync.targetSet != 0) {
            "目标: ${handler.sync.targetX}, ${handler.sync.targetY}, ${handler.sync.targetZ}"
        } else {
            "目标: 未设置"
        }
        val cooldownText = "冷却: ${handler.sync.cooldown}t"
        val range = handler.sync.teleportRange.coerceIn(1, 3)
        val rangeText = "激活范围: ${range}x${range}x${range}"

        val content: UiScope.() -> Unit = {
            Column(
                x = left + 8,
                y = top + 8,
                spacing = 6,
                modifier = Modifier().width(GUI_SIZE.contentWidth)
            ) {
                Flex(
                    alignItems = AlignItems.CENTER,
                    gap = 2,
                ) {
                    Text(title.string, color = 0xFFFFFF)
                    EnergyBar(fraction, barHeight = 8, modifier = Modifier().fractionWidth(1f))
                    Text(
                        "${EnergyFormatUtils.formatEu(energy)} / ${EnergyFormatUtils.formatEu(cap)} EU",
                        color = 0xFFFFFF
                    )
                }
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
