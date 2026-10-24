package ic2_120.client.screen

import ic2_120.content.block.SolidHeatGeneratorBlock
import ic2_120.content.screen.SolidHeatGeneratorScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

@ModScreen(block = SolidHeatGeneratorBlock::class)
class SolidHeatGeneratorScreen(
    val handler: SolidHeatGeneratorScreenHandler,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<SolidHeatGeneratorScreenHandler>(handler, playerInventory, title) {

    init {
        imageWidth = 176
        imageHeight = 166
    }

    override fun renderBg(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(TEXTURE, leftPos, topPos, 0f, 0f, 176, 166, 256, 256)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        val total = menu.sync.burnTotal.coerceAtLeast(1)
        val current = menu.sync.burnTime.coerceIn(0, total)
        val burnFrac = (current.toFloat() / total).coerceIn(0f, 1f)
        val outputRate = menu.sync.getSyncedOutputHeat().coerceAtLeast(0L)

        // 标题居中于 y=6
        context.drawString(font, title, leftPos + (176 - font.width(title)) / 2, topPos + 6, 0x404040, false)

        // 燃烧进度条：纹理 (179,2)-(192,15) = 13×13，渲染区域 (82,29)-(94,41) = 12×12，自底向上填充
        drawBurnBar(context, leftPos + 82, topPos + 29, burnFrac)

        // 输出文本：区域 (48,65)-(128,79)，居中常显，7px
        val infoText = "输出：${outputRate} HU/t"
        val scale = 7f / font.lineHeight
        val scaledWidth = (font.width(infoText) * scale).toInt()
        val scaledHeight = (font.lineHeight * scale).toInt()
        val textX = leftPos + 48 + (80 - scaledWidth) / 2
        val textY = topPos + 65 + (14 - scaledHeight) / 2
        context.pose().pushPose()
        context.pose().translate(textX.toDouble(), textY.toDouble(), 0.0)
        context.pose().scale(scale, scale, 1.0f)
        context.drawString(font, infoText, 0, 0, 0xFFADD8E6.toInt(), false)
        context.pose().popPose()

        renderTooltip(context, mouseX, mouseY)
    }

    /**
     * 燃烧进度条自底向上填充，纹理区域 (179,2)-(192,15) = 13×13。
     */
    private fun drawBurnBar(context: GuiGraphics, gx: Int, gy: Int, fraction: Float) {
        val barW = 12
        val barH = 12
        val fillH = (fraction.coerceIn(0f, 1f) * barH).toInt()
        if (fillH <= 0) return
        context.enableScissor(gx, gy + barH - fillH, gx + barW, gy + barH)
        context.blit(TEXTURE, gx, gy, 179f, 2f, barW, barH, 256, 256)
        context.disableScissor()
    }

    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui/guisolidheatengine.png")
    }
}
