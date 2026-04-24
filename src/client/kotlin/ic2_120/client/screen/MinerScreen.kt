package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.EnergyFormatUtils
import ic2_120.client.t
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.machines.BaseMinerBlockEntity
import ic2_120.content.screen.MinerScreenHandler
import ic2_120.content.screen.GuiSize
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket

@ModScreen(handlers = ["miner", "advanced_miner"])
class MinerScreen(
    handler: MinerScreenHandler, playerInventory: PlayerInventory, title: net.minecraft.text.Text
) : HandledScreen<MinerScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = GUI_SIZE.width
        backgroundHeight = GUI_SIZE.height
        titleY = 4
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
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
        val fraction = (energy.toFloat() / cap.toFloat()).coerceIn(0f, 1f)
        val modeText = if (handler.sync.mode == 0) t("gui.ic2_120.miner.mode_whitelist") else t("gui.ic2_120.miner.mode_blacklist")
        val silkText = if (handler.sync.silkTouch == 0) t("gui.ic2_120.miner.off") else t("gui.ic2_120.miner.on")
        val runningText = if (handler.sync.running == 0) t("gui.ic2_120.miner.stopped") else t("gui.ic2_120.miner.running")
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val consumeRate = handler.sync.getSyncedConsumedAmount()
        val inputText = t("gui.ic2_120.input_eu", EnergyFormatUtils.formatEu(inputRate))
        val consumeText = t("gui.ic2_120.consume_eu", EnergyFormatUtils.formatEu(consumeRate))
        val sideTextWidth = maxOf(textRenderer.getWidth(inputText), textRenderer.getWidth(consumeText))
        val sideTextX = left - sideTextWidth - 4

        val content: UiScope.() -> Unit = {
            Row(
                x = left + 8, y = top + 6, spacing = 8, modifier = Modifier.EMPTY.width(GUI_SIZE.contentWidth)
            ) {
                Column(spacing = 2, modifier = Modifier.EMPTY.width(GuiSize.STANDARD.contentWidth)) {
                    Flex(
                        direction = FlexDirection.ROW,
                        gap = 6,
                        modifier = Modifier.EMPTY.width(GuiSize.STANDARD.contentWidth)
                    ) {
                        Text(title.string, color = 0xFFFFFF)
                        EnergyBar(fraction, barHeight = 8, modifier = Modifier().fractionWidth(1f))
                        Text(t("gui.ic2_120.energy"), color = 0xDDDDDD)
                        Text("$energy / $cap EU", shadow = false)
                    }
                    Row {
                        Text(t("gui.ic2_120.miner.status", runningText), color = 0xAAAAAA, shadow = false)
                        Text(
                            t("gui.ic2_120.miner.scan_cursor", handler.sync.cursorX, handler.sync.cursorY, handler.sync.cursorZ),
                            color = 0xAAAAAA,
                            shadow = false
                        )
                    }
                    Flex(
                        direction = FlexDirection.ROW,
                        justifyContent = JustifyContent.SPACE_BETWEEN,
                        gap = 8,
                        modifier = Modifier().fillMaxWidth(),
                    ) {
                        Flex(
                            direction = FlexDirection.COLUMN,
                            modifier = Modifier().height(18 * 4),
                            justifyContent = JustifyContent.SPACE_BETWEEN,
//                            alignItems = AlignItems.CENTER,
                        ) {
                            Text(t("gui.ic2_120.miner.scanner"), center = true, modifier = Modifier.EMPTY.height(18))
                            Text(t("gui.ic2_120.miner.drill"), center = true, modifier = Modifier.EMPTY.height(18))
                            Text(t("gui.ic2_120.battery_slot"), center = true, modifier = Modifier.EMPTY.height(18))
                            Text(t("gui.ic2_120.miner.pipe"), center = true, modifier = Modifier.EMPTY.height(18))
                        }
                        Column {
                            SlotHost(MinerScreenHandler.SLOT_SCANNER_INDEX)
                            SlotHost(MinerScreenHandler.SLOT_DRILL_INDEX)
                            SlotHost(MinerScreenHandler.SLOT_BATTERY_INDEX)
                            SlotHost(MinerScreenHandler.SLOT_PIPE_INDEX)
                        }
                        if (handler.isAdvanced) {
                            Column(spacing = 0) {
                                for (row in 0 until 3) {
                                    Row(spacing = 0) {
                                        for (col in 0 until 5) {
                                            val idx = MinerScreenHandler.SLOT_FILTER_INDEX_START + row * 5 + col
                                            SlotHost(idx)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Flex(direction = FlexDirection.ROW, gap = 3) {
                        if (handler.isAdvanced) {
                            Button(t("gui.ic2_120.miner.mode", modeText)) {
                                client?.player?.networkHandler?.sendPacket(
                                    ButtonClickC2SPacket(
                                        handler.syncId, MinerScreenHandler.BUTTON_TOGGLE_MODE
                                    )
                                )
                            }
                            Button(t("gui.ic2_120.miner.silk_touch", silkText)) {
                                client?.player?.networkHandler?.sendPacket(
                                    ButtonClickC2SPacket(
                                        handler.syncId, MinerScreenHandler.BUTTON_TOGGLE_SILK
                                    )
                                )
                            }
                        }
                        Button(t("gui.ic2_120.miner.restart")) {
                            client?.player?.networkHandler?.sendPacket(
                                ButtonClickC2SPacket(
                                    handler.syncId, MinerScreenHandler.BUTTON_RESTART
                                )
                            )
                        }
                    }
                }
                Column(
                    spacing = 4, modifier = Modifier.EMPTY.width(GuiSize.UPGRADE_COLUMN_WIDTH).padding(0, 8, 0, 0)
                ) {
                    for (i in MinerScreenHandler.SLOT_UPGRADE_INDEX_START..MinerScreenHandler.SLOT_UPGRADE_INDEX_END) {
                        SlotHost(i)
                    }
                }
            }

            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = MinerScreenHandler.PLAYER_INV_START,
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
            id = slotAnchorId(slotIndex), width = 18, height = 18
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

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    companion object {
        private val GUI_SIZE = GuiSize.UPGRADE_TALL
    }
}
