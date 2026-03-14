package ic2_120.client

import ic2_120.client.compose.ComposeUI
import ic2_120.client.compose.*
import ic2_120.client.ui.FluidBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.SolarDistillerBlock
import ic2_120.content.screen.SolarDistillerScreenHandler
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.sync.SolarDistillerSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text as McText

@ModScreen(block = SolarDistillerBlock::class)
class SolarDistillerScreen(
    handler: SolarDistillerScreenHandler,
    playerInventory: PlayerInventory,
    title: McText
) : HandledScreen<SolarDistillerScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = UpgradeSlotLayout.VANILLA_UI_WIDTH + UpgradeSlotLayout.SLOT_SPACING
        backgroundHeight = 184
        titleY = 4
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y,
            SolarDistillerScreenHandler.PLAYER_INV_Y,
            SolarDistillerScreenHandler.HOTBAR_Y,
            SolarDistillerScreenHandler.SLOT_SIZE
        )
        val borderColor = GuiBackground.BORDER_COLOR
        val borderOffset = 1
        val slotSize = SolarDistillerScreenHandler.SLOT_SIZE

        for (i in 0..7) {
            val slot = handler.slots[i]
            context.drawBorder(x + slot.x - borderOffset, y + slot.y - borderOffset, slotSize, slotSize, borderColor)
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)

        val left = x
        val top = y
        val inputFraction = (handler.sync.waterInputMb.toFloat() / SolarDistillerSync.TANK_CAPACITY_MB).coerceIn(0f, 1f)
        val outputFraction = (handler.sync.distilledOutputMb.toFloat() / SolarDistillerSync.TANK_CAPACITY_MB).coerceIn(0f, 1f)
        val progressFraction = (handler.sync.progress.toFloat() / SolarDistillerSync.PRODUCE_INTERVAL_TICKS).coerceIn(0f, 1f)

        ui.render(context, textRenderer, mouseX, mouseY) {
            Column(x = left + 8, y = top + 8, spacing = 6, modifier = Modifier.EMPTY.width(backgroundWidth - 16)) {
                Text(title.string, color = 0xFFFFFF)
                Flex(direction = FlexDirection.ROW, gap = 10) {
                    Column(spacing = 2) {
                        Text("输入水", color = 0xAAAAAA)
                        FluidBar(inputFraction, barWidth = 8, barHeight = 58, vertical = true, modifier = Modifier.EMPTY.width(8).height(58))
                        Text("${handler.sync.waterInputMb} mB", color = 0xCCCCCC, shadow = false)
                    }
                    Column(spacing = 2) {
                        Text("蒸馏进度", color = 0xAAAAAA)
                        FluidBar(progressFraction, barWidth = 8, barHeight = 58, vertical = true, modifier = Modifier.EMPTY.width(8).height(58))
                        Text("${handler.sync.progress}/${SolarDistillerSync.PRODUCE_INTERVAL_TICKS}", color = 0xCCCCCC, shadow = false)
                    }
                    Column(spacing = 2) {
                        Text("输出液", color = 0xAAAAAA)
                        FluidBar(outputFraction, barWidth = 8, barHeight = 58, vertical = true, modifier = Modifier.EMPTY.width(8).height(58))
                        Text("${handler.sync.distilledOutputMb} mB", color = 0xCCCCCC, shadow = false)
                    }
                }
                Text(
                    if (handler.sync.isWorking != 0) "状态: 工作中（80 tick -> 1 mB）" else "状态: 停止",
                    color = 0xAAAAAA,
                    shadow = false
                )
            }
        }

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)
}
