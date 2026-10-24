package ic2_120.client.screen

import ic2_120.content.block.RtHeatGeneratorBlock
import ic2_120.content.screen.RtHeatGeneratorScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Container
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

@ModScreen(block = RtHeatGeneratorBlock::class)
class RtHeatGeneratorScreen(
    handler: RtHeatGeneratorScreenHandler,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<RtHeatGeneratorScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 176
        backgroundHeight = 161
    }

    override fun drawBackground(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, 176, 161, 256, 256)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        // 标题居中于 y=6
        context.drawText(textRenderer, title, x + (176 - textRenderer.getWidth(title)) / 2, y + 6, 0x404040, false)

        val generatedRate = handler.sync.getSyncedGeneratedHeat()

        // 产热速率文字：区域 (48,59)-(128,73)，居中常显，缩放至 7px
        val heatText = "输出：${generatedRate}/64 HU/t"
        val scale = 7f / textRenderer.fontHeight
        val scaledWidth = (textRenderer.getWidth(heatText) * scale).toInt()
        val scaledHeight = (textRenderer.fontHeight * scale).toInt()
        val textX = x + 48 + (80 - scaledWidth) / 2
        val textY = y + 59 + (14 - scaledHeight) / 2
        context.matrices.push()
        context.matrices.translate(textX.toDouble(), textY.toDouble(), 0.0)
        context.matrices.scale(scale, scale, 1.0f)
        context.drawText(textRenderer, heatText, 0, 0, 0xFFADD8E6.toInt(), false)
        context.matrices.pop()

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    companion object {
        private val TEXTURE = ResourceLocation.of("ic2", "textures/gui/guinuclearheat.png")
    }
}
