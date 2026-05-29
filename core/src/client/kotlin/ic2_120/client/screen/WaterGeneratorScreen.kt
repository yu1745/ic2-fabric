package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.FluidUtils
import ic2_120.client.t
import ic2_120.content.block.WaterGeneratorBlock
import ic2_120.content.screen.GuiSize
import ic2_120.content.screen.WaterGeneratorScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.fluid.Fluids
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = WaterGeneratorBlock::class)
class WaterGeneratorScreen(
    handler: WaterGeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<WaterGeneratorScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = GUI_SIZE.width
        backgroundHeight = GUI_SIZE.height
    }

    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // no-op: panel drawn in render() directly, prevents dark overlay on top of GUI
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, BG_W, BG_H, TEX_SIZE, TEX_SIZE)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        val left = x
        val top = y

        context.drawText(textRenderer, title, left + (backgroundWidth - textRenderer.getWidth(title)) / 2, top + 6, 0x404040, false)

        val waterDroplets = handler.sync.waterAmount.coerceAtLeast(0)

        // 流体纹理 (82,22)-(94,69) = 12×47，自下而上
        if (waterDroplets > 0) run {
            val fluid = Fluids.WATER
            val fluidHandler = FluidRenderHandlerRegistry.INSTANCE.get(fluid) ?: return@run
            val sprite = fluidHandler.getFluidSprites(null, null, fluid.defaultState).getOrNull(0) ?: return@run
            val color = FluidUtils.getFluidColor(fluid)
            if (color == -1) return@run
            val r = ((color shr 16) and 0xFF) / 255f
            val g = ((color shr 8) and 0xFF) / 255f
            val b = (color and 0xFF) / 255f
            val capDroplets = 8 * FluidConstants.BUCKET
            val frac = (waterDroplets.toFloat() / capDroplets).coerceIn(0f, 1f)
            val fillH = (TANK_H * frac).toInt().coerceAtLeast(1)
            val sx = left + TANK_X
            val sy = top + TANK_Y
            val fillY = sy + TANK_H - fillH
            context.enableScissor(sx, fillY, sx + TANK_W, sy + TANK_H)
            for (cy in fillY until (sy + TANK_H) step 16) {
                val tileH = minOf(16, sy + TANK_H - cy)
                for (cx in sx until (sx + TANK_W) step 16) {
                    val tileW = minOf(16, sx + TANK_W - cx)
                    context.drawSprite(cx, cy, 0, tileW, tileH, sprite, r, g, b, 1f)
                }
            }
            context.disableScissor()

            // 容量标示 (178,21)-(190,69) = 12×48，有流体时渲染在流体之上
            context.drawTexture(TEXTURE, left + TANK_OVERLAY_X, top + TANK_OVERLAY_Y,
                TANK_OVERLAY_U.toFloat(), TANK_OVERLAY_V.toFloat(),
                TANK_OVERLAY_W, TANK_OVERLAY_H, TEX_SIZE, TEX_SIZE)
        }

        // 侧边文字
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val outputRate = handler.sync.getSyncedExtractedAmount()
        val inputText = t("gui.ic2_120.generate_eu", EnergyFormatUtils.formatEu(inputRate))
        val outputText = t("gui.ic2_120.output_eu", EnergyFormatUtils.formatEu(outputRate))
        val sideTextWidth = maxOf(textRenderer.getWidth(inputText), textRenderer.getWidth(outputText))
        val sideTextX = left - sideTextWidth - 4
        context.drawText(textRenderer, inputText, sideTextX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, outputText, sideTextX, top + 20, 0xAAAAAA, false)

        drawMouseoverTooltip(context, mouseX, mouseY)

        // 流体槽悬停提示
        val relX = mouseX - left
        val relY = mouseY - top
        if (relX in TANK_X until TANK_X + TANK_W && relY in TANK_Y until TANK_Y + TANK_H) {
            val lines = if (waterDroplets > 0) {
                val mb = waterDroplets / DROPLETS_PER_MB
                val capMb = (8 * FluidConstants.BUCKET / DROPLETS_PER_MB).toLong()
                listOf(Text.literal("水"), Text.literal("${"%,d".format(mb)} / ${"%,d".format(capMb)} mB"))
            } else {
                listOf(Text.literal("空"))
            }
            context.drawTooltip(textRenderer, lines, mouseX, mouseY)
        }
    }

    companion object {
        private val TEXTURE = Identifier.of("ic2", "textures/gui/guiwatergenerator.png")
        private const val TEX_SIZE = 256
        private val GUI_SIZE = GuiSize.STANDARD_UPGRADE
        private val DROPLETS_PER_MB = (FluidConstants.BUCKET / 1000).toInt()

        private const val BG_W = 176
        private const val BG_H = 161

        // 流体槽 (82,22)-(94,69) = 12×47
        private const val TANK_X = 82
        private const val TANK_Y = 22
        private const val TANK_W = 12
        private const val TANK_H = 47

        // 容量标示 (178,21)-(190,69) = 12×48
        private const val TANK_OVERLAY_U = 178
        private const val TANK_OVERLAY_V = 21
        private const val TANK_OVERLAY_W = 12
        private const val TANK_OVERLAY_H = 48
        private const val TANK_OVERLAY_X = 82
        private const val TANK_OVERLAY_Y = 23
    }
}
