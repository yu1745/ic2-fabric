package ic2_120.client

import ic2_120.client.compose.*
import ic2_120.client.ui.GuiBackground
import ic2_120.client.ui.EnergyBar
import ic2_120.content.sync.MfsuSync
import ic2_120.content.block.MfsuBlock
import ic2_120.content.block.machines.MfsuBlockEntity
import ic2_120.content.screen.MfsuScreenHandler
import ic2_120.registry.annotation.ModScreen
import ic2_120.registry.type
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

@ModScreen(block = MfsuBlock::class)
class MfsuScreen(
    handler: MfsuScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<MfsuScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = PANEL_WIDTH
        backgroundHeight = PANEL_HEIGHT
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        val x = (width - backgroundWidth) / 2
        val y = (height - backgroundHeight) / 2
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(context, x, y, 84, 142, 18)

        // 绘制4个装备槽的边框（UI下方横向）
        val borderColor = GuiBackground.BORDER_COLOR
        val slotSize = 18
        val borderOffset = 1
        val armorSlotX = 8
        val armorSlotY = 55
        for (i in 0 until 4) {
            context.drawBorder(x + armorSlotX + i * 18 - borderOffset, y + armorSlotY - borderOffset, slotSize, slotSize, borderColor)
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)

        val left = (width - backgroundWidth) / 2
        val top = (height - backgroundHeight) / 2

        // 计算数据
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val outputRate = handler.sync.getSyncedExtractedAmount()
        val cap = MfsuSync.ENERGY_CAPACITY
        val fraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f

        // 在UI左侧绘制速度文本
        val inputText = "输入 ${formatEu(inputRate)} EU/t"
        val outputText = "输出 ${formatEu(outputRate)} EU/t"
        val inputTextWidth = inputText.length * 6
        val outputTextWidth = outputText.length * 6
        val textX = left - maxOf(inputTextWidth, outputTextWidth) - 4  // 留4像素边距
        context.drawText(textRenderer, inputText, textX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, outputText, textX, top + 20, 0xAAAAAA, false)

        ui.render(context, textRenderer, mouseX, mouseY) {
            Column(x = left + 8, y = top + 8, spacing = 6) {
                Text(title.string, color = 0xFFFFFF)
                Flex(
                    direction = FlexDirection.ROW,
                    alignItems = AlignItems.CENTER,
                    gap = 8,
                    modifier = Modifier.EMPTY.width(CONTENT_WIDTH)
                ) {
                    Text("能量", color = 0xAAAAAA)
                    EnergyBar(
                        fraction,
                        barWidth = 0,
                        barHeight = 9,
                        modifier = Modifier.EMPTY.width(CONTENT_WIDTH - LABEL_WIDTH)
                    )
                }
                Text(
                    "${formatEu(energy)} / ${formatEu(MfsuSync.ENERGY_CAPACITY)} EU",
                    color = 0xCCCCCC,
                    shadow = false
                )
            }
        }

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    private fun formatEu(value: Long): String {
        return when {
            value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
            else -> value.toString()
        }
    }

    companion object {
        private const val PANEL_WIDTH = 176
        private const val PANEL_HEIGHT = 166
        /** 内容区宽度（左右各 8 边距） */
        private const val CONTENT_WIDTH = PANEL_WIDTH - 16
        /** “能量”标签预估宽度 */
        private const val LABEL_WIDTH = 36
    }
}
