package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.EnergyFormatUtils
import ic2_120.client.t
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.EnergyBarOrientation
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.CannerBlock
import ic2_120.content.screen.CannerScreenHandler
import ic2_120.content.screen.GuiSize
import ic2_120.content.sync.CannerSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
import net.minecraft.text.Text

@ModScreen(block = CannerBlock::class)
class CannerScreen(
    handler: CannerScreenHandler, playerInventory: PlayerInventory, title: Text
) : HandledScreen<CannerScreenHandler>(handler, playerInventory, title) {

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
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
        val energyFraction = (energy.toFloat() / cap).coerceIn(0f, 1f)
        val progress = handler.sync.progress.coerceIn(0, CannerSync.PROGRESS_MAX)
        val progressFrac = (progress.toFloat() / CannerSync.PROGRESS_MAX).coerceIn(0f, 1f)
        val leftFluidFrac = run {
            val amt = handler.sync.leftFluidAmountMb.toLong()
            val c = handler.sync.leftFluidCapacityMb.toLong().coerceAtLeast(1)
            (amt.toFloat() / c).coerceIn(0f, 1f)
        }
        val rightFluidFrac = run {
            val amt = handler.sync.rightFluidAmountMb.toLong()
            val c = handler.sync.rightFluidCapacityMb.toLong().coerceAtLeast(1)
            (amt.toFloat() / c).coerceIn(0f, 1f)
        }
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val consumeRate = handler.sync.getSyncedConsumedAmount()
        val modeText = when (handler.sync.getMode()) {
            CannerSync.Mode.BOTTLE_SOLID -> t("gui.ic2_120.canner.mode_bottle_solid")
            CannerSync.Mode.EMPTY_LIQUID -> t("gui.ic2_120.canner.mode_empty_liquid")
            CannerSync.Mode.BOTTLE_LIQUID -> t("gui.ic2_120.canner.mode_bottle_liquid")
            CannerSync.Mode.ENRICH_LIQUID -> t("gui.ic2_120.canner.mode_enrich_liquid")
        }

        val energyText = "${EnergyFormatUtils.formatEu(energy)} / ${EnergyFormatUtils.formatEu(cap)} EU"
        val inputText = t("gui.ic2_120.input_eu", EnergyFormatUtils.formatEu(inputRate))
        val consumeText = t("gui.ic2_120.consume_eu", EnergyFormatUtils.formatEu(consumeRate))
        val sideTextWidth = maxOf(
            textRenderer.getWidth(energyText), textRenderer.getWidth(inputText), textRenderer.getWidth(consumeText)
        )
        val sideTextX = left - sideTextWidth - 4

        val content: UiScope.() -> Unit = {
            Row(
                x = left + 8, y = top + 8, spacing = 8, modifier = Modifier.EMPTY.width(GUI_SIZE.contentWidth)
            ) {
                // 左侧：流体槽 + 机器槽
                Column(
                    spacing = 4, modifier = Modifier.EMPTY.width(GuiSize.STANDARD.contentWidth)
                ) {
                    // 标题、电量文本、能量条同一行
                    Flex(direction = FlexDirection.ROW, alignItems = AlignItems.CENTER, gap = 8) {
                        Text(t("gui.ic2_120.canner.title"), color = 0xFFFFFF)
                        // EnergyBar(
                        //     energyFraction,
                        //     modifier = Modifier.EMPTY.fractionWidth(1f)
                        // )
                        Text(energyText, color = 0xFFFFFF, shadow = false)
                    }
                    Flex(
                        justifyContent = JustifyContent.SPACE_AROUND,
                        alignItems = AlignItems.CENTER,
                        gap = 2,
                    ) {
                        SlotHost(CannerScreenHandler.SLOT_DISCHARGING_INDEX)
                        // 左液槽：顶部放满容器，底部返回空容器
                        Flex(
                            direction = FlexDirection.COLUMN,
                            justifyContent = JustifyContent.SPACE_BETWEEN,
                            alignItems = AlignItems.CENTER,
                            modifier = Modifier().height(70)
                        ) {
                            SlotHost(CannerScreenHandler.SLOT_CONTAINER_INDEX)
                            // 左侧流体条
                            EnergyBar(
                                leftFluidFrac,
                                barWidth = 18,
                                orientation = EnergyBarOrientation.VERTICAL,
                                emptyColor = 0xFF3A3A8A.toInt(),
                                fullColor = 0xFF3AAAF5.toInt(),
                                gradient = false,
                                modifier = Modifier().fractionHeight(1f)
                            )
                            SlotHost(CannerScreenHandler.SLOT_LEFT_EMPTY_INDEX)
                        }
                        
                        Flex(
                            direction = FlexDirection.COLUMN,
                            justifyContent = JustifyContent.SPACE_AROUND,
                            alignItems = AlignItems.CENTER,
                            gap = 2,
                            modifier = Modifier.EMPTY.height(92)
                        ) {
                            SlotHost(CannerScreenHandler.SLOT_MATERIAL_INDEX)
                            Button(modeText, onClick = {
                                client?.player?.networkHandler?.sendPacket(
                                    ButtonClickC2SPacket(handler.syncId, CannerScreenHandler.BUTTON_ID_MODE_CYCLE)
                                )
                            })
                            Button(t("gui.ic2_120.canner.swap_tanks"), onClick = {
                                client?.player?.networkHandler?.sendPacket(
                                    ButtonClickC2SPacket(handler.syncId, CannerScreenHandler.BUTTON_ID_SWAP_TANKS)
                                )
                            })
                            // 进度条
                            EnergyBar(
                                progressFrac,
                                barWidth = 54,
                                barHeight = 6,
                                emptyColor = 0xFF555555.toInt(),
                                fullColor = 0xFF7FD34E.toInt(),
                                gradient = false,
                            )
                        }
                        
                        // 右液槽：顶部放空容器，底部返回满容器
                        Flex(
                            direction = FlexDirection.COLUMN,
                            justifyContent = JustifyContent.SPACE_BETWEEN,
                            alignItems = AlignItems.CENTER,
                            modifier = Modifier().height(70)
                        ) {
                            SlotHost(CannerScreenHandler.SLOT_RIGHT_INPUT_INDEX)
                            // 右侧流体条
                            EnergyBar(
                                rightFluidFrac,
                                barWidth = 18,
                                orientation = EnergyBarOrientation.VERTICAL,
                                emptyColor = 0xFF3A3A8A.toInt(),
                                fullColor = 0xFF3AAAF5.toInt(),
                                gradient = false,
                                modifier = Modifier().fractionHeight(1f)
                            )
                            SlotHost(CannerScreenHandler.SLOT_OUTPUT_INDEX)
                        }
                    }


                   
                }

                // 右侧：升级槽列
                Column(
                    spacing = 4, modifier = Modifier.EMPTY.width(GuiSize.UPGRADE_COLUMN_WIDTH).padding(0, 8, 0, 0)
                ) {
                    for (slotIndex in CannerScreenHandler.SLOT_UPGRADE_INDEX_START..CannerScreenHandler.SLOT_UPGRADE_INDEX_END) {
                        SlotHost(slotIndex)
                    }
                }
            }

            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = CannerScreenHandler.PLAYER_INV_START,
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
            id = slotAnchorId(slotIndex), width = CannerScreenHandler.SLOT_SIZE, height = CannerScreenHandler.SLOT_SIZE
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
        private val GUI_SIZE = GuiSize.UPGRADE_TALL
    }
}
