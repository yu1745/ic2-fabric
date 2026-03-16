package ic2_120.client

import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.client.ui.ProgressBar
import ic2_120.content.sync.GeneratorSync
import ic2_120.content.block.GeneratorBlock
import ic2_120.content.block.machines.GeneratorBlockEntity
import ic2_120.content.block.machines.MachineBlockEntity
import ic2_120.content.screen.GeneratorScreenHandler
import ic2_120.registry.annotation.ModScreen
import ic2_120.registry.type
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

@ModScreen(block = GeneratorBlock::class)
class GeneratorScreen(
    handler: GeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<GeneratorScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = PANEL_WIDTH
        backgroundHeight = PANEL_HEIGHT
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y,
            GeneratorScreenHandler.PLAYER_INV_Y,
            GeneratorScreenHandler.HOTBAR_Y,
            GeneratorScreenHandler.SLOT_SIZE
        )
        val borderColor = GuiBackground.BORDER_COLOR
        val slotSize = GeneratorScreenHandler.SLOT_SIZE
        val borderOffset = 1

        // 燃料槽边框
        val fuelSlot = handler.slots[MachineBlockEntity.FUEL_SLOT]
        context.drawBorder(x + fuelSlot.x - borderOffset, y + fuelSlot.y - borderOffset, slotSize, slotSize, borderColor)

        // 电池槽边框
        val batterySlot = handler.slots[MachineBlockEntity.BATTERY_SLOT]
        context.drawBorder(x + batterySlot.x - borderOffset, y + batterySlot.y - borderOffset, slotSize, slotSize, borderColor)
        // 燃烧进度条：竖向渐变（红→蓝），表示燃料逐渐用尽
        val totalBurn = handler.sync.totalBurnTime.coerceAtLeast(1)
        val burnTime = handler.sync.burnTime.coerceIn(0, totalBurn)
        val burnFrac = (burnTime.toFloat() / totalBurn).coerceIn(0f, 1f)
        val barX = x + fuelSlot.x + slotSize + 2
        val barW = 6  // 竖向条宽度
        val barH = slotSize  // 高度与槽位相同
        val barY = y + fuelSlot.y
        ProgressBar.drawVerticalFuelBar(context, barX, barY, barW, barH, burnFrac)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        val left = x
        val top = y
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        // 直接使用后端滤波后的值
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val outputRate = handler.sync.getSyncedExtractedAmount()
        val cap = GeneratorSync.ENERGY_CAPACITY
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
                Row(spacing = 8) {
                    Text(
                        "${formatEu(energy)} / ${formatEu(cap)} EU",
                        color = 0xCCCCCC,
                        shadow = false
                    )
                    Text(
                        "发电 ${formatEu(inputRate)} EU/t · 输出 ${formatEu(outputRate)} EU/t",
                        color = 0xAAAAAA,
                        shadow = false
                    )
                }
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

