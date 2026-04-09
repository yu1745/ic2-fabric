package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.ui.GuiBackground
import ic2_120.client.ui.HeatProgressBar
import ic2_120.content.block.FermenterBlock
import ic2_120.content.screen.FermenterScreenHandler
import ic2_120.content.screen.GuiSize
import ic2_120.content.sync.FermenterSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text as McText

@ModScreen(block = FermenterBlock::class)
class FermenterScreen(
    handler: FermenterScreenHandler,
    playerInventory: PlayerInventory,
    title: McText
) : HandledScreen<FermenterScreenHandler>(handler, playerInventory, title) {

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

        val heatFraction = (handler.sync.bufferedHeat.toFloat() / 40_000f).coerceIn(0f, 1f)
        val progressFraction = (handler.sync.progress.toFloat() / FermenterSync.PROCESS_INTERVAL_TICKS).coerceIn(0f, 1f)

        val content: UiScope.() -> Unit = {
            Row(
                x = left + 8,
                y = top + 8,
                spacing = 8,
                modifier = Modifier.EMPTY.width(GUI_SIZE.contentWidth)
            ) {
                // 左侧：生物质组
                Column(
                    spacing = 4,
                    modifier = Modifier.EMPTY.padding(0, 8, 0, 0)
                ) {
                    Text("生物质", color = 0xAAAAAA)
                    SlotAnchor(
                        id = slotAnchorId(FermenterScreenHandler.SLOT_INPUT_FILLED_CONTAINER_INDEX),
                        width = FermenterScreenHandler.SLOT_SIZE,
                        height = FermenterScreenHandler.SLOT_SIZE
                    )
                    SlotAnchor(
                        id = slotAnchorId(FermenterScreenHandler.SLOT_INPUT_EMPTY_CONTAINER_INDEX),
                        width = FermenterScreenHandler.SLOT_SIZE,
                        height = FermenterScreenHandler.SLOT_SIZE
                    )
                }

                // 中间：热量 + 进度条
                Column(spacing = 6) {
                    Text(title.string, color = 0xFFFFFF)
                    Text(
                        if (handler.sync.isWorking != 0) "状态: 工作中" else "状态: 停止",
                        color = 0xAAAAAA,
                        shadow = false
                    )
                    Flex(direction = FlexDirection.ROW, alignItems = AlignItems.CENTER, gap = 8) {
                        Text("热量", color = 0xAAAAAA)
                        HeatProgressBar(
                            heatFraction,
                            barWidth = 0,
                            barHeight = 8,
                            startColor = 0xFF663300.toInt(),
                            endColor = 0xFFCC6600.toInt(),
                            gradient = true,
                            modifier = Modifier.EMPTY.fractionWidth(1.0f)
                        )
                        Text("${handler.sync.bufferedHeat} HU", color = 0xFFFFFF, shadow = false)
                    }
                    Flex(direction = FlexDirection.ROW, alignItems = AlignItems.CENTER, gap = 8) {
                        Text("进度", color = 0xAAAAAA)
                        HeatProgressBar(
                            progressFraction,
                            barWidth = 0,
                            barHeight = 8,
                            startColor = 0xFF336633.toInt(),
                            endColor = 0xFF55AA55.toInt(),
                            gradient = true,
                            modifier = Modifier.EMPTY.fractionWidth(1.0f)
                        )
                        Text("${handler.sync.progress}/${FermenterSync.PROCESS_INTERVAL_TICKS}", color = 0xFFFFFF, shadow = false)
                    }
                }

                // 右侧：沼气组
                Column(
                    spacing = 4,
                    modifier = Modifier.EMPTY.padding(0, 8, 0, 0)
                ) {
                    Text("沼气", color = 0xAAAAAA)
                    SlotAnchor(
                        id = slotAnchorId(FermenterScreenHandler.SLOT_OUTPUT_EMPTY_CONTAINER_INDEX),
                        width = FermenterScreenHandler.SLOT_SIZE,
                        height = FermenterScreenHandler.SLOT_SIZE
                    )
                    SlotAnchor(
                        id = slotAnchorId(FermenterScreenHandler.SLOT_OUTPUT_FILLED_CONTAINER_INDEX),
                        width = FermenterScreenHandler.SLOT_SIZE,
                        height = FermenterScreenHandler.SLOT_SIZE
                    )
                }

                // 升级列
                Column(
                    spacing = 4,
                    modifier = Modifier.EMPTY
                        .width(GuiSize.UPGRADE_COLUMN_WIDTH)
                        .padding(0, 8, 0, 0)
                ) {
                    for (slotIndex in FermenterScreenHandler.SLOT_UPGRADE_INDEX_START..FermenterScreenHandler.SLOT_UPGRADE_INDEX_END) {
                        SlotAnchor(
                            id = slotAnchorId(slotIndex),
                            width = FermenterScreenHandler.SLOT_SIZE,
                            height = FermenterScreenHandler.SLOT_SIZE
                        )
                    }
                }
            }

            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = FermenterScreenHandler.PLAYER_INV_START,
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

        val inputText = "传热 ${handler.sync.heatInputPerTick} HU/t"
        val consumeText = "耗热 ${handler.sync.heatConsumePerTick} HU/t"
        val sideTextWidth = maxOf(textRenderer.getWidth(inputText), textRenderer.getWidth(consumeText))
        val sideTextX = left - sideTextWidth - 4
        context.drawText(textRenderer, inputText, sideTextX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, consumeText, sideTextX, top + 20, 0xAAAAAA, false)

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    private fun slotAnchorId(slotIndex: Int): String = "slot.$slotIndex"

    companion object {
        private val GUI_SIZE = GuiSize.STANDARD_UPGRADE
    }
}
