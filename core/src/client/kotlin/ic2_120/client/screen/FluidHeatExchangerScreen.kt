package ic2_120.client.screen

import ic2_120.client.FluidUtils
import ic2_120.client.t
import ic2_120.content.block.FluidHeatExchangerBlock
import ic2_120.content.screen.FluidHeatExchangerScreenHandler
import ic2_120.content.sync.FluidHeatExchangerSync
import ic2_120.registry.annotation.ModScreen
import ic2_120.client.compat.FluidRenderHandlerRegistry
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import ic2_120.client.compat.*
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.level.material.Fluid
import net.minecraft.network.chat.Component as McText
import net.minecraft.resources.Identifier

@ModScreen(block = FluidHeatExchangerBlock::class)
class FluidHeatExchangerScreen(
    val handler: FluidHeatExchangerScreenHandler, playerInventory: Inventory, title: McText
) : LegacyContainerScreen26<FluidHeatExchangerScreenHandler>(handler, playerInventory, title, 179, 204) {

    init {
    }

    override fun renderBg(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(TEXTURE, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, TEXTURE_SIZE, TEXTURE_SIZE)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)

        val left = leftPos
        val top = topPos
        val sync = menu.sync

        val inputMb = sync.inputFluid.coerceAtLeast(0)
        val outputMb = sync.outputFluid.coerceAtLeast(0)
        val inputFrac = if (TANK_DROPLETS > 0) (inputMb.toFloat() / TANK_DROPLETS).coerceIn(0f, 1f) else 0f
        val outputFrac = if (TANK_DROPLETS > 0) (outputMb.toFloat() / TANK_DROPLETS).coerceIn(0f, 1f) else 0f

        val generatedRate = sync.getSyncedGeneratedHeat()
        val outputRate = sync.getSyncedOutputHeat()

        // 标题居中
        context.drawString(font, title, left + (imageWidth - font.width(title)) / 2, top + 6, 0x404040, false)

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
        val textScale = 7f / font.lineHeight
        val scaledW = (font.width(heatText) * textScale).toInt()
        val textAreaW = 159 - 18
        val textAreaY = top + 29
        val textAreaCY = textAreaY + (40 - 29) / 2
        val scaledTextX = left + 18 + (textAreaW - scaledW) / 2
        context.pose().pushPose()
        context.pose().translate(scaledTextX.toDouble(), textAreaCY - 3.5, 0.0)
        context.pose().scale(textScale, textScale, 1f)
        context.drawString(font, heatText, 0, 0, 0xFF88CCFF.toInt(), false)
        context.pose().popPose()

        // uptips 纹理 (4,4)
        context.blit(UPTIPS_TEXTURE, left + 4, top + 4, 0f, 0f, 16, 16, 16, 16)

        renderTooltip(context, mouseX, mouseY)

        // 悬停提示
        val relX = mouseX - left
        val relY = mouseY - top

        // uptips 悬停
        if (relX in 4 until 20 && relY in 4 until 20) {
            context.renderComponentTooltip(
                font,
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
                listOf(inputFluid.defaultFluidState().createLegacyBlock().block.name, McText.literal("${"%,d".format(inputMb / DROPLETS_PER_MB)} / ${"%,d".format(TANK_DROPLETS / DROPLETS_PER_MB)} mB"))
            } else {
                listOf(McText.literal("空"))
            }
            context.renderComponentTooltip(font, lines, mouseX, mouseY)
        }

        // 输出流体槽悬停
        if (relX in 145 until 157 && relY in 47 until 91) {
            val lines = if (outputMb > 0 && outputFluid != null) {
                listOf(outputFluid.defaultFluidState().createLegacyBlock().block.name, McText.literal("${"%,d".format(outputMb / DROPLETS_PER_MB)} / ${"%,d".format(TANK_DROPLETS / DROPLETS_PER_MB)} mB"))
            } else {
                listOf(McText.literal("空"))
            }
            context.renderComponentTooltip(font, lines, mouseX, mouseY)
        }
    }

    /** 在指定区域自下而上渲染流体纹理填充，使用 FluidUtils 着色 */
    private fun drawFluidFill(context: GuiGraphics, gx: Int, gy: Int, w: Int, h: Int, fraction: Float, fluid: Fluid) {
        val fillH = (fraction.coerceIn(0f, 1f) * h).toInt()
        if (fillH <= 0) return

        val renderHandler = FluidRenderHandlerRegistry.INSTANCE.get(fluid) ?: return
        val sprites = renderHandler.getFluidSprites(null, null, fluid.defaultFluidState()) ?: return
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
                context.blit(sx, sy, 0, tileW, tileH, sprite, r, g, b, 1f)
            }
        }

        context.disableScissor()
    }

    companion object {
        private val TEXTURE = Identifier.fromNamespaceAndPath("ic2", "textures/gui/guifluidheatexchanger.png")
        private val UPTIPS_TEXTURE = Identifier.fromNamespaceAndPath("ic2", "textures/gui/uptips.png")
        private const val TEXTURE_SIZE = 256
        private val DROPLETS_PER_MB = (FluidConstants.BUCKET / 1000).toInt()
        private val TANK_DROPLETS = (FluidConstants.BUCKET * FluidHeatExchangerSync.TANK_CAPACITY_BUCKETS).toInt()
    }
}
