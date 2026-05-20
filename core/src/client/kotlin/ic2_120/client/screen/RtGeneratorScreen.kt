package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.t
import ic2_120.content.block.RtGeneratorBlock
import ic2_120.content.screen.RtGeneratorScreenHandler
import ic2_120.content.sync.RtGeneratorSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = RtGeneratorBlock::class)
class RtGeneratorScreen(
    handler: RtGeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<RtGeneratorScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 176
        backgroundHeight = 161
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, 176, 161, 256, 256)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = RtGeneratorSync.ENERGY_CAPACITY
        val energyFrac = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f

        val inputRate = handler.sync.getSyncedInsertedAmount()
        val outputRate = handler.sync.getSyncedExtractedAmount()

        // 能量条渲染区域 (113,34)-(144,50)，纹理 (179,3)-(205,19)，由左至右平铺
        drawEnergyGauge(context, x + 113, y + 34, energyFrac)

        // 标题文字居中于 y=6
        context.drawText(textRenderer, title, x + (176 - textRenderer.getWidth(title)) / 2, y + 6, 0x404040, false)

        // EU发电/输出文字显示在左侧
        val generateText = t("gui.ic2_120.generate_eu", EnergyFormatUtils.formatEu(inputRate))
        val outputText = t("gui.ic2_120.output_eu", EnergyFormatUtils.formatEu(outputRate))
        val sideTextWidth = maxOf(textRenderer.getWidth(generateText), textRenderer.getWidth(outputText))
        val sideTextX = x - sideTextWidth - 4
        context.drawText(textRenderer, generateText, sideTextX, y + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, outputText, sideTextX, y + 20, 0xAAAAAA, false)

        // 能量条悬停 (113,34)-(139,50) = 26×16
        if (mouseX in x + 113 until x + 139 && mouseY in y + 34 until y + 50) {
            context.drawTooltip(
                textRenderer,
                Text.literal("储能：${EnergyFormatUtils.formatEu(energy)} / ${EnergyFormatUtils.formatEu(cap)} EU"),
                mouseX, mouseY
            )
        }

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun drawEnergyGauge(context: DrawContext, gx: Int, gy: Int, fraction: Float) {
        val barW = 26
        val barH = 16
        val fillW = (fraction.coerceIn(0f, 1f) * barW).toInt()
        if (fillW <= 0) return
        context.enableScissor(gx, gy, gx + fillW, gy + barH)
        context.drawTexture(TEXTURE, gx, gy, 179f, 3f, barW, barH, 256, 256)
        context.disableScissor()
    }

    companion object {
        private val TEXTURE = Identifier.of("ic2", "textures/gui/guinucleargeneration.png")
    }
}
