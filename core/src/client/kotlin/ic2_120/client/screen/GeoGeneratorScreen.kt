package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.FluidUtils
import ic2_120.client.t
import ic2_120.content.block.GeoGeneratorBlock
import ic2_120.content.screen.GeoGeneratorScreenHandler
import ic2_120.content.sync.GeoGeneratorSync
import ic2_120.registry.annotation.ModScreen
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.level.material.Fluids
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

@ModScreen(block = GeoGeneratorBlock::class)
class GeoGeneratorScreen(
    val handler: GeoGeneratorScreenHandler,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<GeoGeneratorScreenHandler>(handler, playerInventory, title) {

    init {
        imageWidth = 176
        imageHeight = 161
    }

    private val lavaSprite: TextureAtlasSprite? by lazy {
        val handler = FluidRenderHandlerRegistry.INSTANCE.get(luids.LAVA) ?: return@lazy null
        handler.getFluidSprites(null, null, Fluids.Fluids.LAVA.defaultFluidState().createLegacyBlock())?.getOrNull(0)
    }

    override fun drawBackground(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(TEXTURE, leftPos, topPos, 0f, 0f, 176, 161, 256, 256)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)

        val energy = menu.sync.energy.toLong().coerceAtLeast(0)
        val cap = GeoGeneratorSync.ENERGY_CAPACITY
        val energyFrac = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f

        val lavaMb = menu.sync.lavaAmount.coerceAtLeast(0)
        val lavaCapMb = LAVA_TANK_DROPLETS
        val lavaFrac = if (lavaCapMb > 0) (lavaMb.toFloat() / lavaCapMb).coerceIn(0f, 1f) else 0f

        val inputRate = menu.sync.getSyncedInsertedAmount()
        val outputRate = menu.sync.getSyncedExtractedAmount()

        // 能量条位于 (120, 19)-(144, 35)，纹理源 (178,2)-(202,18) = 24×16
        drawEnergyGauge(context, leftPos + ENERGY_BAR_X, topPos + ENERGY_BAR_Y, energyFrac)

        // 岩浆储量条：区域 (82,23)-(94,69) = 12×46，流体纹理+颜色渲染
        drawFluidTank(context, leftPos + LAVA_BAR_X, topPos + LAVA_BAR_Y, LAVA_BAR_W, LAVA_BAR_H, lavaFrac, lavaSprite, Fluids.LAVA)

        // 容量标示纹理 (178,21)-(190,68) = 12×47 → (82,23)，有岩浆时渲染在流体之上
        if (lavaMb > 0) {
            context.blit(TEXTURE, leftPos + 82, topPos + 23, 178f, 21f, 12, 47, 256, 256)
        }

        // 标题文字居中于 y=6
        context.drawString(font, title, leftPos + (176 - font.width(title)) / 2, topPos + 6, 0x404040, false)

        // EU发电/输出文字显示在左侧
        val generateText = t("gui.ic2_120.generate_eu", EnergyFormatUtils.formatEu(inputRate))
        val outputText = t("gui.ic2_120.output_eu", EnergyFormatUtils.formatEu(outputRate))
        val sideTextWidth = maxOf(font.width(generateText), font.width(outputText))
        val sideTextX = leftPos - sideTextWidth - 4
        context.drawString(font, generateText, sideTextX, topPos + 8, 0xAAAAAA, false)
        context.drawString(font, outputText, sideTextX, topPos + 20, 0xAAAAAA, false)

        renderTooltip(context, mouseX, mouseY)

        // 悬停提示
        val relX = mouseX - leftPos
        val relY = mouseY - topPos
        if (relX in ENERGY_BAR_X until ENERGY_BAR_X + ENERGY_BAR_W &&
            relY in ENERGY_BAR_Y until ENERGY_BAR_Y + ENERGY_BAR_H
        ) {
            context.renderComponentTooltip(font,
                listOf(Component.literal("储能：${EnergyFormatUtils.formatRaw(energy)} / ${EnergyFormatUtils.formatRaw(cap)} EU")),
                mouseX, mouseY)
        }
        if (relX in LAVA_BAR_X until LAVA_BAR_X + LAVA_BAR_W &&
            relY in LAVA_BAR_Y until LAVA_BAR_Y + LAVA_BAR_H
        ) {
            val lavaLines = if (lavaMb > 0) {
                listOf(Component.translatable("gui.ic2_120.geo_generator.lava_tooltip", "%,d".format(lavaMb / DROPLETS_PER_MB), "%,d".format(lavaCapMb / DROPLETS_PER_MB)))
            } else {
                listOf(Component.translatable("ic2.generic.text.empty"))
            }
            context.renderComponentTooltip(font, lavaLines, mouseX, mouseY)
        }
    }

    private fun drawEnergyGauge(context: GuiGraphics, gx: Int, gy: Int, fraction: Float) {
        val barW = 24
        val barH = 16
        val fillW = (fraction.coerceIn(0f, 1f) * barW).toInt()
        if (fillW <= 0) return
        context.enableScissor(gx, gy, gx + fillW, gy + barH)
        context.blit(TEXTURE, gx, gy, 178f, 2f, barW, barH, 256, 256)
        context.disableScissor()
    }

    /** 自下而上平铺流体纹理 + 流体颜色着色 */
    private fun drawFluidTank(context: GuiGraphics, gx: Int, gy: Int, w: Int, h: Int, fraction: Float, sprite: TextureAtlasSprite?, fluid: net.minecraft.world.level.material.Fluid) {
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
                    context.blit(sx, sy, 0, tileW, tileH, sprite, r, g, b, 1f)
                }
            }
        }
        context.disableScissor()
    }

    companion object {
        private val DROPLETS_PER_MB = (FluidConstants.BUCKET / 1000).toInt()
        private val LAVA_TANK_DROPLETS = (FluidConstants.BUCKET * 8).toInt()
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath("ic2", "textures/gui/guiheatsourcegenerator.png")
        private const val ENERGY_BAR_X = 120
        private const val ENERGY_BAR_Y = 19
        private const val ENERGY_BAR_W = 24
        private const val ENERGY_BAR_H = 16
        private const val LAVA_BAR_X = 82
        private const val LAVA_BAR_Y = 23
        private const val LAVA_BAR_W = 12
        private const val LAVA_BAR_H = 46
    }
}
