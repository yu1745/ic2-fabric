package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.client.ui.HeatProgressBar
import ic2_120.client.ui.ProgressBar
import ic2_120.content.block.CentrifugeBlock
import ic2_120.content.screen.CentrifugeScreenHandler
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.sync.CentrifugeSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

@ModScreen(block = CentrifugeBlock::class)
class CentrifugeScreen(
    handler: CentrifugeScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<CentrifugeScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = LAYOUT.PANEL_WIDTH
        backgroundHeight = LAYOUT.PANEL_HEIGHT
        titleY = LAYOUT.TITLE_Y
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, LAYOUT.PANEL_WIDTH, LAYOUT.PANEL_HEIGHT)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y,
            LAYOUT.PLAYER_INV_Y,
            LAYOUT.HOTBAR_Y,
            LAYOUT.SLOT_SIZE
        )

        val borderColor = GuiBackground.BORDER_COLOR
        val slotSize = LAYOUT.SLOT_SIZE
        val borderOffset = 1

        val inputSlot = handler.slots[CentrifugeScreenHandler.SLOT_INPUT_INDEX]
        val dischargingSlot = handler.slots[CentrifugeScreenHandler.SLOT_DISCHARGING_INDEX]
        val output1Slot = handler.slots[CentrifugeScreenHandler.SLOT_OUTPUT_1_INDEX]
        val output2Slot = handler.slots[CentrifugeScreenHandler.SLOT_OUTPUT_2_INDEX]
        val output3Slot = handler.slots[CentrifugeScreenHandler.SLOT_OUTPUT_3_INDEX]

        context.drawBorder(x + inputSlot.x - borderOffset, y + inputSlot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + dischargingSlot.x - borderOffset, y + dischargingSlot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + output1Slot.x - borderOffset, y + output1Slot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + output2Slot.x - borderOffset, y + output2Slot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + output3Slot.x - borderOffset, y + output3Slot.y - borderOffset, slotSize, slotSize, borderColor)

        for (i in CentrifugeScreenHandler.SLOT_UPGRADE_INDEX_START..CentrifugeScreenHandler.SLOT_UPGRADE_INDEX_END) {
            val slot = handler.slots[i]
            context.drawBorder(x + slot.x - borderOffset, y + slot.y - borderOffset, slotSize, slotSize, borderColor)
        }

        // 加工进度条：输入槽(56,35,18×18) 与 输出槽(116,17) 之间
        // barX = 56+18+2=76, barW = 116-76-4=36, barY = 35+(18-8)/2=40
        val progress = handler.sync.progress.coerceIn(0, CentrifugeSync.PROGRESS_MAX)
        val progressFrac = if (CentrifugeSync.PROGRESS_MAX > 0) (progress.toFloat() / CentrifugeSync.PROGRESS_MAX).coerceIn(0f, 1f) else 0f
        ProgressBar.draw(
            context,
            x + LAYOUT.PROGRESS_BAR_X,
            y + LAYOUT.PROGRESS_BAR_Y,
            LAYOUT.PROGRESS_BAR_W,
            LAYOUT.PROGRESS_BAR_H,
            progressFrac,
            gradient = true,
            startColor = 0xFFCCAA00.toInt(),
            endColor = 0xFFCC6600.toInt()
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        val left = x
        val top = y
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val heat = handler.sync.heat.toLong().coerceAtLeast(0)
        val heatFrac = if (CentrifugeSync.HEAT_MAX > 0) (heat.toFloat() / CentrifugeSync.HEAT_MAX).coerceIn(0f, 1f) else 0f
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val consumeRate = handler.sync.getSyncedConsumedAmount()
        val isProcessing = handler.sync.progress > 0

        ui.render(context, textRenderer, mouseX, mouseY) {
            // 信息区：槽位底(71) + 4px 间隙 = 75，玩家背包顶(108) - 信息区高(28) = 80，取 75 留 5px 间隙
            // 每行：标签(20) + 间隙(4) + 条(50) + 间隙(4) + 数值；行高 8，行间距 2
            Column(x = left + LAYOUT.INFO_X, y = top + LAYOUT.INFO_Y, spacing = LAYOUT.INFO_ROW_SPACING) {
                Flex(
                    direction = FlexDirection.ROW,
                    alignItems = AlignItems.CENTER,
                    gap = LAYOUT.INFO_GAP,
                    modifier = Modifier.EMPTY.width(LAYOUT.INFO_WIDTH)
                ) {
                    Text("能量", color = 0xAAAAAA)
                    EnergyBar(
                        energyFraction,
                        barWidth = 0,
                        barHeight = LAYOUT.BAR_HEIGHT,
                        modifier = Modifier.EMPTY.width(LAYOUT.BAR_WIDTH)
                    )
                    Text("$energy/$cap EU", color = 0xCCCCCC, shadow = false)
                }
                Flex(
                    direction = FlexDirection.ROW,
                    alignItems = AlignItems.CENTER,
                    gap = LAYOUT.INFO_GAP,
                    modifier = Modifier.EMPTY.width(LAYOUT.INFO_WIDTH)
                ) {
                    Text("热量", color = 0xAAAAAA)
                    HeatProgressBar(
                        heatFrac,
                        barWidth = LAYOUT.BAR_WIDTH,
                        barHeight = LAYOUT.BAR_HEIGHT,
                        startColor = 0xFF660000.toInt(),
                        endColor = 0xFFCC0000.toInt(),
                        gradient = true,
                        modifier = Modifier.EMPTY
                    )
                    Text("$heat/${CentrifugeSync.HEAT_MAX}", color = 0xCCCCCC, shadow = false)
                }
                Text(
                    "${if (isProcessing) "加工" else "预热"} | 入${formatEu(inputRate)} 耗${formatEu(consumeRate)} EU/t",
                    color = if (isProcessing) 0x00CC00 else 0xCC0000,
                    shadow = false
                )
            }
        }
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    companion object {
        /** 布局常量：按像素精确计算 */
        private object LAYOUT {
            const val PANEL_WIDTH = UpgradeSlotLayout.VANILLA_UI_WIDTH + UpgradeSlotLayout.SLOT_SPACING  // 176+18=194
            const val PANEL_HEIGHT = 184
            const val TITLE_Y = 4
            const val SLOT_SIZE = 18

            // 槽位 (与 CentrifugeScreenHandler 一致)
            const val INPUT_X = 56
            const val INPUT_Y = 35
            const val OUTPUT_X = 116
            const val OUTPUT_Y_1 = 17
            const val MACHINE_BOTTOM = 71   // output3 底: 53+18
            const val PLAYER_INV_Y = 108
            const val HOTBAR_Y = 166

            // 进度条：输入(56,35,18×18) 与 输出(116,17) 之间
            const val PROGRESS_BAR_X = INPUT_X + SLOT_SIZE + 2   // 76
            const val PROGRESS_BAR_Y = INPUT_Y + (SLOT_SIZE - 8) / 2  // 40
            const val PROGRESS_BAR_W = OUTPUT_X - PROGRESS_BAR_X - 4  // 36
            const val PROGRESS_BAR_H = 8

            // 信息区：槽位下方 4px 起，至玩家背包上方留 5px
            const val INFO_X = 8
            const val INFO_Y = MACHINE_BOTTOM + 4   // 75
            const val INFO_ROW_SPACING = 2
            const val INFO_GAP = 4
            const val INFO_WIDTH = 160   // 内容区宽 176-8*2
            const val BAR_WIDTH = 50
            const val BAR_HEIGHT = 8
        }
    }

    private fun formatEu(value: Long): String {
        return when {
            value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
            else -> value.toString()
        }
    }
}
