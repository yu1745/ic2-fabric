package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.EnergyFormatUtils
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.client.ui.HeatProgressBar
import ic2_120.content.block.CentrifugeBlock
import ic2_120.content.screen.CentrifugeScreenHandler
import ic2_120.content.screen.GuiSize
import ic2_120.content.sync.CentrifugeSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.slot.Slot
import net.minecraft.text.Text

@ModScreen(block = CentrifugeBlock::class)
class CentrifugeScreen(
    handler: CentrifugeScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<CentrifugeScreenHandler>(handler, playerInventory, title) {

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
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val progressFrac = if (CentrifugeSync.PROGRESS_MAX > 0) {
            (handler.sync.progress.coerceIn(0, CentrifugeSync.PROGRESS_MAX)
                .toFloat() / CentrifugeSync.PROGRESS_MAX).coerceIn(0f, 1f)
        } else 0f
        val heat = handler.sync.heat.toLong().coerceAtLeast(0)
        val heatFrac =
            if (CentrifugeSync.HEAT_MAX > 0) (heat.toFloat() / CentrifugeSync.HEAT_MAX).coerceIn(0f, 1f) else 0f
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val consumeRate = handler.sync.getSyncedConsumedAmount()
        val isProcessing = handler.sync.progress > 0

        val inputText = "输入 ${EnergyFormatUtils.formatEu(inputRate)} EU/t"
        val consumeText = "耗能 ${EnergyFormatUtils.formatEu(consumeRate)} EU/t"
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
                            SlotHost(CentrifugeScreenHandler.SLOT_INPUT_INDEX)
                            SlotHost(CentrifugeScreenHandler.SLOT_DISCHARGING_INDEX)
                        }
                        // 热量条
                        Column(
//                            direction = FlexDirection.COLUMN,
//                            justifyContent = JustifyContent.CENTER,
//                            alignItems = AlignItems.CENTER,
//                            gap = 2
                            spacing = 2,
                            modifier = Modifier().fractionWidth(1f)
                        ) {
                            EnergyBar(progressFrac, modifier = Modifier.EMPTY.fractionWidth(1.0f))
                            Row {
                                Text("热量", color = 0xAAAAAA)
                                HeatProgressBar(
                                    heatFrac,
//                                barWidth = 0,
                                    barHeight = 8,
                                    startColor = 0xFF660000.toInt(),
                                    endColor = 0xFFCC0000.toInt(),
                                    gradient = true,
//                                    modifier = Modifier().fractionWidth(1.0f)
                                )
                                Text("$heat/${CentrifugeSync.HEAT_MAX}", color = 0xFFFFFF, shadow = false)
                            }
                            Text(
                                "${if (isProcessing) "加工" else "预热"} | 入${EnergyFormatUtils.formatEu(inputRate)} 耗${
                                    EnergyFormatUtils.formatEu(
                                        consumeRate
                                    )
                                } EU/t",
                                color = if (isProcessing) 0x00CC00 else 0xCC0000,
                                shadow = false
                            )

                        }

                        Column(spacing = 4) {
                            SlotHost(CentrifugeScreenHandler.SLOT_OUTPUT_1_INDEX)
                            SlotHost(CentrifugeScreenHandler.SLOT_OUTPUT_2_INDEX)
                            SlotHost(CentrifugeScreenHandler.SLOT_OUTPUT_3_INDEX)
                        }
                    }


                }

                Column(
                    spacing = 4,
                    modifier = Modifier.EMPTY
                        .width(GuiSize.UPGRADE_COLUMN_WIDTH)
                        .padding(0, 8, 0, 0)
                ) {
                    for (slotIndex in CentrifugeScreenHandler.SLOT_UPGRADE_INDEX_START..CentrifugeScreenHandler.SLOT_UPGRADE_INDEX_END) {
                        SlotHost(slotIndex)
                    }
                }
            }

            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = CentrifugeScreenHandler.PLAYER_INV_START,
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
            width = CentrifugeScreenHandler.SLOT_SIZE,
            height = CentrifugeScreenHandler.SLOT_SIZE
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
