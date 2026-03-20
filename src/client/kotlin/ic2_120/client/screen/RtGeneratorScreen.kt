package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.RtGeneratorBlock
import ic2_120.content.block.machines.RtGeneratorBlockEntity
import ic2_120.content.screen.RtGeneratorScreenHandler
import ic2_120.content.sync.RtGeneratorSync
import ic2_120.registry.annotation.ModScreen
import ic2_120.registry.type
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

@ModScreen(block = RtGeneratorBlock::class)
class RtGeneratorScreen(
    handler: RtGeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<RtGeneratorScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = PANEL_WIDTH
        backgroundHeight = PANEL_HEIGHT
        titleY = -1000  // 隐藏默认标题，使用 ComposeUI 自定义布局
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y,
            RtGeneratorScreenHandler.PLAYER_INV_Y,
            RtGeneratorScreenHandler.HOTBAR_Y,
            RtGeneratorScreenHandler.SLOT_SIZE
        )
        val borderColor = GuiBackground.BORDER_COLOR
        val slotSize = RtGeneratorScreenHandler.SLOT_SIZE
        val borderOffset = 1

        // 燃料槽 2x3 网格整体边框
        val fuelSlot0 = handler.slots[0]
        val fuelSlot5 = handler.slots[5]
        val gridX = x + fuelSlot0.x - borderOffset
        val gridY = y + fuelSlot0.y - borderOffset
        val gridW = (fuelSlot5.x - fuelSlot0.x) + slotSize + borderOffset * 2
        val gridH = (fuelSlot5.y - fuelSlot0.y) + slotSize + borderOffset * 2
        context.drawBorder(gridX, gridY, gridW, gridH, borderColor)

        // 电池槽边框
        val batterySlot = handler.slots[RtGeneratorBlockEntity.BATTERY_SLOT]
        context.drawBorder(x + batterySlot.x - borderOffset, y + batterySlot.y - borderOffset, slotSize, slotSize, borderColor)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        val left = x
        val top = y
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = RtGeneratorSync.ENERGY_CAPACITY
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f

        // 使用后端同步的实际输出
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val outputRate = handler.sync.getSyncedExtractedAmount()

        // 能量 I/O 率显示在面板左侧外部（与火力发电机一致）
        val generationText = "发电 ${formatEu(inputRate)} EU/t"
        val outputText = "输出 ${formatEu(outputRate)} EU/t"
        val textX = x - maxOf(generationText.length * 6, outputText.length * 6) - 4
        context.drawText(textRenderer, generationText, textX, y + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, outputText, textX, y + 20, 0xAAAAAA, false)

        // 面板内部显示：标题 + 能量缓冲条
        ui.render(context, textRenderer, mouseX, mouseY) {
            Column(x = left + 8, y = top + 8, spacing = 6, absolute = true) {
                Text(title.string, color = 0xFFFFFF)
                Flex(
                    direction = FlexDirection.ROW,
                    alignItems = AlignItems.CENTER,
                    gap = 8,
                    modifier = Modifier.EMPTY.width(backgroundWidth - 16)
                ) {
                    Text("缓冲", color = 0xAAAAAA)
                    EnergyBar(
                        energyFraction,
                        barWidth = 120,
                        barHeight = 9
                    )
                }
                Text(
                    "${formatEu(energy)} / ${formatEu(cap)} EU",
                    color = 0xCCCCCC,
                    shadow = false
                )
            }
        }
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun formatEu(value: Long): String {
        return when {
            value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
            else -> value.toString()
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    companion object {
        private const val PANEL_WIDTH = 176
        private const val PANEL_HEIGHT = 166
    }
}

