package ic2_120.client.screen

import ic2_120.content.block.RtHeatGeneratorBlock
import ic2_120.content.screen.RtHeatGeneratorScreenHandler
import ic2_120.registry.annotation.ModScreen
import ic2_120.client.compat.*
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

@ModScreen(block = RtHeatGeneratorBlock::class)
class RtHeatGeneratorScreen(
    val handler: RtHeatGeneratorScreenHandler,
    playerInventory: Inventory,
    title: Component
) : LegacyContainerScreen26<RtHeatGeneratorScreenHandler>(handler, playerInventory, title, 176, 161) {

    init {
    }

    override fun renderBg(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(TEXTURE, leftPos, topPos, 0f, 0f, 176, 161, 256, 256)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        // 标题居中于 y=6
        context.drawString(font, title, leftPos + (176 - font.width(title)) / 2, topPos + 6, 0x404040, false)

        val generatedRate = menu.sync.getSyncedGeneratedHeat()

        // 产热速率文字：区域 (48,59)-(128,73)，居中常显，缩放至 7px
        val heatText = "输出：${generatedRate}/64 HU/t"
        val scale = 7f / font.lineHeight
        val scaledWidth = (font.width(heatText) * scale).toInt()
        val scaledHeight = (font.lineHeight * scale).toInt()
        val textX = leftPos + 48 + (80 - scaledWidth) / 2
        val textY = topPos + 59 + (14 - scaledHeight) / 2
        context.pose().pushPose()
        context.pose().translate(textX.toDouble(), textY.toDouble(), 0.0)
        context.pose().scale(scale, scale, 1.0f)
        context.drawString(font, heatText, 0, 0, 0xFFADD8E6.toInt(), false)
        context.pose().popPose()

        renderTooltip(context, mouseX, mouseY)
    }

    companion object {
        private val TEXTURE = Identifier.fromNamespaceAndPath("ic2", "textures/gui/guinuclearheat.png")
    }
}
