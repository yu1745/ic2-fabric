package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.t
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.FluidBar
import ic2_120.client.ui.GuiBackground
import ic2_120.client.ui.HeatProgressBar
import ic2_120.content.block.MatterGeneratorBlock
import ic2_120.content.screen.MatterGeneratorScreenHandler
import ic2_120.content.screen.GuiSize
import ic2_120.content.sync.MatterGeneratorSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text as McText

@ModScreen(block = MatterGeneratorBlock::class)
class MatterGeneratorScreen(
    handler: MatterGeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: McText
) : HandledScreen<MatterGeneratorScreenHandler>(handler, playerInventory, title) {

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

        val energy = handler.sync.energy.toLong().coerceAtLeast(0L)
        val energyCap = handler.sync.energyCapacity.toLong().coerceAtLeast(1L)
        val energyFraction = (energy.toFloat() / energyCap).coerceIn(0f, 1f)
        val fluidAmount = handler.sync.fluidAmountMb.toLong().coerceAtLeast(0L)
        val fluidCap = handler.sync.fluidCapacityMb.toLong().coerceAtLeast(1L)
        val fluidFraction = (fluidAmount.toFloat() / fluidCap).coerceIn(0f, 1f)
        val progressFraction = (handler.sync.progress.toFloat() / MatterGeneratorSync.PROGRESS_MAX).coerceIn(0f, 1f)
        val modeText = if (handler.sync.mode != 0) t("gui.ic2_120.matter_generator.mode_scrap") else t("gui.ic2_120.matter_generator.mode_normal")

        val content: UiScope.() -> Unit = {
            Row(
                x = left + 8,
                y = top + 8,
                spacing = 8,
                modifier = Modifier.EMPTY.width(GUI_SIZE.contentWidth)
            ) {
                Flex(
                    gap = 8,
                    direction = FlexDirection.ROW,
                    justifyContent = JustifyContent.SPACE_BETWEEN,
                    alignItems = AlignItems.START,
                    modifier = Modifier.EMPTY.width(GuiSize.STANDARD.contentWidth)
                ) {
                    Column(
                        spacing = 6,
                        modifier = Modifier.EMPTY.fractionWidth(1.0f)
                    ) {
                        Flex(
                            direction = FlexDirection.ROW,
                            justifyContent = JustifyContent.SPACE_BETWEEN,
                            alignItems = AlignItems.CENTER, gap = 8
                        ) {
                            Text(title.string, color = 0xFFFFFF)
                            Text(modeText, color = 0xAAAAAA, shadow = false)
                        }

                        Flex(direction = FlexDirection.ROW, alignItems = AlignItems.CENTER, gap = 8) {
                            Text(t("gui.ic2_120.energy"), color = 0xAAAAAA)
                            EnergyBar(energyFraction, modifier = Modifier.EMPTY.fractionWidth(1.0f))
                            Text("${formatEu(energy)} / ${formatEu(energyCap)}", color = 0xFFFFFF, shadow = false)
                        }

                        Flex(direction = FlexDirection.ROW, alignItems = AlignItems.CENTER, gap = 8) {
                            Text(t("gui.ic2_120.progress"), color = 0xAAAAAA)
                            HeatProgressBar(
                                progressFraction,
                                barWidth = 0,
                                barHeight = 8,
                                startColor = 0xFF335577.toInt(),
                                endColor = 0xFF66BBFF.toInt(),
                                gradient = true,
                                modifier = Modifier.EMPTY.fractionWidth(1.0f)
                            )
                            Text(
                                "${handler.sync.progress}/${MatterGeneratorSync.PROGRESS_MAX}",
                                color = 0xFFFFFF,
                                shadow = false
                            )
                        }
                        Column(spacing = 4) {
                            Row(spacing = 4) {
                                SlotAnchor(id = slotAnchorId(MatterGeneratorScreenHandler.SLOT_SCRAP_INDEX))
                                SlotAnchor(id = slotAnchorId(MatterGeneratorScreenHandler.SLOT_CONTAINER_INPUT_INDEX))
                                SlotAnchor(id = slotAnchorId(MatterGeneratorScreenHandler.SLOT_CONTAINER_OUTPUT_INDEX))
                            }
                            SlotAnchor(id = slotAnchorId(MatterGeneratorScreenHandler.SLOT_DISCHARGING_INDEX))
                        }

                    }
                    Flex(
                        direction = FlexDirection.COLUMN,
                        justifyContent = JustifyContent.START,
                        alignItems = AlignItems.END,
                        gap = 4
                    ) {
                        Text(t("gui.ic2_120.matter_generator.uu_tank"), color = 0xAAAAAA)
                        FluidBar(
                            fluidFraction,
                            barWidth = 8,
                            barHeight = 52,
                            vertical = true,
                            modifier = Modifier.EMPTY.width(8).height(52)
                        )
                        Text("${handler.sync.fluidAmountMb} mB", color = 0xFFFFFF, shadow = false)
                    }
                }


                Column(
                    spacing = 4,
                    modifier = Modifier.EMPTY
                        .width(GuiSize.UPGRADE_COLUMN_WIDTH)
                        .padding(0, 8, 0, 0)
                ) {
                    for (slotIndex in MatterGeneratorScreenHandler.SLOT_UPGRADE_INDEX_START..MatterGeneratorScreenHandler.SLOT_UPGRADE_INDEX_END) {
                        SlotAnchor(
                            id = slotAnchorId(slotIndex),
                            width = MatterGeneratorScreenHandler.SLOT_SIZE,
                            height = MatterGeneratorScreenHandler.SLOT_SIZE
                        )
                    }
                }
            }

            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = MatterGeneratorScreenHandler.PLAYER_INV_START,
                playerInvY = GUI_SIZE.playerInvY,
                hotbarY = GUI_SIZE.hotbarY
            )
        }

        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)
        applyAnchoredSlots(layout, left, top)

        super.render(context, mouseX, mouseY, delta)
        ui.render(context, textRenderer, mouseX, mouseY, content = content)

        val inputText = t("gui.ic2_120.input_eu", formatEu(handler.sync.getSyncedInsertedAmount()))
        val consumeText = t("gui.ic2_120.consume_eu", formatEu(handler.sync.getSyncedConsumedAmount()))
        val sideTextWidth = maxOf(textRenderer.getWidth(inputText), textRenderer.getWidth(consumeText))
        val sideTextX = left - sideTextWidth - 4
        context.drawText(textRenderer, inputText, sideTextX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, consumeText, sideTextX, top + 20, 0xAAAAAA, false)

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    private fun slotAnchorId(slotIndex: Int): String = "slot.$slotIndex"

    private fun applyAnchoredSlots(layout: ComposeUI.LayoutSnapshot, left: Int, top: Int) {
        handler.slots.forEachIndexed { index, slot ->
            val anchor = layout.anchors[slotAnchorId(index)] ?: return@forEachIndexed
            slot.x = anchor.x - left
            slot.y = anchor.y - top
        }
    }

    private fun formatEu(value: Long): String {
        return when {
            value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
            else -> value.toString()
        }
    }

    companion object {
        private val GUI_SIZE = GuiSize.STANDARD_UPGRADE
    }
}
