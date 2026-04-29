package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.t
import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.FluidBar
import ic2_120.client.ui.GuiBackground
import ic2_120.client.ui.HeatProgressBar
import ic2_120.content.block.ReplicatorBlock
import ic2_120.content.screen.ReplicatorScreenHandler
import ic2_120.content.screen.GuiSize
import ic2_120.content.sync.ReplicatorSync
import ic2_120.content.uu.UuTemplateEntry
import ic2_120.content.uu.findUniqueAdjacentPatternStorage
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = ReplicatorBlock::class)
class ReplicatorScreen(
    handler: ReplicatorScreenHandler, playerInventory: PlayerInventory, title: Text
) : HandledScreen<ReplicatorScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    private val GUI_SIZE = GuiSize.UPGRADE_TALL

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
        val world = client?.world ?: return super.render(context, mouseX, mouseY, delta)
        val storage = findUniqueAdjacentPatternStorage(world, handler.blockPos)
        val templates = storage?.getTemplatesSnapshot().orEmpty()
        val selectedIndex = storage?.selectedTemplateIndex ?: -1
        val energy = handler.sync.energy.toLong().coerceAtLeast(0L)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1L)
        val energyFraction = (energy.toFloat() / cap.toFloat()).coerceIn(0f, 1f)
        val fluidFraction = if (handler.sync.fluidCapacityMb > 0) {
            handler.sync.fluidAmountMb.toFloat() / handler.sync.fluidCapacityMb.toFloat()
        } else {
            0f
        }.coerceIn(0f, 1f)
        val progressFraction = if (handler.sync.progressMaxUb > 0) {
            handler.sync.progressUb.toFloat() / handler.sync.progressMaxUb.toFloat()
        } else {
            0f
        }.coerceIn(0f, 1f)
        val energyText = "${EnergyFormatUtils.formatEu(energy)} / ${EnergyFormatUtils.formatEu(cap)} EU"
        val fluidText = "${handler.sync.fluidAmountMb} / ${handler.sync.fluidCapacityMb} mB"
        val inputText = t("gui.ic2_120.input_eu", EnergyFormatUtils.formatEu(handler.sync.getSyncedInsertedAmount()))
        val consumeText = t("gui.ic2_120.consume_eu", EnergyFormatUtils.formatEu(handler.sync.getSyncedConsumedAmount()))
        val progressLine = t("gui.ic2_120.replicator.progress", handler.sync.progressUb, handler.sync.progressMaxUb)
        val statusLine = statusText(handler.sync.status)
        val leftSideStrings = listOf(
            inputText,
            consumeText,
            energyText,
            progressLine,
            statusLine,
            fluidText
        )
        val sideTextWidth = leftSideStrings.maxOf { textRenderer.getWidth(it) }
        val sideTextX = left - sideTextWidth - 4

        val content: UiScope.() -> Unit = {
            Row(
                x = left + 8,
                y = top + 8,
                spacing = 8,
                modifier = Modifier.EMPTY.width(GUI_SIZE.contentWidth)
            ) {
                Flex(
                    direction = FlexDirection.ROW,
                    gap = 8,
                    modifier = Modifier.EMPTY.width(GuiSize.STANDARD.contentWidth).height(GUI_SIZE.contentHeight)
                ) {
                    Flex(
                        gap = 4, direction = FlexDirection.COLUMN,
                        justifyContent = JustifyContent.START,
                        alignItems = AlignItems.START,
                        modifier = Modifier.EMPTY.fractionWidth(1.0f)
                    ) {
                        Flex(alignItems = AlignItems.CENTER, gap = 8) {
                            Text(title.string, color = 0xFFFFFF)
                            EnergyBar(energyFraction, modifier = Modifier.EMPTY.fractionWidth(1f))
                        }
                        Flex(
                            gap = 2,
                            justifyContent = JustifyContent.START,
                            alignItems = AlignItems.START,
                            modifier = Modifier.EMPTY.fillMaxWidth().height(60),
                        ) {
                            FluidBar(
                                fluidFraction,
                                barWidth = 8,
                                barHeight = 40,
                                vertical = true,
                                modifier = Modifier.EMPTY.width(8).height(52)
                            )
                            Flex(
                                gap = 0,
                                direction = FlexDirection.COLUMN,
                                justifyContent = JustifyContent.START,
                                alignItems = AlignItems.CENTER,
                                modifier = Modifier.EMPTY.fractionWidth(1f)
                            ) {
                                Flex(
                                    gap = 0,
                                    direction = FlexDirection.ROW,
                                    justifyContent = JustifyContent.START,
                                    alignItems = AlignItems.CENTER
                                ) {
                                    Text(t("gui.ic2_120.replicator.product_slot"), color = 0xAAAAAA, shadow = false)
                                    SlotAnchor(
                                        id = slotAnchorId(ReplicatorScreenHandler.SLOT_OUTPUT_INDEX),
                                        width = 18,
                                        height = 18
                                    )
                                }
                                Flex(
                                    gap = 0,
                                    direction = FlexDirection.ROW,
                                    justifyContent = JustifyContent.START,
                                    alignItems = AlignItems.CENTER
                                ) {

                                    Text(t("gui.ic2_120.replicator.input_fluid"), color = 0xAAAAAA, shadow = false)
                                    SlotAnchor(
                                        id = slotAnchorId(ReplicatorScreenHandler.SLOT_CONTAINER_INPUT_INDEX),
                                        width = 18,
                                        height = 18
                                    )
                                }
                            }
                            Flex(
                                gap = 0,
                                direction = FlexDirection.COLUMN,
                                justifyContent = JustifyContent.START,
                                alignItems = AlignItems.CENTER,
                                modifier = Modifier.EMPTY.fractionWidth(1f)

                            ) {
                                Flex(
                                    gap = 0,
                                    direction = FlexDirection.ROW,
                                    justifyContent = JustifyContent.START,
                                    alignItems = AlignItems.CENTER
                                ) {
                                    Text(t("gui.ic2_120.replicator.empty_bucket"), color = 0xAAAAAA, shadow = false)
                                    SlotAnchor(
                                        id = slotAnchorId(ReplicatorScreenHandler.SLOT_CONTAINER_OUTPUT_INDEX),
                                        width = 18,
                                        height = 18
                                    )
                                }
                                Flex(
                                    gap = 0,
                                    direction = FlexDirection.ROW,
                                    justifyContent = JustifyContent.START,
                                    alignItems = AlignItems.CENTER
                                ) {
                                    Text(t("gui.ic2_120.battery_slot"), color = 0xAAAAAA, shadow = false)
                                    SlotAnchor(
                                        id = slotAnchorId(ReplicatorScreenHandler.SLOT_BATTERY_INDEX),
                                        width = 18,
                                        height = 18
                                    )
                                }
                            }
                        }
                        HeatProgressBar(
                            progressFraction,
                            modifier = Modifier.EMPTY.fillMaxWidth(),
                            barHeight = 6
                        )
                        Button(
                            t("gui.ic2_120.replicator.mode", modeText(handler.sync.mode)),
                            modifier = Modifier.EMPTY.fillMaxWidth()
                        ) {
                            client?.player?.networkHandler?.sendPacket(
                                ButtonClickC2SPacket(handler.syncId, ReplicatorScreenHandler.BUTTON_MODE_TOGGLE)
                            )
                        }
                    }
                    Flex(
                        direction = FlexDirection.COLUMN,
                        modifier = Modifier.EMPTY.fractionWidth(1.0f).fractionHeight(1f),
                        gap = 2
                    ) {
                        Text(t("gui.ic2_120.replicator.template_list"), color = 0xFFFFFF)
                        Text(t("gui.ic2_120.count_items", templates.size), color = 0x666666, shadow = false)

                        ScrollView(
                            scrollbarWidth = 8,
                            modifier = Modifier.EMPTY.fractionHeight(1.0f).padding(0, 4)
                        ) {
                            Column(spacing = 2) {
                                if (templates.isEmpty()) {
                                    Text(t("gui.ic2_120.replicator.no_template"), color = 0x666666, shadow = false)
                                } else {
                                    templates.forEachIndexed { index, template ->
                                        Flex(gap = 0, modifier = Modifier.EMPTY.fractionWidth(1.0f)) {
                                            val stack = templateToStack(template)
                                            if (!stack.isEmpty) {
                                                ItemStack(stack, size = 18)
                                            }
                                            Button(
                                                text = templateLine(index, selectedIndex, template),
                                                modifier = Modifier.EMPTY.fractionWidth(1.0f),
                                                tooltip = listOf(
                                                    template.displayName().copy(),
                                                    Text.literal("${template.uuCostUb} uB")
                                                ),
                                                onClick = {
                                                    client?.player?.networkHandler?.sendPacket(
                                                        ButtonClickC2SPacket(
                                                            handler.syncId,
                                                            ReplicatorScreenHandler.BUTTON_SELECT_BASE + index
                                                        )
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Column(
                    spacing = 4,
                    modifier = Modifier.EMPTY.width(GuiSize.UPGRADE_COLUMN_WIDTH).padding(0, 8, 0, 0)
                ) {
                    for (i in ReplicatorScreenHandler.SLOT_UPGRADE_INDEX_START..ReplicatorScreenHandler.SLOT_UPGRADE_INDEX_END) {
                        SlotAnchor(id = slotAnchorId(i), width = 18, height = 18)
                    }
                }
            }

            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = ReplicatorScreenHandler.PLAYER_INV_START,
                playerInvY = GUI_SIZE.playerInvY,
                hotbarY = GUI_SIZE.hotbarY
            )
        }

        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)
        applyAnchoredSlots(layout, left, top)

        super.render(context, mouseX, mouseY, delta)
        ui.render(context, textRenderer, mouseX, mouseY, content = content)
        val tooltip = ui.getTooltipAt(mouseX, mouseY)
        if (!tooltip.isNullOrEmpty()) {
            context.drawTooltip(textRenderer, tooltip, mouseX, mouseY)
        } else {
            drawMouseoverTooltip(context, mouseX, mouseY)
        }
        var sideY = top + 8
        val sideLineStep = 12
        context.drawText(textRenderer, inputText, sideTextX, sideY, 0xAAAAAA, false)
        sideY += sideLineStep
        context.drawText(textRenderer, consumeText, sideTextX, sideY, 0xAAAAAA, false)
        sideY += sideLineStep
        context.drawText(textRenderer, energyText, sideTextX, sideY, 0xFFFFFF, false)
        sideY += sideLineStep
        context.drawText(textRenderer, progressLine, sideTextX, sideY, 0xAAAAAA, false)
        sideY += sideLineStep
        context.drawText(
            textRenderer,
            statusLine,
            sideTextX,
            sideY,
            statusColor(handler.sync.status),
            false
        )
        sideY += sideLineStep
        context.drawText(textRenderer, fluidText, sideTextX, sideY, 0xFFFFFF, false)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean =
        ui.mouseScrolled(mouseX, mouseY, 0.0, amount) || super.mouseScrolled(mouseX, mouseY, amount)

    override fun mouseDragged(
        mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double
    ): Boolean = ui.mouseDragged(mouseX, mouseY, button) || super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        ui.stopDrag()
        return super.mouseReleased(mouseX, mouseY, button)
    }

    private fun slotAnchorId(slotIndex: Int): String = "slot.$slotIndex"

    private fun applyAnchoredSlots(layout: ComposeUI.LayoutSnapshot, left: Int, top: Int) {
        handler.slots.forEachIndexed { index, slot ->
            val anchor = layout.anchors[slotAnchorId(index)] ?: return@forEachIndexed
            slot.x = anchor.x - left
            slot.y = anchor.y - top
        }
    }

    private fun templateLine(index: Int, selectedIndex: Int, template: UuTemplateEntry): String {
        val prefix = if (index == selectedIndex) "> " else ""
        return "$prefix${template.displayName().string} (${template.uuCostUb} uB)"
    }

    private fun templateToStack(template: UuTemplateEntry): ItemStack {
        val id = Identifier.tryParse(template.itemId) ?: return ItemStack.EMPTY
        val item = Registries.ITEM.getOrEmpty(id).orElse(null) ?: return ItemStack.EMPTY
        return if (item == net.minecraft.item.Items.AIR) ItemStack.EMPTY else ItemStack(item)
    }

    private fun modeText(mode: Int): String = if (mode == ReplicatorSync.MODE_CONTINUOUS) t("gui.ic2_120.replicator.mode_continuous") else t("gui.ic2_120.replicator.mode_single")

    private fun statusText(status: Int): String = when (status) {
        ReplicatorSync.STATUS_NO_REDSTONE -> t("gui.ic2_120.replicator.status_no_redstone")
        ReplicatorSync.STATUS_NO_STORAGE -> t("gui.ic2_120.status_no_storage")
        ReplicatorSync.STATUS_NO_TEMPLATE -> t("gui.ic2_120.replicator.status_no_template")
        ReplicatorSync.STATUS_NO_FLUID -> t("gui.ic2_120.replicator.status_no_fluid")
        ReplicatorSync.STATUS_NO_OUTPUT -> t("gui.ic2_120.replicator.status_no_output")
        ReplicatorSync.STATUS_NO_ENERGY -> t("gui.ic2_120.status_no_energy")
        ReplicatorSync.STATUS_RUNNING -> t("gui.ic2_120.replicator.status_running")
        ReplicatorSync.STATUS_COMPLETE -> t("gui.ic2_120.replicator.status_complete")
        else -> t("gui.ic2_120.status_idle")
    }

    private fun statusColor(status: Int): Int = when (status) {
        ReplicatorSync.STATUS_COMPLETE -> 0x55FF55
        ReplicatorSync.STATUS_RUNNING -> 0x55AAFF
        ReplicatorSync.STATUS_NO_REDSTONE, ReplicatorSync.STATUS_NO_STORAGE, ReplicatorSync.STATUS_NO_TEMPLATE, ReplicatorSync.STATUS_NO_FLUID, ReplicatorSync.STATUS_NO_OUTPUT, ReplicatorSync.STATUS_NO_ENERGY -> 0xFF5555

        else -> 0xAAAAAA
    }

    companion object {
        private const val MACHINE_PANEL_WIDTH = 72
        private val TEMPLATE_PANEL_WIDTH = GuiSize.STANDARD.contentWidth - MACHINE_PANEL_WIDTH - 8
    }
}
