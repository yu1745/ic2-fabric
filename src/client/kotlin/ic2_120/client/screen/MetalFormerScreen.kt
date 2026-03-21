package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.MetalFormerBlock
import ic2_120.content.screen.MetalFormerScreenHandler
import ic2_120.content.sync.MetalFormerSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
import net.minecraft.screen.slot.Slot
import net.minecraft.text.Text

@ModScreen(block = MetalFormerBlock::class)
class MetalFormerScreen(
    handler: MetalFormerScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<MetalFormerScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = GUI_SIZE.width
        backgroundHeight = GUI_SIZE.height
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y,
            MetalFormerScreenHandler.PLAYER_INV_Y,
            MetalFormerScreenHandler.HOTBAR_Y,
            MetalFormerScreenHandler.SLOT_SIZE
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val progressFrac = if (MetalFormerSync.PROGRESS_MAX > 0) {
            (handler.sync.progress.coerceIn(0, MetalFormerSync.PROGRESS_MAX)
                .toFloat() / MetalFormerSync.PROGRESS_MAX).coerceIn(0f, 1f)
        } else 0f
        val currentMode = handler.sync.getMode()
        val modeText = when (currentMode) {
            MetalFormerSync.Mode.ROLLING -> "辊压"
            MetalFormerSync.Mode.CUTTING -> "切割"
            MetalFormerSync.Mode.EXTRUDING -> "挤压"
        }
        // 直接使用后端滤波后的值
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val consumeRate = handler.sync.getSyncedConsumedAmount()

        // 在UI左侧绘制速度文本
        val inputText = "输入 ${formatEu(inputRate)} EU/t"
        val consumeText = "耗能 ${formatEu(consumeRate)} EU/t"
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
                        // barWidth = 0,
                        barHeight = 12,
                        // modifier = Modifier.EMPTY.width(58)
                    )

                    Flex(
                        direction = FlexDirection.ROW,
                        alignItems = AlignItems.CENTER,
                        gap = 6,
                        modifier = Modifier.EMPTY.width(GuiSize.STANDARD.contentWidth)
                    ) {
                        Text("模式: $modeText", color = 0xAAAAFF, shadow = false)
                        Button("切换", onClick = {
                            client?.player?.networkHandler?.sendPacket(
                                ButtonClickC2SPacket(handler.syncId, MetalFormerScreenHandler.BUTTON_ID_MODE_CYCLE)
                            )
                        })
                    }

                    Flex(
                        direction = FlexDirection.ROW,
                        alignItems = AlignItems.CENTER,
                        gap = 4
                    ) {
                        Column(spacing = 4) {
                            SlotHost(MetalFormerScreenHandler.SLOT_INPUT_INDEX)
                            SlotHost(MetalFormerScreenHandler.SLOT_DISCHARGING_INDEX)
                        }
                        EnergyBar(progressFrac, modifier = Modifier.EMPTY.fractionWidth(1.0f))
                        SlotHost(MetalFormerScreenHandler.SLOT_OUTPUT_INDEX)
                    }
                }

                Column(
                    spacing = 4,
                    modifier = Modifier.EMPTY
                        .width(GuiSize.UPGRADE_COLUMN_WIDTH)
                        .padding(0, 8, 0, 0)
                ) {
                    for (slotIndex in MetalFormerScreenHandler.SLOT_UPGRADE_INDEX_START..MetalFormerScreenHandler.SLOT_UPGRADE_INDEX_END) {
                        SlotHost(slotIndex)
                    }
                }
            }
        }

        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)
        applyAnchoredSlots(layout, left, top)

        super.render(context, mouseX, mouseY, delta)
        ui.render(context, textRenderer, mouseX, mouseY, content = content)
        // drawProgressSegments(context)
        context.drawText(textRenderer, inputText, sideTextX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, consumeText, sideTextX, top + 20, 0xAAAAAA, false)
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun UiScope.SlotHost(slotIndex: Int) {
        SlotAnchor(
            id = slotAnchorId(slotIndex),
            width = MetalFormerScreenHandler.SLOT_SIZE,
            height = MetalFormerScreenHandler.SLOT_SIZE
        )
    }

    private fun applyAnchoredSlots(layout: ComposeUI.LayoutSnapshot, left: Int, top: Int) {
        handler.slots.forEachIndexed { index, slot ->
            val anchor = layout.anchors[slotAnchorId(index)] ?: return@forEachIndexed
            slot.x = anchor.x - left
            slot.y = anchor.y - top
        }
    }

    // private fun drawProgressSegments(context: DrawContext) {
    //     val progress = handler.sync.progress.coerceIn(0, MetalFormerSync.PROGRESS_MAX)
    //     val progressFrac = if (MetalFormerSync.PROGRESS_MAX > 0) {
    //         (progress.toFloat() / MetalFormerSync.PROGRESS_MAX).coerceIn(0f, 1f)
    //     } else {
    //         0f
    //     }
    //     val segmentSize = 1f / 3f
    //     val barFractions = listOf(
    //         when {
    //             progressFrac <= segmentSize -> progressFrac / segmentSize
    //             else -> 1f
    //         },
    //         when {
    //             progressFrac <= segmentSize -> 0f
    //             progressFrac <= segmentSize * 2 -> (progressFrac - segmentSize) / segmentSize
    //             else -> 1f
    //         },
    //         when {
    //             progressFrac <= segmentSize * 2 -> 0f
    //             else -> (progressFrac - segmentSize * 2) / segmentSize
    //         }
    //     )

    //     val inputSlot = handler.slots[MetalFormerScreenHandler.SLOT_INPUT_INDEX]
    //     val startX = x + inputSlot.x + MetalFormerScreenHandler.SLOT_SIZE + PROGRESS_BAR_START_GAP
    //     val startY = y + inputSlot.y + PROGRESS_BAR_TOP_OFFSET

    //     for (i in 0..2) {
    //         val barX = startX + i * (PROGRESS_BAR_WIDTH + PROGRESS_BAR_GAP)
    //         ProgressBar.draw(context, barX, startY, PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT, barFractions[i])
    //     }
    // }

    private fun slotAnchorId(slotIndex: Int): String = "slot.$slotIndex"

    private fun formatEu(value: Long): String {
        return when {
            value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
            else -> value.toString()
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // 模式切换已由 Compose 的 Button 处理
        if (ui.mouseClicked(mouseX, mouseY, button)) return true
        return super.mouseClicked(mouseX, mouseY, button)
    }

    companion object {
        private val GUI_SIZE = GuiSize.STANDARD_UPGRADE
        private const val PROGRESS_SECTION_WIDTH = 68
        private const val PROGRESS_BAR_START_GAP = 4
        private const val PROGRESS_BAR_TOP_OFFSET = 2
        private const val PROGRESS_BAR_GAP = 4
        private const val PROGRESS_BAR_WIDTH = 20
        private const val PROGRESS_BAR_HEIGHT = 6
    }
}
