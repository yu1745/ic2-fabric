package ic2_120.client.screen

import ic2_120.client.compose.GuiSize
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
            GuiSize.PLAYER_INVENTORY_Y,
            GuiSize.HOTBAR_Y,
            slotSize
        )
    }
}
