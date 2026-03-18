package ic2_120.client.screen

import ic2_120.client.compose.ComposeUI
import ic2_120.client.ui.FluidBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.nuclear.ReactorFluidPortBlock
import ic2_120.content.screen.ReactorFluidPortScreenHandler
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
        backgroundWidth = 176
        backgroundHeight = 166
        titleY = 4
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context,
            x,
            y,
            ReactorFluidPortScreenHandler.PLAYER_INV_Y,
            ReactorFluidPortScreenHandler.HOTBAR_Y,
            ReactorFluidPortScreenHandler.SLOT_SIZE
        )

        val borderColor = GuiBackground.BORDER_COLOR
        val borderOffset = 1
        val slotSize = ReactorFluidPortScreenHandler.SLOT_SIZE

        // 绘制升级槽边框（只有 1 个）
        val slot = handler.slots[ReactorFluidPortScreenHandler.UPGRADE_SLOT_INDEX]
        context.drawBorder(x + slot.x - borderOffset, y + slot.y - borderOffset, slotSize, slotSize, borderColor)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)

        val left = x
        val top = y
        val centerX = left + backgroundWidth / 2

        // 绘制标题
        drawCenteredText(context, title.string, centerX, top + 6, 0xFFFFFF, shadow = true)
        drawCenteredText(context, "流体接口", centerX, top + 14, 0xAAAAAA)

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

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
}
