package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.ui.GuiBackground
import ic2_120.client.ui.ProgressBar
import ic2_120.content.block.storage.EnergyStorageConfig
import ic2_120.content.screen.EnergyStorageScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.registry.Registries
import net.minecraft.text.Text

/**
 * 储电盒 GUI。四个等级（BatBox/CESU/MFE/MFSU）共用。
 */
@ModScreen(handlers = ["batbox", "cesu", "mfe", "mfsu"])
class EnergyStorageScreen(
    handler: EnergyStorageScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<EnergyStorageScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()
    private val capacity: Long = resolveCapacity()
    private val useEquipmentSlots: Boolean = resolveUseEquipmentSlots()

    private fun resolveCapacity(): Long {
        return handler.context.get({ world, pos ->
            val block = world.getBlockState(pos).block
            val id = Registries.BLOCK.getId(block)
            EnergyStorageConfig.fromBlockPath(id.path)?.capacity ?: EnergyStorageConfig.BATBOX.capacity
        }, EnergyStorageConfig.BATBOX.capacity)
    }

    private fun resolveUseEquipmentSlots(): Boolean {
        return handler.context.get({ world, pos ->
            val block = world.getBlockState(pos).block
            val id = Registries.BLOCK.getId(block)
            EnergyStorageConfig.fromBlockPath(id.path)?.useEquipmentSlots ?: false
        }, false)
    }

    init {
        backgroundWidth = PANEL_WIDTH
        backgroundHeight = PANEL_HEIGHT
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        val x = (width - backgroundWidth) / 2
        val y = (height - backgroundHeight) / 2
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(context, x, y, 84, 142, 18)

        val borderColor = GuiBackground.BORDER_COLOR
        val slotSize = 18
        val borderOffset = 1
        val slotY = 55
        val slotSpacing = 18

        val equipLabel = Text.translatable("ic2_120.gui.equipment_slots")
        val chargeLabel = Text.translatable("ic2_120.gui.charge_slots")

        // 装备槽标签（左侧，4 格上方）
        context.drawText(textRenderer, equipLabel, x + 8, y + 37, 0xAAAAAA, false)
        // 充电槽标签（右侧，1 格上方）
        context.drawText(textRenderer, chargeLabel, x + 8 + slotSpacing * 4, y + 37, 0xAAAAAA, false)

        // 装备槽边框（左侧 4 格）
        for (i in 0 until 4) {
            context.drawBorder(x + 8 + i * slotSpacing - borderOffset, y + slotY - borderOffset, slotSize, slotSize, borderColor)
        }
        // 充电槽边框（右侧 1 格）
        context.drawBorder(x + 8 + 4 * slotSpacing - borderOffset, y + slotY - borderOffset, slotSize, slotSize, borderColor)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)

        val left = (width - backgroundWidth) / 2
        val top = (height - backgroundHeight) / 2

        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val outputRate = handler.sync.getSyncedExtractedAmount()
        val cap = capacity
        val fraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f

        val inputText = "输入 ${formatEu(inputRate)} EU/t"
        val outputText = "输出 ${formatEu(outputRate)} EU/t"
        val textX = left - maxOf(inputText.length * 6, outputText.length * 6) - 4
        context.drawText(textRenderer, inputText, textX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, outputText, textX, top + 20, 0xAAAAAA, false)

        val barW = CONTENT_WIDTH - LABEL_WIDTH
        val barX = left + 8 + LABEL_WIDTH
        val barY = top + 18
        ProgressBar.draw(context, barX, barY, barW, 9, fraction, gradient = true)

        ui.render(context, textRenderer, mouseX, mouseY) {
            Column(x = left + 8, y = top + 8, spacing = 6) {
                Text(title.string, color = 0xFFFFFF)
                Text(
                    "${formatEu(energy)} / ${formatEu(cap)} EU",
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
        private const val CONTENT_WIDTH = PANEL_WIDTH - 16
        private const val LABEL_WIDTH = 36
    }
}
