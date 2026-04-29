package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.t
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.CokeKilnBlock
import ic2_120.content.screen.CokeKilnScreenHandler
import ic2_120.content.screen.GuiSize
import ic2_120.content.sync.CokeKilnSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

@ModScreen(block = CokeKilnBlock::class)
class CokeKilnScreen(
    handler: CokeKilnScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<CokeKilnScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = GUI_SIZE.width
        backgroundHeight = GUI_SIZE.height
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context = context,
            screenX = x,
            screenY = y,
            playerInvY = GUI_SIZE.playerInvY,
            hotbarY = GUI_SIZE.hotbarY,
            slotSize = GuiSize.SLOT_SIZE
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y
        val progressFrac = if (CokeKilnSync.PROGRESS_MAX > 0) {
            (handler.sync.progress.coerceIn(0, CokeKilnSync.PROGRESS_MAX).toFloat() / CokeKilnSync.PROGRESS_MAX).coerceIn(0f, 1f)
        } else 0f
        val structureText = if (handler.sync.structureValid > 0) t("gui.ic2_120.coke_kiln.structure_valid") else t("gui.ic2_120.coke_kiln.structure_invalid")

        val content: UiScope.() -> Unit = {
            Column(
                x = left + 8,
                y = top + 8,
                spacing = 6,
                modifier = Modifier.EMPTY.width(GUI_SIZE.contentWidth)
            ) {
                Text(title.string, color = 0xFFFFFF)
                Text(structureText, color = 0xAAAAAA, shadow = false)
                EnergyBar(progressFrac, barHeight = 10)
                Flex(direction = FlexDirection.ROW, alignItems = AlignItems.CENTER, gap = 6) {
                    SlotAnchor(id = slotAnchorId(CokeKilnScreenHandler.SLOT_INPUT), width = CokeKilnScreenHandler.SLOT_SIZE, height = CokeKilnScreenHandler.SLOT_SIZE)
                    Text("->", color = 0xFFFFFF)
                    SlotAnchor(id = slotAnchorId(CokeKilnScreenHandler.SLOT_OUTPUT), width = CokeKilnScreenHandler.SLOT_SIZE, height = CokeKilnScreenHandler.SLOT_SIZE)
                }
            }
            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = CokeKilnScreenHandler.PLAYER_INV_START,
                playerInvY = GUI_SIZE.playerInvY,
                hotbarY = GUI_SIZE.hotbarY
            )
        }

        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)
        handler.slots.forEachIndexed { index, slot ->
            val anchor = layout.anchors[slotAnchorId(index)] ?: return@forEachIndexed
            slot.x = anchor.x - left
            slot.y = anchor.y - top
        }

        super.render(context, mouseX, mouseY, delta)
        ui.render(context, textRenderer, mouseX, mouseY, content = content)
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun slotAnchorId(slotIndex: Int): String = "slot.$slotIndex"

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    companion object {
        private val GUI_SIZE = GuiSize.STANDARD
    }
}
