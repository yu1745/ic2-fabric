package ic2_120.client

import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.client.ui.FluidBar
import ic2_120.client.ui.ProgressBar
import ic2_120.content.sync.OreWashingPlantSync
import ic2_120.content.block.OreWashingPlantBlock
import ic2_120.content.screen.OreWashingPlantScreenHandler
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

@ModScreen(block = OreWashingPlantBlock::class)
class OreWashingPlantScreen(
    handler: OreWashingPlantScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<OreWashingPlantScreenHandler>(handler, playerInventory, title) {

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
            OreWashingPlantScreenHandler.PLAYER_INV_Y,
            OreWashingPlantScreenHandler.HOTBAR_Y,
            OreWashingPlantScreenHandler.SLOT_SIZE
        )

        val borderColor = GuiBackground.BORDER_COLOR
        val slotSize = OreWashingPlantScreenHandler.SLOT_SIZE
        val borderOffset = 1

        // 绘制机器槽位边框
        val inputOreSlot = handler.slots[OreWashingPlantScreenHandler.SLOT_INPUT_ORE_INDEX]
        val inputWaterSlot = handler.slots[OreWashingPlantScreenHandler.SLOT_INPUT_WATER_INDEX]
        val dischargingSlot = handler.slots[OreWashingPlantScreenHandler.SLOT_DISCHARGING_INDEX]
        val output1Slot = handler.slots[OreWashingPlantScreenHandler.SLOT_OUTPUT_1_INDEX]
        val output2Slot = handler.slots[OreWashingPlantScreenHandler.SLOT_OUTPUT_2_INDEX]
        val output3Slot = handler.slots[OreWashingPlantScreenHandler.SLOT_OUTPUT_3_INDEX]
        val emptyOutputSlot = handler.slots[OreWashingPlantScreenHandler.SLOT_OUTPUT_EMPTY_INDEX]

        context.drawBorder(x + inputOreSlot.x - borderOffset, y + inputOreSlot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + inputWaterSlot.x - borderOffset, y + inputWaterSlot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + dischargingSlot.x - borderOffset, y + dischargingSlot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + output1Slot.x - borderOffset, y + output1Slot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + output2Slot.x - borderOffset, y + output2Slot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + output3Slot.x - borderOffset, y + output3Slot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + emptyOutputSlot.x - borderOffset, y + emptyOutputSlot.y - borderOffset, slotSize, slotSize, borderColor)

        // 绘制升级槽边框
        for (i in OreWashingPlantScreenHandler.SLOT_UPGRADE_INDEX_START..OreWashingPlantScreenHandler.SLOT_UPGRADE_INDEX_END) {
            val slot = handler.slots[i]
            context.drawBorder(x + slot.x - borderOffset, y + slot.y - borderOffset, slotSize, slotSize, borderColor)
        }

        // 绘制进度条
        val progress = handler.sync.progress.coerceIn(0, OreWashingPlantSync.PROGRESS_MAX)
        val progressFrac = if (OreWashingPlantSync.PROGRESS_MAX > 0) (progress.toFloat() / OreWashingPlantSync.PROGRESS_MAX).coerceIn(0f, 1f) else 0f
        val barX = x + inputOreSlot.x + slotSize + 2
        val barW = output1Slot.x - (inputOreSlot.x + slotSize) - 4
        val barH = 8
        val barY = y + inputOreSlot.y + (slotSize - barH) / 2
        ProgressBar.draw(context, barX, barY, barW, barH, progressFrac)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        val left = x
        val top = y
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val waterAmount = handler.sync.waterAmountMb.toLong()
        val waterCapacity = 8000L  // 8桶 = 8000 mB
        val waterFraction = if (waterCapacity > 0) (waterAmount.toFloat() / waterCapacity).coerceIn(0f, 1f) else 0f
        val contentW = (backgroundWidth - 16).coerceAtLeast(0)
        val waterBarVisualWidth = 6
        val waterBarVisualHeight = 60
        val waterSectionW = 18
        val rightReservedForUpgrades = 30
        val topRowW = (contentW - rightReservedForUpgrades).coerceAtLeast(0)
        val titleLabel = title.string
        val energyText = "$energy / $cap EU"
        val energyTextW = textRenderer.getWidth(energyText)
        val titleW = textRenderer.getWidth(titleLabel)
        val energyBarW = (topRowW - titleW - energyTextW - waterSectionW - 16).coerceAtLeast(24)

        // 在UI左侧绘制速度文本
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val consumeRate = handler.sync.getSyncedConsumedAmount()
        val inputText = "输入 ${formatEu(inputRate)} EU/t"
        val consumeText = "耗能 ${formatEu(consumeRate)} EU/t"
        val inputTextWidth = inputText.length * 6
        val consumeTextWidth = consumeText.length * 6
        val textX = left - maxOf(inputTextWidth, consumeTextWidth) - 4
        context.drawText(textRenderer, inputText, textX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, consumeText, textX, top + 20, 0xAAAAAA, false)

        ui.render(context, textRenderer, mouseX, mouseY) {
            Column(x = left + 8, y = top + 8, spacing = 2, modifier = Modifier.EMPTY.width(contentW)) {
                Flex(
                    direction = FlexDirection.ROW,
                    justifyContent = JustifyContent.SPACE_BETWEEN,
                    alignItems = AlignItems.START,
                    gap = 8,
                    modifier = Modifier.EMPTY.width(topRowW)
                ) {
                    Flex(
                        direction = FlexDirection.ROW,
                        alignItems = AlignItems.CENTER,
                        gap = 6,
                        modifier = Modifier.EMPTY.width((topRowW - waterSectionW).coerceAtLeast(0))
                    ) {
                        Text(titleLabel, color = 0xFFFFFF)
                        EnergyBar(
                            energyFraction,
                            barWidth = energyBarW,
                            barHeight = 9,
                            modifier = Modifier.EMPTY.width(energyBarW)
                        )
                        Text(energyText, color = 0xCCCCCC, shadow = false)
                    }
                    Column(spacing = 2, modifier = Modifier.EMPTY.width(waterSectionW)) {
                        Text("水", color = 0xAAAAAA)
                        FluidBar(
                            waterFraction,
                            barWidth = waterBarVisualHeight,
                            barHeight = waterBarVisualWidth,
                            modifier = Modifier.EMPTY.width(waterBarVisualWidth).height(waterBarVisualHeight),
                            vertical = true
                        )
                    }
                }
            }
        }
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    companion object {
        private val PANEL_WIDTH = UpgradeSlotLayout.VANILLA_UI_WIDTH + UpgradeSlotLayout.SLOT_SPACING
        private const val PANEL_HEIGHT = 184
    }

    private fun formatEu(value: Long): String {
        return when {
            value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
            else -> value.toString()
        }
    }
}
