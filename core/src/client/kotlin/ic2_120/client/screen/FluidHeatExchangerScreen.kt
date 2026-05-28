package ic2_120.client.screen

import ic2_120.client.FluidUtils
import ic2_120.client.t
import ic2_120.content.block.FluidHeatExchangerBlock
import ic2_120.content.screen.FluidHeatExchangerScreenHandler
import ic2_120.content.sync.FluidHeatExchangerSync
import ic2_120.registry.annotation.ModScreen
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.fluid.Fluid
import net.minecraft.text.Text as McText
import net.minecraft.util.Identifier

@ModScreen(block = FluidHeatExchangerBlock::class)
class FluidHeatExchangerScreen(
    handler: FluidHeatExchangerScreenHandler, playerInventory: PlayerInventory, title: McText
) : HandledScreen<FluidHeatExchangerScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = 179
        backgroundHeight = 204
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(TEXTURE, x, y, 0f, 0f, backgroundWidth, backgroundHeight, TEXTURE_SIZE, TEXTURE_SIZE)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        val left = x
        val top = y
        val sync = handler.sync

        val inputMb = sync.inputFluidMb.coerceAtLeast(0)
        val outputMb = sync.outputFluidMb.coerceAtLeast(0)
        val inputFrac = if (FluidHeatExchangerSync.TANK_CAPACITY_MB > 0) (inputMb.toFloat() / FluidHeatExchangerSync.TANK_CAPACITY_MB).coerceIn(0f, 1f) else 0f
        val outputFrac = if (FluidHeatExchangerSync.TANK_CAPACITY_MB > 0) (outputMb.toFloat() / FluidHeatExchangerSync.TANK_CAPACITY_MB).coerceIn(0f, 1f) else 0f

        val generatedRate = sync.getSyncedGeneratedHeat()
        val outputRate = sync.getSyncedOutputHeat()

        // 标题居中
        context.drawText(textRenderer, title, left + (backgroundWidth - textRenderer.getWidth(title)) / 2, top + 6, 0x404040, false)

        // 输入流体槽 (19,47)-(31,91) = 12×44
        val inputFluid = sync.fluidTypeToFluid(sync.inputFluidType)
        if (inputFrac > 0f && inputFluid != null) {
            drawFluidFill(context, left + 19, top + 47, 12, 44, inputFrac, inputFluid)
        }

        // 输出流体槽 (145,47)-(157,91) = 12×44
        val outputFluid = sync.fluidTypeToFluid(sync.outputFluidType)
        if (outputFrac > 0f && outputFluid != null) {
            drawFluidFill(context, left + 145, top + 47, 12, 44, outputFrac, outputFluid)
        }

        // 文本区域 (18,29)-(159,40) — 显示产热速率和输出速率，居中，淡蓝色，7px
        val heatText = t("gui.ic2_120.fluid_heat_exchanger.heat_line", generatedRate, outputRate)
        val textScale = 7f / textRenderer.fontHeight
        val scaledW = (textRenderer.getWidth(heatText) * textScale).toInt()
        val textAreaW = 159 - 18
        val textAreaY = top + 29
        val textAreaCY = textAreaY + (40 - 29) / 2
        val scaledTextX = left + 18 + (textAreaW - scaledW) / 2
        context.matrices.push()
        context.matrices.translate(scaledTextX.toDouble(), (textAreaCY - 3.5).toDouble(), 0.0)
        context.matrices.scale(textScale, textScale, 1f)
        context.drawText(textRenderer, heatText, 0, 0, 0xFF88CCFF.toInt(), false)
        context.matrices.pop()

        // uptips 纹理 (4,4)
        context.drawTexture(UPTIPS_TEXTURE, left + 4, top + 4, 0f, 0f, 16, 16, 16, 16)

        drawMouseoverTooltip(context, mouseX, mouseY)

        // 悬停提示
        val relX = mouseX - left
        val relY = mouseY - top

        // uptips 悬停
        if (relX in 4 until 20 && relY in 4 until 20) {
            context.drawTooltip(
                textRenderer,
                listOf(
                    McText.translatable("gui.ic2_120.fluid_heat_exchanger.uptips"),
                    McText.literal("§7").append(McText.translatable("item.ic2_120.pulling_upgrade")),
                    McText.literal("§7").append(McText.translatable("item.ic2_120.ejector_upgrade")),
                    McText.literal("§7").append(McText.translatable("item.ic2_120.fluid_pulling_upgrade")),
                    McText.literal("§7").append(McText.translatable("item.ic2_120.fluid_ejector_upgrade"))
                ),
                mouseX, mouseY
            )
        }

        // 输入流体槽悬停
        if (relX in 19 until 31 && relY in 47 until 91) {
            val lines = if (inputMb > 0 && inputFluid != null) {
                listOf(inputFluid.defaultState.blockState.block.name, McText.literal("${"%,d".format(inputMb)} / ${"%,d".format(FluidHeatExchangerSync.TANK_CAPACITY_MB)} mB"))
            } else {
                listOf(McText.literal("空"))
            }
            context.drawTooltip(textRenderer, lines, mouseX, mouseY)
        }

        // 输出流体槽悬停
        if (relX in 145 until 157 && relY in 47 until 91) {
            val lines = if (outputMb > 0 && outputFluid != null) {
                listOf(outputFluid.defaultState.blockState.block.name, McText.literal("${"%,d".format(outputMb)} / ${"%,d".format(FluidHeatExchangerSync.TANK_CAPACITY_MB)} mB"))
            } else {
                listOf(McText.literal("空"))
            }
            context.drawTooltip(textRenderer, lines, mouseX, mouseY)
        }
    }

    /** 在指定区域自下而上渲染流体纹理填充，使用 FluidUtils 着色 */
    private fun drawFluidFill(context: DrawContext, gx: Int, gy: Int, w: Int, h: Int, fraction: Float, fluid: Fluid) {
        val fillH = (fraction.coerceIn(0f, 1f) * h).toInt()
        if (fillH <= 0) return

        val renderHandler = FluidRenderHandlerRegistry.INSTANCE.get(fluid) ?: return
        val sprites = renderHandler.getFluidSprites(null, null, fluid.defaultState) ?: return
        val sprite = sprites[0]

        val color = FluidUtils.getFluidColor(fluid)
        val (r, g, b) = if (color != -1) {
            Triple(((color shr 16) and 0xFF) / 255f, ((color shr 8) and 0xFF) / 255f, (color and 0xFF) / 255f)
        } else {
            Triple(1f, 1f, 1f)
        }

        val fillY = gy + h - fillH
        context.enableScissor(gx, fillY, gx + w, gy + h)

        for (sy in fillY until (gy + h) step 16) {
            val tileH = minOf(16, gy + h - sy)
            for (sx in gx until (gx + w) step 16) {
                val tileW = minOf(16, gx + w - sx)
                context.drawSprite(sx, sy, 0, tileW, tileH, sprite, r, g, b, 1f)
            }
        }

        context.disableScissor()
    }

    companion object {
        private val TEXTURE = Identifier("ic2", "textures/gui/guifluidheatexchanger.png")
        private val UPTIPS_TEXTURE = Identifier("ic2", "textures/gui/uptips.png")
        private const val TEXTURE_SIZE = 256
    }
}
