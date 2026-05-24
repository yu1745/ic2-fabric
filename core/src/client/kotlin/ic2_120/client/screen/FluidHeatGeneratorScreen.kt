package ic2_120.client.screen

import ic2_120.client.FluidUtils
import ic2_120.content.block.FluidHeatGeneratorBlock
import ic2_120.content.fluid.ModFluids
import ic2_120.content.screen.FluidHeatGeneratorScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = FluidHeatGeneratorBlock::class)
class FluidHeatGeneratorScreen(
    handler: FluidHeatGeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<FluidHeatGeneratorScreenHandler>(handler, playerInventory, title) {

    private val fluidSprite by lazy {
        FluidRenderHandlerRegistry.INSTANCE.get(ModFluids.BIOFUEL_STILL)
            ?.getFluidSprites(null, null, ModFluids.BIOFUEL_STILL.defaultState)?.getOrNull(0)
    }

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

        val fuelAmountMb = handler.sync.fuelAmountMb.coerceAtLeast(0)
        val outputRate = handler.sync.getSyncedOutputHeat().coerceAtLeast(0L)

        // 标题居中于 y=6
        context.drawText(textRenderer, title, x + (176 - textRenderer.getWidth(title)) / 2, y + 6, 0x404040, false)

        // 流体槽：区域 (74,24)-(85,70) = 11×46
        drawFluidTank(context, x + 74, y + 24, fuelAmountMb)

        // 容量标示覆盖层：纹理 (180,3)-(191,49)，有流体时才渲染
        if (fuelAmountMb > 0) {
            context.drawTexture(TEXTURE, x + 75, y + 24, 180f, 3f, 12, 46, 256, 256)
        }

        // 文本1：区域 (95,30)-(169,42)，当前输出 HU/t
        drawScaledText(context, "当前输出：${outputRate} HU/t", 95, 30, 74)

        // 文本2：区域 (95,49)-(169,61)，最大热值
        drawScaledText(context, "最大输出热值：32 HU/t", 95, 49, 74, 6f)

        // 流体槽悬停 (74,24)-(86,70) = 12×46
        if (fuelAmountMb > 0 && mouseX in x + 74 until x + 86 && mouseY in y + 24 until y + 70) {
            context.drawTooltip(
                textRenderer,
                Text.literal("生物燃料：${fuelAmountMb} / 8000 mB"),
                mouseX, mouseY
            )
        }

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun drawFluidTank(context: DrawContext, gx: Int, gy: Int, fuelAmountMb: Int) {
        val tankW = 12
        val tankH = 46
        val tankCapMb = 8000
        val fillH = (fuelAmountMb.toFloat() / tankCapMb * tankH).toInt().coerceIn(0, tankH)
        if (fillH <= 0) return

        val fillY = gy + tankH - fillH
        val sprite = fluidSprite ?: return
        val color = FluidUtils.getFluidColor(ModFluids.BIOFUEL_STILL)
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        context.enableScissor(gx, fillY, gx + tankW, gy + tankH)
        for (sy in fillY until (gy + tankH) step 16) {
            val tileH = minOf(16, gy + tankH - sy)
            for (sx in gx until (gx + tankW) step 16) {
                val tileW = minOf(16, gx + tankW - sx)
                context.drawSprite(sx, sy, 0, tileW, tileH, sprite, r, g, b, 1f)
            }
        }
        context.disableScissor()
    }

    private fun drawScaledText(context: DrawContext, text: String, rx: Int, ry: Int, rw: Int, height: Float = 7f) {
        val scale = height / textRenderer.fontHeight
        val scaledWidth = (textRenderer.getWidth(text) * scale).toInt()
        val scaledHeight = (textRenderer.fontHeight * scale).toInt()
        val textX = x + rx + (rw - scaledWidth) / 2
        val textY = y + ry + (12 - scaledHeight) / 2
        context.matrices.push()
        context.matrices.translate(textX.toDouble(), textY.toDouble(), 0.0)
        context.matrices.scale(scale, scale, 1.0f)
        context.drawText(textRenderer, text, 0, 0, 0xFFADD8E6.toInt(), false)
        context.matrices.pop()
    }

    companion object {
        private val TEXTURE = Identifier.of("ic2", "textures/gui/guiliquidheatingmachine.png")
    }
}
