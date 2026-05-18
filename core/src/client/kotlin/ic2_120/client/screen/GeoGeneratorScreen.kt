package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.FluidUtils
import ic2_120.client.t
import ic2_120.content.block.GeoGeneratorBlock
import ic2_120.content.screen.GeoGeneratorScreenHandler
import ic2_120.content.sync.GeoGeneratorSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.fluid.Fluids
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = GeoGeneratorBlock::class)
class GeoGeneratorScreen(
    handler: GeoGeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<GeoGeneratorScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 176
        backgroundHeight = 161
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, 176, 161, 256, 256)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)

        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = GeoGeneratorSync.ENERGY_CAPACITY
        val energyFrac = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f

        val lavaMb = handler.sync.lavaAmountMb.coerceAtLeast(0)
        val lavaCapMb = 8 * 1000
        val lavaFrac = if (lavaCapMb > 0) (lavaMb.toFloat() / lavaCapMb).coerceIn(0f, 1f) else 0f

        val inputRate = handler.sync.getSyncedInsertedAmount()
        val outputRate = handler.sync.getSyncedExtractedAmount()

        // 能量条位于 (118, 19) — 30×17 水平纹理来自 guiheatsourcegenerator.png (178,2)-(207,18)
        drawEnergyGauge(context, x + 118, y + 19, energyFrac)

        // 岩浆储量条位于 (82, 21) — 13×48 垂直
        drawLavaBar(context, x + 82, y + 21, lavaFrac)

        // 标题文字居中于 y=6
        context.drawText(textRenderer, title, x + (176 - textRenderer.getWidth(title)) / 2, y + 6, 0x404040, false)

        // EU发电/输出文字显示在左侧
        val generateText = t("gui.ic2_120.generate_eu", EnergyFormatUtils.formatEu(inputRate))
        val outputText = t("gui.ic2_120.output_eu", EnergyFormatUtils.formatEu(outputRate))
        val sideTextWidth = maxOf(textRenderer.getWidth(generateText), textRenderer.getWidth(outputText))
        val sideTextX = x - sideTextWidth - 4
        context.drawText(textRenderer, generateText, sideTextX, y + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, outputText, sideTextX, y + 20, 0xAAAAAA, false)

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun drawEnergyGauge(context: DrawContext, gx: Int, gy: Int, fraction: Float) {
        val barW = 30
        val barH = 17
        val fillW = (fraction.coerceIn(0f, 1f) * barW).toInt()
        if (fillW <= 0) return
        context.enableScissor(gx, gy, gx + fillW, gy + barH)
        context.drawTexture(TEXTURE, gx, gy, 178f, 2f, barW, barH, 256, 256)
        context.disableScissor()
    }

    private fun drawLavaBar(context: DrawContext, gx: Int, gy: Int, fraction: Float) {
        val barW = 13
        val barH = 48
        // 岩浆填充
        val fillH = (fraction.coerceIn(0f, 1f) * barH).toInt()
        if (fillH > 0) {
            val lavaColor = FluidUtils.getFluidColor(Fluids.LAVA)
            context.fill(gx, gy + barH - fillH, gx + barW, gy + barH, lavaColor)
        }
        // 容器标示纹理覆盖 (179,21)-(189,68)
        context.drawTexture(TEXTURE, gx, gy, 179f, 21f, barW, barH, 256, 256)
    }

    companion object {
        private val TEXTURE = Identifier("ic2", "textures/gui/guiheatsourcegenerator.png")
    }
}
