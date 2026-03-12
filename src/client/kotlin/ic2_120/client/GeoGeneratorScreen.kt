package ic2_120.client

import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.FilteredValue
import ic2_120.client.ui.GuiBackground
import ic2_120.client.ui.ProgressBar
import ic2_120.content.sync.GeoGeneratorSync
import ic2_120.content.block.GeoGeneratorBlock
import ic2_120.content.block.machines.GeoGeneratorBlockEntity
import ic2_120.content.screen.GeoGeneratorScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

@ModScreen(block = GeoGeneratorBlock::class)
class GeoGeneratorScreen(
    handler: GeoGeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<GeoGeneratorScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()
    private var filteredOutputRate by FilteredValue()

    init {
        backgroundWidth = PANEL_WIDTH
        backgroundHeight = PANEL_HEIGHT
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y,
            GeoGeneratorScreenHandler.PLAYER_INV_Y,
            GeoGeneratorScreenHandler.HOTBAR_Y,
            GeoGeneratorScreenHandler.SLOT_SIZE
        )
        val borderColor = GuiBackground.BORDER_COLOR
        val slotSize = GeoGeneratorScreenHandler.SLOT_SIZE
        val borderOffset = 1

        // 燃料槽位边框（岩浆桶/岩浆单元输入）
        val fuelSlot = handler.slots[GeoGeneratorBlockEntity.FUEL_SLOT]
        context.drawBorder(x + fuelSlot.x - borderOffset, y + fuelSlot.y - borderOffset, slotSize, slotSize, borderColor)
        // 空容器槽位边框（空桶/空单元输出）
        val emptyContainerSlot = handler.slots[GeoGeneratorBlockEntity.EMPTY_CONTAINER_SLOT]
        context.drawBorder(x + emptyContainerSlot.x - borderOffset, y + emptyContainerSlot.y - borderOffset, slotSize, slotSize, borderColor)

        // 岩浆储量竖向条（右侧，高度与红框一致：从顶部到方块槽底部）
        val lavaMb = handler.sync.lavaAmountMb.coerceAtLeast(0)
        val lavaCapMb = 8 * 1000
        val lavaFrac = if (lavaCapMb > 0) (lavaMb.toFloat() / lavaCapMb).coerceIn(0f, 1f) else 0f
        val barW = 12
        val barX = x + backgroundWidth - barW - 8
        val barY = y + 8
        val barH = GeoGeneratorScreenHandler.BLOCK_SLOTS_Y + slotSize - 8
        ProgressBar.drawVerticalFuelBar(context, barX, barY, barW, barH, lavaFrac, gradient = false, showTicks = true)

        // 电池槽边框
        val batterySlot = handler.slots[GeoGeneratorBlockEntity.BATTERY_SLOT]
        context.drawBorder(x + batterySlot.x - borderOffset, y + batterySlot.y - borderOffset, slotSize, slotSize, borderColor)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        val left = x
        val top = y
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val inputRate = handler.sync.getSyncedInsertedAmount()
        filteredOutputRate = handler.sync.getSyncedExtractedAmount()
        val cap = GeoGeneratorSync.ENERGY_CAPACITY
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        // 左侧内容区宽度（右侧留出岩浆条空间）
        val rightBarsWidth = 12 + 8
        val contentW = (backgroundWidth - 16 - rightBarsWidth).coerceAtLeast(0)
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
                        "发电 ${formatEu(inputRate)} EU/t · 输出 ${formatEu(filteredOutputRate)} EU/t",
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


