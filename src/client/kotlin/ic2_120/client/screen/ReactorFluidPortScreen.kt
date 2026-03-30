package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.nuclear.ReactorFluidPortBlock
import ic2_120.content.screen.ReactorFluidPortScreenHandler
import ic2_120.content.screen.GuiSize
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text as McText

/**
 * 反应堆流体接口的 Screen。
 * 显示升级槽和中心反应堆的流体状态。
 */
@ModScreen(block = ReactorFluidPortBlock::class)
class ReactorFluidPortScreen(
    handler: ReactorFluidPortScreenHandler,
    playerInventory: PlayerInventory,
    title: McText
) : HandledScreen<ReactorFluidPortScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = GUI_SIZE.width
        backgroundHeight = GUI_SIZE.height
        titleY = 4
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

        val slot = handler.slots[ReactorFluidPortScreenHandler.UPGRADE_SLOT_INDEX]
        context.drawBorder(
            x + slot.x - 1,
            y + slot.y - 1,
            ReactorFluidPortScreenHandler.SLOT_SIZE,
            ReactorFluidPortScreenHandler.SLOT_SIZE,
            GuiBackground.BORDER_COLOR
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y
        val content: UiScope.() -> Unit = {
            SlotHost(ReactorFluidPortScreenHandler.UPGRADE_SLOT_INDEX)
            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = ReactorFluidPortScreenHandler.PLAYER_INV_START,
                playerInvY = GUI_SIZE.playerInvY,
                hotbarY = GUI_SIZE.hotbarY
            )
        }
        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)
        applyAnchoredSlots(layout, left, top)

        super.render(context, mouseX, mouseY, delta)

        val centerX = left + backgroundWidth / 2

        // 绘制标题
        drawCenteredText(context, title.string, centerX, top + 6, 0xFFFFFF, shadow = true)
        drawCenteredText(context, "流体接口", centerX, top + 14, 0xAAAAAA)

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    private fun UiScope.SlotHost(slotIndex: Int) {
        SlotAnchor(
            id = slotAnchorId(slotIndex),
            x = x + (GUI_SIZE.width - ReactorFluidPortScreenHandler.SLOT_SIZE) / 2,
            y = y + 20,
            width = ReactorFluidPortScreenHandler.SLOT_SIZE,
            height = ReactorFluidPortScreenHandler.SLOT_SIZE,
            absolute = true
        )
    }

    private fun applyAnchoredSlots(layout: ComposeUI.LayoutSnapshot, left: Int, top: Int) {
        handler.slots.forEachIndexed { index, slot ->
            val anchor = layout.anchors[slotAnchorId(index)] ?: return@forEachIndexed
            slot.x = anchor.x - left
            slot.y = anchor.y - top
        }
    }

    private fun slotAnchorId(slotIndex: Int): String = "slot.$slotIndex"

    private fun drawCenteredText(
        context: DrawContext,
        text: String,
        centerX: Int,
        y: Int,
        color: Int,
        shadow: Boolean = false
    ) {
        val textX = centerX - textRenderer.getWidth(text) / 2
        if (shadow) context.drawTextWithShadow(textRenderer, text, textX, y, color)
        else context.drawText(textRenderer, text, textX, y, color, false)
    }

    companion object {
        private val GUI_SIZE = GuiSize.STANDARD
    }
}
