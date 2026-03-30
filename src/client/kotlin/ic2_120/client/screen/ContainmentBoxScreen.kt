package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.content.screen.GuiSize
import ic2_120.client.ui.GuiBackground
import ic2_120.content.item.ContainmentBoxInventory
import ic2_120.content.screen.ContainmentBoxScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

/**
 * 防辐射容纳盒：12 格 + 玩家物品栏。
 */
@ModScreen(handler = "containment_box")
class ContainmentBoxScreen(
    handler: ContainmentBoxScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<ContainmentBoxScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()
    private val gui = GuiSize.STANDARD

    init {
        backgroundWidth = gui.width
        backgroundHeight = gui.height
        titleY = 6
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        val inset = GuiBackground.SLOT_ANCHOR_INSET
        val slotSize = GuiSize.SLOT_SIZE
        for (i in 0 until ContainmentBoxInventory.SIZE) {
            val slot = handler.slots[i]
            GuiBackground.drawVanillaLikeSlot(
                context,
                x + slot.x - inset,
                y + slot.y - inset,
                slotSize,
                slotSize
            )
        }
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y,
            gui.playerInvY,
            gui.hotbarY,
            slotSize
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y
        val content: UiScope.() -> Unit = {
            for (i in 0 until ContainmentBoxInventory.SIZE) {
                val slot = handler.slots[i]
                SlotAnchor(
                    id = "slot.$i",
                    x = left + slot.x,
                    y = top + slot.y,
                    width = GuiSize.SLOT_SIZE,
                    height = GuiSize.SLOT_SIZE,
                    absolute = true
                )
            }
            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = ContainmentBoxScreenHandler.PLAYER_INV_START,
                playerInvY = gui.playerInvY,
                hotbarY = gui.hotbarY
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
}
