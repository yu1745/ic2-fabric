package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.content.block.StirlingGeneratorBlock
import ic2_120.content.block.machines.StirlingGeneratorBlockEntity
import ic2_120.content.screen.StirlingGeneratorScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = StirlingGeneratorBlock::class)
class StirlingGeneratorScreen(
    handler: StirlingGeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<StirlingGeneratorScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 176
        backgroundHeight = 166
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, 176, 166, 256, 256)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        val generationRate = handler.sync.getSyncedInsertedAmount().coerceAtLeast(0L)
        val huIn = (generationRate * StirlingGeneratorBlockEntity.HU_PER_EU).coerceAtLeast(0L)
        val euOut = handler.sync.getSyncedExtractedAmount().coerceAtLeast(0L)

        // 标题居中于 y=6
        context.drawText(textRenderer, title, x + (176 - textRenderer.getWidth(title)) / 2, y + 6, 0x404040, false)

        // 输入/输出文本：区域 (40,56)-(137,70)，常显，缩放至 6px
        val infoText = "输入：${formatHu(huIn)} HU/t  输出：${EnergyFormatUtils.formatEu(euOut)} EU/t"
        val scale = 6f / textRenderer.fontHeight
        val scaledWidth = (textRenderer.getWidth(infoText) * scale).toInt()
        val scaledHeight = (textRenderer.fontHeight * scale).toInt()
        val textX = x + 40 + (97 - scaledWidth) / 2
        val textY = y + 56 + (14 - scaledHeight) / 2
        context.matrices.push()
        context.matrices.translate(textX.toDouble(), textY.toDouble(), 0.0)
        context.matrices.scale(scale, scale, 1.0f)
        context.drawText(textRenderer, infoText, 0, 0, 0xFFADD8E6.toInt(), false)
        context.matrices.pop()

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun formatHu(value: Long): String {
        return when {
            value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
            else -> value.toString()
        }
    }

    companion object {
        private val TEXTURE = Identifier.of("ic2", "textures/gui/guithermalenergygenerator.png")
    }
}
