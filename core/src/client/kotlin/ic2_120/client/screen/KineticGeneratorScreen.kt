package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.content.block.KineticGeneratorBlock
import ic2_120.content.screen.KineticGeneratorScreenHandler
import ic2_120.content.sync.KineticGeneratorSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = KineticGeneratorBlock::class)
class KineticGeneratorScreen(
    handler: KineticGeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<KineticGeneratorScreenHandler>(handler, playerInventory, title) {

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

        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = KineticGeneratorSync.ENERGY_CAPACITY
        val energyFrac = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val kuIn = handler.sync.currentKu.coerceAtLeast(0)
        val euOut = handler.sync.outputEu.coerceAtLeast(0)

        // 能量条：渲染区域 (60,23)-(117,37) = 57×14，纹理区域 (179,3)-(236,17) = 57×14，宽度一致
        drawEnergyGauge(context, x + 60, y + 23, energyFrac)

        // 标题居中于 y=6
        context.drawText(textRenderer, title, x + (176 - textRenderer.getWidth(title)) / 2, y + 6, 0x404040, false)

        // 输入/输出文本：区域 (41,49)-(138,63)，居中常显，缩放至 7px
        val infoText = "输入：${kuIn} KU/t  输出：${EnergyFormatUtils.formatRaw(euOut.toLong())} EU/t"
        val scale = 6f / textRenderer.fontHeight
        val scaledWidth = (textRenderer.getWidth(infoText) * scale).toInt()
        val scaledHeight = (textRenderer.fontHeight * scale).toInt()
        val textX = x + 41 + (97 - scaledWidth) / 2
        val textY = y + 49 + (14 - scaledHeight) / 2
        context.matrices.push()
        context.matrices.translate(textX.toDouble(), textY.toDouble(), 0.0)
        context.matrices.scale(scale, scale, 1.0f)
        context.drawText(textRenderer, infoText, 0, 0, 0xFFADD8E6.toInt(), false)
        context.matrices.pop()

        // 能量条悬停 (60,23)-(117,37) = 57×14
        if (mouseX in x + 60 until x + 117 && mouseY in y + 23 until y + 37) {
            context.drawTooltip(
                textRenderer,
                Text.literal("储能：${EnergyFormatUtils.formatRaw(energy)} / ${EnergyFormatUtils.formatRaw(cap)} EU"),
                mouseX, mouseY
            )
        }

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    /**
     * 平铺渲染能量条：纹理区域 (179,3)-(236,17) 从左侧根据 [fraction] 比例填充。
     */
    private fun drawEnergyGauge(context: DrawContext, gx: Int, gy: Int, fraction: Float) {
        val barW = 57
        val barH = 14
        val fillW = (fraction.coerceIn(0f, 1f) * barW).toInt()
        if (fillW <= 0) return
        context.enableScissor(gx, gy, gx + fillW, gy + barH)
        context.drawTexture(TEXTURE, gx, gy, 179f, 3f, barW, barH, 256, 256)
        context.disableScissor()
    }

    companion object {
        private val TEXTURE = Identifier.of("ic2", "textures/gui/guielectrickineticgenerator.png")
    }
}
