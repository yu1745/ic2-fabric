package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.ui.GuiBackground
import ic2_120.content.screen.PumpAttachmentScreenHandler
import ic2_120.content.screen.GuiSize
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

@ModScreen(handlers = ["bronze_pump_attachment", "carbon_pump_attachment"])
class PumpAttachmentScreen(
    handler: PumpAttachmentScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<PumpAttachmentScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = GUI_SIZE.width
        backgroundHeight = GUI_SIZE.height
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y, GUI_SIZE.playerInvY, GUI_SIZE.hotbarY, GuiSize.SLOT_SIZE
        )
        val slot = handler.slots[0]
        context.drawBorder(x + slot.x - 1, y + slot.y - 1, 18, 18, GuiBackground.BORDER_COLOR)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y
        val content: UiScope.() -> Unit = {
            Column(x = left + 8, y = top + 8, spacing = 6, modifier = Modifier.EMPTY.width(GUI_SIZE.contentWidth)) {
                Text(title.string, color = 0xFFFFFF)
                Row(spacing = 8) {
                    SlotHost(0)
                    Text("过滤样本", color = 0xCFCFCF, shadow = false)
                }
            }

            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = PumpAttachmentScreenHandler.PLAYER_INV_START,
                playerInvY = GUI_SIZE.playerInvY,
                hotbarY = GUI_SIZE.hotbarY
            )
        }
        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)
        applyAnchoredSlots(layout, left, top)
        super.render(context, mouseX, mouseY, delta)
        val stack = handler.slots[0].stack
        val line = if (stack.isEmpty) "当前: 任意流体" else "当前: ${stack.name.string}"
        context.drawText(textRenderer, line, x + 8, y + 42, 0xAAAAAA, false)
        ui.render(context, textRenderer, mouseX, mouseY, content = content)
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun UiScope.SlotHost(slotIndex: Int) {
        SlotAnchor(id = slotAnchorId(slotIndex), width = 18, height = 18)
    }

    private fun applyAnchoredSlots(layout: ComposeUI.LayoutSnapshot, left: Int, top: Int) {
        handler.slots.forEachIndexed { index, slot ->
            val anchor = layout.anchors[slotAnchorId(index)] ?: return@forEachIndexed
            slot.x = anchor.x - left
            slot.y = anchor.y - top
        }
    }

    private fun slotAnchorId(slotIndex: Int): String = "slot.$slotIndex"

    companion object {
        private val GUI_SIZE = GuiSize.ATTACHMENT
    }
}
