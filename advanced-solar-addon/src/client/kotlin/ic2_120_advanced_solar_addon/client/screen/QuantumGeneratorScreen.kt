package ic2_120_advanced_solar_addon.client.screen

import ic2_120.client.compose.*
import ic2_120.client.EnergyFormatUtils
import ic2_120.client.ui.GuiBackground
import ic2_120.content.screen.GuiSize
import ic2_120_advanced_solar_addon.content.block.QuantumGeneratorBlock
import ic2_120_advanced_solar_addon.content.screen.QuantumGeneratorScreenHandler
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.slot.Slot
import net.minecraft.text.Text
import ic2_120.registry.annotation.ModScreen

@ModScreen(block = QuantumGeneratorBlock::class)
class QuantumGeneratorScreen(
    handler: QuantumGeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<QuantumGeneratorScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = GUI_SIZE.width
        backgroundHeight = GUI_SIZE.height
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y,
            GUI_SIZE.playerInvY,
            GUI_SIZE.hotbarY,
            GuiSize.SLOT_SIZE
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y
        val production = handler.sync.production
        val tier = handler.sync.tierLevel
        val active = handler.sync.isActive == 1

        val content: UiScope.() -> Unit = {
            Column(
                x = left + 8,
                y = top + 8,
                spacing = 6,
                modifier = Modifier.EMPTY.width(GUI_SIZE.contentWidth)
            ) {
                Text(title.string, color = 0xFFFFFF)

                Flex(direction = FlexDirection.ROW, alignItems = AlignItems.CENTER, gap = 8) {
                    Text(Text.translatable("gui.ic2_120_advanced_solar_addon.max_output").string + " ", color = 0xAAAAAA)
                    Text("${EnergyFormatUtils.formatEu(production)} EU/t", color = 0xFFFFFF)
                }

                Flex(direction = FlexDirection.ROW, alignItems = AlignItems.CENTER, gap = 8) {
                    Text(Text.translatable("gui.ic2_120_advanced_solar_addon.tier").string + ": ", color = 0xAAAAAA)
                    Text("$tier", color = 0xFFFFFF)
                    Text(Text.translatable("gui.ic2_120_advanced_solar_addon.status").string + ": ", color = 0xAAAAAA)
                    val statusText = if (active) Text.translatable("gui.ic2_120_advanced_solar_addon.qg_status.active").string
                                    else Text.translatable("gui.ic2_120_advanced_solar_addon.qg_status.disabled").string
                    val statusColor = if (active) 0x00FF00 else 0xFF0000
                    Text(statusText, color = statusColor)
                }
            }

            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = QuantumGeneratorScreenHandler.PLAYER_INV_START,
                playerInvY = GUI_SIZE.playerInvY,
                hotbarY = GUI_SIZE.hotbarY
            )
        }

        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)
        applyAnchoredSlots(layout, left, top)

        super.render(context, mouseX, mouseY, delta)
        ui.render(context, textRenderer, mouseX, mouseY, content = content)
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun applyAnchoredSlots(layout: ComposeUI.LayoutSnapshot, left: Int, top: Int) {
        handler.slots.forEachIndexed { index, slot ->
            val anchor = layout.anchors[slotAnchorId(index)] ?: return@forEachIndexed
            slot.x = anchor.x - left
            slot.y = anchor.y - top
        }
    }

    private fun slotAnchorId(slotIndex: Int): String = "slot.$slotIndex"

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (ui.mouseClicked(mouseX, mouseY, button)) return true
        return super.mouseClicked(mouseX, mouseY, button)
    }

    companion object {
        private val GUI_SIZE = GuiSize.STANDARD
    }
}
