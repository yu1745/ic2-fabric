package ic2_120.client

import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.WindGeneratorBlock
import ic2_120.content.block.machines.WindGeneratorBlockEntity
import ic2_120.content.screen.WindGeneratorScreenHandler
import ic2_120.content.sync.WindGeneratorSync
import ic2_120.registry.annotation.ModScreen
import ic2_120.registry.type
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = WindGeneratorBlock::class)
class WindGeneratorScreen(
    handler: WindGeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<WindGeneratorScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = PANEL_WIDTH
        backgroundHeight = PANEL_HEIGHT
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y,
            WindGeneratorScreenHandler.PLAYER_INV_Y,
            WindGeneratorScreenHandler.HOTBAR_Y,
            WindGeneratorScreenHandler.SLOT_SIZE
        )
        val borderColor = GuiBackground.BORDER_COLOR
        val slotSize = WindGeneratorScreenHandler.SLOT_SIZE
        val borderOffset = 1

        val batterySlot = handler.slots[WindGeneratorBlockEntity.BATTERY_SLOT]
        context.drawBorder(x + batterySlot.x - borderOffset, y + batterySlot.y - borderOffset, slotSize, slotSize, borderColor)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        val left = x
        val top = y
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val outputRate = handler.sync.getSyncedExtractedAmount()
        val cap = WindGeneratorSync.ENERGY_CAPACITY
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val contentW = (backgroundWidth - 16).coerceAtLeast(0)
        val barW = (contentW - 36).coerceAtLeast(0)

        // 风力指示：发电中显示风车纹理（复用 solar 布局，用 isGenerating 切换）
        val isGenerating = handler.sync.isGenerating != 0
        val iconU = if (isGenerating) 16f else 0f
        context.drawTexture(
            Identifier("ic2", "textures/gui/overlay/solar_sun.png"),
            left + 8, top + 8,
            iconU, 0f,
            16, 16,
            32, 16
        )

        ui.render(context, textRenderer, mouseX, mouseY) {
            Column(x = left + 8, y = top + 28, spacing = 6) {
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

