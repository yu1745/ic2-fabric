package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.t
import ic2_120.content.block.GeneratorBlock
import ic2_120.content.screen.GeneratorScreenHandler
import ic2_120.content.sync.GeneratorSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = GeneratorBlock::class)
class GeneratorScreen(
    handler: GeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<GeneratorScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 176
        backgroundHeight = 161
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, 176, 161, 256, 256)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val energyFrac = if (GeneratorSync.ENERGY_CAPACITY > 0) {
            (energy.toFloat() / GeneratorSync.ENERGY_CAPACITY).coerceIn(0f, 1f)
        } else 0f
        val totalBurn = handler.sync.totalBurnTime.coerceAtLeast(1)
        val burnTime = handler.sync.burnTime.coerceIn(0, totalBurn)
        val burnFrac = (burnTime.toFloat() / totalBurn).coerceIn(0f, 1f)
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val outputRate = handler.sync.getSyncedExtractedAmount()

        // 能量条位于 (78, 35) — 26×17 水平纹理来自 guipowergenerator.png (179,3)-(204,19)
        drawEnergyGauge(context, x + 78, y + 35, energyFrac)

        // 燃料条位于 (58, 38) — 13×13 垂直纹理来自 guipowergenerator.png (179,22)-(191,34)
        drawFuelGauge(context, x + 57, y + 38, burnFrac)

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
        val barW = 26
        val barH = 17
        val fillW = (fraction.coerceIn(0f, 1f) * barW).toInt()
        if (fillW <= 0) return
        context.enableScissor(gx, gy, gx + fillW, gy + barH)
        context.drawTexture(TEXTURE, gx, gy, 179f, 3f, barW, barH, 256, 256)
        context.disableScissor()
    }

    private fun drawFuelGauge(context: DrawContext, gx: Int, gy: Int, fraction: Float) {
        val barW = 13
        val barH = 13
        val fillH = (fraction.coerceIn(0f, 1f) * barH).toInt()
        if (fillH <= 0) return
        context.enableScissor(gx, gy + barH - fillH, gx + barW, gy + barH)
        context.drawTexture(TEXTURE, gx, gy, 178f, 22f, barW, barH, 256, 256)
        context.disableScissor()
    }

    companion object {
        private val TEXTURE = Identifier("ic2", "textures/gui/guipowergenerator.png")
    }
}
