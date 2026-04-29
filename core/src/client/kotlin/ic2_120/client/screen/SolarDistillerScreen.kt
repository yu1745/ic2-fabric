package ic2_120.client.screen

import ic2_120.client.compose.ComposeUI
import ic2_120.client.compose.*
import ic2_120.client.t
import ic2_120.client.ui.FluidBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.SolarDistillerBlock
import ic2_120.content.screen.GuiSize
import ic2_120.content.screen.SolarDistillerScreenHandler
import ic2_120.content.sync.SolarDistillerSync
import ic2_120.registry.annotation.ModScreen
import ic2_120.registry.type
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text as McText

@ModScreen(block = SolarDistillerBlock::class)
class SolarDistillerScreen(
    handler: SolarDistillerScreenHandler,
    playerInventory: PlayerInventory,
    title: McText
) : HandledScreen<SolarDistillerScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = PANEL_WIDTH
        backgroundHeight = PANEL_HEIGHT
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

        val inputFraction = (handler.sync.waterInputMb.toFloat() / SolarDistillerSync.TANK_CAPACITY_MB).coerceIn(0f, 1f)
        val outputFraction =
            (handler.sync.distilledOutputMb.toFloat() / SolarDistillerSync.TANK_CAPACITY_MB).coerceIn(0f, 1f)
        val progressFraction =
            (handler.sync.progress.toFloat() / SolarDistillerSync.PRODUCE_INTERVAL_TICKS).coerceIn(0f, 1f)

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
                    Text(title.string, color = 0xFFFFFF)

                    // 参考 SemifluidGeneratorScreen：两侧上槽入液、下槽出空容器；中间竖条为罐内液位，中央为横向进度条
                    Flex(
                        direction = FlexDirection.ROW,
                        justifyContent = JustifyContent.SPACE_AROUND,
                        alignItems = AlignItems.CENTER,
                        gap = 4,
                        modifier = Modifier().height(MACHINE_ROW_HEIGHT)
                    ) {
                        Flex(
                            direction = FlexDirection.COLUMN,
                            justifyContent = JustifyContent.SPACE_BETWEEN,
                            alignItems = AlignItems.CENTER,
                        ) {
                            SlotHost(SolarDistillerScreenHandler.SLOT_INPUT_WATER_INDEX)
                            SlotHost(SolarDistillerScreenHandler.SLOT_OUTPUT_EMPTY_INDEX)
                        }
                        FluidBar(
                            inputFraction,
                            barWidth = MACHINE_ROW_HEIGHT,
                            barHeight = FLUID_BAR_THICKNESS,
                            vertical = true,
                            modifier = Modifier().fractionHeight(1f)
                        )
                        Flex(
                            direction = FlexDirection.COLUMN,
                            justifyContent = JustifyContent.SPACE_BETWEEN,
                            alignItems = AlignItems.CENTER,
                            modifier = Modifier.EMPTY.width(PROGRESS_BAR_WIDTH)
                        ) {
                            FluidBar(
                                progressFraction,
                                barWidth = PROGRESS_BAR_WIDTH,
                                barHeight = PROGRESS_BAR_HEIGHT,
                                vertical = false,
                                emptyColor = 0xFF444444.toInt(),
                                fullColor = 0xFF88AA44.toInt(),
                            )
                            Column(spacing = 2) {
                                Text(t("gui.ic2_120.solar_distiller.distill_progress"), color = 0xAAAAAA, shadow = false)
                                Text(
                                    "${handler.sync.progress}/${SolarDistillerSync.PRODUCE_INTERVAL_TICKS} tick",
                                    color = 0xFFFFFF,
                                    shadow = false
                                )
                            }
                            Flex(direction = FlexDirection.ROW, alignItems = AlignItems.CENTER, gap = 8) {
                                Text(t("gui.ic2_120.solar_distiller.input_water", handler.sync.waterInputMb), color = 0xAAAAAA, shadow = false)
                                Text(t("gui.ic2_120.solar_distiller.output_distilled", handler.sync.distilledOutputMb), color = 0xAAAAAA, shadow = false)
                            }
                            Text(
                                if (handler.sync.isWorking != 0) t("gui.ic2_120.status_working") else t("gui.ic2_120.status_stopped"),
                                color = 0xAAAAAA,
                                shadow = false
                            )
                        }

                        FluidBar(
                            outputFraction,
                            barWidth = MACHINE_ROW_HEIGHT,
                            barHeight = FLUID_BAR_THICKNESS,
                            vertical = true,
                            emptyColor = 0xFF666666.toInt(),
                            fullColor = 0xFF00CCCC.toInt(),
                            modifier = Modifier().fractionHeight(1f)
                        )
                        Flex(
                            direction = FlexDirection.COLUMN,
                            justifyContent = JustifyContent.SPACE_BETWEEN,
                            alignItems = AlignItems.CENTER,
                        ) {
                            SlotHost(SolarDistillerScreenHandler.SLOT_INPUT_CELL_INDEX)
                            SlotHost(SolarDistillerScreenHandler.SLOT_OUTPUT_CELL_INDEX)
                        }
                    }


                }

                Column(
                    spacing = 4,
                    modifier = Modifier.EMPTY
                        .width(GuiSize.UPGRADE_COLUMN_WIDTH)
                        .padding(0, 8, 0, 0)
                ) {
                    for (slotIndex in SolarDistillerScreenHandler.SLOT_UPGRADE_START..SolarDistillerScreenHandler.SLOT_UPGRADE_END) {
                        SlotHost(slotIndex)
                    }
                }
            }

            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = SolarDistillerScreenHandler.PLAYER_INV_START,
                playerInvY = GUI_SIZE.playerInvY,
                hotbarY = GUI_SIZE.hotbarY
            )
        }

        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)
        applyAnchoredSlots(layout, left, top)

        super.render(context, mouseX, mouseY, delta)

        ui.render(context, textRenderer, mouseX, mouseY, content = content)

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun UiScope.SlotHost(slotIndex: Int) {
        SlotAnchor(
            id = slotAnchorId(slotIndex),
            width = SolarDistillerScreenHandler.SLOT_SIZE,
            height = SolarDistillerScreenHandler.SLOT_SIZE
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
        private val GUI_SIZE = GuiSize.STANDARD_UPGRADE
        private val PANEL_WIDTH = GUI_SIZE.width
        private val PANEL_HEIGHT = GUI_SIZE.height

        /** 与 SemifluidGeneratorScreen 主行同高 */
        private const val MACHINE_ROW_HEIGHT = 60

        private const val FLUID_BAR_THICKNESS = 8

        private const val PROGRESS_BAR_WIDTH = 72
        private const val PROGRESS_BAR_HEIGHT = 8
    }
}
