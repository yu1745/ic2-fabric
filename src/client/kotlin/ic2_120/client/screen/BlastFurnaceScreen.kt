package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.ui.GuiBackground
import ic2_120.client.ui.HeatProgressBar
import ic2_120.content.block.BlastFurnaceBlock
import ic2_120.content.screen.BlastFurnaceScreenHandler
import ic2_120.content.screen.GuiSize
import ic2_120.content.sync.BlastFurnaceSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.slot.Slot
import net.minecraft.text.Text

@ModScreen(block = BlastFurnaceBlock::class)
class BlastFurnaceScreen(
    handler: BlastFurnaceScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<BlastFurnaceScreenHandler>(handler, playerInventory, title) {

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

        val preheat = handler.sync.preheat.toLong().coerceAtLeast(0)
        val preheatCap = BlastFurnaceSync.PREHEAT_MAX.toLong()
        val preheatFrac = if (preheatCap > 0) (preheat.toFloat() / preheatCap).coerceIn(0f, 1f) else 0f

        val progress = handler.sync.progress.coerceIn(0, BlastFurnaceSync.PROGRESS_MAX)
        val progressFrac = if (BlastFurnaceSync.PROGRESS_MAX > 0) (progress.toFloat() / BlastFurnaceSync.PROGRESS_MAX).coerceIn(0f, 1f) else 0f

        val airUsed = handler.sync.progress / BlastFurnaceSync.TICKS_PER_AIR_CELL

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
                    }

                    // 热量条
                    Flex(
                        direction = FlexDirection.ROW,
                        alignItems = AlignItems.CENTER,
                        gap = 8
                    ) {
                        Text("热量", color = 0xAAAAAA)
                        HeatProgressBar(
                            preheatFrac,
                            barWidth = 0,
                            barHeight = 8,
                            startColor = 0xFF660000.toInt(),
                            endColor = 0xFFCC0000.toInt(),
                            gradient = true,
                            modifier = Modifier.EMPTY.fractionWidth(1.0f)
                        )
                        Text("$preheat / $preheatCap HU", color = 0xFFFFFF, shadow = false)
                    }

                    Text(
                        "空气: $airUsed/${BlastFurnaceSync.AIR_CELLS_PER_STEEL} 瓶/周期",
                        color = 0xAAAAAA,
                        shadow = false
                    )

                    // 槽位布局
                    Flex(
                        direction = FlexDirection.ROW,
                        alignItems = AlignItems.CENTER,
                        gap = 4
                    ) {
                        SlotHost(BlastFurnaceScreenHandler.SLOT_INPUT_INDEX)
                        HeatProgressBar(
                            progressFrac,
                            barWidth = 0,
                            barHeight = 8,
                            startColor = 0xFFCC4400.toInt(),
                            endColor = 0xFFCC0000.toInt(),
                            gradient = true,
                            modifier = Modifier.EMPTY.fractionWidth(1.0f)
                        )
                        SlotHost(BlastFurnaceScreenHandler.SLOT_OUTPUT_STEEL_INDEX)
                    }

                    Flex(
                        direction = FlexDirection.ROW,
                        alignItems = AlignItems.CENTER,
                        gap = 4
                    ) {
                        SlotHost(BlastFurnaceScreenHandler.SLOT_AIR_INPUT_INDEX)
                        SlotHost(BlastFurnaceScreenHandler.SLOT_OUTPUT_SLAG_INDEX)
                        SlotHost(BlastFurnaceScreenHandler.SLOT_OUTPUT_EMPTY_INDEX)
                    }
                }

                Column(
                    spacing = 4,
                    modifier = Modifier.EMPTY
                        .width(GuiSize.UPGRADE_COLUMN_WIDTH)
                        .padding(0, 8, 0, 0)
                ) {
                    for (slotIndex in BlastFurnaceScreenHandler.SLOT_UPGRADE_INDEX_START..BlastFurnaceScreenHandler.SLOT_UPGRADE_INDEX_END) {
                        SlotHost(slotIndex)
                    }
                }
            }

            playerInventoryAndHotbarSlotAnchors(
                left = left,
                top = top,
                playerInvStart = BlastFurnaceScreenHandler.PLAYER_INV_START,
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
            width = BlastFurnaceScreenHandler.SLOT_SIZE,
            height = BlastFurnaceScreenHandler.SLOT_SIZE
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
