package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.SolarGeneratorBlock
import ic2_120.content.block.machines.SolarGeneratorBlockEntity
import ic2_120.content.screen.SolarGeneratorScreenHandler
import ic2_120.content.sync.SolarGeneratorSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.slot.Slot
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = SolarGeneratorBlock::class)
class SolarGeneratorScreen(
    handler: SolarGeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<SolarGeneratorScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = GUI_SIZE.width
        backgroundHeight = GUI_SIZE.height
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y,
            SolarGeneratorScreenHandler.PLAYER_INV_Y,
            SolarGeneratorScreenHandler.HOTBAR_Y,
            SolarGeneratorScreenHandler.SLOT_SIZE
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val outputRate = handler.sync.getSyncedExtractedAmount()
        val cap = SolarGeneratorSync.ENERGY_CAPACITY
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val isGenerating = handler.sync.isGenerating != 0

        val inputText = "发电 ${formatEu(inputRate)} EU/t"
        val outputText = "输出 ${formatEu(outputRate)} EU/t"
        val sideTextWidth = maxOf(textRenderer.getWidth(inputText), textRenderer.getWidth(outputText))
        val sideTextX = left - sideTextWidth - 4

        // 绘制太阳图标
        val sunU = if (isGenerating) 16f else 0f
        context.drawTexture(
            Identifier("ic2", "textures/gui/overlay/solar_sun.png"),
            left + 8, top + 8,
            sunU, 0f,
            16, 16,
            32, 16
        )

        val content: UiScope.() -> Unit = {
            Column(
                x = left + 28,
                y = top + 8,
                spacing = 6,
                modifier = Modifier.EMPTY.width(GUI_SIZE.contentWidth - 20)
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
                    SlotAnchor(
                        id = slotAnchorId(SolarGeneratorBlockEntity.BATTERY_SLOT),
                        width = SolarGeneratorScreenHandler.SLOT_SIZE,
                        height = SolarGeneratorScreenHandler.SLOT_SIZE
                    )
                }
            }
        }

        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)
        applyAnchoredSlots(layout, left, top)

        super.render(context, mouseX, mouseY, delta)
        ui.render(context, textRenderer, mouseX, mouseY, content = content)
        context.drawText(textRenderer, inputText, sideTextX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, outputText, sideTextX, top + 20, 0xAAAAAA, false)
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

    private fun formatEu(value: Long): String {
        return when {
            value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
            else -> value.toString()
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    companion object {
        private val GUI_SIZE = GuiSize.STANDARD
    }
}


