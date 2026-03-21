package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.MaceratorBlock
import ic2_120.content.screen.MaceratorScreenHandler
import ic2_120.content.sync.MaceratorSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.slot.Slot
import net.minecraft.text.Text

@ModScreen(block = MaceratorBlock::class)
class MaceratorScreen(
    handler: MaceratorScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<MaceratorScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = GUI_SIZE.width
        backgroundHeight = GUI_SIZE.height
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y,
            MaceratorScreenHandler.PLAYER_INV_Y,
            MaceratorScreenHandler.HOTBAR_Y,
            MaceratorScreenHandler.SLOT_SIZE
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val progressFrac = if (MaceratorSync.PROGRESS_MAX > 0) {
            (handler.sync.progress.coerceIn(0, MaceratorSync.PROGRESS_MAX)
                .toFloat() / MaceratorSync.PROGRESS_MAX).coerceIn(0f, 1f)
        } else 0f
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val consumeRate = handler.sync.getSyncedConsumedAmount()

        val inputText = "输入 ${formatEu(inputRate)} EU/t"
        val consumeText = "耗能 ${formatEu(consumeRate)} EU/t"
        val sideTextWidth = maxOf(textRenderer.getWidth(inputText), textRenderer.getWidth(consumeText))
        val sideTextX = left - sideTextWidth - 4

        val content: UiScope.() -> Unit = {
            Row(
                x = left + 8,
                y = top + 8,
                spacing = 8,
                modifier = Modifier.EMPTY.width(GUI_SIZE.contentWidth)
            ) {
                Column(
                    spacing = 6,
                    modifier = Modifier.EMPTY.width(GuiSize.STANDARD.contentWidth)
                ) {
                    Flex(direction = FlexDirection.ROW, alignItems = AlignItems.CENTER, gap = 8) {
                        Text(title.string, color = 0xFFFFFF)
                        Text("$energy / $cap EU", color = 0xFFFFFF, shadow = false)
                    }
                    EnergyBar(
                        energyFraction,
                        barHeight = 12,
                    )

                    Flex(
                        direction = FlexDirection.ROW,
                        alignItems = AlignItems.CENTER,
                        gap = 4
                    ) {
                        Column(spacing = 4) {
                            SlotHost(MaceratorScreenHandler.SLOT_INPUT_INDEX)
                            SlotHost(MaceratorScreenHandler.SLOT_DISCHARGING_INDEX)
                        }
                        EnergyBar(progressFrac, modifier = Modifier.EMPTY.fractionWidth(1.0f))
                        SlotHost(MaceratorScreenHandler.SLOT_OUTPUT_INDEX)
                    }
                }

                Column(
                    spacing = 4,
                    modifier = Modifier.EMPTY
                        .width(GuiSize.UPGRADE_COLUMN_WIDTH)
                        .padding(0, 8, 0, 0)
                ) {
                    for (slotIndex in MaceratorScreenHandler.SLOT_UPGRADE_INDEX_START..MaceratorScreenHandler.SLOT_UPGRADE_INDEX_END) {
                        SlotHost(slotIndex)
                    }
                }
            }
        }

        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)
        applyAnchoredSlots(layout, left, top)

        super.render(context, mouseX, mouseY, delta)
        ui.render(context, textRenderer, mouseX, mouseY, content = content)
        context.drawText(textRenderer, inputText, sideTextX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, consumeText, sideTextX, top + 20, 0xAAAAAA, false)
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun UiScope.SlotHost(slotIndex: Int) {
        SlotAnchor(
            id = slotAnchorId(slotIndex),
            width = MaceratorScreenHandler.SLOT_SIZE,
            height = MaceratorScreenHandler.SLOT_SIZE
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

    private fun formatEu(value: Long): String {
        return when {
            value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
            else -> value.toString()
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (ui.mouseClicked(mouseX, mouseY, button)) return true
        return super.mouseClicked(mouseX, mouseY, button)
    }

    companion object {
        private val GUI_SIZE = GuiSize.STANDARD_UPGRADE
    }
}
