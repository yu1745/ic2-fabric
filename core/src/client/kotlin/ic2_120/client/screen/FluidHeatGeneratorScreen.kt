package ic2_120.client.screen

import ic2_120.client.FluidUtils
import ic2_120.content.block.FluidHeatGeneratorBlock
import ic2_120.content.fluid.ModFluids
import ic2_120.content.screen.FluidHeatGeneratorScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

@ModScreen(block = FluidHeatGeneratorBlock::class)
class FluidHeatGeneratorScreen(
    val handler: FluidHeatGeneratorScreenHandler,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<FluidHeatGeneratorScreenHandler>(handler, playerInventory, title) {

    private val fluidSprite by lazy {
        FluidRenderHandlerRegistry.INSTANCE.get(ModFluids.BIOFUEL_STILL)
            ?.getFluidSprites(null, null, ModFluids.BIOFUEL_STILL.defaultFluidState().createLegacyBlock())?.getOrNull(0)
    }

    init {
        imageWidth = 176
        imageHeight = 166
    }

    override fun drawBackground(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(TEXTURE, leftPos, topPos, 0f, 0f, 176, 166, 256, 256)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        val fuelAmount = menu.sync.fuelAmount.coerceAtLeast(0)
        val outputRate = menu.sync.getSyncedOutputHeat().coerceAtLeast(0L)

        // 标题居中于 y=6
        context.drawString(font, title, leftPos + (176 - font.width(title)) / 2, topPos + 6, 0x404040, false)

        // 流体槽：区域 (74,24)-(85,70) = 11×46
        drawFluidTank(context, leftPos + 74, topPos + 24, fuelAmount)

        // 容量标示覆盖层：纹理 (180,3)-(191,49)，有流体时才渲染
        if (fuelAmount > 0) {
            context.blit(TEXTURE, leftPos + 75, topPos + 24, 180f, 3f, 12, 46, 256, 256)
        }

        // 文本1：区域 (95,30)-(169,42)，当前输出 HU/t
        drawScaledText(context, "当前输出：${outputRate} HU/t", 95, 30, 74)

        // 文本2：区域 (95,49)-(169,61)，最大热值
        drawScaledText(context, "最大输出热值：32 HU/t", 95, 49, 74, 6f)

        // 流体槽悬停 (74,24)-(86,70) = 12×46
        if (mouseX in leftPos + 74 until leftPos + 86 && mouseY in topPos + 24 until topPos + 70) {
            val fuelLines = if (fuelAmount > 0) {
                val fuelAmountMb = fuelAmount / DROPLETS_PER_MB
                listOf(Component.translatable("gui.ic2_120.fluid_heat_generator.fuel_tooltip", "%,d".format(fuelAmountMb), "8,000"))
            } else {
                listOf(Component.translatable("ic2.generic.text.empty"))
            }
            context.renderComponentTooltip(font, fuelLines, mouseX, mouseY)
        }

        renderTooltip(context, mouseX, mouseY)
    }

    private fun drawFluidTank(context: GuiGraphics, gx: Int, gy: Int, fuelAmount: Int) {
        val tankW = 12
        val tankH = 46
        val tankCap = FluidConstants.BUCKET * 8
        val fillH = (fuelAmount.toFloat() / tankCap * tankH).toInt().coerceIn(0, tankH)
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
                context.blit(sx, sy, 0, tileW, tileH, sprite, r, g, b, 1f)
            }
        }
        context.disableScissor()
    }

    private fun drawScaledText(context: GuiGraphics, text: String, rx: Int, ry: Int, rw: Int, height: Float = 7f) {
        val scale = height / font.lineHeight
        val scaledWidth = (font.width(text) * scale).toInt()
        val scaledHeight = (font.lineHeight * scale).toInt()
        val textX = leftPos + rx + (rw - scaledWidth) / 2
        val textY = topPos + ry + (12 - scaledHeight) / 2
        context.pose().pushPose()
        context.pose().translate(textX.toDouble(), textY.toDouble(), 0.0)
        context.pose().scale(scale, scale, 1.0f)
        context.drawString(font, text, 0, 0, 0xFFADD8E6.toInt(), false)
        context.pose().popPose()
    }

    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui/guiliquidheatingmachine.png")
        private val DROPLETS_PER_MB = (FluidConstants.BUCKET / 1000).toInt()
    }
}
