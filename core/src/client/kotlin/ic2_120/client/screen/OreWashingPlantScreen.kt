package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.EnergyFormatUtils
import ic2_120.client.t
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.sync.OreWashingPlantSync
import ic2_120.content.block.OreWashingPlantBlock
import ic2_120.content.screen.OreWashingPlantScreenHandler
import ic2_120.content.screen.GuiSize
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

@ModScreen(block = OreWashingPlantBlock::class)
class OreWashingPlantScreen(
    handler: OreWashingPlantScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<OreWashingPlantScreenHandler>(handler, playerInventory, title) {

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
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val waterAmount = handler.sync.waterAmountMb.toLong()
        val waterCapacity = 8000L
        val progress = handler.sync.progress.coerceIn(0, OreWashingPlantSync.PROGRESS_MAX)
        val progressFrac = if (OreWashingPlantSync.PROGRESS_MAX > 0) (progress.toFloat() / OreWashingPlantSync.PROGRESS_MAX).coerceIn(0f, 1f) else 0f

        val energyText = "$energy / $cap EU"
        val waterText = "$waterAmount/$waterCapacity mB"
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val consumeRate = handler.sync.getSyncedConsumedAmount()
        val inputText = t("gui.ic2_120.input_eu", EnergyFormatUtils.formatEu(inputRate))
        val consumeText = t("gui.ic2_120.consume_eu", EnergyFormatUtils.formatEu(consumeRate))
        val sideTextWidth = maxOf(
            textRenderer.getWidth(energyText),
            textRenderer.getWidth(waterText),
            textRenderer.getWidth(inputText),
            textRenderer.getWidth(consumeText)
        )
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
                        Text(energyText, color = 0xFFFFFF, shadow = false)
                        Text(waterText, color = 0xFFFFFF, shadow = false)
                    }
                    EnergyBar(energyFraction, barHeight = 12)

                    Flex(
                        direction = FlexDirection.ROW,
                        alignItems = AlignItems.CENTER,
                        gap = 4
                    ) {
                        SlotHost(OreWashingPlantScreenHandler.SLOT_INPUT_ORE_INDEX)
                        EnergyBar(progressFrac, modifier = Modifier.EMPTY.fractionWidth(1.0f))
                        SlotHost(OreWashingPlantScreenHandler.SLOT_INPUT_WATER_INDEX)
                    }

                    Flex(direction = FlexDirection.ROW, alignItems = AlignItems.CENTER, gap = 4) {
                        SlotHost(OreWashingPlantScreenHandler.SLOT_OUTPUT_1_INDEX)
                        SlotHost(OreWashingPlantScreenHandler.SLOT_OUTPUT_2_INDEX)
                        SlotHost(OreWashingPlantScreenHandler.SLOT_OUTPUT_3_INDEX)
                        SlotHost(OreWashingPlantScreenHandler.SLOT_OUTPUT_EMPTY_INDEX)
                    }

                    SlotHost(OreWashingPlantScreenHandler.SLOT_DISCHARGING_INDEX)
                }

                Column(
                    spacing = 4,
                    modifier = Modifier.EMPTY
                        .width(GuiSize.UPGRADE_COLUMN_WIDTH)
                        .padding(0, 8, 0, 0)
                ) {
                    for (slotIndex in OreWashingPlantScreenHandler.SLOT_UPGRADE_INDEX_START..OreWashingPlantScreenHandler.SLOT_UPGRADE_INDEX_END) {
                        SlotHost(slotIndex)
                    }
                }
            }

            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = OreWashingPlantScreenHandler.PLAYER_INV_START,
                playerInvY = GUI_SIZE.playerInvY,
                hotbarY = GUI_SIZE.hotbarY
            )
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
            width = OreWashingPlantScreenHandler.SLOT_SIZE,
            height = OreWashingPlantScreenHandler.SLOT_SIZE
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

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (ui.mouseClicked(mouseX, mouseY, button)) return true
        return super.mouseClicked(mouseX, mouseY, button)
    }

    companion object {
        private val GUI_SIZE = GuiSize.STANDARD_UPGRADE
    }
}
