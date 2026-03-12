package ic2_120.client

import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.client.ui.ProgressBar
import ic2_120.content.sync.ElectricFurnaceSync
import ic2_120.content.block.ElectricFurnaceBlock
import ic2_120.content.screen.ElectricFurnaceScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

@ModScreen(block = ElectricFurnaceBlock::class)
class ElectricFurnaceScreen(
    handler: ElectricFurnaceScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<ElectricFurnaceScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = PANEL_WIDTH
        backgroundHeight = PANEL_HEIGHT
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context,
            x,
            y,
            ElectricFurnaceScreenHandler.PLAYER_INV_Y,
            ElectricFurnaceScreenHandler.HOTBAR_Y,
            ElectricFurnaceScreenHandler.SLOT_SIZE
        )
        // 机器槽周围绘制边框（使用与 HandledScreen 相同的 x,y 及 slot 坐标，保证与物品绘制对齐）
        val borderColor = GuiBackground.BORDER_COLOR
        val slotSize = ElectricFurnaceScreenHandler.SLOT_SIZE
        val borderOffset = 1
        val borderW = slotSize
        val inputSlot = handler.slots[0]
        val outputSlot = handler.slots[1]
        context.drawBorder(x + inputSlot.x - borderOffset, y + inputSlot.y - borderOffset, borderW, borderW, borderColor)
        context.drawBorder(x + outputSlot.x - borderOffset, y + outputSlot.y - borderOffset, borderW, borderW, borderColor)
        // 烧炼进度条：夹在输入槽与输出槽之间，原版风格
        val progress = handler.sync.progress.coerceIn(0, ElectricFurnaceSync.PROGRESS_MAX)
        val progressFrac = if (ElectricFurnaceSync.PROGRESS_MAX > 0) (progress.toFloat() / ElectricFurnaceSync.PROGRESS_MAX).coerceIn(0f, 1f) else 0f
        val barX = x + inputSlot.x + slotSize + 2
        val barW = outputSlot.x - (inputSlot.x + slotSize) - 4
        val barH = 8
        val barY = y + inputSlot.y + (slotSize - barH) / 2
        ProgressBar.draw(context, barX, barY, barW, barH, progressFrac)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)

        val left = x
        val top = y
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val consumeRate = handler.sync.getSyncedConsumedAmount()
        val cap = ElectricFurnaceSync.ENERGY_CAPACITY
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val contentW = (backgroundWidth - 16).coerceAtLeast(0)
        val barW = (contentW - 36).coerceAtLeast(0)

        ui.render(context, textRenderer, mouseX, mouseY) {
            Column(x = left + 8, y = top + 8, spacing = 6) {
                Text(title.string, color = 0xFFFFFF)
                Flex(
                    direction = FlexDirection.ROW,
                    alignItems = AlignItems.CENTER,
                    gap = 8,
                    modifier = Modifier.EMPTY.width(contentW)
                ) {
                    Text("能量", color = 0xAAAAAA)
                    EnergyBar(
                        energyFraction,
                        barWidth = 0,
                        barHeight = 9,
                        modifier = Modifier.EMPTY.width(barW)
                    )
                }
                Text(
                    "$energy / $cap EU",
                    color = 0xCCCCCC,
                    shadow = false
                )
                Text(
                    "输入 ${formatEu(inputRate)} EU/t · 耗能 ${formatEu(consumeRate)} EU/t",
                    color = 0xAAAAAA,
                    shadow = false
                )
            }
        }

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    companion object {
        private const val PANEL_WIDTH = 176
        private const val PANEL_HEIGHT = 166
    }

    private fun formatEu(value: Long): String {
        return when {
            value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
            else -> value.toString()
        }
    }
}

