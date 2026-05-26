package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.FluidUtils
import ic2_120.client.t
import ic2_120.content.block.GeoGeneratorBlock
import ic2_120.content.screen.GeoGeneratorScreenHandler
import ic2_120.content.sync.GeoGeneratorSync
import ic2_120.registry.annotation.ModScreen
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.texture.Sprite
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

    private val lavaSprite: Sprite? by lazy {
        val handler = FluidRenderHandlerRegistry.INSTANCE.get(Fluids.LAVA) ?: return@lazy null
        handler.getFluidSprites(null, null, Fluids.LAVA.defaultState)?.getOrNull(0)
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, 176, 161, 256, 256)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
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

        // 岩浆储量条：区域 (82,23)-(94,69) = 12×46，流体纹理+颜色渲染
        drawFluidTank(context, x + LAVA_BAR_X, y + LAVA_BAR_Y, LAVA_BAR_W, LAVA_BAR_H, lavaFrac, lavaSprite, Fluids.LAVA)

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

        // 悬停提示
        val relX = mouseX - x
        val relY = mouseY - y
        if (relX in ENERGY_BAR_X until ENERGY_BAR_X + ENERGY_BAR_W &&
            relY in ENERGY_BAR_Y until ENERGY_BAR_Y + ENERGY_BAR_H
        ) {
            context.drawTooltip(textRenderer,
                listOf(Text.literal("储能：${EnergyFormatUtils.formatRaw(energy)} / ${EnergyFormatUtils.formatRaw(cap)} EU")),
                mouseX, mouseY)
        }
        if (relX in LAVA_BAR_X until LAVA_BAR_X + LAVA_BAR_W &&
            relY in LAVA_BAR_Y until LAVA_BAR_Y + LAVA_BAR_H
        ) {
            val lavaLines = if (lavaMb > 0) {
                listOf(Text.translatable("gui.ic2_120.geo_generator.lava_tooltip", "%,d".format(lavaMb), "%,d".format(lavaCapMb)))
            } else {
                listOf(Text.translatable("ic2.generic.text.empty"))
            }
            context.drawTooltip(textRenderer, lavaLines, mouseX, mouseY)
        }
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

    /** 自下而上平铺流体纹理 + 流体颜色着色 */
    private fun drawFluidTank(context: DrawContext, gx: Int, gy: Int, w: Int, h: Int, fraction: Float, sprite: Sprite?, fluid: net.minecraft.fluid.Fluid) {
        val fillH = (fraction.coerceIn(0f, 1f) * h).toInt()
        if (fillH <= 0) return
        val fillY = gy + h - fillH
        val color = FluidUtils.getFluidColor(fluid)
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        context.enableScissor(gx, fillY, gx + w, gy + h)
        if (sprite != null) {
            for (sy in fillY until (gy + h) step 16) {
                val tileH = minOf(16, gy + h - sy)
                for (sx in gx until (gx + w) step 16) {
                    val tileW = minOf(16, gx + w - sx)
                    context.drawSprite(sx, sy, 0, tileW, tileH, sprite, r, g, b, 1f)
                }
            }
        }
        context.disableScissor()
    }

    companion object {
        private val TEXTURE = Identifier("ic2", "textures/gui/guiheatsourcegenerator.png")
        private const val ENERGY_BAR_X = 118
        private const val ENERGY_BAR_Y = 19
        private const val ENERGY_BAR_W = 30
        private const val ENERGY_BAR_H = 17
        private const val LAVA_BAR_X = 82
        private const val LAVA_BAR_Y = 23
        private const val LAVA_BAR_W = 12
        private const val LAVA_BAR_H = 46
    }
}
