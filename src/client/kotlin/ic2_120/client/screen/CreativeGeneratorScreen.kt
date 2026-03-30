package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.EnergyFormatUtils
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.CreativeGeneratorBlock
import ic2_120.content.block.machines.CreativeGeneratorBlockEntity
import ic2_120.content.screen.CreativeGeneratorScreenHandler
import ic2_120.content.screen.GuiSize
import ic2_120.content.sync.CreativeGeneratorSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.slot.Slot
import net.minecraft.text.Text as McText

@ModScreen(block = CreativeGeneratorBlock::class)
class CreativeGeneratorScreen(
    handler: CreativeGeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: McText
) : HandledScreen<CreativeGeneratorScreenHandler>(handler, playerInventory, title) {

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
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = CreativeGeneratorSync.ENERGY_CAPACITY
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val generationRate = handler.sync.getSyncedInsertedAmount().coerceAtLeast(0L)
        val outputRate = handler.sync.getSyncedExtractedAmount()
        val total = handler.sync.getTotalGenerated()

        val generationText = "发电 ${EnergyFormatUtils.formatEu(generationRate)} EU/t"
        val outputText = "输出 ${EnergyFormatUtils.formatEu(outputRate)} EU/t"
        val totalText = "累计 ${EnergyFormatUtils.formatEu(total)} EU"
        val sideTextWidth = maxOf(
            textRenderer.getWidth(generationText),
            textRenderer.getWidth(outputText),
            textRenderer.getWidth(totalText)
        )
        val sideTextX = left - sideTextWidth - 4

        val content: UiScope.() -> Unit = {
            Column(
                x = left + 8,
                y = top + 8,
                spacing = 6,
                modifier = Modifier.EMPTY.width(GUI_SIZE.contentWidth)
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
                    justifyContent = JustifyContent.CENTER,
                    alignItems = AlignItems.CENTER,
                    gap = 4
                ) {
                    SlotAnchor(
                        id = slotAnchorId(CreativeGeneratorBlockEntity.BATTERY_SLOT),
                        width = CreativeGeneratorScreenHandler.SLOT_SIZE,
                        height = CreativeGeneratorScreenHandler.SLOT_SIZE
                    )
                }
            }

            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = CreativeGeneratorScreenHandler.PLAYER_INV_START,
                playerInvY = GUI_SIZE.playerInvY,
                hotbarY = GUI_SIZE.hotbarY
            )
        }

        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)
        applyAnchoredSlots(layout, left, top)

        super.render(context, mouseX, mouseY, delta)
        ui.render(context, textRenderer, mouseX, mouseY, content = content)
        context.drawText(textRenderer, generationText, sideTextX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, outputText, sideTextX, top + 20, 0xAAAAAA, false)
        context.drawText(textRenderer, totalText, sideTextX, top + 32, 0xAAAAAA, false)
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

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    companion object {
        private val GUI_SIZE = GuiSize.STANDARD
    }
}
