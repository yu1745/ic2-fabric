package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.ui.GuiBackground
import ic2_120.client.ui.HeatProgressBar
import ic2_120.content.block.BlastFurnaceBlock
import ic2_120.content.screen.BlastFurnaceScreenHandler
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.sync.BlastFurnaceSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

@ModScreen(block = BlastFurnaceBlock::class)
class BlastFurnaceScreen(
    handler: BlastFurnaceScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<BlastFurnaceScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = PANEL_WIDTH
        backgroundHeight = PANEL_HEIGHT
        titleY = 4
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y,
            BlastFurnaceScreenHandler.PLAYER_INV_Y,
            BlastFurnaceScreenHandler.HOTBAR_Y,
            BlastFurnaceScreenHandler.SLOT_SIZE
        )
        val borderColor = GuiBackground.BORDER_COLOR
        val slotSize = BlastFurnaceScreenHandler.SLOT_SIZE
        val borderOffset = 1

        val inputSlot = handler.slots[BlastFurnaceScreenHandler.SLOT_INPUT_INDEX]
        val airInputSlot = handler.slots[BlastFurnaceScreenHandler.SLOT_AIR_INPUT_INDEX]
        val outputSteelSlot = handler.slots[BlastFurnaceScreenHandler.SLOT_OUTPUT_STEEL_INDEX]
        val outputSlagSlot = handler.slots[BlastFurnaceScreenHandler.SLOT_OUTPUT_SLAG_INDEX]
        val emptyOutputSlot = handler.slots[BlastFurnaceScreenHandler.SLOT_OUTPUT_EMPTY_INDEX]

        context.drawBorder(x + inputSlot.x - borderOffset, y + inputSlot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + airInputSlot.x - borderOffset, y + airInputSlot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + outputSteelSlot.x - borderOffset, y + outputSteelSlot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + outputSlagSlot.x - borderOffset, y + outputSlagSlot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + emptyOutputSlot.x - borderOffset, y + emptyOutputSlot.y - borderOffset, slotSize, slotSize, borderColor)

        for (i in BlastFurnaceScreenHandler.SLOT_UPGRADE_INDEX_START..BlastFurnaceScreenHandler.SLOT_UPGRADE_INDEX_END) {
            val slot = handler.slots[i]
            context.drawBorder(x + slot.x - borderOffset, y + slot.y - borderOffset, slotSize, slotSize, borderColor)
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        val left = x
        val top = y

        val preheat = handler.sync.preheat.toLong().coerceAtLeast(0)
        val preheatCap = BlastFurnaceSync.PREHEAT_MAX.toLong()
        val preheatFrac = if (preheatCap > 0) (preheat.toFloat() / preheatCap).coerceIn(0f, 1f) else 0f

        val progress = handler.sync.progress.coerceIn(0, BlastFurnaceSync.PROGRESS_MAX)
        val progressFrac = if (BlastFurnaceSync.PROGRESS_MAX > 0) (progress.toFloat() / BlastFurnaceSync.PROGRESS_MAX).coerceIn(0f, 1f) else 0f

        val airUsed = handler.sync.progress / BlastFurnaceSync.TICKS_PER_AIR_CELL

        ui.render(context, textRenderer, mouseX, mouseY) {
            // 热量区
            Column(x = left + 8, y = top + 4, spacing = 2) {
                Text("热量", color = 0xAAAAAA)
                HeatProgressBar(
                    preheatFrac,
                    barWidth = 60,
                    barHeight = 8,
                    startColor = 0xFF660000.toInt(),
                    endColor = 0xFFCC0000.toInt(),
                    gradient = true,
                    modifier = Modifier.EMPTY
                )
                Text(
                    "$preheat / $preheatCap HU",
                    color = 0xCCCCCC,
                    shadow = false
                )
                Text(
                    "空气: $airUsed/${BlastFurnaceSync.AIR_CELLS_PER_STEEL} 瓶/周期",
                    color = 0xAAAAAA,
                    shadow = false
                )
            }

            // 周期进度条（置于槽位上方，避免遮挡）
            // 位置：inputSlot.x(56) + slotSize(18) + 2 = 76, y = 27
            HeatProgressBar(
                progressFrac,
                barWidth = 36,
                barHeight = 8,
                startColor = 0xFFCC4400.toInt(),
                endColor = 0xFFCC0000.toInt(),
                gradient = true,
                x = left + 76,
                y = top + 27,
                absolute = true
            )
        }

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    companion object {
        private val PANEL_WIDTH = UpgradeSlotLayout.VANILLA_UI_WIDTH + UpgradeSlotLayout.SLOT_SPACING
        private const val PANEL_HEIGHT = 184
    }
}
