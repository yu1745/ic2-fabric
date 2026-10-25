package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.content.block.StirlingGeneratorBlock
import ic2_120.content.block.machines.StirlingGeneratorBlockEntity
import ic2_120.content.screen.StirlingGeneratorScreenHandler
import ic2_120.registry.annotation.ModScreen
import ic2_120.client.compat.*
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

@ModScreen(block = StirlingGeneratorBlock::class)
class StirlingGeneratorScreen(
    val handler: StirlingGeneratorScreenHandler,
    playerInventory: Inventory,
    title: Component
) : LegacyContainerScreen26<StirlingGeneratorScreenHandler>(handler, playerInventory, title, 176, 166) {

    init {
    }

    override fun renderBg(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(TEXTURE, leftPos, topPos, 0f, 0f, 176, 166, 256, 256)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        val generationRate = menu.sync.getSyncedInsertedAmount().coerceAtLeast(0L)
        val huIn = (generationRate * StirlingGeneratorBlockEntity.HU_PER_EU).coerceAtLeast(0L)
        val euOut = menu.sync.getSyncedExtractedAmount().coerceAtLeast(0L)

        // 标题居中于 y=6
        context.drawString(font, title, leftPos + (176 - font.width(title)) / 2, topPos + 6, 0x404040, false)

        // 输入/输出文本：区域 (40,56)-(137,70)，常显，缩放至 6px
        val infoText = "输入：${formatHu(huIn)} HU/t  输出：${EnergyFormatUtils.formatEu(euOut)} EU/t"
        val scale = 6f / font.lineHeight
        val scaledWidth = (font.width(infoText) * scale).toInt()
        val scaledHeight = (font.lineHeight * scale).toInt()
        val textX = leftPos + 40 + (97 - scaledWidth) / 2
        val textY = topPos + 56 + (14 - scaledHeight) / 2
        context.pose().pushPose()
        context.pose().translate(textX.toDouble(), textY.toDouble(), 0.0)
        context.pose().scale(scale, scale, 1.0f)
        context.drawString(font, infoText, 0, 0, 0x55FF55, false)
        context.pose().popPose()

        renderTooltip(context, mouseX, mouseY)
    }

    private fun formatHu(value: Long): String {
        return when {
            value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
            else -> value.toString()
        }
    }

    companion object {
        private val TEXTURE = Identifier.fromNamespaceAndPath("ic2", "textures/gui/guithermalenergygenerator.png")
    }
}
