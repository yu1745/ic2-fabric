package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.compose.*
import ic2_120.client.t
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.AnimalSlaughtererBlock
import ic2_120.content.screen.AnimalSlaughtererScreenHandler
import ic2_120.content.screen.GuiSize
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

@ModScreen(block = AnimalSlaughtererBlock::class)
class AnimalSlaughtererScreen(
    handler: AnimalSlaughtererScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<AnimalSlaughtererScreenHandler>(handler, playerInventory, title) {

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
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y

        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val capacity = handler.sync.energyCapacity.toLong().coerceAtLeast(1L)
        val energyFraction = (energy.toFloat() / capacity).coerceIn(0f, 1f)

        val inputRateText = t("gui.ic2_120.input_eu", EnergyFormatUtils.formatEu(handler.sync.getSyncedInsertedAmount()))
        val consumeRateText = t("gui.ic2_120.consume_eu", EnergyFormatUtils.formatEu(handler.sync.getSyncedConsumedAmount()))

        val animalCount = handler.sync.animalCount
        val statusText = when {
            animalCount >= 32 -> t("gui.ic2_120.animal_slaughterer.slaughtering")
            animalCount >= 16 -> t("gui.ic2_120.animal_slaughterer.breeding")
            else -> t("gui.ic2_120.animal_slaughterer.waiting")
        }

        val content: UiScope.() -> Unit = {
            Row(
                x = left + 8,
                y = top + 6,
                spacing = 8,
                modifier = Modifier.EMPTY.width(GUI_SIZE.contentWidth)
            ) {
                Column(spacing = 4, modifier = Modifier.EMPTY.width(GuiSize.STANDARD.contentWidth)) {
                    Flex(direction = FlexDirection.ROW, alignItems = AlignItems.CENTER, gap = 4) {
                        Text(title.string, color = 0xFFFFFF)
                        EnergyBar(energyFraction, barHeight = 8, modifier = Modifier.EMPTY.fractionWidth(1f))
                        Text("$energy / $capacity EU", color = 0xFFFFFF, shadow = false)
                    }

                    Text(
                        t("gui.ic2_120.animal_slaughterer.animal_count", animalCount, statusText),
                        color = 0xFFFFFF,
                        shadow = false
                    )
                    Text(
                        t("gui.ic2_120.animal_slaughterer.run_slaughtered", handler.sync.slaughteredThisRun),
                        color = 0xAAAAAA,
                        shadow = false
                    )

                    Column(spacing = 0) {
                        for (row in 0 until 3) {
                            Row(spacing = 0) {
                                for (col in 0 until 5) {
                                    val slotIndex = AnimalSlaughtererScreenHandler.SLOT_CONTENT_INDEX_START + row * 5 + col
                                    SlotAnchor(id = slotAnchorId(slotIndex))
                                }
                                if (row == 2) {
                                    SlotAnchor(
                                        id = slotAnchorId(AnimalSlaughtererScreenHandler.SLOT_DISCHARGING_INDEX),
                                        modifier = Modifier.EMPTY.padding(left = 4, top = 0, right = 0, bottom = 0)
                                    )
                                }
                            }
                        }
                    }
                }

                Column(
                    spacing = 4,
                    modifier = Modifier.EMPTY.width(GuiSize.UPGRADE_COLUMN_WIDTH).padding(0, 8, 0, 0)
                ) {
                    for (slotIndex in AnimalSlaughtererScreenHandler.SLOT_UPGRADE_INDEX_START..AnimalSlaughtererScreenHandler.SLOT_UPGRADE_INDEX_END) {
                        SlotAnchor(id = slotAnchorId(slotIndex), width = AnimalSlaughtererScreenHandler.SLOT_SIZE, height = AnimalSlaughtererScreenHandler.SLOT_SIZE)
                    }
                }
            }

            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = AnimalSlaughtererScreenHandler.PLAYER_INV_START,
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

        val sideTextWidth = maxOf(textRenderer.getWidth(inputRateText), textRenderer.getWidth(consumeRateText))
        val sideX = left - sideTextWidth - 4
        context.drawText(textRenderer, inputRateText, sideX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, consumeRateText, sideX, top + 20, 0xAAAAAA, false)

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    private fun slotAnchorId(slotIndex: Int): String = "slot.$slotIndex"

    companion object {
        private val GUI_SIZE = GuiSize.STANDARD_UPGRADE
    }
}
