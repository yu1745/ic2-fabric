package ic2_120.client.screen

import ic2_120.content.block.ElectricHeatGeneratorBlock
import ic2_120.content.screen.ElectricHeatGeneratorScreenHandler
import ic2_120.content.sync.ElectricHeatGeneratorSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = ElectricHeatGeneratorBlock::class)
class ElectricHeatGeneratorScreen(
    handler: ElectricHeatGeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<ElectricHeatGeneratorScreenHandler>(handler, playerInventory, title) {

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
        val cap = ElectricHeatGeneratorSync.ENERGY_CAPACITY
        val energyFrac = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val outputRate = handler.sync.getSyncedOutputHeat().coerceAtLeast(0L)

        // 标题居中于 y=6
        context.drawText(textRenderer, title, x + (176 - textRenderer.getWidth(title)) / 2, y + 6, 0x404040, false)

        // 能量条：纹理区域 (179,3)-(192,16) = 13×13，渲染到 (10,44)，自底向上填充
        val gaugeX = x + 10
        val gaugeY = y + 44
        val gaugeSize = 13
        drawEnergyGauge(context, gaugeX, gaugeY, energyFrac)

        // 鼠标悬停能量条时显示储能
        if (mouseX in gaugeX until (gaugeX + gaugeSize) && mouseY in gaugeY until (gaugeY + gaugeSize)) {
            context.drawTooltip(textRenderer, Text.literal("储能：${energy} / ${cap} EU"), mouseX, mouseY)
        }

        // 输出文本：区域 (29,65)-(149,79)，居中常显，缩放至 7px
        val infoText = "输出 ${outputRate}/100 HU/t"
        val scale = 7f / textRenderer.fontHeight
        val scaledWidth = (textRenderer.getWidth(infoText) * scale).toInt()
        val scaledHeight = (textRenderer.fontHeight * scale).toInt()
        val textX = x + 29 + (120 - scaledWidth) / 2
        val textY = y + 65 + (14 - scaledHeight) / 2
        context.matrices.push()
        context.matrices.translate(textX.toDouble(), textY.toDouble(), 0.0)
        context.matrices.scale(scale, scale, 1.0f)
        context.drawText(textRenderer, infoText, 0, 0, 0xFFADD8E6.toInt(), false)
        context.matrices.pop()

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    /**
     * 能量条自底向上填充，纹理区域 (179,3)-(192,16) = 13×13。
     */
    private fun drawEnergyGauge(context: DrawContext, gx: Int, gy: Int, fraction: Float) {
        val barW = 13
        val barH = 13
        val fillH = (fraction.coerceIn(0f, 1f) * barH).toInt()
        if (fillH <= 0) return
        context.enableScissor(gx, gy + barH - fillH, gx + barW, gy + barH)
        context.drawTexture(TEXTURE, gx, gy, 179f, 3f, barW, barH, 256, 256)
        context.disableScissor()
    }

    companion object {
        private val TEXTURE = Identifier.of("ic2", "textures/gui/guiheatingmachine.png")
    }
}
