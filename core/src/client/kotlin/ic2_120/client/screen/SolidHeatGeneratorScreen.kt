package ic2_120.client.screen

import ic2_120.content.block.SolidHeatGeneratorBlock
import ic2_120.content.screen.SolidHeatGeneratorScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = SolidHeatGeneratorBlock::class)
class SolidHeatGeneratorScreen(
    handler: SolidHeatGeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<SolidHeatGeneratorScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 176
        backgroundHeight = 166
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, 176, 166, 256, 256)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        val total = handler.sync.burnTotal.coerceAtLeast(1)
        val current = handler.sync.burnTime.coerceIn(0, total)
        val burnFrac = (current.toFloat() / total).coerceIn(0f, 1f)
        val outputRate = handler.sync.getSyncedOutputHeat().coerceAtLeast(0L)

        // 标题居中于 y=6
        context.drawText(textRenderer, title, x + (176 - textRenderer.getWidth(title)) / 2, y + 6, 0x404040, false)

        // 燃烧进度条：纹理 (179,2)-(192,15) = 13×13，渲染区域 (82,29)-(94,41) = 12×12，自底向上填充
        drawBurnBar(context, x + 82, y + 29, burnFrac)

        // 输出文本：区域 (48,65)-(128,79)，居中常显，7px
        val infoText = "输出：${outputRate} HU/t"
        val scale = 7f / textRenderer.fontHeight
        val scaledWidth = (textRenderer.getWidth(infoText) * scale).toInt()
        val scaledHeight = (textRenderer.fontHeight * scale).toInt()
        val textX = x + 48 + (80 - scaledWidth) / 2
        val textY = y + 65 + (14 - scaledHeight) / 2
        context.matrices.push()
        context.matrices.translate(textX.toDouble(), textY.toDouble(), 0.0)
        context.matrices.scale(scale, scale, 1.0f)
        context.drawText(textRenderer, infoText, 0, 0, 0xFFADD8E6.toInt(), false)
        context.matrices.pop()

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    /**
     * 燃烧进度条自底向上填充，纹理区域 (179,2)-(192,15) = 13×13。
     */
    private fun drawBurnBar(context: DrawContext, gx: Int, gy: Int, fraction: Float) {
        val barW = 12
        val barH = 12
        val fillH = (fraction.coerceIn(0f, 1f) * barH).toInt()
        if (fillH <= 0) return
        context.enableScissor(gx, gy + barH - fillH, gx + barW, gy + barH)
        context.drawTexture(TEXTURE, gx, gy, 179f, 2f, barW, barH, 256, 256)
        context.disableScissor()
    }

    companion object {
        private val TEXTURE = Identifier("ic2", "textures/gui/guisolidheatengine.png")
    }
}
