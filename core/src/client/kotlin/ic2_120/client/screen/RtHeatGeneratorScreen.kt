package ic2_120.client.screen

import ic2_120.content.block.RtHeatGeneratorBlock
import ic2_120.content.screen.RtHeatGeneratorScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = RtHeatGeneratorBlock::class)
class RtHeatGeneratorScreen(
    handler: RtHeatGeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<RtHeatGeneratorScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 176
        backgroundHeight = 161
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, 176, 161, 256, 256)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)

        val generatedRate = handler.sync.getSyncedGeneratedHeat()

        // 产热速率文字，显示区域 (48,59)-(128,73)，居中常显，缩小 1px
        val heatText = "$generatedRate/64 HU/T"
        val scale = 7f / 8f
        val textWidth = textRenderer.getWidth(heatText)
        val textX = x + 48 + (80 - textWidth) / 2
        val textY = y + 59 + (14 - textRenderer.fontHeight) / 2 + 1
        context.matrices.push()
        context.matrices.scale(scale, scale, 1f)
        val sx = (textX / scale).toInt()
        val sy = (textY / scale).toInt()
        context.drawText(textRenderer, heatText, sx, sy, 0xFFADD8E6.toInt(), false)
        context.matrices.pop()

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    companion object {
        private val TEXTURE = Identifier("ic2", "textures/gui/guinuclearheat.png")
    }
}
